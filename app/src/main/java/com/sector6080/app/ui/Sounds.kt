package com.sector6080.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.sector6080.app.R

enum class Sfx(@RawRes val resource: Int, val volume: Float) {
    CLICK(R.raw.sfx_click, 0.5f),
    TAB(R.raw.sfx_tab, 0.4f),
    ERROR(R.raw.sfx_error, 0.6f),
}

/** Effetti brevi caricati in anticipo, così partono subito al tocco. */
class SoundBoard(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loaded = Sfx.entries.associateWith { pool.load(context, it.resource, 1) }

    var enabled = true

    fun play(sfx: Sfx) {
        if (enabled) pool.play(loaded.getValue(sfx), sfx.volume, sfx.volume, 1, 0, 1f)
    }

    fun release() = pool.release()
}

val LocalSounds = staticCompositionLocalOf<SoundBoard?> { null }
