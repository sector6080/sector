package com.sector6080.app.data

import com.sector6080.app.model.Cell
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.MapSite
import com.sector6080.app.model.Order
import com.sector6080.app.model.TeamMember
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime

private const val SUPABASE_URL = "https://jnfqfowidqwrmmvxnvhe.supabase.co"

/** Chiave pubblica per definizione: la sicurezza sta nelle regole del database. */
private const val SUPABASE_KEY = "sb_publishable_Lbh2m9zIjUCwEF6PVNbc4g_XkdJ7Gcz"

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class PlayerRow(val pu: Int, val rank: String)

@Serializable
data class UnitRow(
    val matricola: String,
    @SerialName("player_name") val playerName: String,
    val type: String,
    val components: List<String>,
    val lore: String,
    val hp: Int,
    val supplies: Int,
    val uses: Map<String, Int>,
    @SerialName("pos_x") val posX: Int,
    @SerialName("pos_y") val posY: Int,
    val transported: Boolean,
    val status: String,
) {
    fun toGameUnit() = GameUnit(
        matricola = matricola,
        player = playerName,
        type = type,
        components = components,
        lore = lore,
        hp = hp,
        supplies = supplies,
        uses = uses,
        position = Cell(posX, posY),
        transported = transported,
        approved = status == "active",
        synced = true,
    )
}

/** Unità della squadra con grado e PU del player. Il proprietario resta privato. */
@Serializable
data class TeamRow(
    val matricola: String,
    @SerialName("player_name") val playerName: String,
    val rank: String,
    val pu: Int,
    val type: String,
    val components: List<String>,
    val lore: String,
    val hp: Int,
    val supplies: Int,
    val uses: Map<String, Int>,
    @SerialName("pos_x") val posX: Int,
    @SerialName("pos_y") val posY: Int,
    val transported: Boolean,
) {
    fun toTeamMember() = TeamMember(
        GameUnit(
            matricola = matricola,
            player = playerName,
            type = type,
            components = components,
            lore = lore,
            hp = hp,
            supplies = supplies,
            uses = uses,
            position = Cell(posX, posY),
            transported = transported,
            approved = true,
            synced = true,
        ),
        rank,
        pu,
    )
}

@Serializable
data class MapData(val rows: List<String>, val sites: List<MapSite>)

@Serializable
data class TurnRow(
    val turn: Int,
    @SerialName("started_at") val startedAt: String? = null,
    val hours: Int,
)

@Serializable
private data class OrdersRow(val orders: List<Order>)

@Serializable
data class ChatRow(
    val author: String,
    val body: String,
    @SerialName("sent_at") val sentAt: String,
) {
    fun toChatMessage() = ChatMessage(author, body, parseMillis(sentAt))
}

fun parseMillis(timestamp: String): Long = OffsetDateTime.parse(timestamp).toInstant().toEpochMilli()

/** Stato del player sul server. [sentOrders] è null se non ha inviato ordini in questo turno. */
data class RemoteSnapshot(
    val player: PlayerRow,
    val unit: GameUnit?,
    val turn: TurnRow,
    val sentOrders: List<Order>?,
)

/**
 * Accesso a Supabase. Le letture sono limitate dalle regole RLS al solo player
 * collegato, e ogni scrittura passa da una funzione del database (`supabase/schema-app.sql`).
 */
class RemoteGame {
    private val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        defaultSerializer = KotlinXSerializer(json)
        install(Auth)
        install(Postgrest)
    }

    /** Riprende la sessione salvata o crea un account anonimo. */
    suspend fun signIn() {
        client.auth.awaitInitialization()
        if (client.auth.currentUserOrNull() == null) client.auth.signInAnonymously()
    }

    suspend fun snapshot(): RemoteSnapshot {
        val player = client.from("players").select().decodeSingle<PlayerRow>()
        val unit = client.from("game_units").select {
            filter { isIn("status", listOf("pending", "active")) }
        }.decodeSingleOrNull<UnitRow>()
        val turn = client.from("game_turn").select().decodeSingle<TurnRow>()
        val sent = client.from("game_orders").select {
            filter { eq("turn", turn.turn) }
        }.decodeSingleOrNull<OrdersRow>()
        return RemoteSnapshot(player, unit?.toGameUnit(), turn, sent?.orders)
    }

    suspend fun createUnit(unit: GameUnit, cost: Int) {
        val position = requireNotNull(unit.position)
        client.postgrest.rpc("create_unit", createUnitParams(unit, cost, position))
    }

    suspend fun sendOrders(orders: List<Order>) {
        val params = buildJsonObject {
            put("p_orders", json.encodeToJsonElement(ListSerializer(Order.serializer()), orders))
        }
        client.postgrest.rpc("send_orders", params)
    }

    suspend fun withdrawOrders() {
        client.postgrest.rpc("withdraw_orders")
    }

    suspend fun sendChat(text: String) {
        client.postgrest.rpc("send_chat", buildJsonObject { put("p_body", text) })
    }

    /** Si legge anche senza account. */
    suspend fun map(): MapData = client.postgrest.rpc("get_map").decodeAs()

    /** Unità approvate di tutti i player: si legge anche senza account. */
    suspend fun team(): List<TeamMember> =
        client.postgrest.rpc("get_team").decodeList<TeamRow>().map { it.toTeamMember() }

    /** Ultimi messaggi, dal più vecchio al più recente. La chat si legge anche senza account. */
    suspend fun chat(): List<ChatMessage> = client.from("chat_messages").select {
        order("id", SortOrder.DESCENDING)
        limit(CHAT_HISTORY)
    }.decodeList<ChatRow>().asReversed().map { it.toChatMessage() }

    private companion object {
        const val CHAT_HISTORY = 100L
    }
}

fun createUnitParams(unit: GameUnit, cost: Int, position: Cell): JsonObject = buildJsonObject {
    put("p_matricola", unit.matricola)
    put("p_player_name", unit.player)
    put("p_type", unit.type)
    put("p_components", json.encodeToJsonElement(ListSerializer(String.serializer()), unit.components))
    put("p_lore", unit.lore)
    put("p_cost", cost)
    put("p_hp", unit.hp)
    put("p_supplies", unit.supplies)
    put("p_uses", buildJsonObject { unit.uses.forEach { (name, left) -> put(name, left) } })
    put("p_x", position.x)
    put("p_y", position.y)
}

/** Trasforma gli errori del server e della rete in un messaggio per il player. */
fun friendlyError(error: Throwable): String {
    val text = error.message.orEmpty()
    return when {
        "PU insufficienti" in text -> "PU insufficienti per questa unità."
        "game_units_matricola_key" in text -> "Matricola già usata: scegline un'altra."
        "one_live_unit_per_player" in text -> "Hai già un'unità attiva o in attesa."
        "Unita non attiva" in text -> "Solo un'unità approvata può inviare ordini."
        "Serve una unita" in text -> "Per scrivere serve un'unità attiva o in attesa."
        "Messaggi troppo ravvicinati" in text -> "Aspetta un attimo prima di scrivere ancora."
        "signups" in text.lowercase() || "anonymous" in text.lowercase() -> "Accesso senza account non disponibile."
        "Could not find" in text || "does not exist" in text -> "Il server non è ancora pronto."
        else -> "Connessione al server non riuscita."
    }
}
