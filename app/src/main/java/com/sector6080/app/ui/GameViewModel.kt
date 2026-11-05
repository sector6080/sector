package com.sector6080.app.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sector6080.app.data.GameRepository
import com.sector6080.app.data.LocalGameRepository
import com.sector6080.app.data.RemoteGame
import com.sector6080.app.data.RemoteSnapshot
import com.sector6080.app.data.SystemAlerts
import com.sector6080.app.data.friendlyError
import com.sector6080.app.data.parseMillis
import com.sector6080.app.model.Access
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.Enemy
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GmOrders
import com.sector6080.app.model.Order
import com.sector6080.app.rules.OrderRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_SILENT_FAILURES = 3

/**
 * Azioni del player e dei Game Master. Il server decide unità, PU, turno, catalogo e mappa:
 * qui resta solo una copia sul telefono, più la catena di ordini in preparazione.
 * Ogni azione GM viene controllata di nuovo dal server.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: GameRepository = LocalGameRepository(app)
    private val remote = RemoteGame.shared
    private val syncLock = Mutex()

    @Volatile
    private var mapGeneration = 0
    private var mapLoaded = false
    private var catalogLoaded = false
    private var failedSyncs = 0

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    val catalog: Catalog get() = _state.value.catalog

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Messaggio per il player quando il server non risponde o rifiuta un'azione. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Immagine della mappa con le texture: si disegna una volta sola, in background. */
    private val _mapImage = MutableStateFlow<ImageBitmap?>(null)
    val mapImage: StateFlow<ImageBitmap?> = _mapImage.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repo.load()
            // I primi prototipi salvavano l'unità senza accesso: senza account si può solo leggere
            _state.value = if (saved.unit != null && saved.access == null) {
                saved.copy(access = Access.ANONYMOUS)
            } else {
                saved
            }
            _loaded.value = true
            synchronize()
        }
        drawMap()
    }

    /** Se arriva una mappa nuova mentre si disegna quella vecchia, vince l'ultima. */
    private fun drawMap() {
        val generation = ++mapGeneration
        viewModelScope.launch(Dispatchers.Default) {
            if (generation == 1) _mapImage.value = MapRenderer.preview().asImageBitmap()
            val image = MapRenderer.render().asImageBitmap()
            if (generation == mapGeneration) _mapImage.value = image
        }
    }

    private fun edit(block: (GameState) -> GameState) {
        _state.update(block)
        val snapshot = _state.value
        viewModelScope.launch { repo.save(snapshot) }
    }

    /** Esegue un'azione sul server e poi riallinea il telefono. */
    private fun act(action: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                // Così un'azione fatta subito dopo l'apertura non parte senza account
                if (_state.value.access != null) remote.signIn()
                action()
            }
                .onSuccess { synchronize() }
                .onFailure { _notice.value = friendlyError(it) }
            _busy.value = false
        }
    }

    fun sync() {
        if (_loaded.value) viewModelScope.launch { synchronize() }
    }

    private suspend fun synchronize() = syncLock.withLock {
        runCatching {
            if (!mapLoaded) {
                val map = remote.map()
                mapLoaded = true
                if (GameMap.replace(map.rows, map.sites)) drawMap()
            }
            if (!catalogLoaded) {
                val catalog = remote.catalog()
                catalogLoaded = true
                edit { it.copy(catalog = catalog) }
            }
            // Prima l'accesso: i canali privati e i nemici dipendono dall'account
            val signedIn = _state.value.access != null
            if (signedIn) remote.signIn()
            val chat = remote.chat()
            val team = remote.team()
            val enemies = remote.enemies()
            val settings = remote.settings()
            edit { it.copy(chat = chat, team = team, enemies = enemies, settings = settings) }
            if (signedIn) {
                val snapshot = remote.snapshot()
                val proposals = remote.proposals()
                val requests = remote.squadRequests()
                val notifications = remote.notifications()
                apply(snapshot)
                edit { it.copy(proposals = proposals, squadRequests = requests, notifications = notifications) }
                SystemAlerts.show(getApplication(), notifications)
                if (snapshot.player.isGm) {
                    val units = remote.gmUnits()
                    val orders = remote.gmOrders()
                    val templates = remote.gmEnemyTemplates()
                    edit { it.copy(gmUnits = units, gmOrders = orders, enemyTemplates = templates) }
                }
            }
        }
            .onSuccess {
                failedSyncs = 0
                _notice.value = null
            }
            // Un attimo di rete assente non merita un avviso: conta solo se si ripete
            .onFailure { if (++failedSyncs >= MAX_SILENT_FAILURES) _notice.value = friendlyError(it) }
        Unit
    }

    private fun apply(snapshot: RemoteSnapshot) = edit { s ->
        val sent = snapshot.sentOrders
        val newTurn = snapshot.turn.turn != s.turn
        s.copy(
            pu = snapshot.player.pu,
            rank = snapshot.player.rank,
            isGm = snapshot.player.isGm,
            unit = snapshot.unit,
            turn = snapshot.turn.turn,
            turnStartedAt = snapshot.turn.startedAt?.let(::parseMillis) ?: 0,
            turnHours = snapshot.turn.hours,
            orders = when {
                snapshot.unit == null -> emptyList()
                sent != null -> sent
                newTurn -> emptyList()
                else -> s.orders
            },
            ordersSent = sent != null,
            approvalClosed = s.approvalClosed && snapshot.unit != null,
        )
    }

    fun continueAnonymously() {
        edit { it.copy(access = Access.ANONYMOUS) }
        sync()
    }

    fun confirmRulesRead() = edit { it.copy(rulesRead = true) }

    fun closeApproval() = edit { it.copy(approvalClosed = true) }

    fun setAccountNoticeClosed(closed: Boolean) = edit { it.copy(accountNoticeClosed = closed) }

    fun setSoundOn(on: Boolean) = edit { it.copy(soundOn = on) }

    fun createUnit(matricola: String, type: String, lore: String) = act {
        remote.signIn()
        remote.createUnit(matricola, type, lore)
    }

    fun buyComponent(name: String) = act { remote.buyComponent(name) }

    fun addOrder(order: Order) = edit { it.copy(orders = it.orders + order) }

    fun removeOrder(index: Int) = edit { it.copy(orders = it.orders.filterIndexed { i, _ -> i != index }) }

    fun sendOrders() = act { remote.sendOrders(_state.value.orders) }

    fun withdrawOrders() = act { remote.withdrawOrders() }

    /** Il server firma il messaggio e controlla chi può scrivere nel canale. */
    fun sendChat(channel: ChatChannel, text: String) = act { remote.sendChat(channel.id, text.trim()) }

    fun setSquadMember(matricola: String, member: Boolean) = act { remote.setSquadMember(matricola, member) }

    fun markNotificationsRead() {
        edit { s -> s.copy(notifications = s.notifications.map { it.copy(read = true) }) }
        act { remote.markNotificationsRead() }
    }

    fun setSquadName(name: String) = act { remote.setSquadName(name.trim()) }

    /** Con [leader] nullo ritira la richiesta. */
    fun requestSquad(leader: String?) = act { remote.requestSquad(leader) }

    fun answerSquadRequest(matricola: String, accept: Boolean) = act {
        remote.answerSquadRequest(matricola, accept)
    }

    fun proposeRank(matricola: String, rank: String) = act { remote.proposeRank(matricola, rank) }

    fun voteRank(id: Long, approve: Boolean) = act { remote.voteRank(id, approve) }

    fun closeProposal(id: Long) = act { remote.closeProposal(id) }

    fun gmReview(matricola: String, approve: Boolean) = act { remote.gmReview(matricola, approve) }

    fun gmChangeUnit(matricola: String, hp: Int, supplies: Int) = act {
        remote.gmUpdateUnit(matricola, hp.coerceAtLeast(0), supplies.coerceAtLeast(0))
    }

    fun gmMarkLost(matricola: String) = act { remote.gmMarkLost(matricola) }

    /** Applica gli ordini con le stesse regole usate dai player. */
    fun gmApplyOrders(orders: GmOrders) = act {
        val units = _state.value.gmUnits.map { it.member.unit }
        val unit = units.first { it.matricola == orders.matricola }
        val occupied = units.filter { it.matricola != unit.matricola }.mapNotNull { it.position }.toSet() +
            _state.value.enemies.map { it.position }
        remote.gmApplyOrders(orders.turn, OrderRules.simulate(catalog, unit, orders.orders, occupied).finalUnit)
    }

    fun gmNextTurn() = act { remote.gmNextTurn() }

    fun gmSetRank(matricola: String, rank: String) = act { remote.gmSetRank(matricola, rank) }

    fun gmDeleteMessage(id: Long) = act { remote.gmDeleteMessage(id) }

    fun gmSetMuted(matricola: String, muted: Boolean) = act { remote.gmSetMuted(matricola, muted) }

    fun gmMoveUnit(matricola: String, cell: Cell) = act { remote.gmMoveUnit(matricola, cell) }

    fun gmSetComponent(matricola: String, name: String, owned: Boolean) = act {
        remote.gmSetComponent(matricola, name, owned)
    }

    /** Un nemico nuovo ha [Enemy.id] zero. A 0 HP il server lo toglie dalla mappa. */
    fun gmSaveEnemy(enemy: Enemy) = act { remote.gmSaveEnemy(enemy.id.takeIf { it != 0L }, enemy) }

    fun gmDeleteEnemy(id: Long) = act { remote.gmDeleteEnemy(id) }
}
