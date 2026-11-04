package com.sector6080.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.GameState
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Danger
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private const val REFRESH_MILLIS = 10_000L

/** Chat di gioco a canali: si aggiorna da sola finché la scheda è aperta. */
@Composable
fun ChatScreen(vm: GameViewModel, state: GameState) {
    var text by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(ChatChannel.COMMON) }
    var deleting by remember { mutableStateOf<ChatMessage?>(null) }
    var showSquad by remember { mutableStateOf(false) }
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)
    val channels = ChatChannel.entries.filter { HierarchyRules.canSee(state, it) }
    // Chi perde il grado può trovarsi su un canale che non vede più
    val channel = selected.takeIf { it in channels } ?: ChatChannel.COMMON
    val blocked = HierarchyRules.whyNotWrite(state, channel)

    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_MILLIS)
            vm.sync()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Chat",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { c ->
                FilterChip(selected = c == channel, onClick = { selected = c }, label = { Text(c.label) })
            }
        }
        if (channel == ChatChannel.SQUAD && HierarchyRules.inSquad(state)) {
            val count = HierarchyRules.squadMembers(state).size
            OutlinedButton(onClick = { showSquad = true }) {
                Text("${HierarchyRules.squadTitle(HierarchyRules.squadName(state))} · $count/${HierarchyRules.MAX_SQUAD}")
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true,
        ) {
            items(state.chat.filter { it.channel == channel.id }.asReversed()) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(10.dp)) {
                            Text(
                                "${m.author} · ${time.format(Date(m.sentAt))}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(m.text)
                        }
                        if (state.isGm) {
                            IconButton(onClick = { deleting = m }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Cancella il messaggio")
                            }
                        }
                    }
                }
            }
        }
        if (blocked != null) {
            Text(blocked, modifier = Modifier.padding(bottom = 16.dp))
        } else {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Scrivi in ${channel.label}") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { vm.sendChat(channel, text); text = "" },
                    enabled = text.isNotBlank(),
                ) { Text("Invia") }
            }
        }
    }

    if (showSquad) SquadDialog(vm, state, onDismiss = { showSquad = false })
    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Cancellare il messaggio?") },
            text = { Text("${message.author}: ${message.text}") },
            confirmButton = {
                TextButton(onClick = { vm.gmDeleteMessage(message.id); deleting = null }) {
                    Text("Cancella", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annulla") } },
        )
    }
}
