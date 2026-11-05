package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.GameState
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Ink
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Signal
import java.text.DateFormat
import java.util.Date

/** Aprendola, tutte le notifiche diventano lette: quelle nuove restano in ambra finché è aperta. */
@Composable
fun NotificationsDialog(vm: GameViewModel, state: GameState, onDismiss: () -> Unit) {
    val items = remember { state.notifications }
    val time = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    LaunchedEffect(Unit) { vm.markNotificationsRead() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifiche") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (items.isEmpty()) Text("Nessuna notifica.")
                items.forEach { notification ->
                    Column {
                        Text(
                            notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (notification.read) Ink else Signal,
                        )
                        if (notification.body.isNotEmpty()) Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            time.format(Date(notification.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkDim,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        dismissButton = {
            TextButton(onClick = { vm.setSoundOn(!state.soundOn) }) {
                Text(if (state.soundOn) "Suoni attivi" else "Suoni spenti")
            }
        },
    )
}
