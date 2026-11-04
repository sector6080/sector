package com.sector6080.app.data

import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.Component
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.GmOrders
import com.sector6080.app.model.GmUnit
import com.sector6080.app.model.MapSite
import com.sector6080.app.model.Order
import com.sector6080.app.model.Stats
import com.sector6080.app.model.TeamMember
import com.sector6080.app.model.UnitType
import com.sector6080.app.model.Weapon
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

/** I Game Master sono il Comando Supremo: il grado si imposta solo dalla console. */
const val GM_RANK = "Comando Supremo"

@Serializable
data class PlayerRow(val pu: Int, val rank: String) {
    /** Serve solo a mostrare la scheda GM: ogni azione viene ricontrollata dal server. */
    val isGm get() = rank == GM_RANK
}

@Serializable
data class UnitTypeRow(
    val name: String,
    val cost: Int,
    val hp: Int,
    val movement: Int,
    val vision: Int,
    val armor: Int,
    val supplies: Int,
)

@Serializable
data class ComponentRow(
    val type: String,
    val name: String,
    val cost: Int,
    val description: String,
    val hp: Int = 0,
    val movement: Int = 0,
    val vision: Int = 0,
    val armor: Int = 0,
    val supplies: Int = 0,
    val perforation: Int? = null,
    val damage: Int? = null,
    val hits: Int? = null,
    @SerialName("area_w") val areaW: Int? = null,
    @SerialName("area_h") val areaH: Int? = null,
    val range: Int? = null,
    @SerialName("max_uses") val maxUses: Int? = null,
    @SerialName("reload_cost") val reloadCost: Int = 1,
    val stationary: Boolean = false,
    val requires: String? = null,
) {
    fun toComponent() = Component(
        name = name,
        unitType = type,
        cost = cost,
        description = description,
        statMods = Stats(hp, movement, vision, armor, supplies),
        weapon = if (damage != null) {
            Weapon(
                perforation = perforation ?: 0,
                damage = damage,
                hits = hits ?: 1,
                area = if (areaW != null && areaH != null) areaW to areaH else null,
            )
        } else {
            null
        },
        range = range,
        maxUses = maxUses,
        reloadCost = reloadCost,
        requiresStationary = stationary,
        requires = requires,
    )
}

fun buildCatalog(types: List<UnitTypeRow>, components: List<ComponentRow>) = Catalog(
    types.map { row ->
        UnitType(
            name = row.name,
            creationCost = row.cost,
            base = Stats(row.hp, row.movement, row.vision, row.armor, row.supplies),
            components = components.filter { it.type == row.name }.sortedBy { it.cost }.map { it.toComponent() },
        )
    },
)

@Serializable
data class UnitRow(
    val matricola: String,
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
        type = type,
        components = components,
        lore = lore,
        hp = hp,
        supplies = supplies,
        uses = uses,
        position = Cell(posX, posY),
        transported = transported,
        approved = status == "active",
    )
}

/** Unità della squadra con grado e PU del player. Il proprietario resta privato. */
@Serializable
data class TeamRow(
    val matricola: String,
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
    val status: String = "active",
    val cost: Int = 0,
) {
    fun toGmUnit() = GmUnit(toTeamMember(), status == "pending", cost)

    fun toTeamMember() = TeamMember(
        GameUnit(
            matricola = matricola,
            type = type,
            components = components,
            lore = lore,
            hp = hp,
            supplies = supplies,
            uses = uses,
            position = Cell(posX, posY),
            transported = transported,
            approved = status == "active",
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
private data class GmOrdersRow(val matricola: String, val turn: Int, val orders: List<Order>)

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
 * collegato, e ogni scrittura passa da una funzione del database (cartella `supabase`).
 * Tipi di unità, componenti e mappa stanno sul server, così si modificano senza aggiornare l'app.
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

    /** Il server calcola costo, statistiche e porto di partenza. */
    suspend fun createUnit(matricola: String, type: String, components: List<String>, lore: String) {
        client.postgrest.rpc("create_unit", createUnitParams(matricola, type, components, lore))
    }

    suspend fun buyComponent(name: String) {
        client.postgrest.rpc("buy_component", buildJsonObject { put("p_name", name) })
    }

    suspend fun catalog(): Catalog = buildCatalog(
        client.from("unit_types").select().decodeList<UnitTypeRow>().sortedBy { it.cost },
        client.from("game_components").select().decodeList<ComponentRow>(),
    )

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

    suspend fun gmUnits(): List<GmUnit> = client.postgrest.rpc("gm_units").decodeList<TeamRow>().map { it.toGmUnit() }

    suspend fun gmOrders(): List<GmOrders> = client.postgrest.rpc("gm_orders").decodeList<GmOrdersRow>()
        .map { GmOrders(it.matricola, it.turn, it.orders) }

    suspend fun gmReview(matricola: String, approve: Boolean) {
        client.postgrest.rpc(
            "gm_review_unit",
            buildJsonObject {
                put("p_matricola", matricola)
                put("p_approve", approve)
            },
        )
    }

    suspend fun gmUpdateUnit(matricola: String, hp: Int, supplies: Int) {
        client.postgrest.rpc(
            "gm_update_unit",
            buildJsonObject {
                put("p_matricola", matricola)
                put("p_hp", hp)
                put("p_supplies", supplies)
            },
        )
    }

    suspend fun gmMarkLost(matricola: String) {
        client.postgrest.rpc("gm_mark_lost", buildJsonObject { put("p_matricola", matricola) })
    }

    suspend fun gmApplyOrders(turn: Int, result: GameUnit) {
        val position = requireNotNull(result.position)
        client.postgrest.rpc(
            "gm_apply_orders",
            buildJsonObject {
                put("p_matricola", result.matricola)
                put("p_turn", turn)
                put("p_hp", result.hp)
                put("p_supplies", result.supplies)
                put("p_uses", buildJsonObject { result.uses.forEach { (name, left) -> put(name, left) } })
                put("p_x", position.x)
                put("p_y", position.y)
                put("p_transported", result.transported)
            },
        )
    }

    suspend fun gmNextTurn() {
        client.postgrest.rpc("gm_next_turn")
    }

    /** Si legge anche senza account. */
    suspend fun map(): MapData = client.postgrest.rpc("get_map").decodeAs()

    /** Unità approvate e in attesa, dal grado più alto: si legge anche senza account. */
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

fun createUnitParams(
    matricola: String,
    type: String,
    components: List<String>,
    lore: String,
): JsonObject = buildJsonObject {
    put("p_matricola", matricola)
    put("p_type", type)
    put("p_components", json.encodeToJsonElement(ListSerializer(String.serializer()), components))
    put("p_lore", lore)
}

/** Trasforma gli errori del server e della rete in un messaggio per il player. */
fun friendlyError(error: Throwable): String {
    val text = error.message.orEmpty()
    return when {
        "PU insufficienti" in text -> "PU insufficienti per questa unità."
        "game_units_matricola_key" in text -> "Matricola già usata: scegline un'altra."
        "one_live_unit_per_player" in text -> "Hai già un'unità attiva o in attesa."
        "Unita non attiva" in text -> "Solo un'unità approvata può inviare ordini."
        "Serve una unita" in text -> "Serve un'unità attiva o in attesa."
        "Prerequisito mancante" in text -> "Prima devi avere il componente richiesto."
        "Componente gia posseduto" in text -> "Hai già questo componente."
        "Componente non disponibile" in text || "Componente duplicato" in text -> "Componente non disponibile."
        "Porti occupati" in text -> "Tutti i porti sono occupati: riprova più tardi."
        "Solo Game Master" in text -> "Azione riservata ai Game Master."
        "Comando Supremo non puo" in text -> "Il Comando Supremo non può avere unità."
        "Messaggi troppo ravvicinati" in text -> "Aspetta un attimo prima di scrivere ancora."
        "signups" in text.lowercase() || "anonymous" in text.lowercase() -> "Accesso senza account non disponibile."
        "Could not find" in text || "does not exist" in text -> "Il server non è ancora pronto."
        else -> "Connessione al server non riuscita."
    }
}
