package com.sector6080.app.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

const val MAP_WIDTH = 80
const val MAP_HEIGHT = 60
const val RANK_SUPREME = "Comando Supremo"
const val RANK_RECRUIT = "Recluta"
const val GOOGLE_BONUS_PU = 1
const val DEFAULT_TURN_HOURS = 24
const val RANK_SOLDIER = "Soldato"
const val RANK_CORPORAL = "Caporale"
const val RANK_CAPTAIN = "Capitano"

/** Casella della mappa, coordinate numeriche 1-based come nel template ordini (X,Y). */
@Serializable
data class Cell(val x: Int, val y: Int) {
    val inBounds get() = x in 1..MAP_WIDTH && y in 1..MAP_HEIGHT

    /** Distanza senza diagonali (regolamento: movimento e range solo orizzontale/verticale). */
    fun distance(o: Cell) = abs(x - o.x) + abs(y - o.y)

    /** Distanza contando una diagonale come un passo. */
    fun diagonalDistance(o: Cell) = maxOf(abs(x - o.x), abs(y - o.y))

    override fun toString() = "$x,$y"
}

@Serializable
data class GameUnit(
    val matricola: String,
    val type: String,
    val components: List<String>,
    val lore: String,
    val hp: Int,
    val supplies: Int,
    /** Utilizzi rimasti per componente. */
    val uses: Map<String, Int>,
    val position: Cell? = null,
    val transported: Boolean = false,
    /** Solo un Game Master può approvare un'unità. */
    val approved: Boolean = false,
    /** Matricola del mezzo su cui viaggia. */
    val carrier: String? = null,
    /** Armatura in più di questo momento, calcolata dal server: Bunker e Sistema ECM. */
    val armorBonus: Int = 0,
)

/** Unità della squadra, approvata o in attesa: tutti possono guardarne la scheda. */
@Serializable
data class TeamMember(
    val unit: GameUnit,
    val rank: String,
    val pu: Int,
    /** Matricola del Caporale che guida il gruppo. */
    val group: String? = null,
    val groupName: String? = null,
    /** Matricola del Capitano che guida lo squadrone. */
    val squadron: String? = null,
    val squadronName: String? = null,
) {
    /** Testo mostrato accanto all'unità. */
    val squadLabel: String?
        get() {
            fun part(label: String, leader: String?, name: String?) = when {
                name != null -> "$label \"$name\""
                leader != null -> "$label senza nome"
                else -> null
            }
            val parts = listOfNotNull(part("gruppo", group, groupName), part("squadrone", squadron, squadronName))
            return parts.joinToString(" · ").ifEmpty { null }
        }
}

/** Vista del Game Master. [pending] indica un'unità in attesa di approvazione. */
@Serializable
data class GmUnit(val member: TeamMember, val pending: Boolean, val muted: Boolean = false)

/** Player visto dal Comando Supremo: chi non ha un'unità non ha una matricola. */
@Serializable
data class GmPlayer(val id: String, val matricola: String?, val rank: String, val pu: Int, val muted: Boolean)

/** Ordini inviati da un'unità e non ancora applicati. */
@Serializable
data class GmOrders(val matricola: String, val turn: Int, val orders: List<Order>)

/** Casella marcata: sulla mappa ha un flare, vale fino al turno indicato. */
@Serializable
data class Mark(val cell: Cell, val expiresTurn: Int)

/** Le azioni [instant] partono subito, le altre allo scadere del turno. */
@Serializable
enum class OrderType(val label: String, val instant: Boolean = false) {
    MOVE("Muoversi"),
    BOARD("Salire su mezzo", instant = true),
    DISEMBARK("Scendere da mezzo", instant = true),
    ACTIVATE("Attivare componente"),
    RELOAD("Ricaricare componente"),
    HEAL("Curarsi"),
    GIVE("Cedere rifornimenti", instant = true),
}

@Serializable
data class Order(
    val type: OrderType,
    /** Casella scelta sulla mappa. */
    val target: Cell? = null,
    val component: String? = null,
    val quantity: Int? = null,
)

/** Come è entrato il player. L'accesso Google non è ancora disponibile. */
@Serializable
enum class Access { ANONYMOUS, GOOGLE }

/** Il server decide chi legge e chi scrive in ogni canale. */
@Serializable
enum class ChatChannel(val id: String, val label: String) {
    EVENTS("eventi", "Eventi"),
    COMMON("comune", "Comune"),
    GROUP("gruppo", "Gruppo"),
    SQUAD("squadrone", "Squadrone"),
    COMMAND("comando", "Comando"),
}

@Serializable
data class ChatMessage(
    val author: String,
    val text: String,
    val sentAt: Long,
    val id: Long = 0,
    val channel: String = ChatChannel.COMMON.id,
)

/** Il telefono riceve solo i nemici dentro la visione comune: il GM li riceve tutti. */
@Serializable
data class Enemy(
    val id: Long,
    val name: String,
    val hp: Int,
    val armor: Int,
    val description: String,
    val position: Cell,
)

/** Le crea il server: promozioni, nuovo turno, menzioni in chat e altri eventi. */
@Serializable
data class GameNotification(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean,
)

/** Modello tra cui il GM sceglie quando fa comparire un nemico. */
@Serializable
data class EnemyTemplate(val name: String, val hp: Int, val armor: Int, val description: String)

/** Soldato o Caporale che chiede di entrare nel gruppo o nello squadrone di [leader]. */
@Serializable
data class SquadRequest(val matricola: String, val type: String, val leader: String)

/** Cambio di grado in votazione tra i Capitani. */
@Serializable
data class RankProposal(
    val id: Long,
    val matricola: String,
    val fromRank: String,
    val newRank: String,
    val proposedBy: String?,
    val votesYes: Int,
    val votesNo: Int,
    val myVote: Boolean?,
    val canClose: Boolean = false,
)

@Serializable
data class GameState(
    val unit: GameUnit? = null,
    val turn: Int = 0,
    val turnStartedAt: Long = 0,
    val turnHours: Int = DEFAULT_TURN_HOURS,
    val orders: List<Order> = emptyList(),
    /** Catena consegnata al Game Master: non è modificabile finché non viene ritirata. */
    val ordersSent: Boolean = false,
    /** Il player ha confermato di aver letto il regolamento. */
    val rulesRead: Boolean = false,
    /** Null finché il player non sceglie come entrare. */
    val access: Access? = null,
    /** I PU appartengono al player e restano se l'unità viene persa. Il grado no. */
    val pu: Int = 0,
    val rank: String = "Recluta",
    /** Si diventa GM solo dalla console di Supabase. */
    val isGm: Boolean = false,
    /** Tipi di unità e componenti ricevuti dal server. */
    val catalog: Catalog = Catalog(),
    /** Valori della tabella game_settings, per esempio i posti negli squadroni. */
    val settings: Map<String, Int> = emptyMap(),
    /** Avvisi chiusi con la X. */
    val approvalClosed: Boolean = false,
    /** Effetti sonori dell'app. Il suono delle notifiche dipende dalle impostazioni di Android. */
    val soundVolume: Float = 1f,
    val chat: List<ChatMessage> = emptyList(),
    /** Tutte le unità, compresa la propria, dal grado più alto. Comprende quelle in attesa. */
    val team: List<TeamMember> = emptyList(),
    /** Membri senza unità per grado: non hanno una matricola, quindi resta solo il numero. */
    val unitless: Map<String, Int> = emptyMap(),
    val enemies: List<Enemy> = emptyList(),
    val marks: List<Mark> = emptyList(),
    val proposals: List<RankProposal> = emptyList(),
    /** Richieste fatte o ricevute dal player. */
    val squadRequests: List<SquadRequest> = emptyList(),
    val notifications: List<GameNotification> = emptyList(),
    val gmUnits: List<GmUnit> = emptyList(),
    val gmOrders: List<GmOrders> = emptyList(),
    val gmPlayers: List<GmPlayer> = emptyList(),
    val enemyTemplates: List<EnemyTemplate> = emptyList(),
)

/** Unità in gioco: quelle in attesa non compaiono sulla mappa e non vedono. */
val GameState.deployed: List<TeamMember> get() = team.filter { it.unit.approved }

/** Caselle occupate dalle altre unità e dai nemici visibili: bloccano movimento e tiro. */
fun GameState.occupiedCells(): Set<Cell> =
    deployed.filter { it.unit.matricola != unit?.matricola }.mapNotNull { it.unit.position }.toSet() +
        enemies.map { it.position }

fun GameState.markedCells(): Set<Cell> = marks.map { it.cell }.toSet()
