package com.sector6080.app.rules

import com.sector6080.app.data.GM_RANK
import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.GameState
import com.sector6080.app.model.RANK_CAPTAIN
import com.sector6080.app.model.RANK_CORPORAL
import com.sector6080.app.model.RANK_SOLDIER
import com.sector6080.app.model.SquadRequest
import com.sector6080.app.model.TeamMember

/** I due livelli: i Soldati entrano nel gruppo di un Caporale, il gruppo nello squadrone di un Capitano. */
enum class Formation(
    val channel: ChatChannel,
    val label: String,
    val leaderRank: String,
    val memberRank: String,
    val limitKey: String,
) {
    GROUP(ChatChannel.GROUP, "Gruppo", RANK_CORPORAL, RANK_SOLDIER, "squad_limit_corporal"),
    SQUADRON(ChatChannel.SQUAD, "Squadrone", RANK_CAPTAIN, RANK_CORPORAL, "squad_limit_captain"),
}

/** Decide cosa mostrare a ogni grado. Il server ricontrolla ogni azione. */
object HierarchyRules {
    const val MAX_SQUAD_NAME = 8
    private val SIGNATURE = Regex("""\(([A-Z]{3}-\d{3})\)""")
    private val EVERYONE = Regex("""(^|\s)@tutti(\s|$)""", RegexOption.IGNORE_CASE)

    /** La matricola nella firma di un messaggio: il Comando Supremo non ne ha. */
    fun authorMatricola(author: String): String? = SIGNATURE.find(author)?.groupValues?.get(1)

    /** Il messaggio cita il player, oppure è un @tutti del Comando Supremo. */
    fun mentionsMe(state: GameState, message: ChatMessage): Boolean {
        val mine = state.unit?.matricola
        return (mine != null && "@$mine" in message.text.uppercase()) ||
            (message.author == GM_RANK && EVERYONE.containsMatchIn(message.text))
    }

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
        ChatChannel.GROUP -> state.isGm || inFormation(state, Formation.GROUP)
        ChatChannel.SQUAD -> state.isGm || inFormation(state, Formation.SQUADRON)
        ChatChannel.COMMAND -> state.isGm || state.rank == RANK_CAPTAIN || state.rank == RANK_CORPORAL
    }

    /** Testo senza numeri, massimo 8 caratteri. */
    fun isValidSquadName(name: String): Boolean =
        name.trim().length in 1..MAX_SQUAD_NAME && name.none(Char::isDigit)

    fun title(formation: Formation, name: String?): String =
        if (name != null) "${formation.label} \"$name\"" else "${formation.label} senza nome"

    private fun me(state: GameState) = state.team.firstOrNull { it.unit.matricola == state.unit?.matricola }

    private fun TeamMember.leader(formation: Formation) = if (formation == Formation.GROUP) group else squadron

    private fun leader(state: GameState, formation: Formation) = me(state)?.leader(formation)

    fun inFormation(state: GameState, formation: Formation): Boolean = leader(state, formation) != null

    fun name(state: GameState, formation: Formation): String? =
        me(state)?.let { if (formation == Formation.GROUP) it.groupName else it.squadronName }

    /** Chi entra direttamente: Soldati nel gruppo, Caporali nello squadrone. */
    fun members(state: GameState, formation: Formation): List<TeamMember> {
        val leader = leader(state, formation) ?: return emptyList()
        return state.team.filter { it.rank == formation.memberRank && it.leader(formation) == leader }
    }

    /** Unità che occupano un posto: nello squadrone contano anche i Soldati dei gruppi. */
    fun size(state: GameState, formation: Formation): Int {
        val leader = leader(state, formation) ?: return 0
        return state.team.count { it.leader(formation) == leader && it.unit.matricola != leader }
    }

    fun limit(state: GameState, formation: Formation): Int? = state.settings[formation.limitKey]

    fun soldiersOf(state: GameState, corporal: TeamMember): Int =
        state.team.count { it.rank == RANK_SOLDIER && it.group == corporal.unit.matricola }

    /** Chi il capo della formazione può ancora scegliere. */
    fun free(state: GameState, formation: Formation): List<TeamMember> = state.team.filter {
        it.rank == formation.memberRank && it.unit.approved && it.leader(formation) == null
    }

    /** I Soldati chiedono di entrare in un gruppo, i Caporali in uno squadrone. */
    fun canRequestSquad(state: GameState, target: TeamMember): Boolean {
        val formation = Formation.entries.firstOrNull { it.memberRank == state.rank } ?: return false
        val mine = me(state) ?: return false
        return mine.unit.approved && mine.leader(formation) == null &&
            target.rank == formation.leaderRank && target.unit.approved &&
            outgoingRequest(state)?.leader != target.unit.matricola
    }

    fun outgoingRequest(state: GameState): SquadRequest? =
        state.squadRequests.firstOrNull { it.matricola == state.unit?.matricola }

    fun incomingRequests(state: GameState): List<SquadRequest> =
        state.squadRequests.filter { it.leader == state.unit?.matricola }

    /** Perché non si può scrivere nel canale, oppure null se si può. */
    fun whyNotWrite(state: GameState, channel: ChatChannel): String? = when (channel) {
        ChatChannel.EVENTS -> "Qui scrive solo il Comando Supremo.".takeUnless { state.isGm }
        ChatChannel.COMMON ->
            "Puoi solo leggere: per scrivere crea prima un'unità.".takeUnless { state.isGm || state.unit != null }
        ChatChannel.GROUP, ChatChannel.SQUAD -> "Il Comando Supremo qui può solo leggere.".takeIf { state.isGm }
        ChatChannel.COMMAND -> null
    }
}
