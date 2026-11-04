package com.sector6080.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.Access
import com.sector6080.app.model.Component
import com.sector6080.app.model.GOOGLE_BONUS_PU
import com.sector6080.app.model.GameState
import com.sector6080.app.model.Stats
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.RULES_URL
import com.sector6080.app.ui.openUrl
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Success
import com.sector6080.app.ui.theme.Warning

@Composable
fun UnitScreen(vm: GameViewModel, state: GameState) {
    when {
        state.unit != null -> UnitDetails(vm, state)
        state.access == null -> AccessGate(vm)
        !state.rulesRead -> RulesGate(vm)
        else -> CreateUnitForm(vm, state)
    }
}

private const val GOOGLE_SOON = "L'accesso con Google arriverà presto."
private const val LOSS_WARNING =
    "Senza accesso con Google, se disinstalli l'app o cambi telefono perdi la tua unità."

@Composable
private fun AccessGate(vm: GameViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Come vuoi entrare?", style = MaterialTheme.typography.headlineSmall)
        Text("Con Google ricevi +$GOOGLE_BONUS_PU PU e la tua unità resta al sicuro.")
        Text("$LOSS_WARNING Puoi collegare Google in qualsiasi momento.", color = Warning)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Entra con Google (+$GOOGLE_BONUS_PU PU)")
        }
        OutlinedButton(onClick = vm::continueAnonymously, modifier = Modifier.fillMaxWidth()) {
            Text("Continua senza account")
        }
        Text(GOOGLE_SOON, style = MaterialTheme.typography.bodySmall)
    }
}

/** Sempre disponibile finché il player non ha collegato Google. */
@Composable
private fun GoogleLinkCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Account non collegato", fontWeight = FontWeight.Bold, color = Warning)
            Text(LOSS_WARNING)
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Collega Google (+$GOOGLE_BONUS_PU PU)")
            }
            Text(GOOGLE_SOON, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RulesGate(vm: GameViewModel) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Hai letto il regolamento?", style = MaterialTheme.typography.headlineSmall)
        Text("Prima di creare la tua unità devi conoscere le regole di Sector 6080.")
        OutlinedButton(onClick = { context.openUrl(RULES_URL) }, modifier = Modifier.fillMaxWidth()) {
            Text("Apri il regolamento")
        }
        Button(onClick = vm::confirmRulesRead, modifier = Modifier.fillMaxWidth()) {
            Text("Sì, l'ho letto")
        }
    }
}

@Composable
private fun CreateUnitForm(vm: GameViewModel, state: GameState) {
    val catalog = vm.catalog
    val budget = state.pu
    val busy by vm.busy.collectAsStateWithLifecycle()
    var typeName by rememberSaveable { mutableStateOf<String?>(null) }
    var matricola by rememberSaveable { mutableStateOf("") }
    var player by rememberSaveable { mutableStateOf("") }
    var lore by rememberSaveable { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }
    var showErrors by remember { mutableStateOf(false) }

    val type = typeName?.let { catalog.type(it) }
    val chosen: List<Component> = type?.components?.filter { it.name in selected }.orEmpty()
    val cost = type?.let { UnitRules.totalCost(it, chosen) } ?: 0
    val errors = UnitRules.validate(matricola, player, lore, type, chosen, budget)

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.padding(top = 8.dp))
            Text("Nuova unità", style = MaterialTheme.typography.headlineSmall)
            Text("Hai $budget PU. Puoi comprare solo componenti del tuo tipo.")
        }
        if (state.access != Access.GOOGLE) item { GoogleLinkCard() }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                catalog.types.forEach { t ->
                    FilterChip(
                        selected = t.name == typeName,
                        onClick = { typeName = t.name; selected.clear() },
                        enabled = t.creationCost <= budget,
                        label = { Text("${t.name} (${t.creationCost} PU)") },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = matricola,
                onValueChange = { matricola = it.uppercase().take(7) },
                label = { Text("Matricola (es. XYZ-789)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = player, onValueChange = { player = it },
                label = { Text("Player") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (type != null) {
            item {
                val stats = UnitRules.maxStats(type, chosen)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Spesi $cost / $budget PU",
                            color = if (cost > budget) Danger else Success,
                            fontWeight = FontWeight.Bold,
                        )
                        StatsRow(stats)
                    }
                }
            }
            type.components.groupBy { it.cost }.toSortedMap().forEach { (price, comps) ->
                item { Text("$price PU", style = MaterialTheme.typography.titleMedium) }
                items(comps, key = { "${type.name}/${it.name}" }) { c ->
                    val checked = c.name in selected
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (checked) selected.remove(c.name) else selected.add(c.name)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(c.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = lore, onValueChange = { lore = it },
                label = { Text("Lore: come si è formata? Perché combatte?") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            if (showErrors) errors.forEach { Text("• $it", color = Danger) }
            Button(
                onClick = {
                    showErrors = true
                    if (errors.isEmpty() && type != null) {
                        vm.createUnit(UnitRules.create(matricola, player, lore, type, chosen), cost)
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) { Text(if (busy) "Invio in corso..." else "Crea unità") }
        }
    }
}

/** Scheda in sola lettura: solo i Game Master possono approvare o modificare un'unità. */
@Composable
private fun UnitDetails(vm: GameViewModel, state: GameState) {
    val unit = state.unit ?: return
    val max = UnitRules.maxStats(vm.catalog, unit)

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.padding(top = 8.dp))
            Text(unit.matricola, style = MaterialTheme.typography.headlineMedium)
            Text("${unit.type} · ${unit.player} · ${state.rank} · ${state.pu} PU")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "HP ${unit.hp}/${max.hp}",
                        fontWeight = FontWeight.Bold,
                        color = if (unit.hp * 3 <= max.hp) Danger else MaterialTheme.colorScheme.onSurface,
                    )
                    StatsRow(max, showHp = false)
                    Text("Rifornimenti ${unit.supplies}/${max.supplies}")
                    Text(
                        "Posizione: ${unit.position ?: "non schierata"}" +
                            if (unit.transported) " · a bordo di un mezzo" else "",
                    )
                    Text(
                        if (unit.approved) "Approvata da un Game Master" else "In attesa di approvazione di un Game Master",
                        color = if (unit.approved) Success else Warning,
                    )
                }
            }
        }
        if (state.access != Access.GOOGLE) item { GoogleLinkCard() }
        item { Text("Componenti", style = MaterialTheme.typography.titleMedium) }
        items(unit.components) { name ->
            val c = vm.catalog.component(unit.type, name)
            Column {
                val uses = c?.maxUses?.let { " (${unit.uses[name] ?: 0}/$it)" } ?: ""
                Text(name + uses, fontWeight = FontWeight.SemiBold)
                Text(
                    c?.description ?: "Componente non più presente nel regolamento",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Text("Lore", style = MaterialTheme.typography.titleMedium)
            Text(unit.lore)
        }
    }
}

@Composable
fun StatsRow(s: Stats, showHp: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHp) Text("HP ${s.hp}")
        Text("Mov ${s.movement}")
        Text("Vis ${s.vision}")
        Text("Armor ${s.armor}")
        if (showHp) Text("Rif ${s.supplies}")
    }
}
