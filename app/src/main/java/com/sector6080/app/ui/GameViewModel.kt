package com.sector6080.app.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sector6080.app.data.CatalogLoader
import com.sector6080.app.data.GameRepository
import com.sector6080.app.data.LocalGameRepository
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Azioni del player. Approvazioni, modifiche alle unità e passaggio del turno
 * spettano ai Game Master e non sono qui: avranno una sezione a parte.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: GameRepository = LocalGameRepository(app)

    val catalog: Catalog = CatalogLoader.load(app)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Immagine della mappa con le texture: si disegna una volta sola, in background. */
    private val _mapImage = MutableStateFlow<ImageBitmap?>(null)
    val mapImage: StateFlow<ImageBitmap?> = _mapImage.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repo.load()
            // i salvataggi vecchi possono avere unità mai schierate
            _state.value = saved.copy(
                unit = saved.unit?.let { it.copy(position = it.position ?: GameMap.spawnCell(it.matricola)) },
            )
            _loaded.value = true
        }
        viewModelScope.launch(Dispatchers.Default) {
            _mapImage.value = MapRenderer.render().asImageBitmap()
        }
    }

    private fun edit(block: (GameState) -> GameState) {
        _state.update(block)
        val snapshot = _state.value
        viewModelScope.launch { repo.save(snapshot) }
    }

    fun confirmRulesRead() = edit { it.copy(rulesRead = true) }

    fun createUnit(unit: GameUnit) = edit { it.copy(unit = unit, orders = emptyList(), ordersSent = false) }

    fun addOrder(order: Order) = edit { it.copy(orders = it.orders + order) }

    fun removeOrder(index: Int) = edit { it.copy(orders = it.orders.filterIndexed { i, _ -> i != index }) }

    fun clearOrders() = edit { it.copy(orders = emptyList()) }

    fun sendOrders() = edit { it.copy(ordersSent = true) }

    fun withdrawOrders() = edit { it.copy(ordersSent = false) }

    fun sendChat(text: String) = edit {
        val author = it.unit?.player ?: "Recluta"
        it.copy(chat = it.chat + ChatMessage(author, text.trim(), System.currentTimeMillis()))
    }
}
