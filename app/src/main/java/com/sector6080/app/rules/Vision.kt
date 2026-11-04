package com.sector6080.app.rules

import com.sector6080.app.model.Cell
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH

/** Tratto continuo di caselle coperte dalla nebbia su una riga. */
data class FogSpan(val y: Int, val fromX: Int, val toX: Int)

/**
 * Visione condivisa: la squadra vede ciò che vede almeno una delle sue unità.
 * Non esiste una visione per singola unità.
 */
object Vision {
    data class Source(val position: Cell, val range: Int)

    fun isVisible(sources: List<Source>, cell: Cell) =
        sources.any { it.position.distance(cell) <= it.range }

    /** Senza unità sulla mappa non c'è nebbia da calcolare. */
    fun fogSpans(sources: List<Source>): List<FogSpan> {
        if (sources.isEmpty()) return emptyList()
        return buildList {
            for (y in 1..MAP_HEIGHT) {
                var start: Int? = null
                for (x in 1..MAP_WIDTH + 1) {
                    val foggy = x <= MAP_WIDTH && !isVisible(sources, Cell(x, y))
                    val open = start
                    if (foggy && open == null) start = x
                    if (!foggy && open != null) {
                        add(FogSpan(y, open, x - 1))
                        start = null
                    }
                }
            }
        }
    }
}
