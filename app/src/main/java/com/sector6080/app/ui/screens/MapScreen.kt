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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sector6080.app.model.Cell
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.MapSite
import com.sector6080.app.model.Order
import com.sector6080.app.model.TeamMember
import com.sector6080.app.model.deployed
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.rules.Vision
import com.sector6080.app.ui.GameViewModel
import com.sector6080.app.ui.theme.Accent
import com.sector6080.app.ui.theme.AccentLight
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Fog
import com.sector6080.app.ui.theme.Navy
import com.sector6080.app.ui.theme.Warning
import kotlinx.coroutines.delay

private const val CELL = 24f
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
    val sim = unit?.let { OrderRules.simulate(vm.catalog, it, state.orders, occupied).finalUnit }
    val vision = unit?.let { UnitRules.maxStats(vm.catalog, it).vision } ?: 0
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
    val valid = remember(picking, unit, state.orders, state.team, state.catalog) {
        if (picking != null && unit != null) {
            OrderRules.validTargets(vm.catalog, unit, state.orders, picking, occupied)
        } else {
            emptySet()
        }
    }

    var mapScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<Cell?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showTeam by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<TeamMember?>(null) }
    var siteSheet by remember { mutableStateOf<MapSite?>(null) }

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
        TurnBar(state, onExpired = vm::sync)
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
                    drawRect(Navy, Offset.Zero, mapSize)
                }
                // Nebbia di guerra: il terreno resta visibile, solo più scuro.
                fog.forEach { span ->
                    drawRect(
                        Fog.copy(alpha = 0.5f),
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
                fun mark(c: Cell, color: Color, inset: Float) = drawRect(
                    color,
                    Offset((c.x - 1) * CELL + inset, (c.y - 1) * CELL + inset),
                    Size(CELL - 2 * inset, CELL - 2 * inset),
                )
                if (planned != null && planned != here) mark(planned, Warning.copy(alpha = 0.8f), 4f)
                allies.forEach { ally ->
                    ally.position?.let {
                        mark(it, Color.White, 2f)
                        mark(it, Target, 4f)
                    }
                }
                if (here != null) {
                    mark(here, Color.White, 2f)
                    mark(here, Accent, 4f)
                }
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
                            state.isGm -> "Comando Supremo: visione della squadra."
                            unit == null -> "Crea un'unità per vederla sulla mappa."
                            here == null -> "Unità non ancora schierata."
                            else -> "Unità in $here · visione $vision" +
                                (if (planned != here) " · dopo gli ordini: $planned" else "")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (sel != null) {
                        Text("Casella $sel · ${GameMap.describe(sel)}" + (here?.let { " · distanza ${it.distance(sel)}" } ?: ""))
                    }
                    val tapped = members.firstOrNull { it.unit.position == sel }
                    val tappedSite = sel?.let { GameMap.siteAt(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showTeam = true }) { Text("Squadra (${members.size})") }
                        if (tapped != null) {
                            Button(onClick = { sheet = tapped }) { Text("Scheda ${tapped.unit.matricola}") }
                        } else if (tappedSite != null) {
                            Button(onClick = { siteSheet = tappedSite }) { Text(tappedSite.name) }
                        }
                    }
                }
            }
        }
    }

    if (showTeam) {
        TeamListDialog(
            team = members,
            onOpen = { sheet = it; showTeam = false },
            onDismiss = { showTeam = false },
        )
    }
    sheet?.let { UnitSheetDialog(vm.catalog, it) { sheet = null } }
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
    Surface(tonalElevation = 3.dp) {
        Text(
            when {
                state.turnStartedAt == 0L -> "Turno ${state.turn}"
                left == 0L -> "Turno ${state.turn} · cambio turno in corso"
                else -> "Turno ${state.turn} · prossimo turno in ${left / 3600}h ${left / 60 % 60}m ${left % 60}s"
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        )
    }
}
