package com.jmr.atlas.data

import com.jmr.atlas.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How a space is laid out (design 0.6). A project board is grouped into bands by *who and what things are about*:
 * each person in the project anchors a band holding the items linked to them (directly, or one step away), items
 * linked to nobody share one band, and people with nothing of their own share an "Also involved" band. Bands are
 * ordered most urgent first. Positions are stable: the same data always gives the same layout.
 *
 * Pure functions over a [Snapshot] — unit-tested.
 */
object Board {
    enum class Kind(val key: String, val title: String, val blurb: String) {
        PEOPLE("people", "People", "Everyone, by space"),
        FILES("files", "Files", "Documents, photos and links"),
        NOTES("notes", "Ideas & notes", "Notes, ideas and decisions"),
        TASKS("tasks", "Tasks", "Everything you have to do"),
        EVERYTHING("all", "Everything", "All of it, by kind"),
    }

    data class Band(
        val key: String,
        val anchor: KObject?,
        val name: String,
        /** USER = you named it, AI = named on this phone by the model, EXAMPLE/AUTO = the default. */
        val nameOrigin: Origin,
        val items: List<KObject>,
        val status: String,
        val late: Boolean,
        val tint: Int,
        /** False for bands whose name is fixed by the space (Tasks: "Overdue"…). */
        val renamable: Boolean = true,
    )

    private val boardTypes = setOf(ObjType.PERSON, ObjType.TASK, ObjType.IDEA, ObjType.NOTE, ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.DECISION)

    fun effective(t: KObject): LocalDate? = listOfNotNull(t.due, t.deadline).minOrNull()
    fun isWaiting(t: KObject) = t.type == ObjType.TASK && t.title.startsWith("Expect ", true)

    /** Members of a project shown on its board: confirmed links only, open tasks only, no events (they're the timeline). */
    fun members(s: Snapshot, projectId: String): List<KObject> =
        s.neighbours(projectId, includeProposed = false).map { it.first }
            .filter { it.type in boardTypes && !it.trashed && !(it.type == ObjType.TASK && it.done) }
            .distinctBy { it.id }

    private fun relWeight(t: RelType) = when (t) { RelType.ASSIGNED_TO -> 0; RelType.DERIVED_FROM -> 1; RelType.DISCUSSED_WITH -> 2; else -> 3 }

    /** Tiles within a band: what's late first, then by date, then most recently touched. */
    fun order(items: List<KObject>, today: LocalDate): List<KObject> = items.sortedWith(
        compareBy<KObject>({ o -> val d = if (o.type == ObjType.TASK) effective(o) else null; if (d != null && d.isBefore(today)) 0 else if (d != null) 1 else 2 },
            { o -> if (o.type == ObjType.TASK) effective(o) ?: LocalDate.MAX else LocalDate.MAX })
            .thenByDescending { it.updated }.thenBy { it.id })

    fun status(items: List<KObject>, today: LocalDate): Pair<String, Boolean> {
        val tasks = items.filter { it.type == ObjType.TASK && !it.done }
        val late = tasks.filter { !isWaiting(it) && effective(it)?.isBefore(today) == true }.sortedBy { effective(it) }
        if (late.size == 1) { val t = late.first(); return "${shortTitle(t.title)} · ${lateBy(effective(t)!!, today)}" to true }
        if (late.size > 1) return "${late.size} late" to true
        val waiting = tasks.filter { isWaiting(it) }.minByOrNull { it.due ?: LocalDate.MAX }
        val next = tasks.filter { !isWaiting(it) && effective(it) != null }.minByOrNull { effective(it)!! }
        if (next != null && (waiting == null || effective(next)!! <= (waiting.due ?: LocalDate.MAX))) return "${shortTitle(next.title)} · ${whenShort(effective(next)!!, today)}" to false
        if (waiting != null) return "Waiting${waiting.due?.let { " · " + whenShort(it, today) } ?: ""}" to false
        val n = items.size
        return (when (n) { 0 -> "Nothing here yet"; 1 -> "1 thing"; else -> "$n things" }) to false
    }

    private fun urgency(items: List<KObject>, today: LocalDate): Long {
        val dates = items.filter { it.type == ObjType.TASK && !it.done }.mapNotNull { effective(it) }
        return dates.minOfOrNull { ChronoUnit.DAYS.between(today, it) } ?: Long.MAX_VALUE
    }

    fun defaultName(key: String, anchor: KObject?): String = when {
        anchor != null -> "With ${anchor.firstName}"
        key == REST -> "Not tied to anyone"
        key == PEOPLE_BAND -> "Also involved"
        else -> "Group"
    }

    const val REST = "rest"
    const val PEOPLE_BAND = "people"

    /** The bands of a project board. Names come from your renames, then the on-device model, then the default. */
    fun bands(s: Snapshot, projectId: String, today: LocalDate = LocalDate.now()): List<Band> {
        val all = members(s, projectId)
        val people = all.filter { it.type == ObjType.PERSON }.sortedBy { it.title }
        val things = all.filter { it.type != ObjType.PERSON }
        val owner = HashMap<String, String>()   // item id -> person id
        for (t in things.sortedBy { it.id }) {
            val best = s.relationsOf(t.id).filter { it.review == Review.CONFIRMED }
                .mapNotNull { r -> people.firstOrNull { it.id == r.other(t.id) }?.let { it to relWeight(r.type) } }
                .minWithOrNull(compareBy({ it.second }, { it.first.title }))
            if (best != null) owner[t.id] = best.first.id
        }
        // one step away: an item linked to an item that already has a person joins that person
        repeat(2) {
            for (t in things.sortedBy { it.id }) {
                if (owner.containsKey(t.id)) continue
                val via = s.relationsOf(t.id).filter { it.review == Review.CONFIRMED }.map { it.other(t.id) }.mapNotNull { owner[it] }.minOrNull()
                if (via != null) owner[t.id] = via
            }
        }
        val names = s.detailsOf(projectId, DetailKind.BAND).associateBy { it.label }
        fun mk(key: String, anchor: KObject?, items: List<KObject>, i: Int): Band {
            val ordered = order(items, today)
            val (st, late) = status(ordered + listOfNotNull(anchor), today)
            val n = names[key]
            return Band(key, anchor, n?.value ?: defaultName(key, anchor), n?.origin ?: Origin.AUTO, ordered, st, late, i % 4)
        }
        val personBands = people.mapNotNull { p -> things.filter { owner[it.id] == p.id }.takeIf { it.isNotEmpty() }?.let { p to it } }
        val lonely = people.filter { p -> personBands.none { it.first.id == p.id } }
        val rest = things.filter { !owner.containsKey(it.id) }
        val ranked = personBands.sortedWith(compareBy({ urgency(it.second + it.first, today) }, { -it.second.size }, { it.first.title }))
        val out = mutableListOf<Band>()
        ranked.forEach { (p, items) -> out += mk(p.id, p, items, out.size) }
        if (rest.isNotEmpty()) {
            val restBand = mk(REST, null, rest, out.size)
            // a late "everything else" band still goes before calmer people bands
            val at = out.indexOfFirst { urgency(it.items + listOfNotNull(it.anchor), today) > urgency(rest, today) }.let { if (it < 0) out.size else it }
            out.add(at, restBand)
        }
        if (lonely.isNotEmpty()) out += mk(PEOPLE_BAND, null, lonely, out.size)
        return out.mapIndexed { i, b -> b.copy(tint = i % 4) }
    }

    /** Bands of the special spaces (People, Files, Ideas & notes, Tasks, Everything). Names are fixed. */
    fun kindBands(s: Snapshot, kind: Kind, today: LocalDate = LocalDate.now()): List<Band> {
        val projects = s.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED }.sortedWith(compareBy({ it.projectStatus?.ordinal ?: 9 }, { it.title }))
        fun byProject(items: List<KObject>): List<Band> {
            val placed = HashSet<String>()
            val out = mutableListOf<Band>()
            projects.forEach { p ->
                val inP = items.filter { o -> o.id !in placed && s.neighbours(o.id, false).any { it.first.id == p.id } }
                if (inP.isNotEmpty()) { placed += inP.map { it.id }; val ord = order(inP, today); val (st, late) = status(ord, today); out += Band("p:${p.id}", null, p.title, Origin.AUTO, ord, st, late, out.size % 4, false) }
            }
            val loose = items.filter { it.id !in placed }
            if (loose.isNotEmpty()) { val ord = order(loose, today); val (st, late) = status(ord, today); out += Band("loose", null, "Not in a space", Origin.AUTO, ord, st, late, out.size % 4, false) }
            return out
        }
        return when (kind) {
            Kind.PEOPLE -> byProject(s.ofType(ObjType.PERSON))
            Kind.FILES -> byProject(s.live.filter { it.type.isFile })
            Kind.NOTES -> byProject(s.live.filter { it.type == ObjType.NOTE || it.type == ObjType.IDEA || it.type == ObjType.DECISION })
            Kind.TASKS -> {
                val open = s.ofType(ObjType.TASK).filter { !it.done }
                val weekEnd = today.plusDays(7)
                val groups = listOf(
                    "Late" to open.filter { !isWaiting(it) && effective(it)?.isBefore(today) == true },
                    "This week" to open.filter { !isWaiting(it) && effective(it)?.let { d -> !d.isBefore(today) && !d.isAfter(weekEnd) } == true },
                    "Waiting on others" to open.filter { isWaiting(it) },
                    "Later" to open.filter { !isWaiting(it) && effective(it)?.isAfter(weekEnd) == true },
                    "No date" to open.filter { !isWaiting(it) && effective(it) == null },
                    "Done lately" to s.ofType(ObjType.TASK).filter { it.done && (it.completedAt ?: 0) > System.currentTimeMillis() - 14 * 86_400_000L },
                )
                groups.filter { it.second.isNotEmpty() }.mapIndexed { i, (name, items) ->
                    val ord = order(items, today); val (st, late) = status(ord, today)
                    Band("t:$name", null, name, Origin.AUTO, ord, if (name == "Done lately") "${items.size} done" else st, late && name == "Late", i % 4, false)
                }
            }
            Kind.EVERYTHING -> listOf(
                "People" to setOf(ObjType.PERSON), "Tasks" to setOf(ObjType.TASK), "Files" to setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK),
                "Ideas & notes" to setOf(ObjType.NOTE, ObjType.IDEA), "Decisions" to setOf(ObjType.DECISION),
            ).mapNotNull { (name, types) -> s.live.filter { it.type in types && !(it.type == ObjType.TASK && it.done) }.takeIf { it.isNotEmpty() }?.let { name to it } }
                .mapIndexed { i, (name, items) -> val ord = order(items, today); val (st, late) = status(ord, today); Band("e:$name", null, name, Origin.AUTO, ord, st, late, i % 4, false) }
        }
    }

    fun kind(key: String) = Kind.entries.firstOrNull { it.key == key }

    // ------------------------------------------------------------ timeline ribbon
    data class Mark(val date: LocalDate, val title: String, val id: String?, val past: Boolean, val isTarget: Boolean = false, val isStart: Boolean = false)

    /** Milestones of a project in date order: its start, its events, its target date. Empty when it has fewer than two dates. */
    fun timeline(s: Snapshot, project: KObject, today: LocalDate = LocalDate.now()): List<Mark> {
        val events = s.neighbours(project.id, false).map { it.first }.filter { it.type == ObjType.EVENT && it.eventDate != null }
            .map { Mark(it.eventDate!!, it.title, it.id, it.eventDate.isBefore(today)) }
        val marks = (events + listOfNotNull(
            project.startDate?.takeIf { st -> events.none { it.date == st } }?.let { Mark(it, "Started", null, it.isBefore(today), isStart = true) },
            project.targetDate?.let { Mark(it, "Done", null, it.isBefore(today), isTarget = true) },
        )).sortedBy { it.date }
        return if (marks.size >= 2) marks else emptyList()
    }

    // ------------------------------------------------------------ covers on Home
    data class Cover(val project: KObject, val people: List<KObject>, val line: String, val late: Boolean, val progress: Float?, val nextAt: Float?)

    fun cover(s: Snapshot, p: KObject, today: LocalDate = LocalDate.now()): Cover {
        val m = members(s, p.id)
        // the people most involved: most items linked to them inside this project
        val ids = m.map { it.id }.toSet()
        val people = m.filter { it.type == ObjType.PERSON }.sortedWith(compareByDescending<KObject> { p -> s.neighbours(p.id, false).count { it.first.id in ids } }.thenBy { it.title }).take(3)
        val late = m.count { it.type == ObjType.TASK && !isWaiting(it) && effective(it)?.isBefore(today) == true }
        val next = timeline(s, p, today).firstOrNull { !it.past && !it.isTarget }
        val line = listOfNotNull(next?.let { "${it.title} ${shortDate(it.date)}" }, if (late > 0) "$late late" else null).joinToString(" · ")
            .ifEmpty { status(m, today).first }
        val start = p.startDate; val end = p.targetDate
        val span = if (start != null && end != null && end.isAfter(start)) ChronoUnit.DAYS.between(start, end).toFloat() else null
        val progress = span?.let { (ChronoUnit.DAYS.between(start, today) / it).coerceIn(0f, 1f) }
        val nextAt = if (span != null && next != null) (ChronoUnit.DAYS.between(start, next.date) / span).coerceIn(0f, 1f) else null
        return Cover(p, people, line, late > 0, progress, nextAt)
    }

    fun spaces(s: Snapshot): List<KObject> = s.ofType(ObjType.PROJECT)
        .filter { it.projectStatus != ProjectStatus.ARCHIVED && it.projectStatus != ProjectStatus.COMPLETED }
        .sortedWith(compareBy({ if (it.pinned) 0 else 1 }, { it.projectStatus?.ordinal ?: 9 }, { -it.updated }))

    // ------------------------------------------------------------ spaces at scale (design 0.7)
    /** Route key of the All spaces screen. */
    const val SPACES_KEY = "spaces"
    /** Spaces and the old Collections (which are just spaces that hold other spaces now). */
    fun allSpaces(s: Snapshot): List<KObject> = s.live.filter { (it.type == ObjType.PROJECT && it.projectStatus != ProjectStatus.ARCHIVED) || it.type == ObjType.COLLECTION }

    /** Spaces held by [id]: a space linked to another space or collection sits inside it. */
    fun inside(s: Snapshot, id: String): List<KObject> {
        val me = s.objects[id] ?: return emptyList()
        return s.neighbours(id, false).map { it.first }.filter { (it.type == ObjType.PROJECT || it.type == ObjType.COLLECTION) && it.id != id && parentOf(s, it)?.id == me.id }
    }
    /** The space a space sits in, if any: a collection it's linked to, or a space it's "part of". */
    fun parentOf(s: Snapshot, o: KObject): KObject? = s.neighbours(o.id, false).firstOrNull { (other, rel) ->
        other.type == ObjType.COLLECTION || (other.type == ObjType.PROJECT && rel.type == RelType.PART_OF && rel.sourceId == o.id)
    }?.first?.takeIf { it.id != o.id }

    /** How much needs attention in a space this week: late counts most, then due soon, then recent activity. */
    fun busyness(s: Snapshot, p: KObject, today: LocalDate): Double {
        val m = members(s, p.id)
        val late = m.count { it.type == ObjType.TASK && !isWaiting(it) && effective(it)?.isBefore(today) == true }
        val soon = m.count { it.type == ObjType.TASK && effective(it)?.let { d -> !d.isBefore(today) && ChronoUnit.DAYS.between(today, d) <= 7 } == true }
        val touched = (m.maxOfOrNull { it.updated } ?: p.updated).coerceAtLeast(p.updated)
        val days = (System.currentTimeMillis() - touched).coerceAtLeast(0) / 86_400_000.0
        return late * 3.0 + soon * 2.0 + 2.0 / (1.0 + days / 7.0)
    }

    /** Up to [max] spaces for Now: the ones you pinned, then the busiest. Second = true when Atlas picked it. */
    fun pinned(s: Snapshot, today: LocalDate, max: Int = 4): List<Pair<KObject, Boolean>> {
        val all = allSpaces(s).filter { it.type == ObjType.PROJECT && it.projectStatus != ProjectStatus.COMPLETED }
        val mine = all.filter { it.pinned }.sortedByDescending { it.updated }
        val auto = all.filter { !it.pinned }.map { it to busyness(s, it, today) }.filter { it.second > 0.6 }.sortedByDescending { it.second }.map { it.first }
        return (mine.map { it to false } + auto.map { it to true }).take(max)
    }

    /** Spaces with something due this week that aren't already pinned. */
    fun active(s: Snapshot, today: LocalDate, exclude: Set<String>): List<KObject> = allSpaces(s).filter { it.type == ObjType.PROJECT && it.id !in exclude }
        .filter { p -> members(s, p.id).any { it.type == ObjType.TASK && !it.done && effective(it)?.let { d -> ChronoUnit.DAYS.between(today, d) <= 7 } == true } }
        .sortedByDescending { busyness(s, it, today) }

    /** Nothing touched for a month and nothing due: folded away in All spaces. */
    fun quiet(s: Snapshot, p: KObject, today: LocalDate): Boolean {
        val m = members(s, p.id)
        val touched = (m.maxOfOrNull { it.updated } ?: 0L).coerceAtLeast(p.updated)
        val due = m.any { it.type == ObjType.TASK && !it.done && effective(it) != null }
        return !p.pinned && !due && System.currentTimeMillis() - touched > 30L * 86_400_000
    }

    /** A space's line on Now: its next milestone or what's due, and lateness. */
    fun spaceLine(s: Snapshot, p: KObject, today: LocalDate): Pair<String, Boolean> {
        if (p.type == ObjType.COLLECTION) return inside(s, p.id).joinToString(" · ") { it.title }.ifBlank { "Holds other spaces" } to false
        val c = cover(s, p, today)
        val nextTask = members(s, p.id).filter { it.type == ObjType.TASK && !it.done && effective(it) != null }.minByOrNull { effective(it)!! }
        val line = c.line.ifBlank { nextTask?.let { "${shortTitle(it.title)} ${whenShort(effective(it)!!, today)}" } ?: "Nothing due" }
        return line to c.late
    }

    // ------------------------------------------------------------ words
    private val MONTH = java.time.format.DateTimeFormatter.ofPattern("d MMM")
    fun shortDate(d: LocalDate) = d.format(MONTH)
    fun shortTitle(t: String) = t.removePrefix("Expect ").let { if (it.length > 28) it.take(26).trimEnd() + "…" else it }
    fun lateBy(d: LocalDate, today: LocalDate): String { val n = ChronoUnit.DAYS.between(d, today); return if (n == 1L) "1 day late" else "$n days late" }
    fun whenShort(d: LocalDate, today: LocalDate): String {
        val n = ChronoUnit.DAYS.between(today, d)
        return when {
            n == 0L -> "today"; n == 1L -> "tomorrow"
            n in 2..6 -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            n < 0 -> lateBy(d, today)
            else -> shortDate(d)
        }
    }
}
