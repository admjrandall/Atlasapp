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

/** OUT = ruled out (Crossroads). */
enum class PartState { LATER, NEXT, HERE, DONE, OUT }

data class Part(val id: String, val name: String, val state: PartState = PartState.LATER, val sort: Int = 0)

/** [hidden] = kinds the person unchecked in the sources step; they stay in the space but not in the journey. */
data class Journey(val spaceId: String, val shape: Shape, val style: String, val parts: List<Part>, val hidden: Set<String> = emptySet()) {
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
        return Journey(spaceId, shape, sd.extra.takeIf { it in shape.styles } ?: shape.styles.firstOrNull().orEmpty(), parts,
            sd.label.split(',').filter { it.isNotBlank() }.toSet())
    }

    /** The details to store for [j] (SHAPE + PARTs, in order). Names are cleaned and capped like band names. */
    fun details(j: Journey): List<Detail> =
        listOf(Detail(objectId = j.spaceId, kind = DetailKind.SHAPE, label = j.hidden.sorted().joinToString(","), value = j.shape.name, extra = j.style)) +
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

/** A stack inside a part: things of one kind, best few first, near-duplicates folded (spec §6). */
data class Bundle(val key: String, val title: String, val items: List<KObject>, val best: List<KObject>, val folded: Int)

/** The kinds a bundle groups by, in the order they show. */
enum class BundleKind(val title: String, val types: Set<ObjType>) {
    PHOTOS("Photos", setOf(ObjType.IMAGE)), DOCS("Documents", setOf(ObjType.DOCUMENT)), LINKS("Links", setOf(ObjType.LINK)),
    PEOPLE("People", setOf(ObjType.PERSON)), NOTES("Notes", setOf(ObjType.NOTE, ObjType.IDEA, ObjType.DECISION)),
    TASKS("Tasks", setOf(ObjType.TASK)), EVENTS("Dates", setOf(ObjType.EVENT));
    companion object { fun of(t: ObjType) = entries.firstOrNull { t in it.types } }
}

object JourneyItems {
    const val BEST = 4

    /** Everything that belongs to the space (direct links), excluding the space itself and other spaces. */
    fun inSpace(s: Snapshot, spaceId: String): List<KObject> =
        s.neighbours(spaceId, includeProposed = false).map { it.first }.filter { !it.trashed && it.type != ObjType.PROJECT }.distinctBy { it.id }

    /** item id → part id for this space. */
    fun places(s: Snapshot, spaceId: String): Map<String, String> = s.detailsOf(spaceId, DetailKind.PLACE).associate { it.label to it.value }

    /** Items placed in [partId]; items with no place (or a place whose part is gone) count as being in the "here" part. */
    fun inPart(s: Snapshot, j: Journey, partId: String): List<KObject> {
        val pl = places(s, j.spaceId); val ids = j.parts.map { it.id }.toSet()
        val fallback = (j.here ?: j.parts.firstOrNull())?.id
        return inSpace(s, j.spaceId).filter { BundleKind.of(it.type)?.name !in j.hidden }.filter { o -> (pl[o.id]?.takeIf { it in ids } ?: fallback) == partId }
    }

    /** Bundles for a part: one per kind; the best few lead (pinned/favorite, then newest); near-duplicate titles fold. */
    fun bundles(s: Snapshot, j: Journey, partId: String): List<Bundle> =
        inPart(s, j, partId).groupBy { BundleKind.of(it.type) }.filterKeys { it != null }.toSortedMap(compareBy { it!!.ordinal })
            .map { (k, os) ->
                val sorted = os.sortedWith(compareByDescending<KObject> { it.pinned || it.favorite }.thenByDescending { it.updated })
                val distinct = sorted.distinctBy { stem(it.title) }
                val best = distinct.take(BEST)
                Bundle(k!!.name, if (os.size == 1) os[0].title else "${os.size} ${k.title.lowercase()}", sorted, best, os.size - best.size)
            }

    private fun stem(t: String) = t.lowercase().replace(Regex("[^a-z ]"), "").replace(Regex("\\s+"), " ").trim().split(' ').take(3).joinToString(" ")

    /** Place [itemId] in [partId] (replacing any earlier place in this space). */
    fun placeDetails(s: Snapshot, spaceId: String, itemId: String, partId: String): List<Detail> =
        s.detailsOf(spaceId, DetailKind.PLACE).filter { it.label != itemId } + Detail(objectId = spaceId, kind = DetailKind.PLACE, label = itemId, value = partId)

    /** Where Atlas would put a new item: a part whose name shares a word with the item, else the "here" part. */
    fun suggestPart(j: Journey, title: String): Part? {
        val words = title.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        return j.parts.firstOrNull { p -> p.name.lowercase().split(Regex("\\W+")).any { it in words } } ?: j.here ?: j.parts.firstOrNull()
    }
}

/** AI shape picking (spec §3 signals). Rules first; the reason is shown to the person. */
object ShapePick {
    data class Pick(val shape: Shape, val reason: String)
    private val deathWords = Regex("\\b(obituary|passed away|in memory|memorial|funeral|rest in peace|1[89]\\d\\d\\s*[–-]\\s*20\\d\\d)\\b", RegexOption.IGNORE_CASE)

    fun pick(s: Snapshot, spaceId: String, today: java.time.LocalDate = java.time.LocalDate.now()): Pick {
        val sp = s.objects[spaceId]
        val items = JourneyItems.inSpace(s, spaceId)
        val text = (listOfNotNull(sp?.title, sp?.body) + items.flatMap { listOf(it.title, it.body, it.url) }).joinToString(" ")
        return pickText(text, sp?.targetDate != null && items.any { it.type == ObjType.EVENT })
    }

    /** The same rules on plain text (a name typed in "New journey"). */
    fun pickText(text: String, datedEvent: Boolean = false): Pick {
        val t = text.lowercase()
        fun has(vararg w: String) = w.any { Regex("\\b$it\\b").containsMatchIn(t) }
        return when {
            deathWords.containsMatchIn(text) -> Pick(Shape.REMEMBER, "It names someone who passed away. I’d make a Keepsake Room from your memories together.")
            datedEvent && has("party", "wedding", "anniversary", "service", "birthday", "reunion") ->
                Pick(Shape.COUNTDOWN, "It has a fixed day coming up. A Countdown tightens toward the day.")
            has("party", "wedding", "anniversary", "reunion") -> Pick(Shape.COUNTDOWN, "It’s an event with a date. A Countdown tightens toward the day.")
            has("trip", "flight", "hotel", "cabin", "itinerary", "vacation", "beach") -> Pick(Shape.VOYAGE, "Places and dates away from home. A Voyage gives each day an island and ends as an album.")
            has("miles", "savings", "save", "goal", "marathon", "pounds", "lbs", "summit") -> Pick(Shape.CLIMB, "It has a number to reach. A Climb shows camps on the way to the summit.")
            has("vs", "versus", "compare", "which", "reviews", "options") -> Pick(Shape.CROSSROADS, "You’re choosing between options. A Crossroads narrows them to one.")
            has("learn", "learning", "practice", "course", "lesson", "skill", "how to") -> Pick(Shape.RIVER, "You’re learning something. A River widens as you understand more.")
            has("taxes", "tax", "yearly", "annual", "registration", "renewal", "every year") -> Pick(Shape.ORBIT, "It comes around again. An Orbit keeps each cycle as a ring.")
            has("recipes", "recipe", "books", "wishlist", "collection", "gift ideas") -> Pick(Shape.SHELF, "Things you keep, not finish. A Shelf sorts them for you.")
            has("grandchild", "grandbaby", "granddaughter", "grandson", "daughter", "son", "baby", "kids", "pet", "dog") ->
                Pick(Shape.TREE, "It’s a person you keep up with. A Living Tree grows with them.")
            else -> Pick(Shape.TRAIL, "Several steps toward something finished. A Trail shows where you are and what’s next.")
        }
    }
}

/** Sources step (spec §4.3): what Atlas found for the space, by kind, each one a checkbox. */
object Sources {
    data class Source(val kind: BundleKind, val count: Int, val label: String)
    fun of(s: Snapshot, spaceId: String): List<Source> = JourneyItems.inSpace(s, spaceId).groupBy { BundleKind.of(it.type) }.filterKeys { it != null }
        .map { (k, os) -> Source(k!!, os.size, "${os.size} ${if (os.size == 1) k.title.lowercase().removeSuffix("s") else k.title.lowercase()}") }
        .sortedBy { it.kind.ordinal }
}
