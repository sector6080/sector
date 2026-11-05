package com.sector6080.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.Access
import com.sector6080.app.model.GOOGLE_BONUS_PU
import com.sector6080.app.model.GameState
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.LocalSounds
import com.sector6080.app.ui.Sfx
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Line
import com.sector6080.app.ui.theme.Signal
import java.text.DateFormat
import java.util.Date

private val GOOGLE_BLUE = Color(0xFF4285F4)
private val GOOGLE_GREEN = Color(0xFF34A853)
private val GOOGLE_YELLOW = Color(0xFFFBBC05)
private val GOOGLE_RED = Color(0xFFEA4335)

/** Mostra solo le notifiche da leggere: chiudendo la finestra diventano lette e spariscono. */
@Composable
fun NotificationsDialog(vm: GameViewModel, state: GameState, onDismiss: () -> Unit) {
    val items = state.notifications.filterNot { it.read }
    val time = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val sounds = LocalSounds.current
    var googleInfo by remember { mutableStateOf(false) }

    fun close() {
        vm.markNotificationsRead()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { close() },
        title = { Text("Notifiche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (items.isEmpty()) Text("Nessuna notifica.")
                    items.forEach { notification ->
                        Column {
                            Text(notification.title, style = MaterialTheme.typography.titleSmall, color = Signal)
                            if (notification.body.isNotEmpty()) {
                                Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                time.format(Date(notification.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkDim,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Volume", style = MaterialTheme.typography.labelMedium, color = InkDim)
                    Slider(
                        value = state.soundVolume,
                        onValueChange = vm::setSoundVolume,
                        onValueChangeFinished = { sounds?.play(Sfx.ALERT) },
                        colors = SliderDefaults.colors(
                            thumbColor = Signal,
                            activeTrackColor = Signal,
                            inactiveTrackColor = Line,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (state.access != Access.GOOGLE) GoogleButton { googleInfo = !googleInfo }
                }
                if (googleInfo) {
                    Text(
                        "Con Google ricevi +$GOOGLE_BONUS_PU PU e la tua unità resta al sicuro. " +
                            "L'accesso con Google arriverà presto.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { close() }) { Text("Chiudi") } },
    )
}

/** Bottoncino rotondo con la G a quattro colori. */
@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClickLabel = "Collega Google", role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val stroke = size.width * 0.22f
            val corner = Offset(stroke / 2, stroke / 2)
            val ring = Size(size.width - stroke, size.height - stroke)
            fun arc(color: Color, start: Float, sweep: Float) =
                drawArc(color, start, sweep, false, corner, ring, style = Stroke(stroke))
            arc(GOOGLE_RED, 215f, 105f)
            arc(GOOGLE_YELLOW, 145f, 70f)
            arc(GOOGLE_GREEN, 40f, 105f)
            arc(GOOGLE_BLUE, 0f, 40f)
            drawRect(
                GOOGLE_BLUE,
                Offset(size.width * 0.5f, size.height * 0.5f - stroke / 2),
                Size(size.width * 0.5f, stroke),
            )
        }
    }
}
