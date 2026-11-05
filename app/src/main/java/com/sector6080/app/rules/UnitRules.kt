package com.sector6080.app.rules

import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Component
import com.sector6080.app.model.GameUnit
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

    /** Restituisce gli errori che impediscono la creazione (lista vuota = ok). */
    fun validate(
        matricola: String,
        lore: String,
        type: UnitType?,
        budget: Int,
    ): List<String> = buildList {
        if (!MATRICOLA.matches(matricola)) add("Matricola: 3 lettere maiuscole, trattino, 3 numeri (es. XYZ-789)")
        if (type == null) {
            add("Scegli il tipo di unità")
            return@buildList
        }
        if (lore.isBlank()) add("Scrivi una piccola lore dell'unità")
        if (type.creationCost > budget) add("Costo ${type.creationCost} PU superiore al budget di $budget PU")
    }

}
