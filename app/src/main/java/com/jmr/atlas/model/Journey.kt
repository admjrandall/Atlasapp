package com.jmr.atlas.model

/**
 * Journeys (spec docs/JOURNEYS-SPEC.md). A journey is a *view* over a space: the space keeps its items and links;
 * the journey adds a shape and ordered parts. Stored as SHAPE and PART details on the space, so no schema change,
 * and backups/exports carry it. The 0.7 board stays the default view of every space.
 */
enum class Shape(val label: String, val time: String, val partNoun: String, val finishes: Boolean, val styles: List<String> = emptyList()) {
    TRAIL("Trail", "Has an end", "stop", true),
    TREE("Living Tree", "Ongoing, grows", "branch", false),
    REMEMBER("Remember", "Looks back", "memory", false, listOf("ROOM", "GARDEN", "LETTER")),
    CROSSROADS("Crossroads", "Converges", "option", true),
    RIVER("River", "Deepens", "source", false),
    ORBIT("Orbit", "Returns", "cycle", false),
    SHELF("Shelf", "Accumulates", "shelf", false),
    VOYAGE("Voyage", "Before · during · after", "island", true),
    CLIMB("Climb", "Toward a target", "camp", true),
    COUNTDOWN("Countdown", "To a fixed day", "lane", true);

    /** The parts a new journey of this shape starts with (renamable, reorderable). */
    val starter: List<String> get() = when (this) {
        TRAIL -> listOf("Research", "Plan", "Do it", "Finish")
        TREE -> emptyList()
        REMEMBER -> emptyList()
        CROSSROADS -> listOf("Option A", "Option B")
        RIVER -> emptyList()
        ORBIT -> emptyList()
        SHELF -> listOf("Favorites", "To try")
        VOYAGE -> listOf("Before you go", "Day 1", "The album")
        CLIMB -> listOf("Base", "Camp 1", "Summit")
        COUNTDOWN -> listOf("Guests", "Place", "Food", "Run of show")
    }

    companion object {
        fun of(name: String?): Shape? = entries.firstOrNull { it.name == name }
        val styleLabels = mapOf("ROOM" to "Keepsake Room", "GARDEN" to "Memory Garden", "LETTER" to "Living Letter")
    }
}

enum class PartState { LATER, NEXT, HERE, DONE }

data class Part(val id: String, val name: String, val state: PartState = PartState.LATER, val sort: Int = 0)

data class Journey(val spaceId: String, val shape: Shape, val style: String, val parts: List<Part>) {
    val here: Part? get() = parts.firstOrNull { it.state == PartState.HERE }
    /** Share of parts done, for shapes that finish; null for shapes that never finish. */
    val progress: Float? get() = if (!shape.finishes || parts.isEmpty()) null else parts.count { it.state == PartState.DONE }.toFloat() / parts.size
    val styleLabel: String get() = Shape.styleLabels[style] ?: ""
}

object Journeys {
    fun of(s: Snapshot, spaceId: String): Journey? {
        val sd = s.detailsOf(spaceId, DetailKind.SHAPE).firstOrNull() ?: return null
        val shape = Shape.of(sd.value) ?: return null
        val parts = s.detailsOf(spaceId, DetailKind.PART).sortedBy { it.sort }
            .map { Part(it.label, it.value, PartState.entries.firstOrNull { e -> e.name == it.extra } ?: PartState.LATER, it.sort) }
        return Journey(spaceId, shape, sd.extra.takeIf { it in shape.styles } ?: shape.styles.firstOrNull().orEmpty(), parts)
    }

    /** The details to store for [j] (SHAPE + PARTs, in order). Names are cleaned and capped like band names. */
    fun details(j: Journey): List<Detail> =
        listOf(Detail(objectId = j.spaceId, kind = DetailKind.SHAPE, value = j.shape.name, extra = j.style)) +
            j.parts.map { Detail(objectId = j.spaceId, kind = DetailKind.PART, label = it.id, value = clean(it.name), extra = it.state.name) }

    /** A new journey of [shape] for a space; the first starter part is "here" for shapes that finish. */
    fun new(spaceId: String, shape: Shape, style: String? = null): Journey {
        val parts = shape.starter.mapIndexed { i, n -> Part(newId(), n, if (shape.finishes && i == 0) PartState.HERE else PartState.LATER, i) }
        return Journey(spaceId, shape, style?.takeIf { it in shape.styles } ?: shape.styles.firstOrNull().orEmpty(), parts)
    }

    /**
     * Change shape: the space's items and links are untouched (they live on the space, not the journey). Parts keep
     * their names and order; states reset to fit shapes that don't finish.
     */
    fun reshape(j: Journey, to: Shape, style: String? = null): Journey =
        j.copy(shape = to, style = style?.takeIf { it in to.styles } ?: to.styles.firstOrNull().orEmpty(),
            parts = j.parts.map { if (to.finishes) it else it.copy(state = PartState.LATER) })

    fun clean(name: String) = name.replace(Regex("[\\p{Cntrl}]"), " ").trim().take(40)
}
