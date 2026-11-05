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
import com.sector6080.app.model.ChatChannel
import com.sector6080.app.model.ChatMessage
import com.sector6080.app.model.Cell
import com.sector6080.app.model.Component
import com.sector6080.app.model.Enemy
import com.sector6080.app.model.GameUnit
import com.sector6080.app.model.GameMap
import com.sector6080.app.model.GameState
import com.sector6080.app.model.MAP_HEIGHT
import com.sector6080.app.model.MAP_WIDTH
import com.sector6080.app.model.Order
import com.sector6080.app.model.OrderType
import com.sector6080.app.model.Terrain
import com.sector6080.app.model.SquadRequest
import com.sector6080.app.model.Stats
import com.sector6080.app.model.TeamMember
import com.sector6080.app.model.UnitType
import com.sector6080.app.model.Weapon
import com.sector6080.app.model.deployed
import com.sector6080.app.model.occupiedCells
import com.sector6080.app.rules.ChatAuthor
import com.sector6080.app.rules.FogSpan
import com.sector6080.app.rules.Formation
import com.sector6080.app.rules.HierarchyRules
import com.sector6080.app.rules.OrderRules
import com.sector6080.app.rules.UnitRules
import com.sector6080.app.rules.Vision
import com.sector6080.app.ui.ComponentFamily
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
        val unit = newUnit("XYZ-789", "lore", type, comps)
        assertEquals(mapOf("Fucile d'Assalto" to 3, "Granata" to 1), unit.uses)
    }

    @Test
    fun budgetLimitsTheUnit() {
        val type = catalog.type("Corazzata")!!
        assertTrue(UnitRules.validate("XYZ-789", "l", type, type.creationCost).isEmpty())
        assertTrue(UnitRules.validate("XYZ-789", "l", type, type.creationCost - 1).isNotEmpty())
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
        assertEquals("L'unità ha già tutti gli HP", OrderRules.whyNot(catalog, unit, emptyList(), Order(OrderType.HEAL)))
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
    fun visibleEnemiesBlockCells() {
        val enemy = Enemy(1, "Pattuglia", 5, 1, "", Cell(30, 30))
        assertEquals(setOf(Cell(30, 30)), GameState(enemies = listOf(enemy)).occupiedCells())
    }

    @Test
    fun ranksChangeOneStepAtATime() {
        assertEquals(listOf("Caporale"), HierarchyRules.nextRanks("Soldato"))
        assertEquals(listOf("Capitano", "Soldato"), HierarchyRules.nextRanks("Caporale"))
        assertEquals(listOf("Caporale"), HierarchyRules.nextRanks("Capitano"))
        assertTrue(HierarchyRules.nextRanks("Recluta").isEmpty())
        assertTrue(HierarchyRules.nextRanks("Comando Supremo").isEmpty())
    }

    @Test
    fun squadNameHasNoDigitsAndAtMostEightCharacters() {
        assertTrue(HierarchyRules.isValidSquadName("Falchi"))
        assertTrue(HierarchyRules.isValidSquadName("Lupi di"))
        assertTrue(HierarchyRules.isValidSquadName(" Otto car "))
        assertFalse(HierarchyRules.isValidSquadName(""))
        assertFalse(HierarchyRules.isValidSquadName("   "))
        assertFalse(HierarchyRules.isValidSquadName("Falco7"))
        assertFalse(HierarchyRules.isValidSquadName("Novelett"+"e"))
    }

    @Test
    fun groupsEnterTheSquadronWithTheirCorporal() {
        val type = catalog.type("Fanteria")!!
        fun unit(matricola: String, approved: Boolean = true) =
            newUnit(matricola, "l", type, emptyList()).copy(approved = approved)
        val team = listOf(
            TeamMember(unit("CAP-001"), "Capitano", 0, squadron = "CAP-001", squadronName = "Aquile"),
            TeamMember(unit("COR-001"), "Caporale", 0, group = "COR-001", groupName = "Falchi", squadron = "CAP-001", squadronName = "Aquile"),
            TeamMember(unit("COR-002"), "Caporale", 0, group = "COR-002"),
            TeamMember(unit("SOL-001"), "Soldato", 0, group = "COR-001", groupName = "Falchi", squadron = "CAP-001", squadronName = "Aquile"),
            TeamMember(unit("SOL-002"), "Soldato", 0),
            TeamMember(unit("REC-001", approved = false), "Recluta", 0),
        )
        val limits = mapOf("squad_limit_corporal" to 6, "squad_limit_captain" to 40)
        fun state(i: Int) = GameState(unit = team[i].unit, rank = team[i].rank, team = team, settings = limits)
        val captain = state(0)
        val corporal = state(1)
        val freeCorporal = state(2)
        val soldier = state(3)
        val loner = state(4)
        fun matricole(list: List<TeamMember>) = list.map { it.unit.matricola }

        assertEquals(listOf("COR-001"), matricole(HierarchyRules.members(captain, Formation.SQUADRON)))
        assertEquals(listOf("SOL-001"), matricole(HierarchyRules.members(corporal, Formation.GROUP)))
        assertEquals(2, HierarchyRules.size(captain, Formation.SQUADRON))
        assertEquals(1, HierarchyRules.size(soldier, Formation.GROUP))
        assertEquals(listOf("COR-002"), matricole(HierarchyRules.free(captain, Formation.SQUADRON)))
        assertEquals(listOf("SOL-002"), matricole(HierarchyRules.free(corporal, Formation.GROUP)))
        assertEquals(1, HierarchyRules.soldiersOf(captain, team[1]))
        assertEquals("Aquile", HierarchyRules.name(soldier, Formation.SQUADRON))
        assertEquals("Falchi", HierarchyRules.name(soldier, Formation.GROUP))
        assertEquals(6, HierarchyRules.limit(corporal, Formation.GROUP))
        assertEquals(40, HierarchyRules.limit(captain, Formation.SQUADRON))

        // Chi sta in un gruppo dentro uno squadrone vede entrambi i canali
        assertTrue(HierarchyRules.canSee(soldier, ChatChannel.GROUP))
        assertTrue(HierarchyRules.canSee(soldier, ChatChannel.SQUAD))
        assertTrue(HierarchyRules.canSee(captain, ChatChannel.SQUAD))
        assertFalse(HierarchyRules.canSee(captain, ChatChannel.GROUP))
        assertTrue(HierarchyRules.canSee(freeCorporal, ChatChannel.GROUP))
        assertFalse(HierarchyRules.canSee(freeCorporal, ChatChannel.SQUAD))
        assertFalse(HierarchyRules.canSee(loner, ChatChannel.GROUP))
        assertFalse(HierarchyRules.canSee(loner, ChatChannel.SQUAD))

        // I Soldati chiedono a un Caporale, i Caporali a un Capitano
        assertTrue(HierarchyRules.canRequestSquad(loner, team[1]))
        assertFalse(HierarchyRules.canRequestSquad(loner, team[0]))
        assertFalse(HierarchyRules.canRequestSquad(soldier, team[2]))
        assertTrue(HierarchyRules.canRequestSquad(freeCorporal, team[0]))
        assertFalse(HierarchyRules.canRequestSquad(corporal, team[0]))
        assertFalse(HierarchyRules.canRequestSquad(captain, team[0]))

        val request = SquadRequest("SOL-002", "Fanteria", "COR-001")
        val waiting = loner.copy(squadRequests = listOf(request))
        assertFalse(HierarchyRules.canRequestSquad(waiting, team[1]))
        assertTrue(HierarchyRules.canRequestSquad(waiting, team[2]))
        assertEquals(request, HierarchyRules.outgoingRequest(waiting))
        assertEquals(listOf(request), HierarchyRules.incomingRequests(corporal.copy(squadRequests = listOf(request))))
        assertTrue(HierarchyRules.incomingRequests(waiting).isEmpty())

        assertEquals("gruppo \"Falchi\" · squadrone \"Aquile\"", team[3].squadLabel)
        assertEquals("gruppo senza nome", team[2].squadLabel)
        assertNull(team[4].squadLabel)
    }

    @Test
    fun mentionsAreReadFromTheChat() {
        assertEquals("ASD-543", HierarchyRules.authorMatricola("Soldato(ASD-543)-Fanteria"))
        assertNull(HierarchyRules.authorMatricola("Comando Supremo"))
        assertEquals(ChatAuthor("Soldato", "ASD-543", "Fanteria"), HierarchyRules.chatAuthor("Soldato(ASD-543)-Fanteria"))
        assertEquals(ChatAuthor("Comando Supremo"), HierarchyRules.chatAuthor("Comando Supremo"))
        assertEquals(
            ChatAuthor("Comando Supremo", "ASD-543", "Fanteria"),
            HierarchyRules.chatAuthor("Comando Supremo\r\n(ASD-543)-Fanteria"),
        )

        val unit = newUnit("ASD-543", "l", catalog.type("Fanteria")!!, emptyList())
        val state = GameState(unit = unit, rank = "Soldato")
        fun message(author: String, text: String) = ChatMessage(author, text, 0)

        assertTrue(HierarchyRules.mentionsMe(state, message("Soldato(AAA-111)-Fanteria", "ciao @asd-543")))
        assertFalse(HierarchyRules.mentionsMe(state, message("Soldato(AAA-111)-Fanteria", "ciao @ABC-123")))
        assertTrue(HierarchyRules.mentionsMe(state, message("Comando Supremo", "@tutti al porto")))
        assertFalse(HierarchyRules.mentionsMe(state, message("Soldato(AAA-111)-Fanteria", "@tutti al porto")))
        assertFalse(HierarchyRules.mentionsMe(GameState(), message("Soldato(AAA-111)-Fanteria", "ciao @ASD-543")))
    }

    @Test
    fun chatChannelsFollowTheRank() {
        val type = catalog.type("Fanteria")!!
        val unit = newUnit("AAA-111", "l", type, emptyList()).copy(approved = true)
        val soldier = GameState(unit = unit, rank = "Soldato", team = listOf(TeamMember(unit, "Soldato", 0)))
        val chosen = soldier.copy(team = listOf(TeamMember(unit, "Soldato", 0, group = "BBB-222")))
        val corporal = soldier.copy(rank = "Caporale")
        val gm = GameState(rank = "Comando Supremo", isGm = true)

        assertFalse(HierarchyRules.canSee(soldier, ChatChannel.COMMAND))
        assertTrue(HierarchyRules.canSee(corporal, ChatChannel.COMMAND))
        assertTrue(HierarchyRules.canSee(gm, ChatChannel.COMMAND))

        assertNotNull(HierarchyRules.whyNotWrite(soldier, ChatChannel.EVENTS))
        assertNull(HierarchyRules.whyNotWrite(gm, ChatChannel.EVENTS))
        assertNull(HierarchyRules.whyNotWrite(soldier, ChatChannel.COMMON))
        assertNotNull(HierarchyRules.whyNotWrite(GameState(), ChatChannel.COMMON))
        assertFalse(HierarchyRules.canSee(soldier, ChatChannel.GROUP))
        assertTrue(HierarchyRules.canSee(chosen, ChatChannel.GROUP))
        assertTrue(HierarchyRules.canSee(gm, ChatChannel.GROUP))
        assertTrue(HierarchyRules.canSee(gm, ChatChannel.SQUAD))
        assertTrue(HierarchyRules.canSee(GameState(), ChatChannel.EVENTS))
        assertTrue(HierarchyRules.canSee(GameState(), ChatChannel.COMMON))
        assertFalse(HierarchyRules.canSee(GameState(), ChatChannel.GROUP))
        assertFalse(HierarchyRules.canSee(GameState(), ChatChannel.SQUAD))

        assertNull(HierarchyRules.whyNotWrite(chosen, ChatChannel.GROUP))
        assertNotNull(HierarchyRules.whyNotWrite(gm, ChatChannel.SQUAD))
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

        val params = createUnitParams("XYZ-789", "Fanteria", "l")
        assertEquals("XYZ-789", params["p_matricola"]!!.jsonPrimitive.content)

        assertEquals(1793750400000, parseMillis("2026-11-04T00:00:00+00:00"))
        assertEquals("Hai già un'unità attiva o in attesa.", friendlyError(Exception("violates one_live_unit_per_player")))
        assertEquals("Matricola già usata: scegline un'altra.", friendlyError(Exception("game_units_matricola_key")))
        assertEquals("Connessione al server non riuscita.", friendlyError(Exception("timeout")))
        assertEquals(
            "Accesso non valido: chiudi e riapri l'app.",
            friendlyError(Exception("permission denied for function send_chat")),
        )
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

        assertTrue(OrderRules.validTargets(catalog, unit, emptyList(), Order(OrderType.DISEMBARK)).isEmpty())
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
            mapOf("Pistola" to 1), 30, 40, false, "active",
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
    fun instantActionsStayOutOfTheChain() {
        val type = catalog.type("Fanteria")!!
        val unit = newUnit("XYZ-789", "l", type, emptyList()).copy(position = Cell(40, 40))
        val result = OrderRules.simulate(catalog, unit, listOf(Order(OrderType.GIVE, quantity = 1, target = Cell(41, 40))))
        assertFalse(result.isValid)
        assertEquals(unit.supplies, result.finalUnit.supplies)
        assertTrue(OrderType.BOARD.instant && OrderType.DISEMBARK.instant && OrderType.GIVE.instant)
        assertFalse(OrderType.MOVE.instant || OrderType.ACTIVATE.instant || OrderType.RELOAD.instant || OrderType.HEAL.instant)
    }

    @Test
    fun vehiclesHaveSeatsAndUnitsCanLeaveThem() {
        val ramp = Component("Rampa da Carico", "Motorizzata", 1, "", capacity = 1)
        val crane = Component("Gru Mobile", "Motorizzata", 2, "", capacity = 1)
        val platform = Component("Piattaforma Mobile", "Corazzata", 3, "", capacity = 2, carries = "Fanteria")
        val rules = Catalog(
            listOf(
                UnitType("Fanteria", 0, Stats(hp = 9, movement = 2, supplies = 4), emptyList()),
                UnitType("Motorizzata", 1, Stats(hp = 12, movement = 3, supplies = 3), listOf(ramp, crane)),
                UnitType("Corazzata", 4, Stats(hp = 14, movement = 3, supplies = 2), listOf(platform)),
            ),
        )
        fun unit(matricola: String, type: String, components: List<String>, cell: Cell, carrier: String? = null) =
            GameUnit(
                matricola, type, components, "l", 9, 2, emptyMap(), cell,
                transported = carrier != null, approved = true, carrier = carrier,
            )

        val soldier = unit("AAA-001", "Fanteria", emptyList(), Cell(40, 40))
        val truck = unit("BBB-002", "Motorizzata", listOf("Rampa da Carico", "Gru Mobile"), Cell(41, 40))
        val tank = unit("CCC-003", "Corazzata", listOf("Piattaforma Mobile"), Cell(40, 41))
        val empty = unit("DDD-004", "Motorizzata", emptyList(), Cell(39, 40))
        val far = truck.copy(matricola = "EEE-005", position = Cell(45, 40))
        val team = listOf(soldier, truck, tank, empty, far)

        assertEquals(listOf(truck, tank), OrderRules.vehiclesInReach(rules, soldier, team))
        assertEquals(2, OrderRules.freeSeats(rules, truck, "Fanteria", team))
        assertEquals(0, OrderRules.freeSeats(rules, tank, "Motorizzata", team))

        val onBoard = listOf(
            unit("FFF-006", "Fanteria", emptyList(), truck.position!!, carrier = truck.matricola),
            unit("GGG-007", "Fanteria", emptyList(), truck.position!!, carrier = truck.matricola),
        )
        assertEquals(listOf(tank), OrderRules.vehiclesInReach(rules, soldier, team + onBoard))
        // chi trasporta qualcuno e chi è già a bordo non salgono su altri mezzi
        assertTrue(OrderRules.vehiclesInReach(rules, truck, team + onBoard).isEmpty())
        assertTrue(OrderRules.vehiclesInReach(rules, onBoard[0], team + onBoard).isEmpty())

        val rider = onBoard[0]
        assertTrue(OrderRules.disembarkCells(soldier, emptySet()).isEmpty())
        assertEquals(
            setOf(Cell(42, 40), Cell(41, 39), Cell(41, 41)),
            OrderRules.validTargets(rules, rider, emptyList(), Order(OrderType.DISEMBARK), setOf(Cell(40, 40))),
        )
    }

    @Test
    fun suppliesGoOnlyToAlliesInReachWithSpace() {
        val depot = Component("Scarico Avanzato", "Fanteria", 1, "")
        val rules = Catalog(listOf(UnitType("Fanteria", 0, Stats(hp = 9, movement = 2, supplies = 4), listOf(depot))))
        fun unit(matricola: String, cell: Cell, supplies: Int, components: List<String> = emptyList()) =
            GameUnit(matricola, "Fanteria", components, "l", 9, supplies, emptyMap(), cell, approved = true)

        val giver = unit("AAA-001", Cell(40, 40), 2)
        val near = unit("BBB-002", Cell(41, 40), 3)
        val full = unit("CCC-003", Cell(40, 41), 4)
        val twoAway = unit("DDD-004", Cell(42, 40), 0)
        val corner = unit("EEE-005", Cell(41, 41), 0)
        val farCorner = unit("FFF-006", Cell(42, 42), 0)
        val team = listOf(giver, near, full, twoAway, corner, farCorner)

        assertEquals(1, OrderRules.supplySpace(rules, near))
        assertEquals(listOf(near, corner), OrderRules.supplyRecipients(rules, giver, team))
        val extended = giver.copy(components = listOf(depot.name))
        assertEquals(listOf(near, twoAway, corner, farCorner), OrderRules.supplyRecipients(rules, extended, team))
        assertTrue(OrderRules.supplyRecipients(rules, giver.copy(supplies = 0), team).isEmpty())
    }

    @Test
    fun rangeBonusesApplyToWeaponsOnly() {
        val rifle = Component("Fucile", "Fanteria", 1, "", weapon = Weapon(2, 2), range = 3, maxUses = 2, firearm = true)
        val grenade = Component("Granata", "Fanteria", 1, "", weapon = Weapon(1, 1), range = 2, maxUses = 1)
        val medkit = Component("Kit", "Fanteria", 1, "", range = 1, maxUses = 1)
        val trap = Component("Trappola", "Fanteria", 1, "", weapon = Weapon(1, 1), maxUses = 1)
        val zoom = Component("Ottica", "Fanteria", 1, "", forFirearms = true, bonusRange = 2)
        val laser = Component("Puntatore", "Fanteria", 1, "", bonusRange = 1)
        val rules = Catalog(
            listOf(UnitType("Fanteria", 0, Stats(hp = 9, movement = 2), listOf(rifle, grenade, medkit, trap, zoom, laser))),
        )
        val unit = GameUnit(
            "AAA-001", "Fanteria", listOf("Fucile", "Ottica", "Puntatore"), "l", 9, 2,
            mapOf("Fucile" to 2), Cell(40, 40), approved = true,
        )

        assertEquals(6, OrderRules.effectiveRange(rules, unit, rifle))
        assertEquals(3, OrderRules.effectiveRange(rules, unit, grenade)) // l'ottica vale solo per le armi da fuoco
        assertEquals(1, OrderRules.effectiveRange(rules, unit, medkit))
        assertNull(OrderRules.effectiveRange(rules, unit, trap))

        val shot = Order(OrderType.ACTIVATE, component = "Fucile", target = Cell(46, 40))
        assertTrue(OrderRules.simulate(rules, unit, listOf(shot)).isValid)
        assertFalse(OrderRules.simulate(rules, unit, listOf(shot.copy(target = Cell(47, 40)))).isValid)
        assertTrue(Cell(46, 40) in OrderRules.validTargets(rules, unit, emptyList(), shot.copy(target = null)))
    }

    @Test
    fun marksAndBridgesFollowTheirRules() {
        val beacon = Component("Faro Laser", "Motorizzata", 1, "", range = 4, maxUses = 1, marks = true)
        val strike = Component("Supporto Aereo", "Motorizzata", 1, "", weapon = Weapon(2, 5), range = 6, maxUses = 1, needsMark = true)
        val bridge = Component("Ponte", "Motorizzata", 1, "", range = 1, maxUses = 1, buildsBridge = true)
        val rules = Catalog(
            listOf(UnitType("Motorizzata", 1, Stats(hp = 12, movement = 3, armor = 1, supplies = 3), listOf(beacon, strike, bridge))),
        )
        val names = listOf(beacon.name, strike.name, bridge.name)
        val unit = GameUnit("AAA-001", "Motorizzata", names, "l", 12, 3, names.associateWith { 1 }, Cell(40, 40), approved = true)
        val target = Cell(43, 40)
        val shot = Order(OrderType.ACTIVATE, component = strike.name, target = target)

        assertFalse(OrderRules.simulate(rules, unit, listOf(shot)).isValid)
        assertTrue(OrderRules.simulate(rules, unit, listOf(shot), marked = setOf(target)).isValid)
        assertEquals(
            setOf(target),
            OrderRules.validTargets(rules, unit, emptyList(), shot.copy(target = null), marked = setOf(target)),
        )
        // il faro marca la casella per gli ordini che lo seguono nella catena
        val mark = Order(OrderType.ACTIVATE, component = beacon.name, target = target)
        assertTrue(OrderRules.simulate(rules, unit, listOf(mark, shot)).isValid)

        val cells = (1..MAP_WIDTH).flatMap { x -> (1..MAP_HEIGHT).map { y -> Cell(x, y) } }
        fun around(c: Cell) = listOf(Cell(c.x + 1, c.y), Cell(c.x - 1, c.y), Cell(c.x, c.y + 1), Cell(c.x, c.y - 1))
            .filter { it.inBounds }
        val river = cells.first { c ->
            GameMap.terrainAt(c) == Terrain.RIVER && around(c).any { GameMap.terrainAt(it).walkable }
        }
        val bank = around(river).first { GameMap.terrainAt(it).walkable }
        val build = Order(OrderType.ACTIVATE, component = bridge.name)
        val spots = OrderRules.validTargets(rules, unit.copy(position = bank), emptyList(), build)
        assertTrue(river in spots)
        assertTrue(spots.all { GameMap.terrainAt(it) == Terrain.RIVER })
        assertTrue(OrderRules.validTargets(rules, unit, emptyList(), build).isEmpty())

        assertEquals(1, UnitRules.maxStats(rules, unit).armor)
        assertEquals(6, UnitRules.maxStats(rules, unit.copy(armorBonus = 5)).armor)
    }

    @Test
    fun everyComponentBelongsToAFamily() {
        fun family(name: String, stats: Stats = Stats(), weapon: Weapon? = null) =
            ComponentFamily.of(Component(name, "Fanteria", 1, "", statMods = stats, weapon = weapon))

        assertEquals(ComponentFamily.PISTOL, family("Pistola Silenziata"))
        assertEquals(ComponentFamily.RIFLE, family("Mitragliatrice Pesante"))
        assertEquals(ComponentFamily.AMMO, family("Proiettili EMP"))
        assertEquals(ComponentFamily.AMMO, family("Proiettili Esplosivi"))
        assertEquals(ComponentFamily.EXPLOSIVE, family("Esplosivo C4"))
        assertEquals(ComponentFamily.ELECTRIC, family("Scaricatore EMP"))
        assertEquals(ComponentFamily.CANNON, family("Mortai Medi"))
        assertEquals(ComponentFamily.MEDICAL, family("Kit Medico"))
        assertEquals(ComponentFamily.ROCKET, family("Piattaforma Lanciarazzi"))
        assertEquals(ComponentFamily.TRANSPORT, family("Piattaforma Mobile"))
        assertEquals(ComponentFamily.RECON, family("Torretta Ricognizione"))
        // un nome nuovo prende la famiglia dai suoi valori
        assertEquals(ComponentFamily.RIFLE, family("Nuova arma", weapon = Weapon(1, 1)))
        assertEquals(ComponentFamily.ARMOR, family("Nuova piastra", Stats(armor = 1)))
        assertEquals(ComponentFamily.ENGINE, family("Nuovo turbo", Stats(movement = 1)))

        ComponentFamily.entries.forEach { entry ->
            assertEquals(16, entry.sprite.size)
            assertTrue(entry.sprite.all { it.length == 16 })
        }
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
