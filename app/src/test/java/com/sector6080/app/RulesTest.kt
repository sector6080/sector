package com.sector6080.app

import com.sector6080.app.data.CatalogParser
import com.sector6080.app.data.ComponentRow
import com.sector6080.app.data.PlayerRow
import com.sector6080.app.data.TeamRow
import com.sector6080.app.data.UnitTypeRow
import com.sector6080.app.data.buildCatalog
import com.sector6080.app.data.UnitRow
import com.sector6080.app.data.createUnitParams
import com.sector6080.app.data.friendlyError
import com.sector6080.app.data.parseMillis
import com.sector6080.app.model.Catalog
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Component
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.Terrain
import com.sector6080.app.model.Stats
import com.sector6080.app.model.TeamMember
import com.sector6080.app.model.UnitType
import com.sector6080.app.model.deployed
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.FogSpan
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.rules.Vision
import kotlinx.serialization.json.jsonArray
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
        listOf("Fanteria", "Motorizzata", "Corazzata", "Artiglieria").map { name ->
            val text = javaClass.classLoader!!.getResource("$name.md")!!.readText()
            CatalogParser.parse(name, text)
        },
    )

    private fun newUnit(
        matricola: String,
        lore: String,
        type: UnitType,
        components: List<Component>,
    ): GameUnit {
        val max = UnitRules.maxStats(type, components)
        return GameUnit(
            matricola = matricola,
            type = type.name,
            components = components.map { it.name },
            lore = lore,
            hp = max.hp,
            supplies = max.supplies,
            uses = components.filter { it.isUsable }.associate { it.name to it.maxUses!! },
            position = Cell(18, 8),
        )
    }

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
        assertTrue(UnitRules.validate("XYZ-789", "lore", type, comps, 10).isEmpty())
        val unit = newUnit("XYZ-789", "lore", type, comps)
        assertEquals(4, UnitRules.totalCost(catalog, unit))
        assertEquals(mapOf("Fucile d'Assalto" to 3, "Granata" to 1), unit.uses)
    }

    @Test
    fun budgetLimitsTheUnit() {
        val type = catalog.type("Fanteria")!!
        val rifle = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val smg = catalog.component("Fanteria", "SMG")!!
        assertTrue(UnitRules.validate("XYZ-789", "l", type, listOf(rifle), 4).isEmpty())
        assertTrue(UnitRules.validate("XYZ-789", "l", type, listOf(rifle, smg), 4).isNotEmpty())
    }

    @Test
    fun otherUnitsBlockMovementAndFireButBuildingsDoNot() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = newUnit("XYZ-789", "l", type, listOf(gun)).copy(position = Cell(40, 40))
        val ally = Cell(41, 40)
        val blocked = setOf(ally)

        val moves = OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.MOVE), blocked)
        assertFalse(ally in moves) // non ci si ferma su un'altra unità
        assertFalse(Cell(42, 40) in moves) // né la si attraversa
        assertTrue(Cell(41, 41) in moves)

        val shot = Order(OrderType.ACTIVATE, component = gun.name)
        val shots = OrderRules.validTargets(catalog, unit, emptyList(), shot, blocked)
        assertTrue(ally in shots) // l'unità si può colpire
        assertFalse(Cell(42, 40) in shots) // ma non si spara attraverso
        assertTrue(Cell(40, 42) in shots)

        // gli edifici non bloccano
        assertEquals(Terrain.BUILDING, GameMap.terrainAt(Cell(25, 16)))
        assertEquals(2, GameMap.steps(Cell(24, 16), Cell(26, 16), 2))
        assertEquals("Magazzino 2", GameMap.siteAt(Cell(25, 16))?.name)
    }

    @Test
    fun explainsWhyAnOrderIsNotPossible() {
        val type = catalog.type("Fanteria")!!
        val gun = catalog.component("Fanteria", "Fucile d'Assalto")!!
        val unit = newUnit("XYZ-789", "l", type, listOf(gun)).copy(position = Cell(40, 40))
        val moved = listOf(Order(OrderType.MOVE, target = Cell(41, 40)))
        assertEquals("Massimo 1 movimento per turno", OrderRules.whyNot(catalog, unit, moved, Order(OrderType.MOVE)))
        assertEquals(
            "L'unità non è a bordo di un mezzo",
            OrderRules.whyNot(catalog, unit, emptyList(), Order(OrderType.DISEMBARK)),
        )
    }

    @Test
    fun onlyTheSupremeCommandIsGm() {
        assertTrue(PlayerRow(0, "Comando Supremo").isGm)
        assertFalse(PlayerRow(0, "Capitano").isGm)
        assertFalse(PlayerRow(0, "Recluta").isGm)
        assertEquals(
            "Il Comando Supremo non può avere unità.",
            friendlyError(Exception("Il Comando Supremo non puo avere unita")),
        )
    }

    @Test
    fun catalogFromServerRows() {
        val built = buildCatalog(
            listOf(UnitTypeRow("Fanteria", 0, 9, 2, 3, 1, 4)),
            listOf(
                ComponentRow("Fanteria", "Mirino", 1, "+1 visione", vision = 1),
                ComponentRow(
                    "Fanteria", "Granata", 2, "Perf. 1", perforation = 1, damage = 1, hits = 1,
                    areaW = 3, areaH = 3, range = 2, maxUses = 1, requires = "Mirino",
                ),
            ),
        )
        assertEquals(Stats(9, 2, 3, 1, 4), built.type("Fanteria")!!.base)
        val grenade = built.component("Fanteria", "Granata")!!
        assertEquals(3 to 3, grenade.weapon!!.area)
        assertEquals("Mirino", grenade.requires)
        assertNull(built.component("Fanteria", "Mirino")!!.weapon)
        assertEquals(1, built.component("Fanteria", "Mirino")!!.statMods.vision)
    }

    @Test
    fun pendingUnitsStayOffTheMap() {
        val type = catalog.type("Fanteria")!!
        val active = newUnit("AAA-111", "l", type, emptyList()).copy(approved = true, position = Cell(10, 10))
        val pending = newUnit("BBB-222", "l", type, emptyList()).copy(position = Cell(20, 20))
        val state = GameState(team = listOf(TeamMember(active, "Soldato", 0), TeamMember(pending, "Recluta", 0)))

        assertEquals(listOf(active), state.deployed.map { it.unit })
        assertEquals(setOf(Cell(10, 10)), state.occupiedCells())
    }

    @Test
    fun serverRowsBecomeGameData() {
        val row = UnitRow(
            "XYZ-789", "Fanteria", listOf("Pistola"), "l", 9, 4, mapOf("Pistola" to 2), 18, 8, false, "active",
        )
        val unit = row.toGameUnit()
        assertEquals(Cell(18, 8), unit.position)
        assertTrue(unit.approved)
        assertFalse(row.copy(status = "pending").toGameUnit().approved)

        val params = createUnitParams("XYZ-789", "Fanteria", listOf("Pistola"), "l")
        assertEquals("XYZ-789", params["p_matricola"]!!.jsonPrimitive.content)
        assertEquals(1, params["p_components"]!!.jsonArray.size)

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
        assertEquals(3254, counts[Terrain.GRASS]) // comprese le 80 caselle della riga 60
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
        assertEquals("Magazzino 1", GameMap.describe(Cell(61, 15)))
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
        val unit = newUnit("XYZ-789", "l", type, emptyList()).copy(position = north)
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
        val unit = newUnit("XYZ-789", "l", type, listOf(gun)).copy(position = Cell(40, 40))

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
    fun serverMapReplacesTheBuiltInOne() {
        val letters = mapOf(
            Terrain.SEA to 'B', Terrain.RIVER to 'L', Terrain.BRIDGE to 'O', Terrain.PORT to 'A',
            Terrain.SAND to 'S', Terrain.GRASS to 'G', Terrain.BUILDING to 'W',
        )
        val rows = (1..MAP_HEIGHT).map { y ->
            (1..MAP_WIDTH).map { x -> letters.getValue(GameMap.terrainAt(Cell(x, y))) }.joinToString("")
        }
        val sites = GameMap.ports + GameMap.refineries + GameMap.warehouses
        assertEquals(9, sites.size)

        assertFalse(GameMap.replace(rows, sites)) // uguale a quella attuale
        assertFalse(GameMap.replace(rows.drop(1), sites)) // dimensioni sbagliate

        val changed = listOf("G" + rows[0].drop(1)) + rows.drop(1)
        try {
            assertTrue(GameMap.replace(changed, sites))
            assertEquals(Terrain.GRASS, GameMap.terrainAt(Cell(1, 1)))
        } finally {
            GameMap.replace(rows, sites)
        }
        assertEquals(Terrain.SEA, GameMap.terrainAt(Cell(1, 1)))
    }

    @Test
    fun teamRowsHideNothingTheSheetNeeds() {
        val member = TeamRow(
            "XYZ-789", "Caporale", 12, "Fanteria", listOf("Pistola"), "storia", 7, 3,
            mapOf("Pistola" to 1), 30, 40, false, "active", 2,
        ).toTeamMember()
        assertEquals("Caporale", member.rank)
        assertEquals(12, member.pu)
        assertEquals("storia", member.unit.lore)
        assertEquals(Cell(30, 40), member.unit.position)
        assertTrue(member.unit.approved)

        val a = Vision.Source(Cell(30, 40), UnitRules.maxStats(catalog, member.unit).vision)
        assertEquals(3, a.range)
        assertTrue(Vision.isVisible(listOf(a), Cell(33, 40)))
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
        val unit = newUnit("XYZ-789", "l", type, listOf(gun)).copy(position = Cell(22, 10))
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
        val unit = newUnit("XYZ-789", "l", type, listOf(gun)).copy(position = Cell(40, 40))

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
