package com.sector6080.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector6080.app.model.RANK_CAPTAIN
import com.sector6080.app.model.RANK_CORPORAL
import com.sector6080.app.model.RANK_SOLDIER
import com.sector6080.app.ui.theme.Danger
import com.sector6080.app.ui.theme.Friend
import com.sector6080.app.ui.theme.Ink
import com.sector6080.app.ui.theme.InkDim
import com.sector6080.app.ui.theme.Line
import com.sector6080.app.ui.theme.LineBright
import com.sector6080.app.ui.theme.OnSignal
import com.sector6080.app.ui.theme.Signal
import com.sector6080.app.ui.theme.Steel
import com.sector6080.app.ui.theme.SteelHigh
import com.sector6080.app.ui.theme.Success
import com.sector6080.app.ui.theme.Void
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton

val PanelShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
private val ControlShape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
private val ControlPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)

/** Griglia tattica sotto a tutte le schermate. */
fun Modifier.tacticalBackdrop(): Modifier = drawBehind {
    drawRect(Void)
    val step = 28.dp.toPx()
    val grid = Line.copy(alpha = 0.22f)
    var x = 0f
    while (x < size.width) {
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Void), center, size.maxDimension * 0.7f))
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ControlShape,
        contentPadding = ControlPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = Signal,
            contentColor = OnSignal,
            disabledContainerColor = SteelHigh,
            disabledContentColor = InkDim,
        ),
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ControlShape,
        contentPadding = ControlPadding,
        border = BorderStroke(1.dp, if (enabled) LineBright else Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink, disabledContentColor = InkDim),
        content = content,
    )
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Steel,
            labelColor = Ink,
            selectedContainerColor = Signal,
            selectedLabelColor = OnSignal,
            disabledContainerColor = Void,
            disabledLabelColor = InkDim,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Line,
            selectedBorderColor = Signal,
            disabledBorderColor = Line,
        ),
    )
}

/** Il pannello base: angoli tagliati, bordo sottile. [accent] colora il bordo. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color = Line,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
    val border = BorderStroke(1.dp, accent)
    if (onClick != null) {
        Surface(onClick, modifier, shape = PanelShape, color = Steel, border = border, content = body)
    } else {
        Surface(modifier, shape = PanelShape, color = Steel, border = border, content = body)
    }
}

/** Come [Panel], ma senza margine interno: lo decide chi lo usa. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = PanelShape, color = Steel, border = BorderStroke(1.dp, Line)) {
        Column(content = content)
    }
}

/** Intestazione di sezione: un quadratino ambra, il titolo e una riga fino al bordo. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) leading() else Box(Modifier.size(6.dp).background(Signal))
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = InkDim, letterSpacing = 2.sp)
        Box(Modifier.weight(1f).height(1.dp).background(Line))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = Signal)
    }
}

/** Barra a tacche, come un indicatore di carica. Sotto un terzo diventa rossa, se [warnLow]. */
@Composable
fun StatBar(
    label: String,
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    color: Color = Success,
    warnLow: Boolean = true,
) {
    val ratio = if (max > 0) (value.toFloat() / max).coerceIn(0f, 1f) else 0f
    val fill by animateFloatAsState(ratio, tween(600), label = "bar")
    val tint = if (warnLow && ratio <= 1f / 3f) Danger else color
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = InkDim,
                modifier = Modifier.weight(1f),
            )
            Text("$value/$max", style = MaterialTheme.typography.labelMedium, color = tint)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val segments = max.coerceIn(1, 24)
            val gap = 2.dp.toPx()
            val width = (size.width - gap * (segments - 1)) / segments
            repeat(segments) { i ->
                val left = i * (width + gap)
                val lit = (fill * segments - i).coerceIn(0f, 1f)
                drawRect(Line, Offset(left, 0f), Size(width, size.height))
                if (lit > 0f) drawRect(tint, Offset(left, 0f), Size(width * lit, size.height))
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.border(1.dp, Line, ControlShape).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = InkDim)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Avviso su una riga, con la sua azione a destra. */
@Composable
fun StatusStrip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = ControlShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            )
            trailing?.invoke()
        }
    }
}

/** Schermata vuota con un solo messaggio. */
@Composable
fun CenteredNotice(title: String, text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(Modifier.alpha(0.6f), 72.dp)
        Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text, color = InkDim, textAlign = TextAlign.Center)
    }
}

/** Mirino della Task Force. */
@Composable
fun Logo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size)) {
        val side = this.size.minDimension
        val radius = side * 0.3f
        val stroke = side * 0.045f
        drawCircle(Signal, radius, style = Stroke(stroke))
        drawRect(Ink, center - Offset(stroke, stroke), Size(stroke * 2, stroke * 2))
        listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f).forEach { (dx, dy) ->
            drawLine(
                Ink,
                center + Offset(dx, dy) * (radius * 0.55f),
                center + Offset(dx, dy) * (side * 0.48f),
                stroke,
            )
        }
        val arm = side * 0.14f
        listOf(
            Offset(0f, 0f) to Offset(1f, 1f),
            Offset(side, 0f) to Offset(-1f, 1f),
            Offset(0f, side) to Offset(1f, -1f),
            Offset(side, side) to Offset(-1f, -1f),
        ).forEach { (corner, inward) ->
            drawLine(Signal, corner, corner + Offset(inward.x * arm, 0f), stroke)
            drawLine(Signal, corner, corner + Offset(0f, inward.y * arm), stroke)
        }
    }
}

/** Testata comune: nome del gioco, sezione aperta e, se c'è, grado e PU del player. */
@Composable
fun CommandHeader(section: String, rank: String?, status: String?) {
    Column(Modifier.fillMaxWidth().background(Void).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Logo(size = 20.dp)
            Text("SECTOR 6080", style = MaterialTheme.typography.titleMedium, letterSpacing = 3.sp, maxLines = 1)
            Text(
                "// ${section.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = Signal,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (rank != null) RankBadge(rank, size = 16.dp)
            if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

/** Barra di navigazione in basso. */
@Composable
fun CommandBar(content: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Steel).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun RowScope.CommandTab(
    label: String,
    selected: Boolean,
    glyph: DrawScope.(Color) -> Unit,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(if (selected) Signal else InkDim, tween(180), label = "tab")
    Column(
        Modifier.weight(1f).clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) Signal else Color.Transparent))
        Spacer(Modifier.height(9.dp))
        Canvas(Modifier.size(22.dp)) { glyph(tint) }
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
        Spacer(Modifier.height(9.dp))
    }
}

/** Icone della barra, disegnate per restare nello stesso stile dei simboli sulla mappa. */
object Glyphs {
    private fun DrawScope.line() = size.width * 0.09f

    val unit: DrawScope.(Color) -> Unit = { color ->
        drawUnitSymbol("Fanteria", center, size.width * 0.92f, color, Color.Transparent, line())
    }

    val orders: DrawScope.(Color) -> Unit = { color ->
        listOf(0.2f, 0.5f, 0.8f).forEach { y ->
            val h = size.height * y
            drawRect(color, Offset(size.width * 0.04f, h - size.width * 0.07f), Size(size.width * 0.14f, size.width * 0.14f))
            drawLine(color, Offset(size.width * 0.32f, h), Offset(size.width * 0.96f, h), line())
        }
    }

    val map: DrawScope.(Color) -> Unit = { color ->
        val radius = size.width * 0.3f
        drawCircle(color, radius, style = Stroke(line()))
        listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f).forEach { (dx, dy) ->
            drawLine(color, center + Offset(dx, dy) * (radius * 0.5f), center + Offset(dx, dy) * (size.width * 0.5f), line())
        }
    }

    val team: DrawScope.(Color) -> Unit = { color -> drawRank(RANK_CORPORAL, color) }

    val chat: DrawScope.(Color) -> Unit = { color ->
        val origin = Offset(size.width * 0.2f, size.height * 0.8f)
        drawCircle(color, size.width * 0.09f, origin)
        listOf(0.42f, 0.74f).forEach { radius ->
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = origin - Offset(size.width * radius, size.width * radius),
                size = Size(size.width * radius * 2, size.width * radius * 2),
                style = Stroke(line()),
            )
        }
    }

    val command: DrawScope.(Color) -> Unit = { color -> drawStar(color) }
}

private fun DrawScope.drawStar(color: Color) {
    val outer = size.minDimension * 0.5f
    val inner = outer * 0.42f
    val star = Path()
    repeat(10) { i ->
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val point = center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
        if (i == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
    }
    star.close()
    drawPath(star, color)
}

private fun DrawScope.drawRank(rank: String, color: Color) {
    val w = size.width
    val stroke = Stroke(w * 0.12f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    fun chevron(y: Float) {
        val path = Path().apply {
            moveTo(w * 0.12f, y * w + w * 0.24f)
            lineTo(w * 0.5f, y * w)
            lineTo(w * 0.88f, y * w + w * 0.24f)
        }
        drawPath(path, color, style = stroke)
    }
    when (rank) {
        RANK_SOLDIER -> chevron(0.38f)
        RANK_CORPORAL -> listOf(0.2f, 0.52f).forEach(::chevron)
        RANK_CAPTAIN -> listOf(0.08f, 0.36f, 0.64f).forEach(::chevron)
        "Comando Supremo" -> drawStar(color)
        else -> drawRect(color, Offset(w * 0.3f, w * 0.3f), Size(w * 0.4f, w * 0.4f), style = Stroke(w * 0.1f))
    }
}

/** Mostrine del grado: più sale il grado, più sono le punte. */
@Composable
fun RankBadge(rank: String, modifier: Modifier = Modifier, color: Color = Signal, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) { drawRank(rank, color) }
}

/** Simbolo NATO dell'unità: cornice blu e segno del tipo. Serve anche sulla mappa. */
fun DrawScope.drawUnitSymbol(type: String, center: Offset, width: Float, color: Color, fill: Color, stroke: Float) {
    val height = width * 0.68f
    val topLeft = center - Offset(width / 2, height / 2)
    val box = Size(width, height)
    val bottomRight = topLeft + Offset(width, height)
    fun cross() {
        drawLine(color, topLeft, bottomRight, stroke)
        drawLine(color, Offset(topLeft.x, bottomRight.y), Offset(bottomRight.x, topLeft.y), stroke)
    }
    drawRect(fill, topLeft, box)
    when (type) {
        "Fanteria" -> cross()
        "Motorizzata" -> {
            cross()
            drawLine(color, Offset(center.x, topLeft.y), Offset(center.x, bottomRight.y), stroke)
        }
        "Corazzata" -> drawOval(
            color,
            center - Offset(width * 0.32f, height * 0.26f),
            Size(width * 0.64f, height * 0.52f),
            style = Stroke(stroke),
        )
        else -> drawCircle(color, height * 0.2f, center)
    }
    drawRect(color, topLeft, box, style = Stroke(stroke))
}

/** I nemici sono rombi rossi, come sulle carte militari. */
fun DrawScope.drawHostileSymbol(center: Offset, side: Float, color: Color, fill: Color, stroke: Float) {
    val half = side * 0.62f
    val diamond = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half, center.y)
        close()
    }
    drawPath(diamond, fill)
    drawPath(diamond, color, style = Stroke(stroke))
}

@Composable
fun UnitSymbol(type: String, modifier: Modifier = Modifier, color: Color = Friend, size: Dp = 36.dp) {
    Canvas(modifier.width(size).height(size * 0.68f)) {
        val stroke = 1.5.dp.toPx()
        drawUnitSymbol(type, center, this.size.width - stroke, color, color.copy(alpha = 0.14f), stroke)
    }
}
