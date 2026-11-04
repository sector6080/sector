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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.GameState
import com.sector6080.app.model.RANK_CAPTAIN
import com.sector6080.app.model.RankProposal
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.HierarchyRules
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
        if (state.proposals.isNotEmpty()) {
            item { Heading("Votazioni (${state.proposals.size})") }
            items(state.proposals, key = { "v-${it.id}" }) { ProposalCard(vm, state, it) }
        }
        if (state.team.isEmpty()) item { Text("Nessuna unità in squadra.") }
        state.team.groupBy { it.rank }.forEach { (rank, members) ->
            item(key = rank) { Heading("$rank (${members.size})") }
            items(members, key = { it.unit.matricola }) { member ->
                val unit = member.unit
                Card(onClick = { sheet = member }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(unit.matricola, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(unit.type, member.squadLabel).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    sheet?.let { member ->
        UnitSheetDialog(vm.catalog, member, onDismiss = { sheet = null }) {
            SheetActions(vm, state, member, onDone = { sheet = null })
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

/** I Capitani votano, il Comando Supremo vede l'andamento e decide dalla scheda GM. */
@Composable
private fun ProposalCard(vm: GameViewModel, state: GameState, proposal: RankProposal) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${proposal.matricola}: da ${proposal.fromRank} a ${proposal.newRank}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Proposta da ${proposal.proposedBy ?: "?"} · sì ${proposal.votesYes} · no ${proposal.votesNo}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.rank == RANK_CAPTAIN && proposal.matricola != state.unit?.matricola) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.voteRank(proposal.id, true) },
                        enabled = !busy && proposal.myVote != true,
                    ) { Text("Sì") }
                    OutlinedButton(
                        onClick = { vm.voteRank(proposal.id, false) },
                        enabled = !busy && proposal.myVote != false,
                    ) { Text("No") }
                }
            }
        }
    }
}

/** Il Capitano propone i cambi di grado. Lo squadrone si gestisce dalla chat. */
@Composable
private fun SheetActions(vm: GameViewModel, state: GameState, member: TeamMember, onDone: () -> Unit) {
    val matricola = member.unit.matricola
    if (state.rank != RANK_CAPTAIN || !member.unit.approved || matricola == state.unit?.matricola) return
    if (state.proposals.any { it.matricola == matricola }) return

    HierarchyRules.nextRanks(member.rank).forEach { rank ->
        OutlinedButton(onClick = { vm.proposeRank(matricola, rank); onDone() }) { Text("Proponi $rank") }
    }
}
