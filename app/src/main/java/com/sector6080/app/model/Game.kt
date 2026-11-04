package com.sector6080.app.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

const val MAP_WIDTH = 80
const val MAP_HEIGHT = 59
const val STARTING_BUDGET = 4
const val GOOGLE_BONUS_PU = 1
const val DEFAULT_TURN_HOURS = 48

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
    val player: String,
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
    /** Falso per le unità create offline nelle vecchie versioni: vanno caricate sul server. */
    val synced: Boolean = false,
)

/** Unità approvata di un altro player: tutti possono guardarne la scheda. */
@Serializable
data class TeamMember(val unit: GameUnit, val rank: String, val pu: Int)

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
    /** PU e grado appartengono al player e restano se l'unità viene persa. */
    val pu: Int = STARTING_BUDGET,
    val rank: String = "Recluta",
    val chat: List<ChatMessage> = emptyList(),
    /** Unità approvate di tutta la squadra, compresa la propria. */
    val team: List<TeamMember> = emptyList(),
)
