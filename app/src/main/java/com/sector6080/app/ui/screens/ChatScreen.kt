package com.sector6080.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.GameState
import com.sector6080.app.rules.Formation
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.Card
import com.sector6080.app.ui.components.FilterChip
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.RankBadge
import com.sector6080.app.ui.components.UnitSymbol
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Friend
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Signal
import java.text.DateFormat
import java.util.Date

/** Chat di gioco a canali. Toccando la firma di un messaggio si menziona chi l'ha scritto. */
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
    val formation = Formation.entries.firstOrNull { it.channel == channel && HierarchyRules.inFormation(state, it) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            channels.forEach { c ->
                FilterChip(selected = c == channel, onClick = { selected = c }, label = { Text(c.label) })
            }
        }
        if (formation != null) {
            val count = HierarchyRules.size(state, formation)
            val limit = HierarchyRules.limit(state, formation) ?: "?"
            val requests = HierarchyRules.incomingRequests(state).size.takeIf { state.rank == formation.leaderRank } ?: 0
            OutlinedButton(onClick = { showSquad = true }) {
                Text(
                    "${HierarchyRules.title(formation, HierarchyRules.name(state, formation))} · $count/$limit" +
                        when (requests) {
                            0 -> ""
                            1 -> " · 1 richiesta"
                            else -> " · $requests richieste"
                        },
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true,
        ) {
            items(state.chat.filter { it.channel == channel.id }.asReversed()) { m ->
                val mine = state.unit?.matricola?.let { it in m.author } == true
                val tint = if (mine) Friend else InkDim
                val author = HierarchyRules.chatAuthor(m.author)
                Card(
                    Modifier.fillMaxWidth().padding(
                        start = if (mine) 36.dp else 0.dp,
                        end = if (mine) 0.dp else 36.dp,
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val signature = author.matricola
                                    ?.takeIf { blocked == null && it != state.unit?.matricola }
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .then(
                                            if (signature != null) {
                                                Modifier.clickable { text = "$text@$signature " }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    RankBadge(author.rank, size = 14.dp)
                                    author.type?.let { UnitSymbol(it, size = 22.dp) }
                                    Text(
                                        author.matricola ?: author.rank,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = tint,
                                    )
                                }
                                Text(
                                    time.format(Date(m.sentAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkDim,
                                )
                            }
                            Text(
                                m.text,
                                color = if (HierarchyRules.mentionsMe(state, m)) Signal else Color.Unspecified,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (state.isGm) {
                            IconButton(onClick = { deleting = m }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Cancella il messaggio", tint = InkDim)
                            }
                        }
                    }
                }
            }
        }
        if (blocked != null) {
            Text(blocked, color = InkDim, modifier = Modifier.padding(bottom = 16.dp))
        } else {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isGm) TextButton(onClick = { text = "@tutti $text" }) { Text("@tutti") }
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

    if (showSquad && formation != null) SquadDialog(vm, state, formation, onDismiss = { showSquad = false })
    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Cancellare il messaggio?") },
            text = {
                val author = HierarchyRules.chatAuthor(message.author)
                Text("${author.matricola ?: author.rank}: ${message.text}")
            },
            confirmButton = {
                TextButton(onClick = { vm.gmDeleteMessage(message.id); deleting = null }) {
                    Text("Cancella", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annulla") } },
        )
    }
}
