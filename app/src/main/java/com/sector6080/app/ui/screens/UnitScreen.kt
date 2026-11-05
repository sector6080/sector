package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.Access
import com.sector6080.app.model.Component
import com.sector6080.app.model.GOOGLE_BONUS_PU
import com.sector6080.app.model.GameState
import com.sector6080.app.model.Stats
import com.sector6080.app.model.UnitType
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.RULES_URL
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.Logo
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.Panel
import com.sector6080.app.ui.components.RankBadge
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.components.StatBar
import com.sector6080.app.ui.components.StatTile
import com.sector6080.app.ui.components.StatusStrip
import com.sector6080.app.ui.components.UnitSymbol
import com.sector6080.app.ui.openUrl
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Friend
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Line
import com.sector6080.app.ui.theme.LineBright
import com.sector6080.app.ui.theme.Mono
import com.sector6080.app.ui.theme.Signal
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

private const val COMPONENTS_AFTER_APPROVAL = "Puoi acquistare i componenti solo dopo l'approvazione."
private const val GOOGLE_SOON = "L'accesso con Google arriverà presto."
private const val LOSS_WARNING =
    "Senza accesso con Google, se disinstalli l'app o cambi telefono perdi la tua unità."

/** Apertura delle schermate di ingresso, con il motto della Task Force. */
@Composable
private fun GateHero() {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Logo(size = 112.dp)
        Spacer(Modifier.height(8.dp))
        Text("SECTOR 6080", style = MaterialTheme.typography.headlineLarge)
        Text("TASK FORCE 6080 · ANNO 2087", style = MaterialTheme.typography.labelSmall, color = Signal)
        Text(
            "Qui non si combatte solo per la sopravvivenza. Si combatte per non diventare come loro.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AccessGate(vm: GameViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GateHero()
        SectionLabel("Accesso")
        Text("Come vuoi entrare?", style = MaterialTheme.typography.headlineSmall)
        Text("Con Google ricevi +$GOOGLE_BONUS_PU PU e la tua unità resta al sicuro.")
        StatusStrip("$LOSS_WARNING Puoi collegare Google in qualsiasi momento.", Warning)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Entra con Google (+$GOOGLE_BONUS_PU PU)")
        }
        OutlinedButton(onClick = vm::continueAnonymously, modifier = Modifier.fillMaxWidth()) {
            Text("Continua senza account")
        }
        Text(GOOGLE_SOON, style = MaterialTheme.typography.bodySmall)
    }
}

/** Avviso che si chiude con la X, nello stesso punto del testo che sostituisce. */
@Composable
private fun CloseButton(onClose: () -> Unit) {
    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Chiudi") }
}

/** Avviso finché Google non è collegato: la X lo chiude. */
private fun LazyListScope.accountCard(vm: GameViewModel, state: GameState) {
    if (state.access == Access.GOOGLE || state.accountNoticeClosed) return
    item { GoogleLinkCard(onClose = { vm.setAccountNoticeClosed(true) }) }
}

/** Ad avviso chiuso resta solo un link discreto in fondo alla pagina. */
private fun LazyListScope.accountLink(vm: GameViewModel, state: GameState) {
    if (state.access == Access.GOOGLE || !state.accountNoticeClosed) return
    item {
        TextButton(onClick = { vm.setAccountNoticeClosed(false) }) {
            Text(
                "Collega Google",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoogleLinkCard(onClose: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Account non collegato",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            CloseButton(onClose)
        }
        Text(LOSS_WARNING, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Collega Google (+$GOOGLE_BONUS_PU PU)")
        }
        Text(GOOGLE_SOON, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RulesGate(vm: GameViewModel) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GateHero()
        SectionLabel("Regolamento")
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
private fun TypeCard(type: UnitType, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val stats = type.base
    Panel(
        modifier.alpha(if (enabled) 1f else 0.4f),
        accent = if (selected) Signal else Line,
        onClick = onClick.takeIf { enabled },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UnitSymbol(type.name, Modifier.weight(1f, fill = false), color = if (selected) Signal else Friend, size = 34.dp)
            Spacer(Modifier.weight(1f))
            Text("${type.creationCost} PU", style = MaterialTheme.typography.labelMedium, color = Signal)
        }
        Text(type.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "HP ${stats.hp} · MOV ${stats.movement}\nVIS ${stats.vision} · ARM ${stats.armor}",
            style = MaterialTheme.typography.labelSmall,
            color = InkDim,
        )
    }
}

@Composable
private fun CreateUnitForm(vm: GameViewModel, state: GameState) {
    val catalog = vm.catalog
    val budget = state.pu
    val busy by vm.busy.collectAsStateWithLifecycle()
    var typeName by rememberSaveable { mutableStateOf<String?>(null) }
    var matricola by rememberSaveable { mutableStateOf("") }
    var lore by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    val type = typeName?.let { catalog.type(it) }
    val errors = UnitRules.validate(matricola, lore, type, budget)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Nuova unità", style = MaterialTheme.typography.headlineSmall)
            Text("Hai $budget PU. Il tipo si paga all'approvazione.", color = InkDim)
            Text(COMPONENTS_AFTER_APPROVAL, color = Signal)
        }
        accountCard(vm, state)
        item { SectionLabel("01 · Tipo di unità") }
        items(catalog.types.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { t ->
                    TypeCard(t, t.name == typeName, t.creationCost <= budget, Modifier.weight(1f)) {
                        typeName = t.name
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { SectionLabel("02 · Matricola") }
        item {
            OutlinedTextField(
                value = matricola,
                onValueChange = { matricola = it.uppercase().take(7) },
                label = { Text("Matricola (es. XYZ-789)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionLabel("03 · Lore") }
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
                        vm.createUnit(matricola, type.name, lore.trim())
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) { Text(if (busy) "Invio in corso..." else "Crea unità") }
        }
        accountLink(vm, state)
    }
}

/** I valori dell'unità li modificano solo i Game Master: il player può comprare componenti. */
@Composable
private fun UnitDetails(vm: GameViewModel, state: GameState) {
    val unit = state.unit ?: return
    val max = UnitRules.maxStats(vm.catalog, unit)
    val busy by vm.busy.collectAsStateWithLifecycle()
    var shopOpen by rememberSaveable { mutableStateOf(false) }
    var buying by remember { mutableStateOf<Component?>(null) }
    val shop = vm.catalog.type(unit.type)?.components.orEmpty().filter { it.name !in unit.components }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Panel(Modifier.fillMaxWidth(), accent = LineBright) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UnitSymbol(unit.type, size = 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(unit.matricola, style = MaterialTheme.typography.headlineMedium, fontFamily = Mono, fontWeight = FontWeight.Bold)
                        Text("${unit.type} · ${state.rank}", color = InkDim)
                    }
                    RankBadge(state.rank, size = 30.dp)
                }
                Spacer(Modifier.height(4.dp))
                StatBar("HP", unit.hp, max.hp)
                StatBar("Rifornimenti", unit.supplies, max.supplies, color = Friend)
                Spacer(Modifier.height(2.dp))
                StatsRow(max, showHp = false, position = unit.position?.toString() ?: "—")
                if (unit.transported) Text("A bordo di un mezzo", style = MaterialTheme.typography.bodySmall)
                if (!unit.approved) {
                    StatusStrip("In attesa di approvazione di un Game Master", Warning)
                } else if (!state.approvalClosed) {
                    StatusStrip("Approvata da un Game Master", Success) { CloseButton(vm::closeApproval) }
                }
            }
        }
        accountCard(vm, state)
        item { SectionLabel("Componenti", trailing = "${unit.components.size}") }
        items(unit.components) { name ->
            val c = vm.catalog.component(unit.type, name)
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    c?.maxUses?.let {
                        Text("${unit.uses[name] ?: 0}/$it", style = MaterialTheme.typography.labelMedium, color = Signal)
                    }
                }
                Text(
                    c?.description ?: "Componente non più presente nel regolamento",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            if (unit.approved) {
                OutlinedButton(onClick = { shopOpen = !shopOpen }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (shopOpen) "Chiudi il negozio" else "Compra componenti · ${state.pu} PU")
                }
            } else {
                Text(COMPONENTS_AFTER_APPROVAL, color = InkDim)
            }
        }
        if (shopOpen && unit.approved) {
            items(shop, key = { "shop/" + it.name }) { c ->
                val missing = c.requires?.takeIf { req -> unit.components.none { it.equals(req, ignoreCase = true) } }
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(c.description, style = MaterialTheme.typography.bodySmall)
                            if (missing != null) {
                                Text("Richiede $missing", color = Warning, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${c.cost} PU", style = MaterialTheme.typography.labelMedium, color = Signal)
                            TextButton(
                                onClick = { buying = c },
                                enabled = !busy && missing == null && c.cost <= state.pu,
                            ) { Text("Compra") }
                        }
                    }
                }
            }
        }
        item { SectionLabel("Lore") }
        item { Text(unit.lore) }
        accountLink(vm, state)
    }

    buying?.let { c ->
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text("Comprare ${c.name}?") },
            text = { Text("Costa ${c.cost} PU. Ti restano ${state.pu - c.cost} PU. Se perdi l'unità, perdi anche il componente.") },
            confirmButton = {
                TextButton(onClick = { vm.buyComponent(c.name); buying = null }) { Text("Compra") }
            },
            dismissButton = { TextButton(onClick = { buying = null }) { Text("Annulla") } },
        )
    }
}

@Composable
fun StatsRow(s: Stats, showHp: Boolean = true, position: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showHp) StatTile("HP", "${s.hp}", Modifier.weight(1f))
        StatTile("Mov", "${s.movement}", Modifier.weight(1f))
        StatTile("Vis", "${s.vision}", Modifier.weight(1f))
        StatTile("Arm", "${s.armor}", Modifier.weight(1f))
        if (showHp) StatTile("Rif", "${s.supplies}", Modifier.weight(1f))
        if (position != null) StatTile("Pos", position, Modifier.weight(1.5f))
    }
}
