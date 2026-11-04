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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.CenteredNotice
import com.sector6080.app.ui.components.FilterChip
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.Panel
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.components.StatTile
import com.sector6080.app.ui.components.StatusStrip
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
        if (state.isGm) {
            CenteredNotice("Comando Supremo", "Il Comando Supremo non dà ordini: gli ordini dei player sono nella scheda GM.")
        } else {
            CenteredNotice("Nessuna unità", "Crea prima la tua unità nella scheda Unità.")
        }
        return
    }
    val result = OrderRules.simulate(vm.catalog, unit, state.orders, state.occupiedCells())

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!unit.approved) {
            item { StatusStrip("Unità in attesa di approvazione: un Game Master deve approvarla.", Warning) }
        }
        if (state.ordersSent) {
            item {
                Panel(Modifier.fillMaxWidth(), accent = Success) {
                    Text("Ordini inviati al Game Master", color = Success, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = vm::withdrawOrders, modifier = Modifier.fillMaxWidth()) {
                        Text("Ritira e modifica")
                    }
                }
            }
        } else {
            item { NewOrderForm(vm, state, unit, onPickOnMap) }
        }
        item {
            SectionLabel("Catena ordini", trailing = "${state.orders.size}")
            if (state.orders.isEmpty()) {
                Text("Nessun ordine. Puoi darne quanti vuoi, in qualsiasi sequenza.", color = InkDim)
            }
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
        if (!state.ordersSent) {
            item {
                val after = result.finalUnit
                SectionLabel("Dopo gli ordini")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatTile("Pos", after.position?.toString() ?: "—", Modifier.weight(1f))
                    StatTile("Rif", "${after.supplies}", Modifier.weight(1f))
                    StatTile("HP", "${after.hp}", Modifier.weight(1f))
                }
                Button(
                    onClick = vm::sendOrders,
                    enabled = unit.approved && state.orders.isNotEmpty() && result.isValid,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(
                        when {
                            !unit.approved -> "Puoi inviare dopo l'approvazione"
                            result.isValid -> "Invia ordini"
                            else -> "Correggi gli ordini in rosso"
                        },
                    )
                }
            }
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
    var type by remember { mutableStateOf<OrderType?>(null) }
    var component by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }

    val usable = unit.components.filter { catalog.component(unit.type, it)?.isUsable == true }
    val possible = remember(unit, state.orders, state.team, state.enemies, state.catalog) {
        OrderType.entries.associateWith { t ->
            when (t) {
                OrderType.ACTIVATE, OrderType.RELOAD -> usable.any {
                    OrderRules.isPossible(catalog, unit, state.orders, Order(t, component = it), occupied)
                }
                OrderType.GIVE -> OrderRules.isPossible(catalog, unit, state.orders, Order(t, quantity = 1), occupied)
                else -> OrderRules.isPossible(catalog, unit, state.orders, Order(t), occupied)
            }
        }
    }
    val current = type
    val canDo = current != null && possible[current] == true
    val needsComponent = current == OrderType.ACTIVATE || current == OrderType.RELOAD
    val simulated = OrderRules.simulate(catalog, unit, state.orders, occupied).finalUnit

    fun reset() {
        type = null
        component = null
        quantity = 1
    }

    Panel(Modifier.fillMaxWidth()) {
        Text("Nuovo ordine", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OrderType.entries.sortedByDescending { possible[it] == true }.forEach { t ->
                FilterChip(
                    selected = t == current,
                    onClick = { type = t; component = null; quantity = 1 },
                    label = { Text(t.label) },
                    modifier = Modifier.alpha(if (possible[t] == true) 1f else 0.5f),
                )
            }
        }
        if (needsComponent) {
            if (usable.isEmpty()) Text("Non hai componenti utilizzabili.", color = InkDim)
            val options = usable.map { name ->
                name to OrderRules.isPossible(catalog, unit, state.orders, Order(current, component = name), occupied)
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
            val draft = Order(current, quantity = 1.takeIf { current == OrderType.GIVE })
            Text(OrderRules.whyNot(catalog, unit, state.orders, draft, occupied), color = Warning)
        }
        if (current == OrderType.GIVE && canDo) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { quantity-- }, enabled = quantity > 1) { Text("−") }
                Text("$quantity rifornimenti", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { quantity++ }, enabled = quantity < simulated.supplies) { Text("+") }
            }
        }
        if (current != null && canDo && (!needsComponent || component != null)) {
            val draft = Order(
                type = current,
                component = if (needsComponent) component else null,
                quantity = quantity.takeIf { current == OrderType.GIVE },
            )
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
