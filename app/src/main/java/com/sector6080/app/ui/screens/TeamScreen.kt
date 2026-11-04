package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.GameState
import com.sector6080.app.model.TeamMember
import com.sector6080.app.ui.GameViewModel

/** Tutte le unità divise per grado, nell'ordine dato dal server. Toccandone una si apre la scheda. */
@Composable
fun TeamScreen(vm: GameViewModel, state: GameState) {
    var sheet by remember { mutableStateOf<TeamMember?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Squadra",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (state.team.isEmpty()) item { Text("Nessuna unità in squadra.") }
        state.team.groupBy { it.rank }.forEach { (rank, members) ->
            item(key = rank) {
                Text(
                    "$rank (${members.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(members, key = { it.unit.matricola }) { member ->
                val unit = member.unit
                Card(onClick = { sheet = member }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(unit.matricola, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${unit.type} · ${member.pu} PU" + if (unit.approved) "" else " · in attesa",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    sheet?.let { UnitSheetDialog(vm.catalog, it) { sheet = null } }
}
