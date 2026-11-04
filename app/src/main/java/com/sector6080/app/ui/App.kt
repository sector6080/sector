package com.sector6080.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.sector6080.app.ui.screens.OrdersScreen
import com.sector6080.app.ui.screens.TeamScreen
import com.sector6080.app.ui.screens.UnitScreen
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Ink
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Void

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
    var tab by rememberSaveable { mutableStateOf(Tab.UNIT) }
    // Ordine in attesa della casella da scegliere sulla mappa
    var picking by remember { mutableStateOf<Order?>(null) }
    val notice by vm.notice.collectAsStateWithLifecycle()

    // Ogni cambio di scheda riallinea i dati con il server
    LaunchedEffect(tab, loaded) { vm.sync() }

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
                    // La scheda GM esiste solo per chi ha il ruolo sul server
                    Tab.entries.filter { it != Tab.GM || state.isGm }.forEach { t ->
                        CommandTab(t.label, tab == t, t.glyph, onClick = { picking = null; tab = t })
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                if (!loaded) {
                    BootScreen()
                    return@Box
                }
                Crossfade(tab, animationSpec = tween(160), label = "scheda") { current ->
                    when (current) {
                        Tab.UNIT -> UnitScreen(vm, state)
                        Tab.ORDERS -> OrdersScreen(vm, state, onPickOnMap = { picking = it; tab = Tab.MAP })
                        Tab.MAP -> MapScreen(
                            vm, state, picking,
                            onConfirm = { vm.addOrder(it); picking = null; tab = Tab.ORDERS },
                            onCancel = { picking = null; tab = Tab.ORDERS },
                        )
                        Tab.TEAM -> TeamScreen(vm, state)
                        Tab.CHAT -> ChatScreen(vm, state)
                        Tab.GM -> if (state.isGm) GmScreen(vm, state) else UnitScreen(vm, state)
                    }
                }
            }
        }
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

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
