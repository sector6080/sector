package com.sector6080.app.rules

import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.GameState
import com.sector6080.app.model.RANK_CAPTAIN
import com.sector6080.app.model.RANK_CORPORAL
import com.sector6080.app.model.RANK_SOLDIER
import com.sector6080.app.model.TeamMember

/** Decide cosa mostrare a ogni grado. Il server ricontrolla ogni azione. */
object HierarchyRules {
    const val MAX_SQUAD = 6
    const val MAX_SQUAD_NAME = 8

    /** Costituzione, art. 1: si cambia un grado alla volta. */
    fun nextRanks(rank: String): List<String> = when (rank) {
        RANK_SOLDIER -> listOf(RANK_CORPORAL)
        RANK_CORPORAL -> listOf(RANK_CAPTAIN, RANK_SOLDIER)
        RANK_CAPTAIN -> listOf(RANK_CORPORAL)
        else -> emptyList()
    }

    /** Eventi e Comune sono per tutti: gli altri canali compaiono solo a chi ne fa parte. */
    fun canSee(state: GameState, channel: ChatChannel): Boolean = when (channel) {
        ChatChannel.EVENTS, ChatChannel.COMMON -> true
        ChatChannel.SQUAD -> state.isGm || inSquad(state)
        ChatChannel.COMMAND -> state.isGm || state.rank == RANK_CAPTAIN || state.rank == RANK_CORPORAL
    }

    /** Testo senza numeri, massimo 8 caratteri. */
    fun isValidSquadName(name: String): Boolean =
        name.trim().length in 1..MAX_SQUAD_NAME && name.none(Char::isDigit)

    fun squadTitle(name: String?): String = if (name != null) "Squadrone \"$name\"" else "Squadrone senza nome"

    private fun me(state: GameState) = state.team.firstOrNull { it.unit.matricola == state.unit?.matricola }

    fun squadName(state: GameState): String? = me(state)?.squadName

    /** Soldati dello squadrone di cui fa parte il player. */
    fun squadMembers(state: GameState): List<TeamMember> {
        val leader = if (state.rank == RANK_CORPORAL) state.unit?.matricola else me(state)?.squad
        return state.team.filter { leader != null && it.squad == leader }
    }

    /** Soldati approvati che un Caporale può ancora scegliere. */
    fun freeSoldiers(state: GameState): List<TeamMember> =
        state.team.filter { it.rank == RANK_SOLDIER && it.squad == null && it.unit.approved }

    /** Il Caporale guida il proprio squadrone, il Soldato deve essere scelto. */
    fun inSquad(state: GameState): Boolean = state.rank == RANK_CORPORAL ||
        state.team.any { it.unit.matricola == state.unit?.matricola && it.squad != null }

    /** Perché non si può scrivere nel canale, oppure null se si può. */
    fun whyNotWrite(state: GameState, channel: ChatChannel): String? = when (channel) {
        ChatChannel.EVENTS -> "Qui scrive solo il Comando Supremo.".takeUnless { state.isGm }
        ChatChannel.COMMON ->
            "Puoi solo leggere: per scrivere crea prima un'unità.".takeUnless { state.isGm || state.unit != null }
        ChatChannel.SQUAD -> "Il Comando Supremo può solo leggere gli squadroni.".takeIf { state.isGm }
        ChatChannel.COMMAND -> null
    }
}
