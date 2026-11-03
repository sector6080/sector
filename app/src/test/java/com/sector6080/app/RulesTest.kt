package com.sector6080.app

import com.sector6080.app.data.CatalogLoader
import com.sector6080.app.data.CatalogParser
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.STARTING_BUDGET
import com.sector6080.app.model.START_CELL
import com.sector6080.app.model.Stats
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le note del vault sono sul classpath di test (vedi app/build.gradle.kts). */
class RulesTest {
    private val catalog = Catalog(
        CatalogLoader.TYPE_FILES.map { name ->
            val text = javaClass.classLoader!!.getResource("$name.md")!!.readText()
            CatalogParser.parse(name, text)
        },
    )

    @Test
    fun baseStatsMatchRulebook() {
        assertEquals(Stats(9, 2, 3, 1, 4), catalog.type("Fanteria")!!.base)
        assertEquals(Stats(12, 3, 3, 2, 8), catalog.type("Motorizzata")!!.base)
        assertEquals(Stats(14, 3, 2, 3, 4), catalog.type("Corazzata")!!.base)
        assertEquals(Stats(8, 1, 5, 1, 4), catalog.type("Artiglieria")!!.base)
        assertEquals(listOf(0, 1, 4, 5), catalog.types.map { it.creationCost })
    }

    @Test
    fun parsesWeaponsAndModifiers() {
        val mg = catalog.component("Fanteria", "Mitragliatrice Leggera")!!
        assertEquals(4, mg.cost)
        val weapon = mg.weapon!!
        assertEquals(2, weapon.perforation)
        assertEquals(3, weapon.hits)
        assertEquals(2, weapon.damage)
        assertEquals(3, mg.range)
        assertEquals(2, mg.maxUses)

        assertEquals(Stats(hp = 4, movement = -1), catalog.component("Fanteria", "Imbottitura Pesante")!!.statMods)
        // bonus temporaneo: non cambia le statistiche permanenti
        assertEquals(Stats(), catalog.component("Fanteria", "Scudo Energizzato")!!.statMods)
        assertEquals(3, catalog.component("Fanteria", "Granata")!!.weapon!!.area!!.first)
        assertEquals(2, catalog.component("Corazzata", "Cannone da 125mm")!!.reloadCost)
        assertTrue(catalog.component("Artiglieria", "Cannone da 75mm")!!.requiresStationary)
        assertEquals("Rampa da Carico", catalog.component("Motorizzata", "Gru Mobile")!!.requires)
        // "+10 danni" è un bonus, non un'arma
        assertNull(catalog.component("Artiglieria", "Testate Nucleari")!!.weapon)
    }

    @Test
    fun exampleUnitFromRulebook() {
        val type = catalog.type("Fanteria")!!
        val comps = listOf("Fucile d'Assalto", "Granata").map { catalog.component("Fanteria", it)!! }
        assertTrue(UnitRules.validate("XYZ-789", "@User546", "lore", type, comps, 10).isEmpty())
        val unit = UnitRules.create("XYZ-789", "@User546", "lore", type, comps, 10)
        assertEquals(6, unit.puLeft)
        assertEquals(mapOf("Fucile d'Assalto" to 3, "Granata" to 1), unit.uses)
    }

    @Test
    fun creationBudgetIsFivePu() {
        val type = catalog.type("Fanteria")!!
        val rifle = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val smg = catalog.component("Fanteria", "SMG")!!
        val mg = catalog.component("Fanteria", "Mitragliatrice Leggera")!!
        assertEquals(5, STARTING_BUDGET)
        assertTrue(UnitRules.validate("XYZ-789", "p", "l", type, listOf(rifle, smg), STARTING_BUDGET).isEmpty())
        assertTrue(UnitRules.validate("XYZ-789", "p", "l", type, listOf(rifle, mg), STARTING_BUDGET).isNotEmpty())
        assertEquals(START_CELL, UnitRules.create("XYZ-789", "p", "l", type, listOf(rifle), STARTING_BUDGET).position)
    }

    @Test
    fun mapOnlyOffersValidCells() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, listOf(gun), 5).copy(position = Cell(10, 10))

        val move = Order(OrderType.MOVE)
        val moves = OrderRules.validTargets(catalog, unit, emptyList(), move)
        assertEquals(12, moves.size) // rombo di raggio 2 senza la casella di partenza
        assertTrue(Cell(12, 10) in moves)
        assertFalse(Cell(12, 11) in moves)

        val chain = listOf(Order(OrderType.MOVE, target = Cell(11, 10)))
        assertTrue(OrderRules.validTargets(catalog, unit, chain, move).isEmpty())
        assertFalse(OrderRules.isPossible(catalog, unit, chain, move))
        // le caselle successive partono dalla posizione dopo la catena
        val shots = OrderRules.validTargets(catalog, unit, chain, Order(OrderType.ACTIVATE, component = gun.name))
        assertTrue(Cell(14, 10) in shots)

        assertEquals(4, OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.GIVE, quantity = 1)).size)
        assertFalse(OrderRules.isPossible(catalog, unit, emptyList(), Order(OrderType.DISEMBARK)))
        // arma già carica: ricaricarla sprecherebbe un rifornimento
        assertFalse(OrderRules.isPossible(catalog, unit, emptyList(), Order(OrderType.RELOAD, component = gun.name)))
    }

    @Test
    fun orderChainFollowsRules() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, listOf(gun), 10).copy(position = Cell(10, 10))

        val r = OrderRules.simulate(
            catalog, unit,
            listOf(
                Order(OrderType.MOVE, target = Cell(11, 11)), // distanza 2 = movimento 2: ok
                Order(OrderType.MOVE, target = Cell(12, 11)), // secondo movimento: vietato
                Order(OrderType.ACTIVATE, target = Cell(14, 11), component = gun.name), // range 3: ok
                Order(OrderType.ACTIVATE, target = Cell(14, 11), component = gun.name), // 2a attivazione: vietata
                Order(OrderType.RELOAD, component = gun.name),
            ),
        )
        assertNull(r.errors[0])
        assertNotNull(r.errors[1])
        assertNull(r.errors[2])
        assertNotNull(r.errors[3])
        assertNull(r.errors[4])
        assertEquals(Cell(11, 11), r.finalUnit.position)
        assertEquals(3, r.finalUnit.supplies)
        assertEquals(3, r.finalUnit.uses[gun.name])
    }
}
