package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.GameState
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.Formation
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.ui.GameViewModel

/** Si apre dalla chat: chi guida sceglie nome e membri, gli altri possono solo guardare. */
@Composable
fun SquadDialog(vm: GameViewModel, state: GameState, formation: Formation, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val leading = state.rank == formation.leaderRank
    val current = HierarchyRules.name(state, formation)
    val members = HierarchyRules.members(state, formation)
    val size = HierarchyRules.size(state, formation)
    val limit = HierarchyRules.limit(state, formation)
    val full = limit != null && size >= limit
    val squadron = formation == Formation.SQUADRON
    var name by rememberSaveable { mutableStateOf(current.orEmpty()) }

    fun describe(member: TeamMember) = if (squadron) {
        val groupName = member.groupName?.let { " \"$it\"" }.orEmpty()
        "${member.unit.matricola}$groupName · ${HierarchyRules.soldiersOf(state, member)} Soldati"
    } else {
        "${member.unit.matricola} · ${member.unit.type}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(HierarchyRules.title(formation, current)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (leading) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { text -> name = text.filterNot(Char::isDigit).take(HierarchyRules.MAX_SQUAD_NAME) },
                        label = { Text("Nome, senza numeri") },
                        singleLine = true,
                    )
                    TextButton(
                        onClick = { vm.setSquadName(name) },
                        enabled = !busy && HierarchyRules.isValidSquadName(name) && name.trim() != current,
                    ) { Text("Salva il nome") }

                    val requests = HierarchyRules.incomingRequests(state)
                    if (requests.isNotEmpty()) {
                        Text("Richieste ${requests.size}", style = MaterialTheme.typography.titleSmall)
                    }
                    requests.forEach { request ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${request.matricola} · ${request.type}", modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { vm.answerSquadRequest(request.matricola, true) },
                                enabled = !busy && !full,
                            ) { Text("Accetta") }
                            TextButton(
                                onClick = { vm.answerSquadRequest(request.matricola, false) },
                                enabled = !busy,
                            ) { Text("Rifiuta") }
                        }
                    }
                }

                Text(
                    "${if (squadron) "Unità" else "Soldati"} $size/${limit ?: "?"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (members.isEmpty()) Text("Nessuno")
                members.forEach { member ->
                    MemberRow(describe(member), "Togli".takeIf { leading }, !busy) {
                        vm.setSquadMember(member.unit.matricola, false)
                    }
                }

                if (leading) {
                    val free = HierarchyRules.free(state, formation)
                    Text(
                        if (squadron) "Gruppi liberi" else "Soldati liberi",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (free.isEmpty()) Text("Nessuno")
                    free.forEach { member ->
                        MemberRow(describe(member), "Aggiungi", !busy && !full) {
                            vm.setSquadMember(member.unit.matricola, true)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

@Composable
private fun MemberRow(text: String, action: String?, enabled: Boolean, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction, enabled = enabled) { Text(action) }
    }
}
