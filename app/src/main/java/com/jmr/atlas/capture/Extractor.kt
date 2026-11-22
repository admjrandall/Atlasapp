package com.jmr.atlas.capture

import com.jmr.atlas.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Deterministic, on-device understanding of captured text (no AI). Every proposal quotes the evidence it came from.
 * Phase 4 adds an AI provider that produces the same Proposal shape, validated against the same rules.
 */
object Extractor {

    data class Proposal(
        val key: String,
        val kind: SuggestionKind,
        val title: String,
        val evidence: String,
        val objType: ObjType? = null,
        val targetId: String? = null,
        val relType: RelType = RelType.RELATED_TO,
        val due: LocalDate? = null,
        val linkKeys: List<String> = emptyList(),
    )

    private val NOT_NAMES = setOf("I", "He", "She", "They", "We", "It", "The", "A", "An", "My", "Our", "This", "That", "Today", "Tomorrow", "Yesterday", "Remember",
        "Atlas", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December", "Also", "Then", "And", "But", "So", "Need", "Call", "Email", "Talked", "Met", "Follow", "Please", "Hi", "Thanks")
    private const val NAME = "([A-Z][a-z]+(?:\\s[A-Z][a-z]+)?)"
    private val cueBefore = Regex("\\b(?i:talked|spoke|met|meeting|called|call|emailed|texted|chatted|lunch|dinner|coffee|visited|heard from|got a quote from|quote from)\\s+(?i:with\\s+|to\\s+)?$NAME")
    private val cueAfter = Regex("\\b$NAME\\s+(?:said|says|told me|will|promised|mentioned|recommended|suggested|agreed|sent|asked|thinks|called|emailed|quoted)\\b")
    private val topicRx = Regex("\\babout\\s+(.+?)(?:[.;!?]|,|\\s+and\\s+|$)", RegexOption.IGNORE_CASE)
    private val actionRx = Regex("\\b(?:remember to|need to|have to|must|don't forget to|dont forget to|remind me to|should|todo:?|to do:?)\\s+(.+?)(?:[.!?]|$)", RegexOption.IGNORE_CASE)
    private val sendRx = Regex("\\b(\\w+)\\s+(?:said\\s+(?:he|she|they)(?:'d|\\s+would)\\s+|will\\s+|'ll\\s+|is going to\\s+|promised to\\s+|(?:could|can|would)\\s+)?send\\s+(?:me|us)\\s+(?:an?\\s+|the\\s+)?(.+?)(?:\\s+(?:by|on|before))?(?:\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|tomorrow|next week))?(?:[.,;!]|$)", RegexOption.IGNORE_CASE)
    private val startRx = Regex("\\b(?:could|can|will|plans? to|going to|wants to)\\s+start\\s+(next month|next week|tomorrow|on\\s+\\w+day|in\\s+(?:january|february|march|april|may|june|july|august|september|october|november|december)|(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday))", RegexOption.IGNORE_CASE)
    private val STOP = setOf("about", "after", "again", "also", "because", "before", "being", "could", "doing", "during", "estimate", "every", "follow", "friday", "monday",
        "month", "next", "other", "remember", "replacing", "saturday", "should", "sunday", "talked", "thursday", "today", "tomorrow", "tuesday", "wednesday", "their",
        "there", "these", "thing", "think", "those", "updated", "which", "while", "would", "week", "with", "said", "send", "start", "will", "from", "have", "that", "this", "what", "when")

    fun extract(text: String, snap: Snapshot, today: LocalDate = LocalDate.now()): List<Proposal> {
        val out = mutableListOf<Proposal>()
        val sentences = text.split(Regex("(?<=[.!?])\\s+|\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        fun sentenceWith(s: String) = sentences.firstOrNull { it.contains(s, ignoreCase = true) } ?: text.take(160)

        // ---- people: existing first (by full name, then distinctive first name), then new
        val people = snap.ofType(ObjType.PERSON)
        val mentionedExisting = linkedMapOf<String, KObject>()
        people.forEach { p ->
            val first = p.title.substringBefore(" ")
            val last = p.title.substringAfter(" ", "")
            // "Jamie" alone can mean Jamie Carter; "Jamie Lee" is someone else.
            val firstOnly = Regex("\\b${Regex.escape(first)}\\b(?:\\s+([A-Z][a-z]+))?").findAll(text).any { m ->
                val next = m.groupValues[1]; next.isEmpty() || next in NOT_NAMES || next.equals(last, true)
            }
            val hit = Regex("\\b${Regex.escape(p.title)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                (first.length >= 3 && firstOnly && people.count { it.title.substringBefore(" ").equals(first, true) } == 1)
            if (hit) mentionedExisting[p.id] = p
        }
        mentionedExisting.values.forEach { p ->
            out += Proposal("p:${p.id}", SuggestionKind.LINK_EXISTING, p.title, "“${sentenceWith(p.title.substringBefore(" "))}”", ObjType.PERSON, p.id, RelType.DISCUSSED_WITH)
        }
        val newNames = (cueBefore.findAll(text).map { it.groupValues[1] } + cueAfter.findAll(text).map { it.groupValues[1] })
            .map { it.trim() }.filter { n -> n.split(" ").none { it in NOT_NAMES } }
            .filter { n -> mentionedExisting.values.none { p -> p.title.equals(n, true) || p.title.substringBefore(" ").equals(n.substringBefore(" "), true) } }
            .distinct().take(4).toList()
        newNames.forEach { n -> out += Proposal("new:$n", SuggestionKind.NEW_PERSON, n, "“${sentenceWith(n)}”", ObjType.PERSON, relType = RelType.DISCUSSED_WITH) }
        val personKeys = out.filter { it.objType == ObjType.PERSON }.map { it.key }
        val firstPersonName = out.firstOrNull { it.objType == ObjType.PERSON }?.title

        // ---- projects: named, connected to a mentioned person, or sharing a distinctive word
        val projects = snap.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED }
        val projHits = linkedMapOf<String, String>()
        projects.forEach { pr -> if (Regex("\\b${Regex.escape(pr.title)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) projHits[pr.id] = "Mentions “${pr.title}”" }
        mentionedExisting.values.forEach { p ->
            snap.neighbours(p.id, includeProposed = false).filter { it.first.type == ObjType.PROJECT && it.first.projectStatus != ProjectStatus.ARCHIVED }
                .forEach { (pr, _) -> projHits.putIfAbsent(pr.id, "${p.title} is already connected to ${pr.title}") }
        }
        // Capitalized words (names) never count as topical overlap — "Jamie" says nothing about which project.
        val nameWords = Regex("\\b[A-Z][a-z]+").findAll(text).map { it.value.lowercase() }.toSet() + people.flatMap { it.title.lowercase().split(" ") }
        val words = Regex("[A-Za-z]{4,}").findAll(text.lowercase()).map { it.value }.filter { it !in STOP && it !in nameWords }.toList().distinct()
        projects.filter { it.id !in projHits }.forEach { pr ->
            val own = "${pr.title} ${pr.body}".lowercase()
            val around = snap.neighbours(pr.id, false).filter { it.first.type != ObjType.PERSON }.joinToString(" ") { it.first.title }.lowercase()
            val hitOwn = words.firstOrNull { w -> Regex("\\b${w}s?\\b").containsMatchIn(own) }
            val hitAround = if (hitOwn == null) words.firstOrNull { w -> Regex("\\b${w}s?\\b").containsMatchIn(around) } else null
            (hitOwn ?: hitAround)?.let { w -> projHits[pr.id] = "Mentions “$w”, which appears in ${pr.title}${if (hitOwn == null) "'s connected items" else ""}" }
        }
        projHits.entries.take(2).forEach { (id, why) ->
            out += Proposal("pr:$id", SuggestionKind.LINK_EXISTING, snap.objects[id]!!.title, why, ObjType.PROJECT, id, RelType.BELONGS_TO)
        }
        val projectKeys = out.filter { it.objType == ObjType.PROJECT }.map { it.key }

        // ---- other existing objects named verbatim (documents, ideas, collections) — reference links
        snap.live.filter { it.type in setOf(ObjType.DOCUMENT, ObjType.IDEA, ObjType.COLLECTION, ObjType.DECISION) && it.title.length >= 5 }
            .filter { Regex("\\b${Regex.escape(it.title)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) }.take(3)
            .forEach { o -> out += Proposal("o:${o.id}", SuggestionKind.LINK_EXISTING, o.title, "Mentions “${o.title}”", o.type, o.id, RelType.REFERENCES) }

        val topic = topicRx.find(text)?.groupValues?.get(1)?.trim()?.removePrefix("the ")?.takeIf { it.length in 3..60 }
        val existingTasks = snap.ofType(ObjType.TASK).filter { !it.done }.map { it.title.lowercase() }
        fun isDup(t: String) = existingTasks.any { it == t.lowercase() }
        val links = personKeys.take(1) + projectKeys.take(1)

        // ---- expected deliverables: "He will send me an updated estimate Friday"
        sendRx.findAll(text).forEach { m ->
            val who = m.groupValues[1].let { if (it.lowercase() in setOf("he", "she", "they")) firstPersonName else it.takeIf { n -> n.first().isUpperCase() && n !in NOT_NAMES } }
            val thing = m.groupValues[2].trim().removeSuffix(".").takeIf { it.length in 3..50 } ?: return@forEach
            val due = m.groupValues[3].takeIf { it.isNotBlank() }?.let { findDate(it, today) } ?: findDate(m.value, today)
            val title = "Expect $thing" + (who?.let { " from $it" } ?: "")
            if (!isDup(title)) out += Proposal("t:expect:$thing", SuggestionKind.NEW_TASK, title.replaceFirstChar { it.uppercase() }, "“${m.value.trim()}”", ObjType.TASK, due = due, linkKeys = links)
        }
        // ---- explicit actions: "Remember to follow up"
        actionRx.findAll(text).forEach { m ->
            var action = m.groupValues[1].trim()
            if (action.length < 3 || action.length > 90) return@forEach
            if (Regex("^follow[ -]?up\\b", RegexOption.IGNORE_CASE).containsMatchIn(action) && action.split(" ").size <= 3)
                action = "Follow up" + (firstPersonName?.let { " with $it" } ?: "") + (topic?.let { " about $it" } ?: "")
            val title = action.replaceFirstChar { it.uppercase() }
            val due = findDate(m.value, today)
            if (!isDup(title)) out += Proposal("t:act:$title", SuggestionKind.NEW_TASK, title, "“${m.value.trim()}”", ObjType.TASK, due = due, linkKeys = links)
        }
        // ---- possible milestone: "he could start next month"
        startRx.find(text)?.let { m ->
            val date = findDate(m.groupValues[1], today)
            val title = (topic?.replaceFirstChar { it.uppercase() } ?: "Work") + " could start"
            out += Proposal("e:start", SuggestionKind.NEW_EVENT, title, "“${sentenceWith(m.value)}”", ObjType.EVENT, due = date, linkKeys = links)
        }
        return out
    }

    // ---------------------------------------------------------------- dates
    private val weekdays = DayOfWeek.entries.associateBy { it.name.lowercase() }
    private val months = Month.entries.associateBy { it.name.lowercase() }

    fun findDate(s: String, today: LocalDate): LocalDate? {
        val t = s.lowercase()
        if (Regex("\\btomorrow\\b").containsMatchIn(t)) return today.plusDays(1)
        if (Regex("\\btoday\\b|\\btonight\\b").containsMatchIn(t) && !Regex("\\btalked|met|spoke").containsMatchIn(t)) return today
        if (Regex("\\bnext month\\b").containsMatchIn(t)) return today.plusMonths(1).withDayOfMonth(1)
        if (Regex("\\bnext week\\b").containsMatchIn(t)) return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        Regex("\\bin (\\d{1,2}) days?\\b").find(t)?.let { return today.plusDays(it.groupValues[1].toLong()) }
        Regex("\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})\\b").find(t)?.let { m ->
            val d = runCatching { LocalDate.of(today.year, months[m.groupValues[1]]!!, m.groupValues[2].toInt()) }.getOrNull() ?: return null
            return if (d.isBefore(today)) d.plusYears(1) else d
        }
        Regex("\\bin (january|february|march|april|may|june|july|august|september|october|november|december)\\b").find(t)?.let { m ->
            val d = LocalDate.of(today.year, months[m.groupValues[1]]!!, 1); return if (d.isBefore(today)) d.plusYears(1) else d
        }
        Regex("\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").find(t)?.let { m ->
            return today.with(TemporalAdjusters.next(weekdays[m.groupValues[1]]!!))
        }
        return null
    }
}
