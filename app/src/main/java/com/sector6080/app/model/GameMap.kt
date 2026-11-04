package com.sector6080.app.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Terrain(val label: String, val walkable: Boolean) {
    SEA("Mare", false),
    RIVER("Fiume", false),
    BRIDGE("Ponte", true),
    PORT("Porto", true),
    SAND("Sabbia", true),
    GRASS("Prato", true),
    BUILDING("Edificio", true),
}

/** Struttura sulla mappa: rettangolo con angolo in alto a sinistra in (x, y). */
@Serializable
data class MapSite(
    val name: String,
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    val description: String = "",
) {
    operator fun contains(c: Cell) = c.x in x until x + w && c.y in y until y + h
}

/**
 * Mappa di gioco. La versione ufficiale sta nella tabella `game_map` del server:
 * questa copia, presa dal foglio "Sector 6080 (master)", serve finché non arriva.
 * Ogni riga è compressa (es. 49B2A = 49 B e 2 A). K è il bordo nero del foglio.
 * B mare, L fiume, O ponte, A porto, S sabbia, G prato, W edificio.
 */
object GameMap {
    private const val SHEET =
        "82K/K80BK/K80BK/K80BK/K49B2A4S16B9SK/K49B2A12S9B8SK/K49B2A12S9B8SK/K48B16S5B2AB8SK/" +
            "K17B2A27B18S2B3S2A9SK/K10B7S2A16B7S4B23S2A9SK/K10B7S2A4S9B4S2W4S4B34SK/K25S7B4S2W39S2WSK/" +
            "K25SB51S2WSK/K77S3GK/K2S2W57S5G11S3GK/K2S2W47S3G6SW5G11S3GK/K24SW26S18G6S5GK/" +
            "K12SW19S3G12S22G6S5GK/K30S5G7S38GK/K10S26G6S38GK/K9S71GK/K8S72GK/K8S58G14LK/K6S59G6L9GK/" +
            "K5S56G2LO5L11GK/K5S50G6L19GK/K2L3S44G5LOL24GK/K3L2S26G9LO8L6G3L22GK/K2S2LS20G3LO2L25G4L20GK/" +
            "K5L14G6L32G5L18GK/K5LO11LO2L41G2LO3L13GK/K2S3LO4L54G6L10GK/K3S66G11LK/K3S77GK/KG2S77GK/" +
            "K2GS77GK/K2GS77GK/K2GS77GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/" +
            "K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/82K"

    @Volatile
    private var sites: List<MapSite> = listOf(
        MapSite("Porto 1", 18, 8, 2, 3),
        MapSite("Porto 2", 50, 4, 2, 3),
        MapSite("Porto 3", 70, 7, 2, 3),
        MapSite("Raffineria 1", 37, 10, 2, 2),
        MapSite("Raffineria 2", 78, 11, 2, 2),
        MapSite("Raffineria 3", 3, 14, 2, 2),
        MapSite("Magazzino 1", 61, 15),
        MapSite("Magazzino 2", 25, 16),
        MapSite("Magazzino 3", 13, 17),
    )

    @Volatile
    private var grid: List<String> = SHEET.split("/").drop(1).dropLast(1).map { row ->
        Regex("""(\d*)([A-Z])""").findAll(row).joinToString("") { m ->
            m.groupValues[2].repeat(m.groupValues[1].toIntOrNull() ?: 1)
        }.removeSurrounding("K")
    }

    val ports get() = sites.filter { it.name.startsWith("Porto") }
    val refineries get() = sites.filter { it.name.startsWith("Raffineria") }
    val warehouses get() = sites.filter { it.name.startsWith("Magazzino") }

    init {
        require(isValid(grid)) { "Mappa di dimensioni errate" }
    }

    private fun isValid(rows: List<String>) =
        rows.size == MAP_HEIGHT && rows.all { it.length == MAP_WIDTH }

    /**
     * Sostituisce la mappa con quella del server.
     * Restituisce true solo se è valida ed è diversa da quella attuale.
     */
    fun replace(rows: List<String>, newSites: List<MapSite>): Boolean {
        if (!isValid(rows) || (rows == grid && newSites == sites)) return false
        grid = rows
        sites = newSites
        return true
    }

    fun terrainAt(c: Cell): Terrain = when (grid[c.y - 1][c.x - 1]) {
        'L' -> Terrain.RIVER
        'O' -> Terrain.BRIDGE
        'A' -> Terrain.PORT
        'S' -> Terrain.SAND
        'G' -> Terrain.GRASS
        'W' -> Terrain.BUILDING
        else -> Terrain.SEA
    }

    fun siteAt(c: Cell): MapSite? = sites.firstOrNull { c in it }

    fun describe(c: Cell): String = siteAt(c)?.name ?: terrainAt(c).label

    /**
     * Passi per andare da [from] a [to] senza diagonali, senza entrare in acqua
     * e senza attraversare le caselle [blocked], occupate da altre unità.
     * Gli edifici non bloccano. Null se la casella non si raggiunge entro [limit] passi.
     */
    fun steps(from: Cell, to: Cell, limit: Int, blocked: Set<Cell> = emptySet()): Int? {
        if (from.distance(to) > limit) return null
        val seen = hashSetOf(from)
        var frontier = listOf(from)
        for (step in 1..limit) {
            val next = mutableListOf<Cell>()
            for (c in frontier) {
                for (n in listOf(Cell(c.x + 1, c.y), Cell(c.x - 1, c.y), Cell(c.x, c.y + 1), Cell(c.x, c.y - 1))) {
                    if (!n.inBounds || !terrainAt(n).walkable || n in blocked || !seen.add(n)) continue
                    if (n == to) return step
                    next += n
                }
            }
            frontier = next
        }
        return null
    }

    /** Vero se una casella [blocked] sta sulla linea retta tra chi spara e il bersaglio. */
    fun isLineBlocked(from: Cell, to: Cell, blocked: Set<Cell>): Boolean {
        val length = maxOf(abs(to.x - from.x), abs(to.y - from.y))
        return (1 until length).any { i ->
            val x = from.x + (to.x - from.x).toDouble() * i / length
            val y = from.y + (to.y - from.y).toDouble() * i / length
            Cell(x.roundToInt(), y.roundToInt()) in blocked
        }
    }
}
