package com.sector6080.app.model

/** Statistiche di un'unità (regolamento §4.1). */
data class Stats(
    val hp: Int = 0,
    val movement: Int = 0,
    val vision: Int = 0,
    val armor: Int = 0,
    val supplies: Int = 0,
) {
    operator fun plus(o: Stats) = Stats(
        hp + o.hp,
        movement + o.movement,
        vision + o.vision,
        armor + o.armor,
        supplies + o.supplies,
    )

    fun atLeastZero() = Stats(
        hp.coerceAtLeast(1),
        movement.coerceAtLeast(0),
        vision.coerceAtLeast(0),
        armor.coerceAtLeast(0),
        supplies.coerceAtLeast(0),
    )
}

data class Weapon(
    val perforation: Int,
    val damage: Int,
    /** Numero di attacchi separati (es. "3x2 danni" = 3 attacchi da 2). */
    val hits: Int = 1,
    /** Area d'effetto, es. 3x3. Null = bersaglio singolo. */
    val area: Pair<Int, Int>? = null,
)

data class Component(
    val name: String,
    val unitType: String,
    val cost: Int,
    /** Testo originale dell'effetto, come nel regolamento. */
    val description: String,
    /** Modificatori permanenti alle statistiche (non quelli "per un turno"). */
    val statMods: Stats = Stats(),
    val weapon: Weapon? = null,
    val range: Int? = null,
    /** Utilizzi massimi; null = componente passivo. */
    val maxUses: Int? = null,
    /** Rifornimenti per ricaricare (default 1, alcuni pesanti 2 o 3). */
    val reloadCost: Int = 1,
    /** "Non può muoversi" / "deve stare fermo": non si usa nello stesso turno di un movimento. */
    val requiresStationary: Boolean = false,
    /** Nome di un altro componente richiesto (es. Gru Mobile → Rampa da Carico). */
    val requires: String? = null,
) {
    val isUsable get() = maxUses != null
}

data class UnitType(
    val name: String,
    val creationCost: Int,
    val base: Stats,
    val components: List<Component>,
)

data class Catalog(val types: List<UnitType>) {
    fun type(name: String) = types.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun component(type: String, name: String) =
        type(type)?.components?.firstOrNull { it.name == name }
}
