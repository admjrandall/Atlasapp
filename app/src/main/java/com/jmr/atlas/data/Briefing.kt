package com.jmr.atlas.data

import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** The Now screen and the morning notification read the same deterministic briefing. No AI, every line is sourced. */
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

    fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))
    fun span(days: Long) = when { days < 7 -> "$days ${if (days == 1L) "day" else "days"}"; days < 60 -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"}"; else -> "${days / 30} months" }
    fun rel(d: LocalDate, today: LocalDate): String { val n = ChronoUnit.DAYS.between(d, today); return when (n) { 0L -> "today"; 1L -> "yesterday"; else -> "$n days ago" } }
}
