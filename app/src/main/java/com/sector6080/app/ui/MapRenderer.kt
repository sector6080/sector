package com.sector6080.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.sector6080.app.model.Cell
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.MapSite
import com.sector6080.app.model.Terrain
import kotlin.math.floor
import kotlin.math.sin

/**
 * Disegna la mappa con texture procedurali: nessun file di immagine, i terreni
 * nascono da rumore casuale ma sempre uguale, così la mappa resta identica a ogni avvio.
 * Le coste sono leggermente irregolari, le strutture restano allineate alle caselle.
 */
object MapRenderer {
    /** Pixel per casella dell'immagine. */
    const val TILE = 24

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private val GRASS_DARK = rgb(52, 98, 46)
    private val GRASS_LIGHT = rgb(104, 150, 74)
    private val SAND_DARK = rgb(190, 164, 108)
    private val SAND_LIGHT = rgb(226, 204, 148)
    private val SEA_DEEP = rgb(18, 58, 108)
    private val SEA_LIGHT = rgb(34, 92, 152)
    private val RIVER_DEEP = rgb(44, 112, 176)
    private val RIVER_LIGHT = rgb(86, 160, 214)
    private val FOAM = rgb(222, 234, 232)
    private val WET_SAND = rgb(168, 150, 104)

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(s: Int) = (((a shr s) and 255) + (((b shr s) and 255) - ((a shr s) and 255)) * k).toInt()
        return rgb(ch(16), ch(8), ch(0))
    }

    private fun hash(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
    }

    private fun noise(x: Float, y: Float): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val fx = x - xi
        val fy = y - yi
        val u = fx * fx * (3 - 2 * fx)
        val v = fy * fy * (3 - 2 * fy)
        val top = hash(xi, yi) + (hash(xi + 1, yi) - hash(xi, yi)) * u
        val bottom = hash(xi, yi + 1) + (hash(xi + 1, yi + 1) - hash(xi, yi + 1)) * u
        return top + (bottom - top) * v
    }

    private fun fbm(x: Float, y: Float) =
        noise(x, y) * 0.55f + noise(x * 2.1f, y * 2.1f) * 0.3f + noise(x * 4.3f, y * 4.3f) * 0.15f

    /** Terreno "naturale" sotto una coordinata: le strutture si disegnano dopo, sopra. */
    private fun ground(u: Float, v: Float): Terrain {
        val c = Cell((floor(u).toInt() + 1).coerceIn(1, MAP_WIDTH), (floor(v).toInt() + 1).coerceIn(1, MAP_HEIGHT))
        return when (val t = GameMap.terrainAt(c)) {
            Terrain.BRIDGE -> Terrain.RIVER
            Terrain.PORT, Terrain.BUILDING -> Terrain.SAND
            else -> t
        }
    }

    private fun Terrain.isWater() = this == Terrain.SEA || this == Terrain.RIVER

    private fun texel(px: Int, py: Int): Int {
        val u = (px + 0.5f) / TILE
        val v = (py + 0.5f) / TILE
        // leggera deformazione: le coste non seguono il profilo a gradini delle caselle
        val wu = u + (noise(u * 1.7f, v * 1.7f) - 0.5f) * 0.5f
        val wv = v + (noise(u * 1.7f + 40f, v * 1.7f + 9f) - 0.5f) * 0.5f
        val terrain = ground(wu, wv)

        val near = 0.16f
        val nearWater = !terrain.isWater() && (
            ground(wu + near, wv).isWater() || ground(wu - near, wv).isWater() ||
                ground(wu, wv + near).isWater() || ground(wu, wv - near).isWater()
            )
        val nearLand = terrain.isWater() && (
            !ground(wu + near, wv).isWater() || !ground(wu - near, wv).isWater() ||
                !ground(wu, wv + near).isWater() || !ground(wu, wv - near).isWater()
            )

        val shade = fbm(u * 0.9f, v * 0.9f)
        val grain = hash(px, py)
        return when {
            nearLand -> if (terrain == Terrain.SEA) mix(FOAM, SEA_LIGHT, grain * 0.7f) else mix(FOAM, RIVER_LIGHT, 0.45f + grain * 0.4f)
            nearWater -> mix(WET_SAND, SAND_DARK, grain)
            terrain == Terrain.SEA -> {
                val wave = sin((v * 2.6f + shade * 5f) * 3.1416f)
                val base = mix(SEA_DEEP, SEA_LIGHT, shade * 1.1f)
                if (wave > 0.96f && grain > 0.35f) mix(base, FOAM, 0.12f) else base
            }
            terrain == Terrain.RIVER -> {
                val flow = sin((v * 4f + u * 0.7f + shade * 3f) * 3.1416f)
                mix(RIVER_DEEP, RIVER_LIGHT, shade * 0.7f + (if (flow > 0.7f) 0.35f else 0f))
            }
            terrain == Terrain.SAND -> {
                val base = mix(SAND_DARK, SAND_LIGHT, fbm(u * 1.3f, v * 1.3f) * 1.25f - 0.1f)
                if (grain > 0.9f) mix(base, SAND_DARK, 0.45f) else base
            }
            else -> {
                val base = mix(GRASS_DARK, GRASS_LIGHT, fbm(u * 1.1f, v * 1.1f) * 1.3f - 0.15f)
                when {
                    grain > 0.93f -> mix(base, GRASS_LIGHT, 0.5f)
                    grain < 0.06f -> mix(base, GRASS_DARK, 0.6f)
                    else -> base
                }
            }
        }
    }

    fun render(): Bitmap {
        val w = MAP_WIDTH * TILE
        val h = MAP_HEIGHT * TILE
        val pixels = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) pixels[y * w + x] = texel(x, y)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        val canvas = Canvas(bitmap)
        for (x in 1..MAP_WIDTH) for (y in 1..MAP_HEIGHT) {
            if (GameMap.terrainAt(Cell(x, y)) == Terrain.BRIDGE) drawBridge(canvas, x, y)
        }
        GameMap.ports.forEach { drawPort(canvas, it) }
        GameMap.refineries.forEach { drawRefinery(canvas, it) }
        GameMap.warehouses.forEach { drawWarehouse(canvas, it) }
        return bitmap
    }

    private val fill = Paint().apply { isAntiAlias = false }

    private fun Canvas.box(l: Float, t: Float, r: Float, b: Float, color: Int) {
        fill.color = color
        drawRect(l, t, r, b, fill)
    }

    private fun bounds(s: MapSite) = RectF(
        (s.x - 1f) * TILE, (s.y - 1f) * TILE, (s.x - 1f + s.w) * TILE, (s.y - 1f + s.h) * TILE,
    )

    private fun drawBridge(c: Canvas, x: Int, y: Int) {
        fun isLand(cx: Int, cy: Int) = Cell(cx, cy).inBounds && GameMap.terrainAt(Cell(cx, cy)) != Terrain.RIVER
        val l = (x - 1f) * TILE
        val t = (y - 1f) * TILE
        val wood = rgb(142, 96, 56)
        val dark = rgb(92, 60, 34)
        // il ponte attraversa il fiume: le assi vanno da una sponda all'altra
        val northSouth = isLand(x, y - 1) || isLand(x, y + 1)
        if (northSouth) {
            c.box(l + 4, t - 2, l + TILE - 4, t + TILE + 2, wood)
            c.box(l + 4, t - 2, l + 6, t + TILE + 2, dark)
            c.box(l + TILE - 6, t - 2, l + TILE - 4, t + TILE + 2, dark)
            for (i in 0 until TILE step 4) c.box(l + 6, t + i, l + TILE - 6, t + i + 1, dark)
        } else {
            c.box(l - 2, t + 4, l + TILE + 2, t + TILE - 4, wood)
            c.box(l - 2, t + 4, l + TILE + 2, t + 6, dark)
            c.box(l - 2, t + TILE - 6, l + TILE + 2, t + TILE - 4, dark)
            for (i in 0 until TILE step 4) c.box(l + i, t + 6, l + i + 1, t + TILE - 6, dark)
        }
    }

    private fun drawPort(c: Canvas, s: MapSite) {
        val r = bounds(s)
        c.box(r.left, r.top, r.right, r.bottom, rgb(120, 82, 50))
        var y = r.top
        while (y < r.bottom) {
            c.box(r.left, y, r.right, y + 1, rgb(84, 56, 34))
            y += 5
        }
        c.box(r.left, r.top, r.left + 2, r.bottom, rgb(68, 46, 28))
        c.box(r.right - 2, r.top, r.right, r.bottom, rgb(68, 46, 28))
        // gru di carico
        val cx = r.left + (r.right - r.left) * 0.5f
        val cy = r.top + TILE * 1.5f
        fill.color = rgb(232, 176, 44)
        c.drawRect(cx - 2, cy - 14, cx + 2, cy + 14, fill)
        c.drawRect(cx - 14, cy - 14, cx + 14, cy - 10, fill)
        c.box(cx + 10, cy - 10, cx + 12, cy - 2, rgb(50, 50, 54))
        c.box(cx + 8, cy - 2, cx + 14, cy + 3, rgb(176, 62, 48))
    }

    private fun drawRefinery(c: Canvas, s: MapSite) {
        val r = bounds(s)
        c.box(r.left + 1, r.top + 1, r.right - 1, r.bottom - 1, rgb(122, 126, 130))
        for (i in 0..2) {
            c.box(r.left + 1, r.top + 12 + i * 12, r.right - 1, r.top + 13 + i * 12, rgb(100, 104, 108))
        }
        fun tank(cx: Float, cy: Float, radius: Float) {
            fill.isAntiAlias = true
            fill.color = rgb(70, 74, 78)
            c.drawCircle(cx, cy, radius + 1, fill)
            fill.color = rgb(214, 216, 212)
            c.drawCircle(cx, cy, radius, fill)
            fill.color = rgb(170, 174, 172)
            c.drawCircle(cx + 2, cy + 2, radius * 0.45f, fill)
            fill.isAntiAlias = false
        }
        tank(r.left + 13, r.top + 13, 9f)
        tank(r.left + 34, r.top + 13, 9f)
        tank(r.left + 13, r.top + 34, 9f)
        // ciminiera con fiamma
        c.box(r.left + 30, r.top + 28, r.left + 36, r.top + 44, rgb(64, 66, 70))
        c.box(r.left + 31, r.top + 23, r.left + 35, r.top + 29, rgb(236, 120, 40))
        c.box(r.left + 32, r.top + 21, r.left + 34, r.top + 25, rgb(252, 204, 72))
    }

    private fun drawWarehouse(c: Canvas, s: MapSite) {
        val r = bounds(s)
        c.box(r.left + 1, r.top + 3, r.right - 1, r.bottom - 2, rgb(64, 76, 92))
        c.box(r.left + 2, r.top + 4, r.right - 2, r.bottom - 4, rgb(116, 138, 160))
        c.box(r.left + 2, r.top + 11, r.right - 2, r.top + 13, rgb(84, 102, 122))
        var x = r.left + 4
        while (x < r.right - 3) {
            c.box(x, r.top + 4, x + 1, r.bottom - 4, rgb(96, 116, 136))
            x += 4
        }
        c.box(r.left + 8, r.bottom - 7, r.left + 16, r.bottom - 2, rgb(216, 164, 60))
    }
}
