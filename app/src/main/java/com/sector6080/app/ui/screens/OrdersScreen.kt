package com.sector6080.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Success
import com.sector6080.app.ui.theme.Warning
import kotlinx.coroutines.delay

@Composable
fun OrdersScreen(vm: GameViewModel, state: GameState, onPickOnMap: (Order) -> Unit) {
    val unit = state.unit
    if (unit == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Crea prima la tua unità nella scheda Unità.")
        }
        return
    }
    val result = OrderRules.simulate(vm.catalog, unit, state.orders)

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.padding(top = 8.dp))
            TurnHeader(state)
            if (!unit.approved) {
                Text("Unità in attesa di approvazione: un Game Master deve approvarla.", color = Warning)
            }
        }
        if (state.ordersSent) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ordini inviati al Game Master", color = Success, fontWeight = FontWeight.Bold)
                        Text(
                            "Prototipo: gli ordini restano su questo telefono finché non ci sarà il server.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(onClick = vm::withdrawOrders, modifier = Modifier.fillMaxWidth()) {
                            Text("Ritira e modifica")
                        }
                    }
                }
            }
        } else {
            item { NewOrderForm(vm, state, unit, onPickOnMap) }
        }
        item {
            Text("Catena ordini (${state.orders.size})", style = MaterialTheme.typography.titleMedium)
            if (state.orders.isEmpty()) Text("Nessun ordine. Puoi darne quanti vuoi, in qualsiasi sequenza.")
        }
        itemsIndexed(state.orders) { i, order ->
            val error = result.errors.getOrNull(i)
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (error == null) MaterialTheme.colorScheme.surfaceContainer else Danger.copy(alpha = 0.25f),
                ),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1}. ${OrderRules.describe(order)}", fontWeight = FontWeight.SemiBold)
                        if (error != null) Text(error, color = Danger, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!state.ordersSent) TextButton(onClick = { vm.removeOrder(i) }) { Text("Rimuovi") }
                }
            }
        }
        if (!state.ordersSent) {
            item {
                val after = result.finalUnit
                Text(
                    "Dopo gli ordini: posizione ${after.position ?: "—"} · rifornimenti ${after.supplies} · HP ${after.hp}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = vm::sendOrders,
                    enabled = state.orders.isNotEmpty() && result.isValid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (result.isValid) "Invia ordini" else "Correggi gli ordini in rosso") }
                OutlinedButton(onClick = vm::clearOrders, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Svuota")
                }
            }
        }
    }
}

@Composable
private fun TurnHeader(state: GameState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }
    Text("Turno ${state.turn}", style = MaterialTheme.typography.headlineSmall)
    if (state.turnStartedAt > 0) {
        val left = state.turnStartedAt + state.turnHours * 3_600_000L - now
        if (left > 0) {
            val h = left / 3_600_000
            val m = left / 60_000 % 60
            Text("Mancano ${h}h ${m}min alla fine del turno", color = Success)
        } else {
            Text("Turno scaduto: in attesa del prossimo", color = Danger)
        }
    }
}

/**
 * Niente testo libero: si sceglie tra opzioni possibili in questo momento,
 * e le caselle si scelgono toccandole sulla mappa.
 */
@Composable
private fun NewOrderForm(vm: GameViewModel, state: GameState, unit: GameUnit, onPickOnMap: (Order) -> Unit) {
    val catalog = vm.catalog
    var type by remember { mutableStateOf<OrderType?>(null) }
    var component by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }

    val usable = unit.components.filter { catalog.component(unit.type, it)?.isUsable == true }
    val available = remember(unit, state.orders) {
        OrderType.entries.associateWith { t ->
            when (t) {
                OrderType.ACTIVATE, OrderType.RELOAD -> usable.filter {
                    OrderRules.isPossible(catalog, unit, state.orders, Order(t, component = it))
                }.isNotEmpty()
                OrderType.GIVE -> OrderRules.isPossible(catalog, unit, state.orders, Order(t, quantity = 1))
                else -> OrderRules.isPossible(catalog, unit, state.orders, Order(t))
            }
        }
    }
    val current = type?.takeIf { available[it] == true }
    val needsComponent = current == OrderType.ACTIVATE || current == OrderType.RELOAD
    val suppliesLeft = OrderRules.simulate(catalog, unit, state.orders).finalUnit.supplies

    fun reset() {
        type = null
        component = null
        quantity = 1
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nuovo ordine", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OrderType.entries.forEach { t ->
                    FilterChip(
                        selected = t == current,
                        onClick = { type = t; component = null; quantity = 1 },
                        enabled = available[t] == true,
                        label = { Text(t.label) },
                    )
                }
            }
            if (needsComponent) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    usable.forEach { name ->
                        val max = catalog.component(unit.type, name)?.maxUses
                        FilterChip(
                            selected = component == name,
                            onClick = { component = name },
                            enabled = OrderRules.isPossible(catalog, unit, state.orders, Order(current, component = name)),
                            label = { Text("$name (${unit.uses[name] ?: 0}/$max)") },
                        )
                    }
                }
            }
            if (current == OrderType.GIVE) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { quantity-- }, enabled = quantity > 1) { Text("−") }
                    Text("$quantity rifornimenti", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { quantity++ }, enabled = quantity < suppliesLeft) { Text("+") }
                }
            }
            if (current != null && (!needsComponent || component != null)) {
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
}
