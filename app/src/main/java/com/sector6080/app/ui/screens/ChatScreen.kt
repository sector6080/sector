package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.GameState
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Warning
import java.text.DateFormat
import java.util.Date

/** Chat di gioco: per ora solo interfaccia, i messaggi restano sul telefono. */
@Composable
fun ChatScreen(vm: GameViewModel, state: GameState) {
    var text by rememberSaveable { mutableStateOf("") }
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Chat",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Prototipo: i messaggi restano su questo telefono finché non ci sarà il server.",
            color = Warning,
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true,
        ) {
            items(state.chat.asReversed()) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "${m.author} · ${time.format(Date(m.sentAt))}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(m.text)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Scrivi un messaggio") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { vm.sendChat(text); text = "" },
                enabled = text.isNotBlank(),
            ) { Text("Invia") }
        }
    }
}
