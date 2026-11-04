package com.sector6080.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Enemy
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.GmUnit
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.Card
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.theme.Danger

/** Nemico ancora da creare: l'id viene dal server. */
private val NEW_ENEMY = Enemy(0, "", 0, 0, "", Cell(1, 1))

/** Visibile solo ai GM. Ogni azione viene comunque controllata dal server. */
@Composable
fun GmScreen(vm: GameViewModel, state: GameState) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirmLost by remember { mutableStateOf<String?>(null) }
    var confirmTurn by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf<GameUnit?>(null) }
    var equipping by remember { mutableStateOf<String?>(null) }
    var enemyForm by remember { mutableStateOf<Enemy?>(null) }
    val pending = state.gmUnits.filter { it.pending }
    val active = state.gmUnits.filter { !it.pending }

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.padding(top = 4.dp))
            Text("Turno ${state.turn}", style = MaterialTheme.typography.headlineSmall)
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

        item {
            Title("Nemici (${state.enemies.size})")
            OutlinedButton(onClick = { enemyForm = NEW_ENEMY }, enabled = !busy) { Text("Nuovo nemico") }
        }
        items(state.enemies, key = { "e-${it.id}" }) { enemy ->
            EnemyCard(vm, enemy, busy, onEdit = { enemyForm = enemy })
        }

        item { Title("Unità attive (${active.size})") }
        items(active, key = { "a-" + it.member.unit.matricola }) { gm ->
            val unit = gm.member.unit
            ActiveUnitCard(
                vm, gm, busy,
                onMove = { moving = unit },
                onEquip = { equipping = unit.matricola },
                onLost = { confirmLost = unit.matricola },
            )
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
    moving?.let { unit ->
        MoveDialog(
            unit,
            onMove = { vm.gmMoveUnit(unit.matricola, it); moving = null },
            onDismiss = { moving = null },
        )
    }
    // L'unità si rilegge dal server, così l'elenco cambia dopo ogni modifica
    state.gmUnits.firstOrNull { it.member.unit.matricola == equipping }?.let { gm ->
        ComponentsDialog(vm, gm.member.unit, busy, onDismiss = { equipping = null })
    }
    enemyForm?.let { enemy ->
        EnemyDialog(
            enemy,
            onSave = { vm.gmSaveEnemy(it); enemyForm = null },
            onDismiss = { enemyForm = null },
        )
    }
}

@Composable
private fun Title(text: String) = SectionLabel(text)

@Composable
private fun ActiveUnitCard(
    vm: GameViewModel,
    gm: GmUnit,
    busy: Boolean,
    onMove: () -> Unit,
    onEquip: () -> Unit,
    onLost: () -> Unit,
) {
    val unit = gm.member.unit
    val max = UnitRules.maxStats(vm.catalog, unit)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${unit.matricola} · ${unit.type}", fontWeight = FontWeight.SemiBold)
            val details = listOfNotNull(
                gm.member.rank,
                "${gm.member.pu} PU",
                "casella ${unit.position}",
                gm.member.squadLabel,
                "silenziata".takeIf { gm.muted },
            )
            Text(details.joinToString(" · "))
            Adjuster("HP ${unit.hp}/${max.hp}", busy) { vm.gmChangeUnit(unit.matricola, unit.hp + it, unit.supplies) }
            Adjuster("Rifornimenti ${unit.supplies}/${max.supplies}", busy) {
                vm.gmChangeUnit(unit.matricola, unit.hp, unit.supplies + it)
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                HierarchyRules.nextRanks(gm.member.rank).forEach { rank ->
                    TextButton(onClick = { vm.gmSetRank(unit.matricola, rank) }, enabled = !busy) {
                        Text("Rendi $rank")
                    }
                }
                TextButton(onClick = onMove, enabled = !busy) { Text("Sposta") }
                TextButton(onClick = onEquip, enabled = !busy) { Text("Componenti") }
                TextButton(onClick = { vm.gmSetMuted(unit.matricola, !gm.muted) }, enabled = !busy) {
                    Text(if (gm.muted) "Ridai la parola" else "Silenzia")
                }
            }
            TextButton(onClick = onLost, enabled = !busy) { Text("Segna come persa", color = Danger) }
        }
    }
}

@Composable
private fun EnemyCard(vm: GameViewModel, enemy: Enemy, busy: Boolean, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(enemy.name, fontWeight = FontWeight.SemiBold)
            Text("Armatura ${enemy.armor} · casella ${enemy.position}")
            if (enemy.description.isNotEmpty()) {
                Text(enemy.description, style = MaterialTheme.typography.bodySmall)
            }
            // A 0 HP il server toglie il nemico dalla mappa
            Adjuster("HP ${enemy.hp}", busy) { vm.gmSaveEnemy(enemy.copy(hp = enemy.hp + it)) }
            Row {
                TextButton(onClick = onEdit, enabled = !busy) { Text("Modifica") }
                TextButton(onClick = { vm.gmDeleteEnemy(enemy.id) }, enabled = !busy) {
                    Text("Elimina", color = Danger)
                }
            }
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

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** Le coordinate si leggono dalla mappa, toccando la casella. */
@Composable
private fun CellFields(x: String, y: String, onX: (String) -> Unit, onY: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("X (1-80)", x, Modifier.weight(1f), onX)
        NumberField("Y (1-60)", y, Modifier.weight(1f), onY)
    }
}

private fun parseCell(x: String, y: String): Cell? {
    val cell = Cell(x.toIntOrNull() ?: return null, y.toIntOrNull() ?: return null)
    return cell.takeIf { it.inBounds }
}

@Composable
private fun MoveDialog(unit: GameUnit, onMove: (Cell) -> Unit, onDismiss: () -> Unit) {
    var x by rememberSaveable { mutableStateOf(unit.position?.x?.toString().orEmpty()) }
    var y by rememberSaveable { mutableStateOf(unit.position?.y?.toString().orEmpty()) }
    val cell = parseCell(x, y)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spostare ${unit.matricola}") },
        text = { CellFields(x, y, { x = it }, { y = it }) },
        confirmButton = {
            TextButton(onClick = { cell?.let(onMove) }, enabled = cell != null) { Text("Sposta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Il GM aggiunge e toglie componenti senza spendere PU. */
@Composable
private fun ComponentsDialog(vm: GameViewModel, unit: GameUnit, busy: Boolean, onDismiss: () -> Unit) {
    val components = vm.catalog.type(unit.type)?.components.orEmpty()
        .sortedByDescending { it.name in unit.components }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Componenti di ${unit.matricola}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                components.forEach { c ->
                    val owned = c.name in unit.components
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.name,
                            fontWeight = if (owned) FontWeight.SemiBold else null,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.gmSetComponent(unit.matricola, c.name, !owned) }, enabled = !busy) {
                            Text(if (owned) "Togli" else "Aggiungi")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

@Composable
private fun EnemyDialog(enemy: Enemy, onSave: (Enemy) -> Unit, onDismiss: () -> Unit) {
    val isNew = enemy == NEW_ENEMY
    var name by rememberSaveable { mutableStateOf(enemy.name) }
    var hp by rememberSaveable { mutableStateOf(if (isNew) "" else enemy.hp.toString()) }
    var armor by rememberSaveable { mutableStateOf(enemy.armor.toString()) }
    var description by rememberSaveable { mutableStateOf(enemy.description) }
    var x by rememberSaveable { mutableStateOf(if (isNew) "" else enemy.position.x.toString()) }
    var y by rememberSaveable { mutableStateOf(if (isNew) "" else enemy.position.y.toString()) }
    val cell = parseCell(x, y)
    val hpValue = hp.toIntOrNull()?.takeIf { it > 0 }
    val armorValue = armor.toIntOrNull()
    val ready = name.isNotBlank() && hpValue != null && armorValue != null && cell != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuovo nemico" else "Modifica ${enemy.name}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nome") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("HP", hp, Modifier.weight(1f)) { hp = it }
                    NumberField("Armatura", armor, Modifier.weight(1f)) { armor = it }
                }
                CellFields(x, y, { x = it }, { y = it })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    label = { Text("Descrizione") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (hpValue != null && armorValue != null && cell != null) {
                        onSave(enemy.copy(name = name, hp = hpValue, armor = armorValue, description = description, position = cell))
                    }
                },
                enabled = ready,
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
