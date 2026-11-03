package com.sector6080.app.rules

import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Component
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.START_CELL
import com.sector6080.app.model.Stats
import com.sector6080.app.model.UnitType

/** Regole di creazione delle unità (regolamento §4.1–4.2). */
object UnitRules {
    val MATRICOLA = Regex("""^[A-Z]{3}-\d{3}$""")

    fun maxStats(type: UnitType, components: List<Component>): Stats =
        components.fold(type.base) { acc, c -> acc + c.statMods }.atLeastZero()

    fun maxStats(catalog: Catalog, unit: GameUnit): Stats {
        val type = catalog.type(unit.type) ?: return Stats()
        return maxStats(type, unit.components.mapNotNull { catalog.component(unit.type, it) })
    }

    fun totalCost(type: UnitType, components: List<Component>) =
        type.creationCost + components.sumOf { it.cost }

    /** Restituisce gli errori che impediscono la creazione (lista vuota = ok). */
    fun validate(
        matricola: String,
        player: String,
        lore: String,
        type: UnitType?,
        components: List<Component>,
        budget: Int,
    ): List<String> = buildList {
        if (!MATRICOLA.matches(matricola)) add("Matricola: 3 lettere maiuscole, trattino, 3 numeri (es. XYZ-789)")
        if (player.isBlank()) add("Inserisci il nome del player")
        if (type == null) {
            add("Scegli il tipo di unità")
            return@buildList
        }
        if (lore.isBlank()) add("Scrivi una piccola lore dell'unità")
        val cost = totalCost(type, components)
        if (cost > budget) add("Costo $cost PU superiore al budget di $budget PU")
        val names = components.map { it.name }.toSet()
        for (c in components) {
            if (c.unitType != type.name) add("${c.name} non è un componente di ${type.name}")
            val req = c.requires
            if (req != null && names.none { it.equals(req, ignoreCase = true) }) {
                add("${c.name} richiede $req")
            }
        }
    }

    fun create(
        matricola: String,
        player: String,
        lore: String,
        type: UnitType,
        components: List<Component>,
        budget: Int,
    ): GameUnit {
        val max = maxStats(type, components)
        return GameUnit(
            matricola = matricola,
            player = player.trim(),
            type = type.name,
            components = components.map { it.name },
            lore = lore.trim(),
            puLeft = budget - totalCost(type, components),
            hp = max.hp,
            supplies = max.supplies,
            uses = components.filter { it.isUsable }.associate { it.name to it.maxUses!! },
            position = START_CELL,
        )
    }
}
