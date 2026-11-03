package com.sector6080.app.rules

import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType

/**
 * Valida una catena di ordini simulandola in sequenza (regolamento §3).
 * Ogni ordine viene controllato sullo stato lasciato dagli ordini precedenti.
 */
object OrderRules {

    data class Result(
        /** Errore per ogni ordine (null = valido), stesso indice della catena. */
        val errors: List<String?>,
        /** Stato dell'unità dopo gli ordini validi. */
        val finalUnit: GameUnit,
    ) {
        val isValid get() = errors.all { it == null }
    }

    fun simulate(catalog: Catalog, start: GameUnit, orders: List<Order>): Result {
        val max = UnitRules.maxStats(catalog, start)
        var unit = start
        var moved = false
        var usedStationary = false
        val activated = mutableSetOf<String>()
        val errors = mutableListOf<String?>()

        for (o in orders) {
            val pos = unit.position
            val error: String? = when (o.type) {
                OrderType.MOVE -> {
                    val t = o.target
                    when {
                        pos == null -> "L'unità non è ancora schierata sulla mappa"
                        t == null || !t.inBounds -> "Destinazione fuori mappa"
                        unit.transported -> "Le unità trasportate non possono muoversi"
                        moved -> "Massimo 1 movimento per turno"
                        usedStationary -> "Hai usato un'arma che richiede di restare fermi"
                        t == pos -> "La destinazione è la posizione attuale"
                        pos.distance(t) > max.movement ->
                            "Troppo lontano: ${pos.distance(t)} caselle, movimento ${max.movement}"
                        else -> {
                            unit = unit.copy(position = t); moved = true; null
                        }
                    }
                }

                OrderType.BOARD -> {
                    val t = o.target
                    when {
                        pos == null -> "L'unità non è ancora schierata sulla mappa"
                        unit.transported -> "L'unità è già a bordo di un mezzo"
                        t == null || !t.inBounds -> "Posizione del mezzo fuori mappa"
                        pos.distance(t) != 1 -> "Il mezzo deve essere su una casella adiacente"
                        else -> {
                            unit = unit.copy(position = t, transported = true); null
                        }
                    }
                }

                OrderType.DISEMBARK -> {
                    val t = o.target
                    when {
                        pos == null -> "L'unità non è ancora schierata sulla mappa"
                        !unit.transported -> "L'unità non è a bordo di un mezzo"
                        t == null || !t.inBounds -> "Casella fuori mappa"
                        pos.distance(t) != 1 -> "Si scende solo su caselle adiacenti libere"
                        else -> {
                            unit = unit.copy(position = t, transported = false); null
                        }
                    }
                }

                OrderType.ACTIVATE -> {
                    val name = o.component.orEmpty()
                    val c = catalog.component(unit.type, name)
                    val left = unit.uses[name] ?: 0
                    val t = o.target
                    when {
                        c == null || name !in unit.components -> "Componente non posseduto"
                        !c.isUsable -> "${c.name} è passivo, non si attiva"
                        name in activated -> "Ogni componente si attiva max 1 volta per turno"
                        left <= 0 -> "${c.name} è scarico: ricaricalo con un rifornimento"
                        c.weapon != null && unit.transported -> "Le unità trasportate non possono attaccare"
                        c.requiresStationary && moved -> "${c.name} non si usa dopo essersi mossi"
                        c.range != null && t == null -> "Indica la casella bersaglio"
                        c.range != null && pos == null -> "L'unità non è ancora schierata sulla mappa"
                        t != null && !t.inBounds -> "Bersaglio fuori mappa"
                        c.range != null && pos != null && t != null && pos.distance(t) > c.range ->
                            "Bersaglio fuori portata: ${pos.distance(t)} caselle, range ${c.range}"
                        else -> {
                            activated += name
                            if (c.requiresStationary) usedStationary = true
                            unit = unit.copy(uses = unit.uses + (name to left - 1)); null
                        }
                    }
                }

                OrderType.RELOAD -> {
                    val name = o.component.orEmpty()
                    val c = catalog.component(unit.type, name)
                    when {
                        c == null || name !in unit.components -> "Componente non posseduto"
                        !c.isUsable -> "${c.name} non ha utilizzi da ricaricare"
                        (unit.uses[name] ?: 0) >= c.maxUses!! -> "${c.name} è già carico"
                        unit.supplies < c.reloadCost ->
                            "Servono ${c.reloadCost} rifornimenti, ne hai ${unit.supplies}"
                        else -> {
                            unit = unit.copy(
                                supplies = unit.supplies - c.reloadCost,
                                uses = unit.uses + (name to c.maxUses),
                            ); null
                        }
                    }
                }

                OrderType.HEAL -> when {
                    unit.supplies < 1 -> "Nessun rifornimento disponibile"
                    unit.hp >= max.hp -> "L'unità ha già tutti gli HP"
                    else -> {
                        unit = unit.copy(supplies = unit.supplies - 1, hp = max.hp); null
                    }
                }

                OrderType.GIVE -> {
                    val q = o.quantity ?: 0
                    val t = o.target
                    val reach = if (SUPPLY_EXTENDER in unit.components) 2 else 1
                    when {
                        pos == null -> "L'unità non è ancora schierata sulla mappa"
                        t == null || !t.inBounds -> "Casella fuori mappa"
                        t == pos -> "Non puoi cedere rifornimenti a te stesso"
                        pos.distance(t) > reach -> "Il destinatario deve essere entro $reach caselle"
                        q <= 0 -> "Quantità non valida"
                        q > unit.supplies -> "Hai solo ${unit.supplies} rifornimenti"
                        else -> {
                            unit = unit.copy(supplies = unit.supplies - q); null
                        }
                    }
                }
            }
            errors += error
        }
        return Result(errors, unit)
    }

    fun describe(o: Order): String = when (o.type) {
        OrderType.MOVE -> "movimento a ${o.target}"
        OrderType.BOARD -> "sale su ${o.target}"
        OrderType.DISEMBARK -> "scende a ${o.target}"
        OrderType.ACTIVATE -> "usa ${o.component}" + (o.target?.let { " su $it" } ?: "")
        OrderType.RELOAD -> "ricarica ${o.component}"
        OrderType.HEAL -> "si cura"
        OrderType.GIVE -> "cede ${o.quantity} rifornimenti alla casella ${o.target}"
    }

    /** L'ordine ha bisogno di una casella scelta sulla mappa? */
    fun needsTarget(catalog: Catalog, unit: GameUnit, order: Order): Boolean = when (order.type) {
        OrderType.MOVE, OrderType.BOARD, OrderType.DISEMBARK, OrderType.GIVE -> true
        OrderType.ACTIVATE -> order.component?.let { catalog.component(unit.type, it) }?.range != null
        OrderType.RELOAD, OrderType.HEAL -> false
    }

    /**
     * Caselle che l'ordine può usare come bersaglio dopo la catena attuale.
     * Prova ogni casella con [simulate], così la mappa non può mai contraddire le regole.
     */
    fun validTargets(catalog: Catalog, start: GameUnit, orders: List<Order>, draft: Order): Set<Cell> {
        val origin = simulate(catalog, start, orders).finalUnit.position ?: return emptySet()
        val componentRange = draft.component?.let { catalog.component(start.type, it) }?.range ?: 0
        val reach = maxOf(UnitRules.maxStats(catalog, start).movement, 2, componentRange)
        return buildSet {
            for (x in origin.x - reach..origin.x + reach) {
                for (y in origin.y - reach..origin.y + reach) {
                    val cell = Cell(x, y)
                    if (!cell.inBounds || origin.distance(cell) > reach) continue
                    val result = simulate(catalog, start, orders + draft.copy(target = cell))
                    if (result.errors.last() == null) add(cell)
                }
            }
        }
    }

    /** L'ordine si può aggiungere ora alla catena? Serve a disattivare le scelte impossibili. */
    fun isPossible(catalog: Catalog, start: GameUnit, orders: List<Order>, draft: Order): Boolean =
        if (needsTarget(catalog, start, draft)) {
            validTargets(catalog, start, orders, draft).isNotEmpty()
        } else {
            simulate(catalog, start, orders + draft).errors.last() == null
        }

    private const val SUPPLY_EXTENDER = "Scarico Avanzato"
}
