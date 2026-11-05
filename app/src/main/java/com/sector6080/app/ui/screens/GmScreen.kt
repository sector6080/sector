package com.sector6080.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.sector6080.app.model.EnemyTemplate
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.GmPlayer
import com.sector6080.app.model.GmUnit
import com.sector6080.app.model.RANK_SUPREME
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.Card
import com.sector6080.app.ui.components.FilterChip
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.Panel
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Signal

/** Nemico ancora da creare: l'id viene dal server. */
private val NEW_ENEMY = Enemy(0, "", 0, 0, "", Cell(1, 1))

/** Sezioni a tendina: con tanti ordini, nemici o unità si apre solo quella che serve. */
private enum class GmSection(val label: String) {
    PENDING("Unità in attesa"),
    ORDERS("Ordini del turno"),
    ENEMIES("Nemici"),
    UNITS("Unità attive"),
    PLAYERS("Player"),
}

/** Visibile solo ai GM. Ogni azione viene comunque controllata dal server. */
@Composable
fun GmScreen(vm: GameViewModel, state: GameState) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirmLost by remember { mutableStateOf<String?>(null) }
    var confirmTurn by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<GmPlayer?>(null) }
    var moving by remember { mutableStateOf<GameUnit?>(null) }
    var equipping by remember { mutableStateOf<String?>(null) }
    var enemyForm by remember { mutableStateOf<Enemy?>(null) }
    var open by rememberSaveable { mutableStateOf(setOf(GmSection.PENDING.name)) }
    val pending = state.gmUnits.filter { it.pending }
    val active = state.gmUnits.filter { !it.pending }

    fun LazyListScope.section(section: GmSection, count: Int, content: LazyListScope.() -> Unit) {
        val isOpen = section.name in open
        item(key = section.name) {
            SectionHeader(section.label, count, isOpen) {
                open = if (isOpen) open - section.name else open + section.name
            }
        }
        if (isOpen) {
            content()
            if (count == 0) item(key = section.name + " vuota") { Text("Nessuno.", color = InkDim) }
        }
    }

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

        section(GmSection.PENDING, pending.size) {
            items(pending, key = { "p-" + it.member.unit.matricola }) { gm ->
                val unit = gm.member.unit
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${unit.matricola} · ${unit.type}", fontWeight = FontWeight.SemiBold)
                        Text("Costo ${vm.catalog.type(unit.type)?.creationCost ?: 0} PU, pagato all'approvazione")
                        Text("Lore: ${unit.lore}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.gmReview(unit.matricola, true) }, enabled = !busy) { Text("Approva") }
                            OutlinedButton(onClick = { vm.gmReview(unit.matricola, false) }, enabled = !busy) {
                                Text("Rifiuta")
                            }
                        }
                    }
                }
            }
        }

        section(GmSection.ORDERS, state.gmOrders.size) {
            items(state.gmOrders, key = { "o-${it.matricola}-${it.turn}" }) { orders ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${orders.matricola} · turno ${orders.turn}", fontWeight = FontWeight.SemiBold)
                        orders.orders.forEach { Text("• " + OrderRules.describe(it), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        section(GmSection.ENEMIES, state.enemies.size) {
            item {
                OutlinedButton(onClick = { enemyForm = NEW_ENEMY }, enabled = !busy) { Text("Nuovo nemico") }
            }
            items(state.enemies, key = { "e-${it.id}" }) { enemy ->
                EnemyCard(vm, enemy, busy, onEdit = { enemyForm = enemy })
            }
        }

        section(GmSection.UNITS, active.size) {
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

        section(GmSection.PLAYERS, state.gmPlayers.size) {
            items(state.gmPlayers, key = { "g-" + it.id }) { player ->
                val deployed = active.any { it.member.unit.matricola == player.matricola }
                PlayerCard(vm, player, deployed, busy, onDelete = { deleting = player })
            }
        }

        item { Spacer(Modifier.padding(bottom = 8.dp)) }
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
    deleting?.let { player ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Cancellare ${player.matricola ?: "questo player"}?") },
            text = {
                Text("Spariscono per sempre l'account, l'unità, i PU, gli ordini e le notifiche. Non si può annullare.")
            },
            confirmButton = {
                TextButton(onClick = { vm.gmDeletePlayer(player.id); deleting = null }) {
                    Text("Cancella", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annulla") } },
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
            state.enemyTemplates,
            onSave = { vm.gmSaveEnemy(it); enemyForm = null },
            onDismiss = { enemyForm = null },
        )
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$count", style = MaterialTheme.typography.labelMedium, color = Signal)
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "Chiudi" else "Apri",
            )
        }
    }
}

/** PU, grado e silenzio. Il grado Comando Supremo si imposta solo dalla console. */
@Composable
private fun PlayerCard(vm: GameViewModel, player: GmPlayer, deployed: Boolean, busy: Boolean, onDelete: () -> Unit) {
    val supreme = player.rank == RANK_SUPREME
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                player.matricola ?: if (supreme) RANK_SUPREME else "Senza unità",
                fontWeight = FontWeight.SemiBold,
            )
            val details = listOfNotNull(
                player.rank,
                "silenziato".takeIf { player.muted },
                "codice ${player.id.take(6)}".takeIf { player.matricola == null },
            )
            Text(details.joinToString(" · "))
            Adjuster("${player.pu} PU", busy) { vm.gmChangePu(player.id, it) }
            if (!supreme) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    player.matricola?.takeIf { deployed }?.let { matricola ->
                        HierarchyRules.nextRanks(player.rank).forEach { rank ->
                            TextButton(onClick = { vm.gmSetRank(matricola, rank) }, enabled = !busy) {
                                Text("Rendi $rank")
                            }
                        }
                    }
                    TextButton(onClick = { vm.gmMutePlayer(player.id, !player.muted) }, enabled = !busy) {
                        Text(if (player.muted) "Ridai la parola" else "Silenzia")
                    }
                }
                TextButton(onClick = onDelete, enabled = !busy) { Text("Cancella player", color = Danger) }
            }
        }
    }
}

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
                "armatura ${max.armor}",
                gm.member.squadLabel,
                "silenziata".takeIf { gm.muted },
            )
            Text(details.joinToString(" · "))
            Adjuster("HP ${unit.hp}/${max.hp}", busy) { vm.gmChangeUnit(unit.matricola, unit.hp + it, unit.supplies) }
            Adjuster("Rifornimenti ${unit.supplies}/${max.supplies}", busy) {
                vm.gmChangeUnit(unit.matricola, unit.hp, unit.supplies + it)
            }
            Row {
                TextButton(onClick = onMove, enabled = !busy) { Text("Sposta") }
                TextButton(onClick = onEquip, enabled = !busy) { Text("Componenti") }
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
private fun EnemyDialog(
    enemy: Enemy,
    templates: List<EnemyTemplate>,
    onSave: (Enemy) -> Unit,
    onDismiss: () -> Unit,
) {
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
                // Il template riempie i campi: il GM può ancora ritoccarli prima di salvare
                if (isNew && templates.isEmpty()) {
                    Text("Nessun template: si aggiungono nella tabella enemy_templates.", style = MaterialTheme.typography.bodySmall)
                }
                if (isNew) {
                    templates.forEach { template ->
                        FilterChip(
                            selected = name == template.name,
                            onClick = {
                                name = template.name
                                hp = template.hp.toString()
                                armor = template.armor.toString()
                                description = template.description
                            },
                            label = { Text("${template.name} · HP ${template.hp} · Arm ${template.armor}") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
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
