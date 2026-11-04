package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GmUnit
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Danger

/** Visibile solo ai GM. Ogni azione viene comunque controllata dal server. */
@Composable
fun GmScreen(vm: GameViewModel, state: GameState) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirmLost by remember { mutableStateOf<String?>(null) }
    var confirmTurn by remember { mutableStateOf(false) }
    val pending = state.gmUnits.filter { it.pending }
    val active = state.gmUnits.filter { !it.pending }

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.padding(top = 8.dp))
            Text("Game Master", style = MaterialTheme.typography.headlineSmall)
            Text("Turno ${state.turn}")
            OutlinedButton(onClick = { confirmTurn = true }, enabled = !busy) {
                Text("Passa subito al turno successivo")
            }
        }

        item { Title("Unità in attesa (${pending.size})") }
        items(pending, key = { "p-" + it.member.unit.matricola }) { gm ->
            val unit = gm.member.unit
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${unit.matricola} · ${unit.type}", fontWeight = FontWeight.SemiBold)
                    Text("Costo ${gm.cost} PU")
                    Text(
                        "Componenti: " + unit.components.ifEmpty { listOf("nessuno") }.joinToString(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Lore: ${unit.lore}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.gmReview(unit.matricola, true) }, enabled = !busy) { Text("Approva") }
                        OutlinedButton(onClick = { vm.gmReview(unit.matricola, false) }, enabled = !busy) {
                            Text("Rifiuta e rimborsa")
                        }
                    }
                }
            }
        }

        item { Title("Ordini da applicare (${state.gmOrders.size})") }
        items(state.gmOrders, key = { "o-${it.matricola}-${it.turn}" }) { orders ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${orders.matricola} · turno ${orders.turn}", fontWeight = FontWeight.SemiBold)
                    orders.orders.forEach { Text("• " + OrderRules.describe(it), style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = { vm.gmApplyOrders(orders) }, enabled = !busy) { Text("Applica") }
                }
            }
        }

        item { Title("Unità attive (${active.size})") }
        items(active, key = { "a-" + it.member.unit.matricola }) { gm ->
            ActiveUnitCard(vm, gm, busy) { confirmLost = gm.member.unit.matricola }
        }
    }

    confirmLost?.let { matricola ->
        AlertDialog(
            onDismissRequest = { confirmLost = null },
            title = { Text("Segnare $matricola come persa?") },
            text = { Text("Il player tiene i PU, ma perde l'unità, i componenti e il grado: torna Recluta.") },
            confirmButton = {
                TextButton(onClick = { vm.gmMarkLost(matricola); confirmLost = null }) {
                    Text("Segna persa", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLost = null }) { Text("Annulla") } },
        )
    }
    if (confirmTurn) {
        AlertDialog(
            onDismissRequest = { confirmTurn = false },
            title = { Text("Passare al turno ${state.turn + 1}?") },
            text = { Text("Il timer riparte e vengono pagati gli stipendi.") },
            confirmButton = {
                TextButton(onClick = { vm.gmNextTurn(); confirmTurn = false }) { Text("Conferma") }
            },
            dismissButton = { TextButton(onClick = { confirmTurn = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ActiveUnitCard(vm: GameViewModel, gm: GmUnit, busy: Boolean, onLost: () -> Unit) {
    val unit = gm.member.unit
    val max = UnitRules.maxStats(vm.catalog, unit)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${unit.matricola} · ${unit.type}", fontWeight = FontWeight.SemiBold)
            Text("${gm.member.rank} · ${gm.member.pu} PU · casella ${unit.position}")
            Adjuster("HP ${unit.hp}/${max.hp}", busy) { vm.gmChangeUnit(unit.matricola, unit.hp + it, unit.supplies) }
            Adjuster("Rifornimenti ${unit.supplies}/${max.supplies}", busy) {
                vm.gmChangeUnit(unit.matricola, unit.hp, unit.supplies + it)
            }
            TextButton(onClick = onLost, enabled = !busy) { Text("Segna come persa", color = Danger) }
        }
    }
}

@Composable
private fun Adjuster(label: String, busy: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onChange(-1) }, enabled = !busy) { Text("−") }
        OutlinedButton(onClick = { onChange(1) }, enabled = !busy) { Text("+") }
        Text(label)
    }
}
