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
import com.sector6080.app.model.RANK_CORPORAL
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.ui.GameViewModel

/** Si apre dalla chat Squadrone: il Caporale sceglie nome e Soldati, gli altri membri guardano. */
@Composable
fun SquadDialog(vm: GameViewModel, state: GameState, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val leader = state.rank == RANK_CORPORAL
    val current = HierarchyRules.squadName(state)
    val members = HierarchyRules.squadMembers(state)
    val full = members.size >= HierarchyRules.MAX_SQUAD
    var name by rememberSaveable { mutableStateOf(current.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(HierarchyRules.squadTitle(current)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (leader) {
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
                }

                Text("Soldati ${members.size}/${HierarchyRules.MAX_SQUAD}", style = MaterialTheme.typography.titleSmall)
                if (members.isEmpty()) Text("Nessuno")
                members.forEach { member ->
                    SoldierRow(member, "Togli".takeIf { leader }, !busy) {
                        vm.setSquadMember(member.unit.matricola, false)
                    }
                }

                if (leader) {
                    val free = HierarchyRules.freeSoldiers(state)
                    Text("Soldati liberi", style = MaterialTheme.typography.titleSmall)
                    if (free.isEmpty()) Text("Nessuno")
                    free.forEach { member ->
                        SoldierRow(member, "Aggiungi", !busy && !full) {
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
private fun SoldierRow(member: TeamMember, action: String?, enabled: Boolean, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${member.unit.matricola} · ${member.unit.type}", modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction, enabled = enabled) { Text(action) }
    }
}
