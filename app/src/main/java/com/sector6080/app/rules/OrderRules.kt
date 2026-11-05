package com.sector6080.app.rules

import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Component
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.Terrain

/**
 * Valida una catena di ordini simulandola in sequenza (regolamento §3).
 * Ogni ordine viene controllato sullo stato lasciato dagli ordini precedenti.
 * Il server rifà gli stessi controlli quando esegue gli ordini, allo scadere del turno.
 * Salire, scendere e cedere rifornimenti non stanno nella catena: partono subito dalla situazione reale.
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

    fun simulate(
        catalog: Catalog,
        start: GameUnit,
        orders: List<Order>,
        occupied: Set<Cell> = emptySet(),
        marked: Set<Cell> = emptySet(),
    ): Result {
        val max = UnitRules.maxStats(catalog, start)
        val marks = marked.toMutableSet()
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
                        !GameMap.terrainAt(t).walkable -> "Non si può entrare in acqua"
                        t in occupied -> "Casella occupata da un'altra unità"
                        GameMap.steps(pos, t, max.movement, occupied) == null ->
                            "Troppo lontano o percorso bloccato: movimento ${max.movement}"
                        else -> {
                            unit = unit.copy(position = t); moved = true; null
                        }
                    }
                }

                OrderType.ACTIVATE -> {
                    val name = o.component.orEmpty()
                    val c = catalog.component(unit.type, name)
                    val left = unit.uses[name] ?: 0
                    val t = o.target
                    val range = c?.let { effectiveRange(catalog, unit, it) }
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
                        range != null && pos != null && t != null && pos.distance(t) > range ->
                            "Bersaglio fuori portata: ${pos.distance(t)} caselle, range $range"
                        c.needsMark && t !in marks -> "La casella non è marcata"
                        c.buildsBridge && t != null && GameMap.terrainAt(t) != Terrain.RIVER ->
                            "Il ponte si costruisce solo sul fiume"
                        c.weapon != null && pos != null && t != null && GameMap.isLineBlocked(pos, t, occupied) ->
                            "Un'altra unità blocca il tiro"
                        else -> {
                            activated += name
                            if (c.requiresStationary) usedStationary = true
                            if (c.marks && t != null) marks += t
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

                OrderType.BOARD, OrderType.DISEMBARK, OrderType.GIVE -> "Azione immediata: non va nella catena"
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
        OrderType.MOVE, OrderType.DISEMBARK -> true
        OrderType.ACTIVATE -> order.component?.let { catalog.component(unit.type, it) }?.range != null
        OrderType.BOARD, OrderType.GIVE, OrderType.RELOAD, OrderType.HEAL -> false
    }

    /**
     * Caselle che l'ordine può usare come bersaglio dopo la catena attuale.
     * Prova ogni casella con [simulate], così la mappa non può mai contraddire le regole.
     */
    fun validTargets(
        catalog: Catalog,
        start: GameUnit,
        orders: List<Order>,
        draft: Order,
        occupied: Set<Cell> = emptySet(),
        marked: Set<Cell> = emptySet(),
    ): Set<Cell> {
        if (draft.type == OrderType.DISEMBARK) return disembarkCells(start, occupied)
        val origin = simulate(catalog, start, orders, occupied, marked).finalUnit.position ?: return emptySet()
        val componentRange = draft.component?.let { catalog.component(start.type, it) }
            ?.let { effectiveRange(catalog, start, it) } ?: 0
        val reach = maxOf(UnitRules.maxStats(catalog, start).movement, componentRange)
        return buildSet {
            for (x in origin.x - reach..origin.x + reach) {
                for (y in origin.y - reach..origin.y + reach) {
                    val cell = Cell(x, y)
                    if (!cell.inBounds || origin.distance(cell) > reach) continue
                    val result = simulate(catalog, start, orders + draft.copy(target = cell), occupied, marked)
                    if (result.errors.last() == null) add(cell)
                }
            }
        }
    }

    /** L'ordine si può aggiungere ora alla catena? Serve a disattivare le scelte impossibili. */
    fun isPossible(
        catalog: Catalog,
        start: GameUnit,
        orders: List<Order>,
        draft: Order,
        occupied: Set<Cell> = emptySet(),
        marked: Set<Cell> = emptySet(),
    ): Boolean = if (needsTarget(catalog, start, draft)) {
        validTargets(catalog, start, orders, draft, occupied, marked).isNotEmpty()
    } else {
        simulate(catalog, start, orders + draft, occupied, marked).errors.last() == null
    }

    /** Spiega perché un ordine non si può dare, provandolo su una casella vicina. */
    fun whyNot(
        catalog: Catalog,
        start: GameUnit,
        orders: List<Order>,
        draft: Order,
        occupied: Set<Cell> = emptySet(),
        marked: Set<Cell> = emptySet(),
    ): String {
        val origin = simulate(catalog, start, orders, occupied, marked).finalUnit.position
        val probe = if (needsTarget(catalog, start, draft)) origin?.let { Cell(it.x + 1, it.y) } else null
        return simulate(catalog, start, orders + draft.copy(target = probe), occupied, marked).errors.last()
            ?: "Nessuna casella disponibile"
    }

    /** Range con i bonus dei componenti, che valgono solo per le armi. */
    fun effectiveRange(catalog: Catalog, unit: GameUnit, component: Component): Int? {
        val base = component.range ?: return null
        if (component.weapon == null) return base
        return base + unit.components.mapNotNull { catalog.component(unit.type, it) }
            .filter { !it.forFirearms || component.firearm }
            .sumOf { it.bonusRange }
    }

    /** Posti liberi sul mezzo per un passeggero di questo tipo. */
    fun freeSeats(catalog: Catalog, vehicle: GameUnit, passengerType: String, team: List<GameUnit>): Int {
        val seats = vehicle.components.mapNotNull { catalog.component(vehicle.type, it) }
            .filter { it.carries == null || it.carries == passengerType }
            .sumOf { it.capacity }
        return seats - team.count { it.carrier == vehicle.matricola }
    }

    /** Mezzi su caselle adiacenti con un posto libero. Chi trasporta qualcuno non può salire. */
    fun vehiclesInReach(catalog: Catalog, unit: GameUnit, team: List<GameUnit>): List<GameUnit> {
        val position = unit.position ?: return emptyList()
        if (unit.transported || team.any { it.carrier == unit.matricola }) return emptyList()
        return team.filter { other ->
            other.matricola != unit.matricola && !other.transported &&
                other.position?.distance(position) == 1 &&
                freeSeats(catalog, other, unit.type, team) > 0
        }
    }

    /** Caselle adiacenti al mezzo su cui si può scendere. */
    fun disembarkCells(unit: GameUnit, occupied: Set<Cell>): Set<Cell> {
        val at = unit.position?.takeIf { unit.transported } ?: return emptySet()
        return listOf(Cell(at.x + 1, at.y), Cell(at.x - 1, at.y), Cell(at.x, at.y + 1), Cell(at.x, at.y - 1))
            .filter { it.inBounds && GameMap.terrainAt(it).walkable && it !in occupied }
            .toSet()
    }

    /** Quanti rifornimenti può ancora ricevere l'unità. */
    fun supplySpace(catalog: Catalog, unit: GameUnit): Int =
        (UnitRules.maxStats(catalog, unit).supplies - unit.supplies).coerceAtLeast(0)

    /** Alleati a portata che possono ricevere rifornimenti. */
    fun supplyRecipients(catalog: Catalog, unit: GameUnit, team: List<GameUnit>): List<GameUnit> {
        val position = unit.position?.takeIf { unit.supplies > 0 } ?: return emptyList()
        val reach = if (SUPPLY_EXTENDER in unit.components) 2 else 1
        return team.filter { other ->
            val distance = other.position?.diagonalDistance(position)
            other.matricola != unit.matricola && distance != null && distance <= reach &&
                supplySpace(catalog, other) > 0
        }
    }

    private const val SUPPLY_EXTENDER = "Scarico Avanzato"
}
