package com.jmr.atlas.data

import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Home and the morning notification read the same deterministic briefing. No AI, every line is sourced. */
object Briefing {
    data class Item(val task: KObject, val context: String, val late: Boolean)
    data class Data(
        val headline: String,
        val needsYou: List<Item>,
        val waiting: List<Pair<KObject, KObject?>>,
        val today: List<Calendars.Event>,
        val nextMilestone: KObject?,
    )

    fun build(s: Snapshot, events: List<Calendars.Event>, today: LocalDate = LocalDate.now()): Data {
        val open = s.ofType(ObjType.TASK).filter { !it.done }
        fun proj(t: KObject) = s.neighbours(t.id, false).map { it.first }.firstOrNull { it.type == ObjType.PROJECT }
        val waitingTasks = open.filter { it.title.startsWith("Expect ", true) }
        val actionable = open - waitingTasks.toSet()
        fun effective(t: KObject) = listOfNotNull(t.due, t.deadline).minOrNull()
        val overdue = actionable.filter { effective(it)?.isBefore(today) == true }
        val dueToday = actionable.filter { effective(it) == today }
        val needs = (overdue.sortedBy { effective(it) } + dueToday.sortedByDescending { it.priority.ordinal }).map { t ->
            val d = effective(t)!!
            val p = proj(t)?.title ?: s.neighbours(t.id, false).map { it.first }.let { n -> n.firstOrNull { it.type == ObjType.COLLECTION } ?: n.firstOrNull { it.type == ObjType.PERSON } }?.title
            val when_ = when {
                d.isBefore(today) -> "was due ${rel(d, today)}"
                t.deadline == today && t.due != today -> "deadline today"
                else -> "today"
            }
            Item(t, listOfNotNull(p, when_).joinToString(" · "), d.isBefore(today))
        }
        val waiting = waitingTasks.sortedBy { it.due ?: LocalDate.MAX }.map { t -> t to s.neighbours(t.id, false).map { it.first }.firstOrNull { it.type == ObjType.PERSON } }
        val todays = events.filter { it.day == today }
        val milestone = s.ofType(ObjType.EVENT).filter { it.eventDate != null && !it.eventDate.isBefore(today) }.minByOrNull { it.eventDate!! }

        val parts = mutableListOf<String>()
        if (overdue.isNotEmpty()) parts += "${overdue.size} overdue"
        if (dueToday.isNotEmpty()) parts += "${dueToday.size} due today"
        if (waiting.isNotEmpty()) parts += "waiting on ${waiting.size}"
        val first = todays.firstOrNull { !it.allDay && it.end > System.currentTimeMillis() }
        val tail = mutableListOf<String>()
        first?.let { tail += "${it.title} at ${time(it.begin)}" }
        milestone?.let { m -> val n = ChronoUnit.DAYS.between(today, m.eventDate); tail += "${m.title} ${if (n == 0L) "today" else "in " + span(n)}" }
        val headline = buildString {
            append(if (parts.isEmpty()) "Nothing is due today" else parts.joinToString(", ").replaceFirstChar { it.uppercase() })
            append(".")
            if (tail.isNotEmpty()) append(" " + tail.joinToString(". ") + ".")
        }
        return Data(headline, needs, waiting, todays, milestone)
    }

    // ================================================================ 0.6 Home: one sentence and one list
    /** A run of the briefing sentence. [id] makes it tappable: an object id, or "space:<projectId>". */
    data class Seg(val text: String, val id: String? = null)

    data class Agenda(val task: KObject, val whenText: String, val late: Boolean, val waiting: Boolean, val space: KObject?)

    private val EXPECT = Regex("^Expect (.+?) from (.+)$", RegexOption.IGNORE_CASE)

    fun projectOf(s: Snapshot, o: KObject): KObject? = s.neighbours(o.id, false).map { it.first }.firstOrNull { it.type == ObjType.PROJECT }

    /** “Two things slipped: A and B. Jamie owes you an updated estimate Wednesday. Demolition starts in 3 weeks.” Deterministic. */
    fun sentence(s: Snapshot, today: LocalDate = LocalDate.now()): List<Seg> {
        val open = s.ofType(ObjType.TASK).filter { !it.done }
        val waiting = open.filter { Board.isWaiting(it) }
        val act = open - waiting.toSet()
        val late = act.filter { Board.effective(it)?.isBefore(today) == true }.sortedBy { Board.effective(it) }
        val out = mutableListOf<Seg>()
        fun t(x: String) { out += Seg(x) }
        fun e(o: KObject, text: String = o.title) { out += Seg(text, o.id) }
        when (late.size) {
            0 -> {}
            1 -> { e(late[0]); t(" is ${Board.lateBy(Board.effective(late[0])!!, today)}. ") }
            2 -> { t("Two things slipped: "); e(late[0]); t(" and "); e(late[1]); t(". ") }
            else -> { t("${late.size} things slipped, starting with "); e(late[0]); t(". ") }
        }
        val soon = act.filter { Board.effective(it)?.let { d -> !d.isBefore(today) && ChronoUnit.DAYS.between(today, d) <= 6 } == true }.minByOrNull { Board.effective(it)!! }
        if (soon != null) { e(soon); t(" is due ${Board.whenShort(Board.effective(soon)!!, today)}. ") }
        val w = waiting.filter { it.due == null || ChronoUnit.DAYS.between(today, it.due) <= 7 }.minByOrNull { it.due ?: LocalDate.MAX }
        val m = w?.let { EXPECT.find(it.title) }
        if (w != null && m != null) {
            val who = s.neighbours(w.id, false).map { it.first }.firstOrNull { it.type == ObjType.PERSON } ?: s.ofType(ObjType.PERSON).firstOrNull { it.title.equals(m.groupValues[2].trim(), true) }
            if (who != null) { e(who, who.firstName); t(" owes you "); e(w, articled(m.groupValues[1])); t(w.due?.let { " " + Board.whenShort(it, today) }.orEmpty() + ". ") }
        }
        val ms = s.ofType(ObjType.EVENT).filter { it.eventDate != null && !it.eventDate.isBefore(today) && ChronoUnit.DAYS.between(today, it.eventDate) <= 90 }.minByOrNull { it.eventDate!! }
        if (ms != null) {
            val n = ChronoUnit.DAYS.between(today, ms.eventDate)
            val p = projectOf(s, ms)
            out += Seg(ms.title, p?.let { "space:${it.id}" } ?: ms.id)
            t(if (n == 0L) " is today." else " in ${span(n)}.")
        }
        if (out.isEmpty()) t("Nothing is late and nothing is due this week.")
        return out.mapIndexed { i, sg -> if (i == out.lastIndex) sg.copy(text = sg.text.trimEnd()) else sg }
    }

    private fun articled(x: String): String { val v = x.trim(); return if (Regex("^(an?|the|his|her|their|your|my) ", RegexOption.IGNORE_CASE).containsMatchIn(v)) v else "an ${v}".replace(Regex("^an ([^aeiouAEIOU])"), "a $1") }

    /** This week, once: late first, then by date; things you're waiting on show who you're waiting on. */
    fun week(s: Snapshot, today: LocalDate = LocalDate.now()): List<Agenda> {
        val open = s.ofType(ObjType.TASK).filter { !it.done }
        val rows = open.mapNotNull { t ->
            val d = Board.effective(t); val waiting = Board.isWaiting(t)
            val include = when { d == null -> false; d.isBefore(today) -> true; else -> ChronoUnit.DAYS.between(today, d) <= 7 }
            if (!include) return@mapNotNull null
            val who = if (waiting) s.neighbours(t.id, false).map { it.first }.firstOrNull { it.type == ObjType.PERSON } else null
            val late = !waiting && d!!.isBefore(today)
            val w = when { late -> "Late · " + Board.lateBy(d!!, today).removeSuffix(" late"); else -> Board.whenShort(d!!, today).replaceFirstChar { it.uppercase() } } +
                (if (waiting) " · waiting on ${who?.firstName ?: "someone"}" else "")
            Agenda(t, w, late, waiting, projectOf(s, t))
        }
        return rows.sortedWith(compareBy({ if (it.late) 0 else 1 }, { Board.effective(it.task) }, { it.task.title }))
    }

    fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))
    fun span(days: Long) = when { days < 7 -> "$days ${if (days == 1L) "day" else "days"}"; days < 60 -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"}"; else -> "${days / 30} months" }
    fun rel(d: LocalDate, today: LocalDate): String { val n = ChronoUnit.DAYS.between(d, today); return when (n) { 0L -> "today"; 1L -> "yesterday"; else -> "$n days ago" } }
}
