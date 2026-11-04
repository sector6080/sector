package com.sector6080.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sector6080.app.model.Order
import com.sector6080.app.ui.screens.ChatScreen
import com.sector6080.app.ui.screens.LinksScreen
import com.sector6080.app.ui.screens.MapScreen
import com.sector6080.app.ui.screens.OrdersScreen
import com.sector6080.app.ui.screens.UnitScreen

const val RULES_URL =
    "https://docs.google.com/document/d/1Sfr3UhyqTXyWu_aW1eq-qaq62DXUypjbo3hwyRBSUkY/edit?usp=sharing"

/** Invito al server Discord (facoltativo). */
const val DISCORD_URL = "https://discord.gg/Ah3TTPEcsR"

private enum class Tab(val label: String, val icon: ImageVector) {
    UNIT("Unità", Icons.Filled.Person),
    ORDERS("Ordini", Icons.AutoMirrored.Filled.List),
    MAP("Mappa", Icons.Filled.LocationOn),
    CHAT("Chat", Icons.Filled.Email),
    LINKS("Link", Icons.Filled.Info),
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

    Scaffold(
        topBar = {
            notice?.let {
                Surface(color = MaterialTheme.colorScheme.error, contentColor = Color.White) {
                    Text(it, Modifier.fillMaxWidth().statusBarsPadding().padding(10.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { picking = null; tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (!loaded) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            when (tab) {
                Tab.UNIT -> UnitScreen(vm, state)
                Tab.ORDERS -> OrdersScreen(vm, state, onPickOnMap = { picking = it; tab = Tab.MAP })
                Tab.MAP -> MapScreen(
                    vm, state, picking,
                    onConfirm = { vm.addOrder(it); picking = null; tab = Tab.ORDERS },
                    onCancel = { picking = null; tab = Tab.ORDERS },
                )
                Tab.CHAT -> ChatScreen(vm, state)
                Tab.LINKS -> LinksScreen()
            }
        }
    }
}

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
