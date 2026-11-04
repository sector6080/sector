package com.sector6080.app.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

const val MAP_WIDTH = 80
const val MAP_HEIGHT = 60
const val GOOGLE_BONUS_PU = 1
const val DEFAULT_TURN_HOURS = 24

/** Casella della mappa, coordinate numeriche 1-based come nel template ordini (X,Y). */
@Serializable
data class Cell(val x: Int, val y: Int) {
    val inBounds get() = x in 1..MAP_WIDTH && y in 1..MAP_HEIGHT

    /** Distanza senza diagonali (regolamento: movimento e range solo orizzontale/verticale). */
    fun distance(o: Cell) = abs(x - o.x) + abs(y - o.y)

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
)

/** Unità della squadra, approvata o in attesa: tutti possono guardarne la scheda. */
@Serializable
data class TeamMember(val unit: GameUnit, val rank: String, val pu: Int)

/** Vista del Game Master. [pending] indica un'unità in attesa di approvazione. */
@Serializable
data class GmUnit(val member: TeamMember, val pending: Boolean, val cost: Int)

/** Ordini inviati da un'unità e non ancora applicati. */
@Serializable
data class GmOrders(val matricola: String, val turn: Int, val orders: List<Order>)

@Serializable
enum class OrderType(val label: String) {
    MOVE("Muoversi"),
    BOARD("Salire su mezzo"),
    DISEMBARK("Scendere da mezzo"),
    ACTIVATE("Attivare componente"),
    RELOAD("Ricaricare componente"),
    HEAL("Curarsi"),
    GIVE("Cedere rifornimenti"),
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

@Serializable
data class ChatMessage(
    val author: String,
    val text: String,
    val sentAt: Long,
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
    /** Avvisi chiusi con la X. */
    val approvalClosed: Boolean = false,
    val accountNoticeClosed: Boolean = false,
    val chat: List<ChatMessage> = emptyList(),
    /** Tutte le unità, compresa la propria, dal grado più alto. Comprende quelle in attesa. */
    val team: List<TeamMember> = emptyList(),
    val gmUnits: List<GmUnit> = emptyList(),
    val gmOrders: List<GmOrders> = emptyList(),
)

/** Unità in gioco: quelle in attesa non compaiono sulla mappa e non vedono. */
val GameState.deployed: List<TeamMember> get() = team.filter { it.unit.approved }

/** Caselle occupate dalle altre unità: bloccano movimento e tiro. */
fun GameState.occupiedCells(): Set<Cell> =
    deployed.filter { it.unit.matricola != unit?.matricola }.mapNotNull { it.unit.position }.toSet()
