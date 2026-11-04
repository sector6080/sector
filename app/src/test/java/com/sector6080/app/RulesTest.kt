package com.sector6080.app

import com.sector6080.app.data.CatalogLoader
import com.sector6080.app.data.CatalogParser
import com.sector6080.app.data.UnitRow
import com.sector6080.app.data.createUnitParams
import com.sector6080.app.data.friendlyError
import com.sector6080.app.data.parseMillis
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.STARTING_BUDGET
import com.sector6080.app.model.Terrain
import com.sector6080.app.model.Stats
import com.sector6080.app.rules.FogSpan
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.rules.Vision
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val unit = UnitRules.create("XYZ-789", "@User546", "lore", type, comps)
        assertEquals(4, UnitRules.totalCost(catalog, unit))
        assertEquals(mapOf("Fucile d'Assalto" to 3, "Granata" to 1), unit.uses)
    }

    @Test
    fun newPlayersHaveFourPu() {
        val type = catalog.type("Fanteria")!!
        val rifle = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val smg = catalog.component("Fanteria", "SMG")!!
        assertEquals(4, STARTING_BUDGET)
        assertTrue(UnitRules.validate("XYZ-789", "p", "l", type, listOf(rifle), STARTING_BUDGET).isEmpty())
        assertTrue(UnitRules.validate("XYZ-789", "p", "l", type, listOf(rifle, smg), STARTING_BUDGET).isNotEmpty())
        val spawn = UnitRules.create("XYZ-789", "p", "l", type, listOf(rifle)).position!!
        assertEquals(Terrain.PORT, GameMap.terrainAt(spawn))
    }

    @Test
    fun serverRowsBecomeGameData() {
        val row = UnitRow(
            "XYZ-789", "p", "Fanteria", listOf("Pistola"), "l", 9, 4, mapOf("Pistola" to 2), 18, 8, false, "active",
        )
        val unit = row.toGameUnit()
        assertEquals(Cell(18, 8), unit.position)
        assertTrue(unit.approved)
        assertTrue(unit.synced)
        assertFalse(row.copy(status = "pending").toGameUnit().approved)

        val params = createUnitParams(unit, 2, Cell(18, 8))
        assertEquals("XYZ-789", params["p_matricola"]!!.jsonPrimitive.content)
        assertEquals(2, params["p_cost"]!!.jsonPrimitive.int)
        assertEquals(2, params["p_uses"]!!.jsonObject["Pistola"]!!.jsonPrimitive.int)

        assertEquals(1793750400000, parseMillis("2026-11-04T00:00:00+00:00"))
        assertEquals("Hai già un'unità attiva o in attesa.", friendlyError(Exception("violates one_live_unit_per_player")))
        assertEquals("Matricola già usata: scegline un'altra.", friendlyError(Exception("game_units_matricola_key")))
        assertEquals("Connessione al server non riuscita.", friendlyError(Exception("timeout")))
    }

    @Test
    fun mapMatchesSheet() {
        val cells = (1..MAP_WIDTH).flatMap { x -> (1..MAP_HEIGHT).map { y -> Cell(x, y) } }
        val counts = cells.groupingBy { GameMap.terrainAt(it) }.eachCount()
        // conteggi dei colori nel foglio "Sector 6080 (master)"
        assertEquals(3174, counts[Terrain.GRASS])
        assertEquals(785, counts[Terrain.SAND])
        assertEquals(582, counts[Terrain.SEA])
        assertEquals(138, counts[Terrain.RIVER])
        assertEquals(8, counts[Terrain.BRIDGE])
        assertEquals(18, counts[Terrain.PORT])
        assertEquals(15, counts[Terrain.BUILDING])

        for (site in GameMap.ports) assertTrue(cells.filter { it in site }.all { GameMap.terrainAt(it) == Terrain.PORT })
        for (site in GameMap.refineries + GameMap.warehouses) {
            assertTrue(cells.filter { it in site }.all { GameMap.terrainAt(it) == Terrain.BUILDING })
        }
        assertEquals("Porto 2", GameMap.describe(Cell(50, 4)))
        assertEquals("Magazzino", GameMap.describe(Cell(61, 15)))
    }

    @Test
    fun waterBlocksMovementExceptBridges() {
        // un ponte con acqua a est e a ovest: da sopra si attraversa solo passandoci
        val bridge = (1..MAP_WIDTH).flatMap { x -> (1..MAP_HEIGHT).map { y -> Cell(x, y) } }.first {
            GameMap.terrainAt(it) == Terrain.BRIDGE &&
                GameMap.terrainAt(Cell(it.x - 1, it.y)) == Terrain.RIVER &&
                GameMap.terrainAt(Cell(it.x + 1, it.y)) == Terrain.RIVER &&
                GameMap.terrainAt(Cell(it.x, it.y - 1)).walkable &&
                GameMap.terrainAt(Cell(it.x, it.y + 1)).walkable
        }
        val north = Cell(bridge.x, bridge.y - 1)
        val south = Cell(bridge.x, bridge.y + 1)
        assertEquals(2, GameMap.steps(north, south, 2))
        assertNull(GameMap.steps(north, south, 1))
        assertNull(GameMap.steps(north, Cell(bridge.x + 1, bridge.y), 3))

        val type = catalog.type("Motorizzata")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, emptyList()).copy(position = north)
        val moves = OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.MOVE))
        assertTrue(south in moves)
        assertFalse(moves.any { !GameMap.terrainAt(it).walkable })
        assertEquals(
            "Non si può entrare in acqua",
            OrderRules.simulate(catalog, unit, listOf(Order(OrderType.MOVE, target = Cell(bridge.x + 1, bridge.y)))).errors[0],
        )
    }

    @Test
    fun mapOnlyOffersValidCells() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, listOf(gun)).copy(position = Cell(40, 40))

        val move = Order(OrderType.MOVE)
        val moves = OrderRules.validTargets(catalog, unit, emptyList(), move)
        assertEquals(12, moves.size) // rombo di raggio 2 senza la casella di partenza
        assertTrue(Cell(42, 40) in moves)
        assertFalse(Cell(42, 41) in moves)

        val chain = listOf(Order(OrderType.MOVE, target = Cell(41, 40)))
        assertTrue(OrderRules.validTargets(catalog, unit, chain, move).isEmpty())
        assertFalse(OrderRules.isPossible(catalog, unit, chain, move))
        // le caselle successive partono dalla posizione dopo la catena
        val shots = OrderRules.validTargets(catalog, unit, chain, Order(OrderType.ACTIVATE, component = gun.name))
        assertTrue(Cell(44, 40) in shots)

        assertEquals(4, OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.GIVE, quantity = 1)).size)
        assertFalse(OrderRules.isPossible(catalog, unit, emptyList(), Order(OrderType.DISEMBARK)))
        // arma già carica: ricaricarla sprecherebbe un rifornimento
        assertFalse(OrderRules.isPossible(catalog, unit, emptyList(), Order(OrderType.RELOAD, component = gun.name)))
    }

    @Test
    fun visionIsSharedByTheWholeTeam() {
        val a = Vision.Source(Cell(10, 10), 1)
        val b = Vision.Source(Cell(12, 10), 1)
        val team = listOf(a, b)

        assertTrue(Vision.isVisible(team, Cell(9, 10))) // solo a
        assertTrue(Vision.isVisible(team, Cell(13, 10))) // solo b
        assertFalse(Vision.isVisible(team, Cell(14, 10)))
        assertFalse(Vision.isVisible(team, Cell(9, 9))) // niente diagonali

        assertEquals(
            listOf(FogSpan(10, 1, 8), FogSpan(10, 14, MAP_WIDTH)),
            Vision.fogSpans(team).filter { it.y == 10 },
        )
        assertTrue(Vision.fogSpans(emptyList()).isEmpty())
    }

    @Test
    fun weaponsReachOverAndOntoWater() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, listOf(gun)).copy(position = Cell(22, 10))
        val sea = Cell(22, 8)
        val beyondSea = Cell(19, 10)
        assertEquals(Terrain.SEA, GameMap.terrainAt(sea))

        val shots = OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.ACTIVATE, component = gun.name))
        assertTrue(sea in shots)
        assertTrue(beyondSea in shots)
        assertTrue(shots.any { !GameMap.terrainAt(it).walkable })
        // il movimento invece resta bloccato
        assertFalse(sea in OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.MOVE)))
    }

    @Test
    fun orderChainFollowsRules() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = UnitRules.create("XYZ-789", "p", "l", type, listOf(gun)).copy(position = Cell(40, 40))

        val r = OrderRules.simulate(
            catalog, unit,
            listOf(
                Order(OrderType.MOVE, target = Cell(41, 41)), // distanza 2 = movimento 2: ok
                Order(OrderType.MOVE, target = Cell(42, 41)), // secondo movimento: vietato
                Order(OrderType.ACTIVATE, target = Cell(44, 41), component = gun.name), // range 3: ok
                Order(OrderType.ACTIVATE, target = Cell(44, 41), component = gun.name), // 2a attivazione: vietata
                Order(OrderType.RELOAD, component = gun.name),
            ),
        )
        assertNull(r.errors[0])
        assertNotNull(r.errors[1])
        assertNull(r.errors[2])
        assertNotNull(r.errors[3])
        assertNull(r.errors[4])
        assertEquals(Cell(41, 41), r.finalUnit.position)
        assertEquals(3, r.finalUnit.supplies)
        assertEquals(3, r.finalUnit.uses[gun.name])
    }
}
