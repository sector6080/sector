package com.sector6080.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sector6080.app.model.Order
import com.sector6080.app.ui.components.CommandBar
import com.sector6080.app.ui.components.CommandHeader
import com.sector6080.app.ui.components.CommandTab
import com.sector6080.app.ui.components.Glyphs
import com.sector6080.app.ui.components.Logo
import com.sector6080.app.ui.components.tacticalBackdrop
import com.sector6080.app.ui.screens.ChatScreen
import com.sector6080.app.ui.screens.GmScreen
import com.sector6080.app.ui.screens.MapScreen
import com.sector6080.app.ui.screens.NotificationsDialog
import com.sector6080.app.ui.screens.OrdersScreen
import com.sector6080.app.ui.screens.TeamScreen
import com.sector6080.app.ui.screens.UnitScreen
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Ink
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Void
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

const val RULES_URL =
    "https://docs.google.com/document/d/1Sfr3UhyqTXyWu_aW1eq-qaq62DXUypjbo3hwyRBSUkY/edit?usp=sharing"

private enum class Tab(val label: String, val glyph: DrawScope.(Color) -> Unit) {
    UNIT("Unità", Glyphs.unit),
    ORDERS("Ordini", Glyphs.orders),
    MAP("Mappa", Glyphs.map),
    TEAM("Squadra", Glyphs.team),
    CHAT("Chat", Glyphs.chat),
    GM("GM", Glyphs.command),
}

@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(Tab.UNIT) }
    // Il Comando Supremo non ha unità: al posto di Unità e Ordini ha la scheda GM
    val tabs = Tab.entries.filter { if (state.isGm) it != Tab.UNIT && it != Tab.ORDERS else it != Tab.GM }
    val tab = selected.takeIf { it in tabs } ?: if (state.isGm) Tab.GM else Tab.UNIT
    // Ordine in attesa della casella da scegliere sulla mappa
    var picking by remember { mutableStateOf<Order?>(null) }
    val notice by vm.notice.collectAsStateWithLifecycle()

    var showNotifications by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val sounds = remember { SoundBoard(context.applicationContext) }
    // Il ruolo cambia le schede: il pager riparte da quella giusta, senza scorrere le altre
    val pager = key(state.isGm) { rememberPagerState(initialPage = tabs.indexOf(tab).coerceAtLeast(0)) { tabs.size } }

    DisposableEffect(sounds) { onDispose { sounds.release() } }
    SideEffect { sounds.enabled = state.soundOn }
    LaunchedEffect(notice) { if (notice != null) sounds.play(Sfx.ERROR) }

    // Scheda scelta dalla barra o da un ordine: il pager ci scorre
    LaunchedEffect(tab, tabs) {
        val page = tabs.indexOf(tab)
        if (page != pager.currentPage) pager.animateScrollToPage(page)
    }

    // Scheda raggiunta con lo swipe
    LaunchedEffect(pager, tabs) {
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            val reached = tabs.getOrNull(page)
            if (reached != null && reached != selected) {
                picking = null
                selected = reached
                sounds.play(Sfx.TAB)
            }
        }
    }

    // Ogni cambio di scheda riallinea i dati con il server
    LaunchedEffect(tab, loaded) { vm.sync() }

    // Solo con l'app aperta: a schermo spento le notifiche passano da AlertsWorker
    LaunchedEffect(lifecycle, tab) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.sync()
            while (true) {
                delay(if (tab == Tab.CHAT) CHAT_REFRESH_MILLIS else REFRESH_MILLIS)
                vm.sync()
            }
        }
    }

    LaunchedEffect(state.access) {
        val needed = state.access != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needed) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    CompositionLocalProvider(LocalSounds provides sounds) {
        Box(Modifier.fillMaxSize().tacticalBackdrop()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = Ink,
                topBar = {
                    Column {
                        CommandHeader(
                            section = tab.label,
                            rank = state.rank.takeIf { state.unit != null || state.isGm },
                            status = "${state.pu} PU".takeIf { state.unit != null },
                            unread = state.notifications.count { !it.read },
                            onBell = { showNotifications = true }.takeIf { state.access != null },
                        )
                        notice?.let {
                            Text(
                                it,
                                color = Void,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().background(Danger).padding(10.dp),
                            )
                        }
                    }
                },
                bottomBar = {
                    CommandBar {
                        tabs.forEach { t ->
                            CommandTab(t.label, tab == t, t.glyph, onClick = { picking = null; selected = t })
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                    if (!loaded) {
                        BootScreen()
                        return@Box
                    }
                    // Sulla mappa il trascinamento sposta la mappa: si cambia scheda dalla barra
                    HorizontalPager(
                        state = pager,
                        userScrollEnabled = tab != Tab.MAP,
                        key = { tabs[it].name },
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (tabs[page]) {
                            Tab.UNIT -> UnitScreen(vm, state)
                            Tab.ORDERS -> OrdersScreen(vm, state, onPickOnMap = { picking = it; selected = Tab.MAP })
                            Tab.MAP -> MapScreen(
                                vm, state, picking,
                                onConfirm = { vm.addOrder(it); picking = null; selected = Tab.ORDERS },
                                onCancel = { picking = null; selected = Tab.ORDERS },
                            )
                            Tab.TEAM -> TeamScreen(vm, state)
                            Tab.CHAT -> ChatScreen(vm, state)
                            Tab.GM -> GmScreen(vm, state)
                        }
                    }
                }
            }
        }

        if (showNotifications) NotificationsDialog(vm, state, onDismiss = { showNotifications = false })
    }
}

@Composable
private fun BootScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(size = 96.dp)
        CircularProgressIndicator()
        Text("COLLEGAMENTO AL COMANDO", style = MaterialTheme.typography.labelMedium, color = InkDim)
    }
}

private const val REFRESH_MILLIS = 30_000L
private const val CHAT_REFRESH_MILLIS = 10_000L

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
