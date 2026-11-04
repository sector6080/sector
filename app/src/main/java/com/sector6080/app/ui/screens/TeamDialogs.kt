package com.sector6080.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.UnitRules

@Composable
fun TeamListDialog(team: List<TeamMember>, onOpen: (TeamMember) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Squadra (${team.size})") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (team.isEmpty()) Text("Nessuna unità approvata sulla mappa.")
                team.forEach { member ->
                    Column(Modifier.fillMaxWidth().clickable { onOpen(member) }.padding(vertical = 8.dp)) {
                        Text(member.unit.matricola, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${member.unit.type} · ${member.rank} · ${member.pu} PU",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

/** Scheda in sola lettura di un'unità della squadra. */
@Composable
fun UnitSheetDialog(catalog: Catalog, member: TeamMember, onDismiss: () -> Unit) {
    val unit = member.unit
    val max = UnitRules.maxStats(catalog, unit)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(unit.matricola) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("${unit.type} · ${member.rank} · ${member.pu} PU")
                if (!unit.approved) Text("In attesa di approvazione")
                Text("HP ${unit.hp}/${max.hp}", fontWeight = FontWeight.Bold)
                StatsRow(max, showHp = false)
                Text("Rifornimenti ${unit.supplies}/${max.supplies}")
                Text(
                    "Posizione: ${unit.position ?: "non schierata"}" +
                        if (unit.transported) " · a bordo di un mezzo" else "",
                )
                Text("Componenti", style = MaterialTheme.typography.titleSmall)
                if (unit.components.isEmpty()) Text("Nessuno")
                unit.components.forEach { name ->
                    val component = catalog.component(unit.type, name)
                    val uses = component?.maxUses?.let { " (${unit.uses[name] ?: 0}/$it)" } ?: ""
                    Text(name + uses, fontWeight = FontWeight.SemiBold)
                    if (component != null) Text(component.description, style = MaterialTheme.typography.bodySmall)
                }
                Text("Lore", style = MaterialTheme.typography.titleSmall)
                Text(unit.lore)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
