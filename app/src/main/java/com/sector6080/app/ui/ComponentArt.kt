package com.sector6080.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sector6080.app.model.Component
import com.sector6080.app.ui.theme.Line
import kotlin.random.Random

/**
 * Famiglie dei componenti, in ordine di controllo: vince la prima con una parola che compare nel nome.
 * Nel disegno # è il colore della famiglia, + quello chiaro, - quello scuro e . lo sfondo.
 */
enum class ComponentFamily(val color: Int, val keywords: List<String>, val sprite: List<String>) {
    MEDICAL(
        0xE84646,
        listOf("Medic", "Ospedale", "Siringa", "Cucina", "Kit da Supporto"),
        listOf(
            "................",
            "................",
            "......####......",
            "......#++#......",
            "......#++#......",
            "..#####++#####..",
            "..#++++++++++#..",
            "..#++++++++++#..",
            "..#####++#####..",
            "......#++#......",
            "......#++#......",
            "......####......",
            "................",
            "................",
            "................",
            "................",
        ),
    ),
    REPAIR(
        0xF0C846,
        listOf("Riparazione", "Parti di Ricambio"),
        listOf(
            "................",
            "..........###...",
            ".........##.....",
            ".........##..##.",
            ".........######.",
            "........#++####.",
            ".......#++###...",
            "......#++##.....",
            ".....#++##......",
            "....#++##.......",
            "...#++##........",
            "..#++##.........",
            ".#+-##..........",
            ".###............",
            "..#.............",
            "................",
        ),
    ),
    FLAME(
        0xFF8A2C,
        listOf("Lanciafiamme"),
        listOf(
            ".......#........",
            ".......##.......",
            "......###.......",
            "......####......",
            ".....#####..#...",
            "..#..######.#...",
            "..#.#######.##..",
            ".##.###+#####...",
            ".#####+++####...",
            ".####+++++###...",
            ".####++#+++##...",
            "..###+###+##....",
            "..###+#####.....",
            "...#########....",
            ".....#####......",
            "................",
        ),
    ),
    ROCKET(
        0xE06046,
        listOf("Missile", "Lanciamissili", "Lanciarazzi", "Razzi", "MLRS", "Testate", "Supporto Aereo"),
        listOf(
            ".......##.......",
            "......####......",
            "......#++#......",
            "......#++#......",
            ".....##++##.....",
            ".....#+++-#.....",
            ".....#+++-#.....",
            ".....#+++-#.....",
            ".....#+++-#.....",
            ".....#+++-#.....",
            "....##+++-##....",
            "...###+++-###...",
            "...##.#--#.##...",
            "...#..####..#...",
            ".......##.......",
            "................",
        ),
    ),
    EXPLOSIVE(
        0x80A854,
        listOf("Granata", "Esplosivo", "Trappola", "Demolitore"),
        listOf(
            "................",
            ".........###....",
            ".......##..##...",
            "......####..#...",
            "......####......",
            ".....######.....",
            "....#-#-#-##....",
            "....########....",
            "...#-#-#-#-##...",
            "...##########...",
            "...#-#-#-#-##...",
            "...##########...",
            "....#-#-#-##....",
            "....########....",
            ".....######.....",
            "................",
        ),
    ),
    AMMO(
        0xE2B848,
        listOf("Proiettili", "Munizioni"),
        listOf(
            "................",
            "...#....#....#..",
            "..###..###..###.",
            "..###..###..###.",
            "..---..---..---.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..+#+..+#+..+#+.",
            "..---..---..---.",
            "..###..###..###.",
            "................",
            "................",
        ),
    ),
    SCOPE(
        0xFF5656,
        listOf("Mirino", "Ottica", "Puntamento", "Mira Laser", "Sistema di Guida", "Fuoco di Correzione", "Piattaforma Stabilizzata"),
        listOf(
            ".......##.......",
            ".....######.....",
            "...###.##.###...",
            "..##...##...##..",
            "..#....##....#..",
            ".##....##....##.",
            ".#.....++.....#.",
            "#######++#######",
            "#######++#######",
            ".#.....++.....#.",
            ".##....##....##.",
            "..#....##....#..",
            "..##...##...##..",
            "...###.##.###...",
            ".....######.....",
            ".......##.......",
        ),
    ),
    PISTOL(
        0xB0BEB6,
        listOf("Pistola"),
        listOf(
            "................",
            "................",
            "................",
            "..############..",
            "..#++++++++++#..",
            "..###########...",
            "..---######.....",
            ".....###..##....",
            ".....###.##.....",
            "....####........",
            "....###.........",
            "...####.........",
            "...###..........",
            "...---..........",
            "................",
            "................",
        ),
    ),
    RIFLE(
        0x96AA9E,
        listOf("Fucile", "Mitra", "SMG", "Cecchino"),
        listOf(
            "................",
            "................",
            "................",
            "................",
            ".........##.....",
            ".#...###########",
            "###############-",
            "#++++#####-----.",
            "###--###.##.....",
            "##.....#..##....",
            ".......#...##...",
            "............#...",
            "................",
            "................",
            "................",
            "................",
        ),
    ),
    CANNON(
        0xC6A670,
        listOf("Cannone", "Obice", "Mortai", "Lanciagranate", "Torretta Automatica"),
        listOf(
            "................",
            ".............##.",
            "...........###..",
            ".........###....",
            ".......###......",
            ".....#####......",
            "...######.......",
            "..######........",
            "...-####+.......",
            "...#+--+#.......",
            "..#+-##-+#......",
            "..#+-##-+#......",
            "...#+--+#.##....",
            "....####...##...",
            ".............#..",
            "................",
        ),
    ),
    RECON(
        0x5CD6B0,
        listOf("Radar", "Binocolo", "Drone", "Droni", "Fari", "Faro", "Ricognizione", "Radio", "Mappa Tattica", "Postazione"),
        listOf(
            "................",
            "................",
            "................",
            ".....######.....",
            "...###----###...",
            "..##..####..##..",
            ".##..##++##..##.",
            "##...#++++#...##",
            "##...#++++#...##",
            ".##..##++##..##.",
            "..##..####..##..",
            "...###----###...",
            ".....######.....",
            "................",
            "................",
            "................",
        ),
    ),
    ELECTRIC(
        0x9682FF,
        listOf("EMP", "Jammer", "ECM"),
        listOf(
            "................",
            ".........####...",
            "........####....",
            ".......####.....",
            "......####......",
            ".....####.......",
            "....#########...",
            "...#########....",
            ".......####.....",
            "......####......",
            ".....####.......",
            "....####........",
            "...###..........",
            "...##...........",
            "...#............",
            "................",
        ),
    ),
    TRANSPORT(
        0xE6B028,
        listOf("Rampa", "Gru", "Piattaforma Mobile", "Ponte"),
        listOf(
            "................",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            "......####......",
            ".....##..##.....",
            ".....##..##.....",
            "......####......",
            ".......##.......",
            ".......##....#..",
            "..##...##...##..",
            "..##...##...##..",
            "..###..##..###..",
            "...##########...",
            "....########....",
            "................",
        ),
    ),
    SUPPLY(
        0xB07C46,
        listOf("Zaino", "Portapacchi", "Portacontainer", "Serbatoio", "Cassone", "Deposito", "Rifornimento", "Scarico"),
        listOf(
            "................",
            ".##############.",
            ".##############.",
            ".#++#++++++#++#.",
            ".#+##++++++##+#.",
            ".#+#+#++++#+#+#.",
            ".#+#++#++#++#+#.",
            ".#+#+++##+++#+#.",
            ".#+#+++##+++#+#.",
            ".#+#++#++#++#+#.",
            ".#+#+#++++#+#+#.",
            ".#+##++++++##+#.",
            ".#++#++++++#++#.",
            ".##############.",
            ".##############.",
            "................",
        ),
    ),
    ENGINE(
        0xAA9682,
        listOf("Motore", "Rally", "Fuga"),
        listOf(
            "................",
            "......####......",
            "...##.####.##...",
            "..############..",
            "..####....####..",
            "...##......##...",
            ".###........###.",
            ".###........###.",
            ".###........###.",
            ".###........###.",
            "...##......##...",
            "..####....####..",
            "..############..",
            "...##.####.##...",
            "......####......",
            "................",
        ),
    ),
    ARMOR(
        0x63A0DC,
        listOf("Armatura", "Corazza", "Scudo", "Scudi", "Giubbotto", "Imbottitura", "Scafo", "Bunker", "Gomme", "Pneumatici", "Camuffata", "Stivali"),
        listOf(
            "................",
            "..############..",
            "..#+++++-----#..",
            "..#+++++-----#..",
            "..#+++++-----#..",
            "..#+++++-----#..",
            "..#+++++-----#..",
            "..#+++++-----#..",
            "...#++++----#...",
            "...#++++----#...",
            "....#+++---#....",
            "....#+++---#....",
            ".....#++--#.....",
            "......#+-#......",
            ".......##.......",
            "................",
        ),
    ),
    ;

    companion object {
        fun of(component: Component): ComponentFamily =
            entries.firstOrNull { family -> family.keywords.any { it in component.name } }
                ?: when {
                    component.weapon != null -> RIFLE
                    component.statMods.armor > 0 -> ARMOR
                    component.statMods.movement > 0 -> ENGINE
                    component.statMods.vision > 0 -> RECON
                    component.statMods.supplies > 0 -> SUPPLY
                    component.statMods.hp > 0 -> REPAIR
                    else -> ELECTRIC
                }
    }
}

/** Texturine del negozio, generate dal codice come la mappa: lo sfondo cambia con il nome del componente. */
object ComponentArt {
    private const val SIZE = 16
    private const val OPAQUE = 0xFF shl 24
    private const val PANEL = 0x121917
    private val cache = HashMap<String, ImageBitmap>()

    private fun red(color: Int) = color shr 16 and 0xFF
    private fun green(color: Int) = color shr 8 and 0xFF
    private fun blue(color: Int) = color and 0xFF

    private fun mix(a: Int, b: Int, amount: Float): Int {
        fun channel(x: Int, y: Int) = (x + (y - x) * amount).toInt()
        return OPAQUE or (channel(red(a), red(b)) shl 16) or
            (channel(green(a), green(b)) shl 8) or channel(blue(a), blue(b))
    }

    private fun draw(component: Component): ImageBitmap {
        val family = ComponentFamily.of(component)
        val main = OPAQUE or family.color
        val light = mix(main, 0xFFFFFF, 0.5f)
        val dark = mix(main, 0x000000, 0.5f)
        val base = mix(PANEL, main, 0.16f)
        val noise = Random(component.name.hashCode())
        val pixels = IntArray(SIZE * SIZE) { index ->
            when (family.sprite[index / SIZE][index % SIZE]) {
                '#' -> main
                '+' -> light
                '-' -> dark
                else -> mix(base, main, noise.nextFloat() * 0.14f)
            }
        }
        return Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    @Synchronized
    fun image(component: Component): ImageBitmap = cache.getOrPut(component.name) { draw(component) }
}

@Composable
fun ComponentTile(component: Component, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val image = remember(component.name) { ComponentArt.image(component) }
    Image(
        image,
        contentDescription = null,
        modifier = modifier.size(size).border(1.dp, Line),
        filterQuality = FilterQuality.None,
    )
}
