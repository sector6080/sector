package com.sector6080.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.deployed
import com.sector6080.app.model.markedCells
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.CenteredNotice
import com.sector6080.app.ui.components.FilterChip
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.Panel
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Line
import com.sector6080.app.ui.theme.Signal
import com.sector6080.app.ui.theme.Success
import com.sector6080.app.ui.theme.Warning

@Composable
fun OrdersScreen(vm: GameViewModel, state: GameState, onPickOnMap: (Order) -> Unit) {
    val unit = state.unit
    if (unit == null) {
        CenteredNotice("Nessuna unità", "Crea prima la tua unità nella scheda Unità.")
        return
    }
    if (!unit.approved) {
        CenteredNotice("Unità in attesa", "Potrai dare ordini dopo l'approvazione di un Game Master.")
        return
    }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val team = state.deployed.map { it.unit }
    val occupied = state.occupiedCells()
    val result = OrderRules.simulate(vm.catalog, unit, state.orders, occupied, state.markedCells())

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Salire, scendere e cedere compaiono solo quando si possono fare, e partono subito
        if (OrderRules.disembarkCells(unit, occupied).isNotEmpty()) {
            item {
                InstantAction("Scendere da ${unit.carrier ?: "mezzo"}", "Scendi", busy) {
                    onPickOnMap(Order(OrderType.DISEMBARK))
                }
            }
        }
        items(OrderRules.vehiclesInReach(vm.catalog, unit, team), key = { "mezzo/" + it.matricola }) { vehicle ->
            InstantAction("Salire su ${vehicle.matricola}", "Sali", busy) { vm.boardVehicle(vehicle.matricola) }
        }
        items(OrderRules.supplyRecipients(vm.catalog, unit, team), key = { "rifornimenti/" + it.matricola }) { other ->
            val max = UnitRules.maxStats(vm.catalog, other).supplies
            InstantAction("Cedere rifornimenti a ${other.matricola}", "+1", busy, "${other.supplies}/$max") {
                vm.giveSupplies(other.matricola, 1)
            }
        }
        if (state.ordersSent) {
            item {
                Panel(Modifier.fillMaxWidth(), accent = Success) {
                    Text("Ordini inviati", color = Success, style = MaterialTheme.typography.titleMedium)
                    Text("Il server li esegue allo scadere del turno.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = vm::withdrawOrders, modifier = Modifier.fillMaxWidth()) {
                        Text("Ritira e modifica")
                    }
                }
            }
        } else {
            item { NewOrderForm(vm, state, unit, onPickOnMap) }
        }
        if (state.orders.isNotEmpty()) {
            item { SectionLabel("Catena ordini", trailing = "${state.orders.size}") }
        }
        itemsIndexed(state.orders) { i, order ->
            OrderStep(
                index = i + 1,
                last = i == state.orders.lastIndex,
                text = OrderRules.describe(order),
                error = result.errors.getOrNull(i),
                onRemove = { vm.removeOrder(i) }.takeUnless { state.ordersSent },
            )
        }
        if (!state.ordersSent && state.orders.isNotEmpty()) {
            item {
                Button(
                    onClick = vm::sendOrders,
                    enabled = result.isValid,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text(if (result.isValid) "Invia ordini" else "Correggi gli ordini in rosso") }
                Text(
                    "Partono allo scadere del turno.",
                    color = InkDim,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun InstantAction(text: String, action: String, busy: Boolean, amount: String? = null, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (amount != null) Text(amount, style = MaterialTheme.typography.labelMedium, color = Signal)
            OutlinedButton(onClick = onClick, enabled = !busy) { Text(action) }
        }
    }
}

/** Un passo della catena: il numero a sinistra, collegato al passo successivo. */
@Composable
private fun OrderStep(index: Int, last: Boolean, text: String, error: String?, onRemove: (() -> Unit)?) {
    val color = if (error == null) Signal else Danger
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(28.dp).border(1.dp, color), contentAlignment = Alignment.Center) {
                Text("$index", style = MaterialTheme.typography.labelMedium, color = color)
            }
            if (!last) Box(Modifier.weight(1f).width(1.dp).background(Line))
        }
        Panel(Modifier.weight(1f), accent = if (error == null) Line else Danger) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text, fontWeight = FontWeight.SemiBold)
                    if (error != null) Text(error, color = Danger, style = MaterialTheme.typography.bodySmall)
                }
                if (onRemove != null) TextButton(onClick = onRemove) { Text("Rimuovi") }
            }
        }
    }
}

/**
 * Niente testo libero: le scelte fattibili stanno a sinistra, le altre restano visibili
 * con il motivo, e le caselle si scelgono toccandole sulla mappa.
 */
@Composable
private fun NewOrderForm(vm: GameViewModel, state: GameState, unit: GameUnit, onPickOnMap: (Order) -> Unit) {
    val catalog = vm.catalog
    val occupied = state.occupiedCells()
    val marked = state.markedCells()
    var type by remember { mutableStateOf<OrderType?>(null) }
    var component by remember { mutableStateOf<String?>(null) }

    val usable = unit.components.filter { catalog.component(unit.type, it)?.isUsable == true }
    val types = OrderType.entries.filterNot { it.instant }
    val possible = remember(unit, state.orders, state.team, state.enemies, state.marks, state.catalog) {
        types.associateWith { t ->
            when (t) {
                OrderType.ACTIVATE, OrderType.RELOAD -> usable.any {
                    OrderRules.isPossible(catalog, unit, state.orders, Order(t, component = it), occupied, marked)
                }
                else -> OrderRules.isPossible(catalog, unit, state.orders, Order(t), occupied, marked)
            }
        }
    }
    val current = type
    val canDo = current != null && possible[current] == true
    val needsComponent = current == OrderType.ACTIVATE || current == OrderType.RELOAD
    val simulated = OrderRules.simulate(catalog, unit, state.orders, occupied, marked).finalUnit

    fun reset() {
        type = null
        component = null
    }

    Panel(Modifier.fillMaxWidth()) {
        Text("Nuovo ordine", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            types.sortedByDescending { possible[it] == true }.forEach { t ->
                FilterChip(
                    selected = t == current,
                    onClick = { type = t; component = null },
                    label = { Text(t.label) },
                    modifier = Modifier.alpha(if (possible[t] == true) 1f else 0.5f),
                )
            }
        }
        if (needsComponent) {
            if (usable.isEmpty()) Text("Non hai componenti utilizzabili.", color = InkDim)
            val options = usable.map { name ->
                name to OrderRules.isPossible(catalog, unit, state.orders, Order(current, component = name), occupied, marked)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.sortedByDescending { it.second }.forEach { (name, enabled) ->
                    val max = catalog.component(unit.type, name)?.maxUses ?: 0
                    val left = simulated.uses[name] ?: 0
                    val status = when {
                        enabled -> ""
                        current == OrderType.ACTIVATE && left <= 0 -> " · scarico"
                        current == OrderType.RELOAD && left >= max -> " · carico"
                        current == OrderType.RELOAD -> " · mancano rifornimenti"
                        else -> " · non disponibile"
                    }
                    FilterChip(
                        selected = component == name,
                        onClick = { component = name },
                        enabled = enabled,
                        label = { Text("$name ($left/$max)$status") },
                    )
                }
            }
        } else if (current != null && !canDo) {
            Text(OrderRules.whyNot(catalog, unit, state.orders, Order(current), occupied, marked), color = Warning)
        }
        if (current != null && canDo && (!needsComponent || component != null)) {
            val draft = Order(type = current, component = if (needsComponent) component else null)
            if (OrderRules.needsTarget(catalog, unit, draft)) {
                Button(onClick = { onPickOnMap(draft) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scegli la casella sulla mappa")
                }
            } else {
                Button(onClick = { vm.addOrder(draft); reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Aggiungi alla catena")
                }
            }
        }
    }
}
