package com.jmr.atlas.capture

import com.jmr.atlas.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Reads what you typed in the input box the way you'd read a text message (design 0.6, replaces the "New task" form).
 * "Order tile samples by Friday for the kitchen" → Task · "Order tile samples" · due Fri · in Home Renovation.
 * Every part is shown before saving and can be changed with a tap. Runs on the phone; deterministic; unit-tested.
 */
object Sentence {
    enum class Kind(val label: String, val type: ObjType) { TASK("Task", ObjType.TASK), NOTE("Note", ObjType.NOTE), IDEA("Idea", ObjType.IDEA) }

    data class Parsed(
        val raw: String,
        val question: Boolean,
        val kind: Kind,
        val title: String,
        val due: LocalDate?,
        /** The words that set the date ("by Friday"), so the preview can show what was read. */
        val dueWords: String?,
        val space: KObject?,
        val people: List<KObject>,
    )

    private val VERBS = setOf("order", "call", "book", "buy", "email", "text", "send", "pay", "finalize", "finish", "compare", "pick", "schedule", "ask",
        "get", "fix", "check", "review", "write", "renew", "cancel", "return", "submit", "sign", "print", "clean", "measure", "update", "follow", "remind",
        "make", "file", "prepare", "plan", "confirm", "reply", "respond", "meet", "visit", "drop", "bring", "install", "replace", "research", "find", "set")
    private val QWORDS = setOf("what", "whats", "what's", "when", "who", "whos", "who's", "why", "how", "where", "which", "is", "are", "do", "does", "did", "can", "could", "should", "will", "has", "have", "show", "find", "list")
    private val WH = setOf("what", "whats", "what's", "when", "whens", "who", "whos", "who's", "why", "how", "where", "wheres", "where's", "which")
    private val STOP = setOf("the", "and", "for", "with", "from", "this", "that", "into", "about", "your", "have", "will", "they", "them", "then", "than", "need", "needs")
    private val DAYS = DayOfWeek.entries.associateBy { it.name.lowercase() } + DayOfWeek.entries.associateBy { it.name.lowercase().take(3) }
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    fun isQuestion(text: String): Boolean {
        val t = text.trim(); if (t.isEmpty()) return false
        if (t.endsWith("?")) return true
        val first = t.lowercase().split(Regex("\\s+")).first().trim(',', '.')
        // wh-words always ask; "is/does/can…" only ask with a "?" (so "Do laundry" stays a task)
        return first in WH || first in setOf("show", "list") && t.lowercase().contains(" me ")
    }

    /** A date phrase and the date it means, if the text has one. */
    fun date(text: String, today: LocalDate): Pair<String, LocalDate>? {
        val low = text.lowercase()
        fun hit(r: Regex, f: (MatchResult) -> LocalDate?): Pair<String, LocalDate>? = r.find(low)?.let { m -> f(m)?.let { text.substring(m.range) to it } }
        val pre = "(?:\\b(?:by|on|due|before|until|this|for)\\s+)?"
        return hit(Regex("$pre\\b(today|tonight)\\b")) { today }
            ?: hit(Regex("$pre\\btomorrow\\b")) { today.plusDays(1) }
            ?: hit(Regex("\\bin (\\d{1,2}) (day|days|week|weeks)\\b")) { m -> val n = m.groupValues[1].toLong(); if (m.groupValues[2].startsWith("week")) today.plusWeeks(n) else today.plusDays(n) }
            ?: hit(Regex("(?:\\b(?:by|on|due|before|until|for)\\s+)?\\b(next\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\\b")) { m ->
                val d = DAYS[m.groupValues[2]] ?: return@hit null
                val base = today.with(TemporalAdjusters.next(d)); if (m.groupValues[1].isNotBlank() && base.isBefore(today.plusDays(7))) base.plusWeeks(1) else base }
            ?: hit(Regex("$pre\\b(${MONTHS.joinToString("|")})[a-z]*\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b")) { m -> md(MONTHS.indexOf(m.groupValues[1]) + 1, m.groupValues[2].toInt(), today) }
            ?: hit(Regex("$pre\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(${MONTHS.joinToString("|")})[a-z]*\\b")) { m -> md(MONTHS.indexOf(m.groupValues[2]) + 1, m.groupValues[1].toInt(), today) }
            ?: hit(Regex("$pre\\b(\\d{1,2})/(\\d{1,2})\\b")) { m -> md(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today) }
    }

    private fun md(month: Int, day: Int, today: LocalDate): LocalDate? = runCatching {
        val d = LocalDate.of(today.year, month, day); if (d.isBefore(today.minusDays(1))) d.plusYears(1) else d
    }.getOrNull()

    private fun words(s: String) = Regex("[\\p{L}\\p{N}']+").findAll(s.lowercase()).map { it.value.trim('\'') }.filter { it.length >= 4 && it !in STOP }.toSet()

    /** People named in the text: full name, or a first name that only one person has. */
    fun people(text: String, s: Snapshot): List<KObject> {
        val all = s.ofType(ObjType.PERSON)
        return all.filter { p ->
            Regex("\\b${Regex.escape(p.title)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                (p.firstName.length >= 3 && Regex("\\b${Regex.escape(p.firstName)}('s)?\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) && all.count { it.firstName.equals(p.firstName, true) } == 1)
        }
    }

    /** The space a sentence belongs to: the one you're in, else the project whose words or people it mentions most. */
    fun space(text: String, s: Snapshot, people: List<KObject>, current: String?): KObject? {
        current?.let { id -> s.objects[id]?.takeIf { it.type == ObjType.PROJECT && !it.trashed }?.let { return it } }
        val w = words(text)
        val projects = s.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED && it.projectStatus != ProjectStatus.COMPLETED }
        val scored = projects.map { p ->
            val vocab = words(p.title + " " + p.body + " " + p.summary)
            val titleHit = if (text.contains(p.title, true)) 5 else 0
            val wordHits = (w intersect vocab).size * 2
            val peopleHits = people.count { person -> s.neighbours(person.id, false).any { it.first.id == p.id } }
            p to (titleHit + wordHits + peopleHits)
        }.filter { it.second > 0 }.sortedByDescending { it.second }
        if (scored.isEmpty()) return null
        if (scored.size > 1 && scored[0].second == scored[1].second) return null
        return scored[0].first
    }

    fun parse(text: String, s: Snapshot, today: LocalDate = LocalDate.now(), currentSpace: String? = null): Parsed {
        val raw = text.trim()
        val q = isQuestion(raw)
        val ppl = people(raw, s)
        val sp = space(raw, s, ppl, currentSpace)
        val dt = if (q) null else date(raw, today)
        var title = raw
        dt?.let { title = title.replace(it.first, " ") }
        // trailing "for the kitchen" / "for Home Renovation" just says where it goes
        title = title.replace(Regex("\\s+(?:for|in|re:?)\\s+(?:the\\s+)?[\\p{L}' ]{3,30}$", RegexOption.IGNORE_CASE)) { m ->
            if (sp != null && (words(m.value) intersect words(sp.title + " " + sp.body)).isNotEmpty()) "" else m.value }
        title = title.replace(Regex("\\s{2,}"), " ").trim().trim(',', ';', '-', '.').trim()
        val idea = Regex("^(idea|maybe|what if)\\b[:,]?\\s*", RegexOption.IGNORE_CASE)
        val first = title.lowercase().split(Regex("\\s+")).firstOrNull().orEmpty()
        val kind = when {
            idea.containsMatchIn(title) -> Kind.IDEA
            first in VERBS || dt != null -> Kind.TASK
            else -> Kind.NOTE
        }
        if (kind == Kind.IDEA) title = title.replace(idea, "")
        title = title.replaceFirstChar { it.uppercase() }.ifBlank { raw }
        return Parsed(raw, q, kind, title.take(160), dt?.second, dt?.first?.trim(), sp, ppl)
    }

    /** Words worth searching for in a question ("what could delay demolition?" → "delay demolition"). */
    fun searchTerms(question: String): String =
        Regex("[\\p{L}\\p{N}']+").findAll(question.lowercase()).map { it.value }.filter { it !in QWORDS && it.length > 2 && it !in STOP && it !in setOf("could", "would", "should", "about", "there", "anything", "something", "need", "left", "before", "after", "next", "still") }
            .joinToString(" ")
}
