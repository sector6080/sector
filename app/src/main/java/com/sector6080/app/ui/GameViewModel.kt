package com.sector6080.app.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sector6080.app.data.CatalogLoader
import com.sector6080.app.data.GameRepository
import com.sector6080.app.data.LocalGameRepository
import com.sector6080.app.data.RemoteGame
import com.sector6080.app.data.RemoteSnapshot
import com.sector6080.app.data.friendlyError
import com.sector6080.app.data.parseMillis
import com.sector6080.app.model.Access
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.rules.UnitRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Azioni del player. Il server decide unità, PU, turno e chat: qui resta solo una copia
 * sul telefono, più la catena di ordini in preparazione. Approvazioni, modifiche alle
 * unità e passaggio del turno spettano ai Game Master.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: GameRepository = LocalGameRepository(app)
    private val remote = RemoteGame()
    private val syncLock = Mutex()

    @Volatile
    private var mapGeneration = 0
    private var mapLoaded = false

    val catalog: Catalog = CatalogLoader.load(app)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

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
            // i salvataggi vecchi possono avere unità mai schierate o l'accesso Google simulato
            _state.value = saved.copy(
                unit = saved.unit?.let { it.copy(position = it.position ?: GameMap.spawnCell(it.matricola)) },
                access = Access.ANONYMOUS.takeIf { saved.unit != null || saved.access != null },
            )
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
            runCatching { action() }
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
            val chat = remote.chat()
            val team = remote.team()
            edit { it.copy(chat = chat, team = team) }
            if (_state.value.access != null) {
                remote.signIn()
                val snapshot = remote.snapshot()
                val local = _state.value.unit
                if (snapshot.unit == null && local != null && !local.synced) {
                    remote.createUnit(local, UnitRules.totalCost(catalog, local))
                    apply(remote.snapshot())
                } else {
                    apply(snapshot)
                }
            }
        }
            .onSuccess { _notice.value = null }
            .onFailure { _notice.value = friendlyError(it) }
        Unit
    }

    private fun apply(snapshot: RemoteSnapshot) = edit { s ->
        val sent = snapshot.sentOrders
        val newTurn = snapshot.turn.turn != s.turn
        s.copy(
            pu = snapshot.player.pu,
            rank = snapshot.player.rank,
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
        )
    }

    fun continueAnonymously() {
        edit { it.copy(access = Access.ANONYMOUS) }
        sync()
    }

    fun confirmRulesRead() = edit { it.copy(rulesRead = true) }

    fun createUnit(unit: GameUnit, cost: Int) = act {
        remote.signIn()
        remote.createUnit(unit, cost)
    }

    fun addOrder(order: Order) = edit { it.copy(orders = it.orders + order) }

    fun removeOrder(index: Int) = edit { it.copy(orders = it.orders.filterIndexed { i, _ -> i != index }) }

    fun clearOrders() = edit { it.copy(orders = emptyList()) }

    fun sendOrders() = act { remote.sendOrders(_state.value.orders) }

    fun withdrawOrders() = act { remote.withdrawOrders() }

    /** Il server firma il messaggio con la matricola e rifiuta chi non ha un'unità. */
    fun sendChat(text: String) = act { remote.sendChat(text.trim()) }
}
