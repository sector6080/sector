package com.sector6080.app.ui.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Enemy
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.MapSite
import com.sector6080.app.model.Order
import com.sector6080.app.model.TeamMember
import com.sector6080.app.model.deployed
import com.sector6080.app.model.markedCells
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.rules.Vision
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.components.Button
import com.sector6080.app.ui.components.OutlinedButton
import com.sector6080.app.ui.components.drawHostileSymbol
import com.sector6080.app.ui.components.drawUnitSymbol
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Flare
import com.sector6080.app.ui.theme.Fog
import com.sector6080.app.ui.theme.Friend
import com.sector6080.app.ui.theme.Hostile
import com.sector6080.app.ui.theme.Ink
import com.sector6080.app.ui.theme.Signal
import com.sector6080.app.ui.theme.Steel
import com.sector6080.app.ui.theme.Void
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val CELL = 24f

private fun middle(cell: Cell) = Offset((cell.x - 0.5f) * CELL, (cell.y - 0.5f) * CELL)

/** Flare delle caselle marcate: raggi che pulsano attorno a un nucleo chiaro. */
private fun DrawScope.drawFlare(cell: Cell, size: Float, pulse: Float, stroke: Float) {
    val center = middle(cell)
    drawCircle(Flare.copy(alpha = 0.4f - 0.25f * pulse), radius = size * (0.45f + 0.35f * pulse), center = center)
    repeat(8) { i ->
        val angle = i * PI.toFloat() / 4f
        val direction = Offset(cos(angle), sin(angle))
        val length = size * (if (i % 2 == 0) 0.62f else 0.42f) * (0.85f + 0.25f * pulse)
        drawLine(Flare, center + direction * (size * 0.16f), center + direction * length, stroke, StrokeCap.Round)
    }
    drawCircle(Ink, radius = size * 0.12f, center = center)
}

/** Angoli del mirino attorno alla casella scelta. */
private fun DrawScope.drawBrackets(cell: Cell, stroke: Float) {
    val left = (cell.x - 1) * CELL
    val top = (cell.y - 1) * CELL
    val arm = CELL * 0.35f
    listOf(
        Offset(left, top) to Offset(arm, arm),
        Offset(left + CELL, top) to Offset(-arm, arm),
        Offset(left, top + CELL) to Offset(arm, -arm),
        Offset(left + CELL, top + CELL) to Offset(-arm, -arm),
    ).forEach { (corner, inward) ->
        drawLine(Ink, corner, corner + Offset(inward.x, 0f), stroke)
        drawLine(Ink, corner, corner + Offset(0f, inward.y), stroke)
    }
}
private val Target = Color(0xFF5CF08A)

/**
 * Mappa con zoom (pizzico) e spostamento (trascinamento).
 * La nebbia di guerra scurisce le caselle che nessuna unità della squadra vede.
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
    val mapImage by vm.mapImage.collectAsStateWithLifecycle()
    val occupied = state.occupiedCells()
    val marked = state.markedCells()
    val sim = unit?.let { OrderRules.simulate(vm.catalog, it, state.orders, occupied, marked).finalUnit }
    val here = unit?.position
    val planned = sim?.position
    // La propria unità si vede anche quando è ancora in attesa
    val members = when {
        unit == null || state.deployed.any { it.unit.matricola == unit.matricola } -> state.deployed
        else -> state.deployed + TeamMember(unit, state.rank, state.pu)
    }
    val allies = members.map { it.unit }.filter { it.matricola != unit?.matricola }
    // Visione comune: la somma delle visioni di tutte le unità
    val sources = members.mapNotNull { member ->
        member.unit.position?.let { Vision.Source(it, UnitRules.maxStats(vm.catalog, member.unit).vision) }
    }
    val fog = remember(sources) { Vision.fogSpans(sources) }
    val valid = remember(picking, unit, state.orders, state.team, state.enemies, state.marks, state.catalog) {
        if (picking != null && unit != null) {
            OrderRules.validTargets(vm.catalog, unit, state.orders, picking, occupied, marked)
        } else {
            emptySet()
        }
    }

    var mapScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<Cell?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var sheet by remember { mutableStateOf<TeamMember?>(null) }
    var siteSheet by remember { mutableStateOf<MapSite?>(null) }
    var enemySheet by remember { mutableStateOf<Enemy?>(null) }

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

    val pulse by rememberInfiniteTransition(label = "segnale").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing)),
        label = "segnale",
    )

    Column(Modifier.fillMaxSize()) {
        TurnBar(state, onExpired = vm::sync)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { canvasSize = it },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
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
                            selected = if (p.x >= 0 && p.y >= 0 && c.inBounds && c != selected) c else null
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    withTransform({
                        translate(offset.x, offset.y)
                        scale(mapScale, mapScale, pivot = Offset.Zero)
                    }) {
                        val cellSize = Size(CELL, CELL)
                        val mapSize = Size(MAP_WIDTH * CELL, MAP_HEIGHT * CELL)
                        val image = mapImage
                        if (image != null) {
                            drawImage(
                                image,
                                dstSize = IntSize(mapSize.width.toInt(), mapSize.height.toInt()),
                                // pixel netti da vicino, immagine morbida quando è rimpicciolita
                                filterQuality = if (mapScale >= 1f) FilterQuality.None else FilterQuality.Low,
                            )
                        } else {
                            drawRect(Steel, Offset.Zero, mapSize)
                        }
                        // Nebbia di guerra: il terreno resta visibile, solo più scuro.
                        fog.forEach { span ->
                            drawRect(
                                Fog.copy(alpha = 0.45f),
                                Offset((span.fromX - 1) * CELL, (span.y - 1) * CELL),
                                Size((span.toX - span.fromX + 1) * CELL, CELL),
                            )
                        }
                        // Griglia: compare solo quando si avvicina la mappa
                        val gridAlpha = ((mapScale - 0.7f) * 0.5f).coerceIn(0f, 0.3f)
                        if (gridAlpha > 0f) {
                            val line = Color.White.copy(alpha = gridAlpha)
                            val width = 1f / mapScale
                            for (x in 0..MAP_WIDTH) drawLine(line, Offset(x * CELL, 0f), Offset(x * CELL, mapSize.height), width)
                            for (y in 0..MAP_HEIGHT) drawLine(line, Offset(0f, y * CELL), Offset(mapSize.width, y * CELL), width)
                        }
                        valid.forEach {
                            val tl = Offset((it.x - 1) * CELL, (it.y - 1) * CELL)
                            drawRect(Target.copy(alpha = 0.35f), tl, cellSize)
                            drawRect(Target, tl, cellSize, style = Stroke(1.5f))
                        }
                        // I simboli restano leggibili anche con la mappa rimpicciolita
                        val symbol = maxOf(CELL, 16.dp.toPx() / mapScale)
                        val stroke = 1.5.dp.toPx() / mapScale
                        val backing = Void.copy(alpha = 0.8f)
                        if (here != null && planned != null && planned != here) {
                            drawLine(
                                Signal,
                                middle(here),
                                middle(planned),
                                stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3, stroke * 3)),
                            )
                            drawCircle(Signal, stroke * 2.5f, middle(planned))
                        }
                        allies.filterNot { it.transported }.forEach { ally ->
                            ally.position?.let { drawUnitSymbol(ally.type, middle(it), symbol, Friend, backing, stroke) }
                        }
                        state.enemies.forEach { drawHostileSymbol(middle(it.position), symbol, Hostile, backing, stroke) }
                        if (unit != null && here != null) {
                            drawUnitSymbol(unit.type, middle(here), symbol, Signal, backing, stroke)
                        }
                        selected?.let { drawBrackets(it, stroke * 1.5f) }
                    }
                }
                // Solo questo strato si ridisegna di continuo: la mappa sotto resta ferma
                Canvas(Modifier.fillMaxSize()) {
                    withTransform({
                        translate(offset.x, offset.y)
                        scale(mapScale, mapScale, pivot = Offset.Zero)
                    }) {
                        val base = maxOf(CELL, 16.dp.toPx() / mapScale)
                        fun ping(cell: Cell, color: Color) = drawCircle(
                            color.copy(alpha = (1f - pulse) * 0.7f),
                            radius = base * (0.7f + pulse * 1.3f),
                            center = middle(cell),
                            style = Stroke(1.5.dp.toPx() / mapScale),
                        )
                        here?.let { ping(it, Signal) }
                        state.enemies.forEach { ping(it.position, Hostile) }
                        state.marks.forEach { drawFlare(it.cell, base, pulse, 1.5.dp.toPx() / mapScale) }
                    }
                }
            }

            // Sta sopra la mappa, così la mappa non si sposta quando compare
            val sel = selected
            if (picking != null || sel != null) {
                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Steel) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (picking != null) {
                            Text(
                                "Ordine: ${picking.type.label}" + (picking.component?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.titleMedium,
                            )
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
                        } else if (sel != null) {
                            val mark = state.marks.firstOrNull { it.cell == sel }
                            Text(
                                "Casella $sel · ${GameMap.describe(sel)}" +
                                    (mark?.let { " · marcata fino al turno ${it.expiresTurn}" } ?: "") +
                                    (here?.let { " · distanza ${it.distance(sel)}" } ?: ""),
                                style = MaterialTheme.typography.labelMedium,
                                color = Signal,
                            )
                            val tapped = members.sortedBy { it.unit.transported }.firstOrNull { it.unit.position == sel }
                            val tappedSite = GameMap.siteAt(sel)
                            val tappedEnemy = state.enemies.firstOrNull { it.position == sel }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (tappedEnemy != null) {
                                    Button(onClick = { enemySheet = tappedEnemy }) { Text(tappedEnemy.name) }
                                } else if (tapped != null) {
                                    Button(onClick = { sheet = tapped }) { Text("Scheda ${tapped.unit.matricola}") }
                                } else if (tappedSite != null) {
                                    Button(onClick = { siteSheet = tappedSite }) { Text(tappedSite.name) }
                                }
                                OutlinedButton(onClick = { selected = null }) { Text("Chiudi") }
                            }
                        }
                    }
                }
            }
        }
    }

    sheet?.let { UnitSheetDialog(vm.catalog, it, onDismiss = { sheet = null }) }
    enemySheet?.let { enemy ->
        AlertDialog(
            onDismissRequest = { enemySheet = null },
            title = { Text(enemy.name) },
            text = {
                Text(
                    "Nemico · HP ${enemy.hp} · Armatura ${enemy.armor} · casella ${enemy.position}" +
                        if (enemy.description.isEmpty()) "" else "\n\n${enemy.description}",
                )
            },
            confirmButton = { TextButton(onClick = { enemySheet = null }) { Text("Chiudi") } },
        )
    }
    siteSheet?.let { site ->
        AlertDialog(
            onDismissRequest = { siteSheet = null },
            title = { Text(site.name) },
            text = { Text(site.description.ifEmpty { "Nessuna descrizione." }) },
            confirmButton = { TextButton(onClick = { siteSheet = null }) { Text("Chiudi") } },
        )
    }
}

/** Turno corrente e conto alla rovescia, aggiornato ogni secondo. */
@Composable
private fun TurnBar(state: GameState, onExpired: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val end = state.turnStartedAt + state.turnHours * 3_600_000L
    LaunchedEffect(end) {
        var notified = false
        while (true) {
            now = System.currentTimeMillis()
            if (state.turnStartedAt > 0 && now >= end && !notified) {
                notified = true
                onExpired()
            }
            delay(1_000)
        }
    }
    val left = ((end - now) / 1_000).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().background(Steel).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TURNO ${state.turn}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (state.turnStartedAt != 0L) {
            Text(
                if (left == 0L) {
                    "cambio turno in corso"
                } else {
                    "prossimo turno in ${left / 3600}h ${left / 60 % 60}m ${left % 60}s"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (left < 3600) Danger else Signal,
            )
        }
    }
}
