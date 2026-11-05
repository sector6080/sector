package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.components.RankBadge
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.components.StatBar
import com.sector6080.app.ui.components.UnitSymbol
import com.sector6080.app.ui.theme.Friend
import com.sector6080.app.ui.theme.Mono
import com.sector6080.app.ui.theme.Warning

/** Scheda in sola lettura di un'unità della squadra. */
@Composable
fun UnitSheetDialog(
    catalog: Catalog,
    member: TeamMember,
    onDismiss: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    val unit = member.unit
    val max = UnitRules.maxStats(catalog, unit)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UnitSymbol(unit.type, size = 40.dp)
                Text(unit.matricola, fontFamily = Mono, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RankBadge(member.rank)
                    Text("${unit.type} · ${member.rank} · ${member.pu} PU")
                }
                if (!unit.approved) Text("In attesa di approvazione", color = Warning)
                member.squadLabel?.let { Text(it.replaceFirstChar(Char::uppercase)) }
                actions()
                StatBar("HP", unit.hp, max.hp)
                StatBar("Rifornimenti", unit.supplies, max.supplies, color = Friend)
                StatsRow(max, showHp = false)
                Text(
                    "Posizione: ${unit.position ?: "non schierata"}" +
                        if (unit.transported) " · a bordo di un mezzo" else "",
                )
                SectionLabel("Componenti")
                if (unit.components.isEmpty()) Text("Nessuno")
                unit.components.forEach { name ->
                    val component = catalog.component(unit.type, name)
                    val uses = component?.maxUses?.let { " (${unit.uses[name] ?: 0}/$it)" } ?: ""
                    Text(name + uses, fontWeight = FontWeight.SemiBold)
                    if (component != null) Text(component.description, style = MaterialTheme.typography.bodySmall)
                }
                SectionLabel("Lore")
                Text(unit.lore)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
