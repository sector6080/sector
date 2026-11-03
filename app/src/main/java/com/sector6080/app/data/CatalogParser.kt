package com.sector6080.app.data

import com.sector6080.app.model.Component
import com.sector6080.app.model.Stats
import com.sector6080.app.model.UnitType
import com.sector6080.app.model.Weapon

/**
 * Legge le note .md del vault Obsidian (cartella Gioco/Componenti) e ne ricava
 * statistiche base e componenti. Le note restano la fonte di verità del regolamento.
 *
 * Formato atteso:
 *   **Costo di creazione: 0 PU** · HP 9 · Movimento 2 · Visione 3 · Armor 1 · Rifornimenti 4/4
 *   ## 2 PU
 *   - Fucile d'Assalto – Perf. 1 | Range 3 | 2 danni | (3/3)
 */
object CatalogParser {
    private val costHeader = Regex("""^##\s+(\d+)\s*PU""")
    private val item = Regex("""^-\s+(.+?)\s+[–-]\s+(.+)$""")

    private val creationCost = Regex("""Costo di creazione:\s*(\d+)""")
    private val baseHp = Regex("""\bHP\s+(\d+)""")
    private val baseMov = Regex("""Movimento\s+(\d+)""")
    private val baseVis = Regex("""Visione\s+(\d+)""")
    private val baseArmor = Regex("""Armor\s+(\d+)""")
    private val baseSupplies = Regex("""Rifornimenti\s+(\d+)""")

    private val perf = Regex("""Perf\.?\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val range = Regex("""\bRange\s+(\d+)""", RegexOption.IGNORE_CASE)
    // (?<![+\d]) esclude i bonus tipo "+10 danni"
    private val damage = Regex("""(?<![+\d])(?:(\d+)x)?(\d+)\s+dann[oi]""", RegexOption.IGNORE_CASE)
    private val area = Regex("""area\s+(\d+)x(\d+)""", RegexOption.IGNORE_CASE)
    private val uses = Regex("""\((\d+)/(\d+)\)""")
    private val usesAlt = Regex("""\((\d+)\s+usi\)""")
    private val reload = Regex("""ricarica:\s*(\d+)\s+rifornimenti""", RegexOption.IGNORE_CASE)
    private val requires = Regex("""richiede\s+([^)]+)""", RegexOption.IGNORE_CASE)
    private val statMod = Regex(
        """([+-]\d+)\s+(max\s+hp|hp|movimento|visione|armor|armatura|max\s+rifornimenti|rifornimenti\s+max|rifornimenti)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val temporary = listOf("per un turno", "per 1 turno", "durante il turno", "quando", "turni")

    fun parse(typeName: String, markdown: String): UnitType {
        var cost = 0
        var base = Stats()
        var currentPrice: Int? = null
        val components = mutableListOf<Component>()

        for (raw in markdown.lines()) {
            val line = raw.trim()
            if (line.contains("Costo di creazione")) {
                cost = creationCost.find(line)?.groupValues?.get(1)?.toInt() ?: 0
                base = Stats(
                    hp = baseHp.int(line),
                    movement = baseMov.int(line),
                    vision = baseVis.int(line),
                    armor = baseArmor.int(line),
                    supplies = baseSupplies.int(line),
                )
                continue
            }
            val header = costHeader.find(line)
            if (header != null) {
                currentPrice = header.groupValues[1].toInt()
                continue
            }
            if (line.startsWith("#")) {
                currentPrice = null
                continue
            }
            val price = currentPrice ?: continue
            val m = item.find(line) ?: continue
            components += parseComponent(typeName, price, m.groupValues[1].trim(), m.groupValues[2].trim())
        }
        return UnitType(typeName, cost, base, components)
    }

    fun parseComponent(type: String, cost: Int, name: String, desc: String): Component {
        val perfV = perf.find(desc)?.groupValues?.get(1)?.toInt()
        val rangeV = range.find(desc)?.groupValues?.get(1)?.toInt()
        val dmg = damage.find(desc)
        val weapon = if (dmg != null && (perfV != null || rangeV != null)) {
            Weapon(
                perforation = perfV ?: 0,
                damage = dmg.groupValues[2].toInt(),
                hits = dmg.groupValues[1].toIntOrNull() ?: 1,
                area = area.find(desc)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() },
            )
        } else null

        val maxUses = uses.findAll(desc).lastOrNull()?.groupValues?.get(2)?.toInt()
            ?: usesAlt.find(desc)?.groupValues?.get(1)?.toInt()

        // I modificatori valgono solo nel primo segmento e solo se permanenti
        val effect = desc.substringBefore("|").replace(Regex("""\([^)]*\)"""), "")
        val isTemporary = temporary.any { effect.contains(it, ignoreCase = true) }
        var mods = Stats()
        if (!isTemporary) {
            for (s in statMod.findAll(effect)) {
                val v = s.groupValues[1].toInt()
                mods += when (s.groupValues[2].lowercase().replace(Regex("""\s+"""), " ")) {
                    "hp", "max hp" -> Stats(hp = v)
                    "movimento" -> Stats(movement = v)
                    "visione" -> Stats(vision = v)
                    "armor", "armatura" -> Stats(armor = v)
                    else -> Stats(supplies = v)
                }
            }
        }

        val lower = desc.lowercase()
        return Component(
            name = name,
            unitType = type,
            cost = cost,
            description = desc,
            statMods = mods,
            weapon = weapon,
            range = rangeV,
            maxUses = maxUses,
            reloadCost = reload.find(desc)?.groupValues?.get(1)?.toInt() ?: 1,
            requiresStationary = "non può muoversi" in lower || "deve stare fermo" in lower,
            requires = requires.find(desc)?.groupValues?.get(1)?.trim(),
        )
    }

    private fun Regex.int(s: String) = find(s)?.groupValues?.get(1)?.toInt() ?: 0
}
