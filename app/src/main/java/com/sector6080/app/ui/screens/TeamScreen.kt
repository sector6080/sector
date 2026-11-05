package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.GameState
import com.sector6080.app.model.RANK_CAPTAIN
import com.sector6080.app.model.RANK_RECRUIT
import com.sector6080.app.model.RANK_SUPREME
import com.sector6080.app.model.RankProposal
import com.sector6080.app.model.TeamMember
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.CenteredNotice
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.Panel
import com.sector6080.app.ui.components.RankBadge
import com.sector6080.app.ui.components.SectionLabel
import com.sector6080.app.ui.components.StatusStrip
import com.sector6080.app.ui.components.UnitSymbol
import com.sector6080.app.ui.theme.Mono
import com.sector6080.app.ui.theme.Signal

/**
 * Tutte le unità divise per grado, nell'ordine dato dal server. Toccandone una si apre la scheda.
 * Comando Supremo e Reclute senza unità compaiono solo come numero, in cima e in fondo.
 */
@Composable
fun TeamScreen(vm: GameViewModel, state: GameState) {
    var sheet by remember { mutableStateOf<TeamMember?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HierarchyRules.outgoingRequest(state)?.let { request ->
            item {
                StatusStrip("Richiesta inviata a ${request.leader}", Signal) {
                    TextButton(onClick = { vm.requestSquad(null) }) { Text("Ritira") }
                }
            }
        }
        if (state.proposals.isNotEmpty()) {
            item { SectionLabel("Votazioni", trailing = "${state.proposals.size}") }
            items(state.proposals, key = { "v-${it.id}" }) { ProposalCard(vm, state, it) }
        }
        if (state.team.isEmpty() && state.unitless.isEmpty()) {
            item { CenteredNotice("Squadra vuota", "Nessuna unità in squadra.") }
        }
        val groups = state.team.groupBy { it.rank }
        (listOf(RANK_SUPREME) + groups.keys + RANK_RECRUIT).distinct().forEach { rank ->
            val members = groups[rank].orEmpty()
            val unitless = state.unitless[rank] ?: 0
            if (members.isEmpty() && unitless == 0) return@forEach
            item(key = rank) {
                SectionLabel(rank, trailing = "${members.size + unitless}", leading = { RankBadge(rank, size = 18.dp) })
            }
            items(members, key = { it.unit.matricola }) { member ->
                val unit = member.unit
                Panel(Modifier.fillMaxWidth(), onClick = { sheet = member }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        UnitSymbol(unit.type, size = 36.dp)
                        Column {
                            Text(unit.matricola, fontFamily = Mono, fontWeight = FontWeight.Bold)
                            Text(
                                listOfNotNull(unit.type, member.squadLabel).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (unitless > 0) item(key = "$rank senza unità") { UnitlessRow(rank, unitless) }
        }
    }

    sheet?.let { member ->
        UnitSheetDialog(vm.catalog, member, onDismiss = { sheet = null }) {
            SheetActions(vm, state, member, onDone = { sheet = null })
        }
    }
}

/** Chi non ha un'unità non ha una matricola: niente nome e niente scheda, solo quanti sono. */
@Composable
private fun UnitlessRow(rank: String, players: Int) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RankBadge(rank, size = 36.dp)
            Column {
                Text(if (rank == RANK_SUPREME) rank else "Senza unità", fontWeight = FontWeight.Bold)
                Text(
                    if (rank == RANK_SUPREME) "$players Game Master" else "$players player",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** I Capitani votano, il Comando Supremo vede l'andamento e decide dalla scheda GM. Chi l'ha creata può chiuderla. */
@Composable
private fun ProposalCard(vm: GameViewModel, state: GameState, proposal: RankProposal) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    Panel(Modifier.fillMaxWidth(), accent = Signal) {
        Text(
            "${proposal.matricola}: da ${proposal.fromRank} a ${proposal.newRank}",
            style = MaterialTheme.typography.titleMedium,
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
        if (proposal.canClose) {
            OutlinedButton(onClick = { vm.closeProposal(proposal.id) }, enabled = !busy) { Text("Chiudi votazione") }
        }
    }
}

/** Soldati e Caporali chiedono di entrare, il Capitano propone i cambi di grado. */
@Composable
private fun SheetActions(vm: GameViewModel, state: GameState, member: TeamMember, onDone: () -> Unit) {
    val matricola = member.unit.matricola
    if (HierarchyRules.canRequestSquad(state, member)) {
        Button(onClick = { vm.requestSquad(matricola); onDone() }) { Text("Chiedi di entrare") }
    }
    if (state.rank != RANK_CAPTAIN || !member.unit.approved || matricola == state.unit?.matricola) return
    if (state.proposals.any { it.matricola == matricola }) return

    HierarchyRules.nextRanks(member.rank).forEach { rank ->
        OutlinedButton(onClick = { vm.proposeRank(matricola, rank); onDone() }) { Text("Proponi $rank") }
    }
}
