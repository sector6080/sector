package com.sector6080.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.Cell
import com.sector6080.app.model.GameState
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.Order
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Accent
import com.sector6080.app.ui.theme.AccentLight
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Fog
import com.sector6080.app.ui.theme.Navy
import com.sector6080.app.ui.theme.Warning

private const val CELL = 24f

/**
 * Griglia 80×60 con zoom (pizzico) e spostamento (trascinamento).
 * La nebbia di guerra copre le caselle oltre la visione dell'unità (senza diagonali).
 * Parte con tutta la mappa in vista. Con [picking] si avvicina all'unità, evidenzia
 * le caselle valide e permette di confermare solo quelle.
 */
@Composable
fun MapScreen(
    vm: GameViewModel,
    state: GameState,
    picking: Order?,
    onConfirm: (Order) -> Unit,
    onCancel: () -> Unit,
) {
    val unit = state.unit
    val sim = unit?.let { OrderRules.simulate(vm.catalog, it, state.orders).finalUnit }
    val vision = unit?.let { UnitRules.maxStats(vm.catalog, it).vision } ?: 0
    val here = unit?.position
    val planned = sim?.position
    val valid = remember(picking, unit, state.orders) {
        if (picking != null && unit != null) {
            OrderRules.validTargets(vm.catalog, unit, state.orders, picking)
        } else {
            emptySet()
        }
    }

    var mapScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<Cell?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun fitScale() = canvasSize.width / (MAP_WIDTH * CELL)

    LaunchedEffect(canvasSize, picking) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        val focus = planned.takeIf { picking != null }
        if (focus != null) {
            val reach = valid.maxOfOrNull { it.distance(focus) } ?: 1
            val cells = (2 * reach + 3) * CELL
            val scale = (minOf(canvasSize.width, canvasSize.height) / cells).coerceIn(fitScale(), 3f)
            mapScale = scale
            offset = Offset(
                canvasSize.width / 2f - (focus.x - 0.5f) * CELL * scale,
                canvasSize.height / 2f - (focus.y - 0.5f) * CELL * scale,
            )
        } else {
            mapScale = fitScale()
            offset = Offset(0f, ((canvasSize.height - MAP_HEIGHT * CELL * mapScale) / 2).coerceAtLeast(0f))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (mapScale * zoom).coerceIn(fitScale(), 4f)
                        // zoom attorno al punto tra le dita
                        offset = centroid - (centroid - offset) * (newScale / mapScale) + pan
                        mapScale = newScale
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val p = (tap - offset) / mapScale
                        val c = Cell((p.x / CELL).toInt() + 1, (p.y / CELL).toInt() + 1)
                        selected = if (p.x >= 0 && p.y >= 0 && c.inBounds) c else null
                    }
                },
        ) {
            withTransform({
                translate(offset.x, offset.y)
                scale(mapScale, mapScale, pivot = Offset.Zero)
            }) {
                val cellSize = Size(CELL, CELL)
                for (x in 1..MAP_WIDTH) for (y in 1..MAP_HEIGHT) {
                    val c = Cell(x, y)
                    val visible = here != null && here.distance(c) <= vision
                    val tl = Offset((x - 1) * CELL, (y - 1) * CELL)
                    drawRect(if (visible || here == null) Navy else Fog, tl, cellSize)
                    drawRect(Color(0x33FFFFFF), tl, cellSize, style = Stroke(0.5f))
                }
                valid.forEach {
                    drawRect(Accent.copy(alpha = 0.4f), Offset((it.x - 1) * CELL, (it.y - 1) * CELL), cellSize)
                }
                fun mark(c: Cell, color: Color, inset: Float) = drawRect(
                    color,
                    Offset((c.x - 1) * CELL + inset, (c.y - 1) * CELL + inset),
                    Size(CELL - 2 * inset, CELL - 2 * inset),
                )
                if (planned != null && planned != here) mark(planned, Warning.copy(alpha = 0.7f), 4f)
                if (here != null) mark(here, Accent, 3f)
                selected?.let {
                    drawRect(AccentLight, Offset((it.x - 1) * CELL, (it.y - 1) * CELL), cellSize, style = Stroke(3f))
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val sel = selected
                if (picking != null) {
                    Text("Ordine: ${picking.type.label}" + (picking.component?.let { " · $it" } ?: ""),
                        fontWeight = FontWeight.SemiBold)
                    when {
                        sel == null -> Text("Tocca una casella evidenziata.")
                        sel in valid -> Text(OrderRules.describe(picking.copy(target = sel)))
                        else -> Text("Casella $sel non valida per questo ordine.", color = Danger)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCancel) { Text("Annulla") }
                        Button(
                            onClick = { sel?.let { onConfirm(picking.copy(target = it)) } },
                            enabled = sel != null && sel in valid,
                        ) { Text("Conferma") }
                    }
                } else {
                    Text(
                        when {
                            unit == null -> "Crea un'unità per vederla sulla mappa."
                            here == null -> "Unità non ancora schierata."
                            else -> "Unità in $here · visione $vision" +
                                (if (planned != here) " · dopo gli ordini: $planned" else "")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (sel != null) {
                        Text("Casella $sel" + (here?.let { " · distanza ${it.distance(sel)}" } ?: ""))
                    }
                }
            }
        }
    }
}
