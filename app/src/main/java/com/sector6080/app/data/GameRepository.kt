package com.sector6080.app.data

import android.content.Context
import com.sector6080.app.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Copia locale dei dati di gioco. Il server resta la fonte di verità:
 * vedi [RemoteGame].
 */
interface GameRepository {
    suspend fun load(): GameState
    suspend fun save(state: GameState)
}

class LocalGameRepository(context: Context) : GameRepository {
    private val file = File(context.filesDir, "game_state.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true }

    override suspend fun load(): GameState = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(GameState.serializer(), file.readText()) }.getOrDefault(GameState())
    }

    override suspend fun save(state: GameState) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(GameState.serializer(), state))
        tmp.renameTo(file)
        Unit
    }
}
