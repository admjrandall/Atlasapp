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
    /**
     * What a line becomes. TASK/NOTE/IDEA are new items; STEP, REMEMBER and UPDATE add to the item you're on
     * (a task's checklist, a person's things to remember, a project's status updates).
     */
    enum class Kind(val label: String, val type: ObjType) {
        TASK("Task", ObjType.TASK), NOTE("Note", ObjType.NOTE), IDEA("Idea", ObjType.IDEA),
        STEP("Step", ObjType.TASK), REMEMBER("Remember", ObjType.NOTE), UPDATE("Update", ObjType.NOTE),
        /** "New space Ankle Surgery": makes a space (a project) to put things in. */
        SPACE("Space", ObjType.PROJECT);
        val addsToContext get() = this == STEP || this == REMEMBER || this == UPDATE
    }

    /** The kinds on offer for a line typed while looking at [context]: new items always, plus what that item can hold. */
    fun kindsFor(context: KObject?): List<Kind> = listOf(Kind.TASK, Kind.NOTE, Kind.IDEA, Kind.SPACE) + when (context?.type) {
        ObjType.TASK -> listOf(Kind.STEP); ObjType.PERSON -> listOf(Kind.REMEMBER); ObjType.PROJECT -> listOf(Kind.UPDATE); else -> emptyList()
    }

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
        /** Items named with @ ("@estimate", "@Kitchen layout") — linked when saved. */
        val mentions: List<KObject> = emptyList(),
        /** Where you said to put it ("add to Ankle Surgery", "@Ankle Surgery") when that already exists. */
        val target: KObject? = null,
        /** …and when it doesn't: a space by this name is made when you send. */
        val newSpace: String? = null,
    )

    private const val THIS = "(?:\\s+(?:this|it|these|them|that))?"
    /** "add to X", "add this to X", "put it in X", "file under X", "save to X" — at the end of the line. */
    private val ADD_TO = Regex("(?:^|[\\s,;.-]+)(?:add$THIS\\s+to|put$THIS\\s+in(?:to)?|file$THIS\\s+(?:under|in)|save$THIS\\s+(?:to|in))\\s+(?:the\\s+|my\\s+)?([\\p{L}\\p{N}][\\p{L}\\p{N}'&\\- ]{0,48})[.!]?$", RegexOption.IGNORE_CASE)
    /** "new space Ankle Surgery", "create a group called Ankle Surgery", "new project: Ankle Surgery". */
    private val NEW_SPACE = Regex("^(?:new|create|make|start)\\s+(?:a\\s+)?(?:new\\s+)?(?:space|group|project|folder)\\s*(?:called|named|for|:)?\\s*(.+)$", RegexOption.IGNORE_CASE)

    /** A name as people write it: "ankle surgery" → "Ankle Surgery"; anything already capitalised is left alone. */
    fun nameCase(t: String): String = t.trim().let { n -> if (n == n.lowercase()) n.split(Regex("\\s+")).joinToString(" ") { w -> if (w.length <= 2 && w in setOf("of", "on", "in", "to", "at", "an", "a")) w else w.replaceFirstChar { it.uppercase() } }.replaceFirstChar { it.uppercase() } else n }

    /** An existing item called [name]: same title first, then a project or person whose title contains it. */
    fun find(name: String, s: Snapshot): KObject? {
        val n = name.trim(); if (n.length < 2) return null
        val live = s.live.filter { it.type != ObjType.COLLECTION }
        return live.firstOrNull { it.title.equals(n, true) }
            ?: live.filter { (it.type == ObjType.PROJECT || it.type == ObjType.PERSON) && it.title.contains(n, true) }.maxByOrNull { it.updated }
    }

    private val MENTION = Regex("@([\\p{L}\\p{N}][\\p{L}\\p{N}'-]*(?:\\s+[\\p{L}\\p{N}][\\p{L}\\p{N}'-]*){0,3})")

    /** Resolves "@words" to an existing item: the longest run of words that matches a title, people first, then projects, then the rest. */
    fun mentions(text: String, s: Snapshot): List<Pair<String, KObject>> = MENTION.findAll(text).mapNotNull { m ->
        val words = m.groupValues[1].split(Regex("\\s+"))
        (words.size downTo 1).asSequence().mapNotNull { n ->
            val q = words.take(n).joinToString(" ")
            s.live.filter { it.title.contains(q, true) && it.type != ObjType.COLLECTION }
                .sortedWith(compareBy({ if (it.title.equals(q, true)) 0 else 1 }, { when (it.type) { ObjType.PERSON -> 0; ObjType.PROJECT -> 1; else -> 2 } }, { -it.updated }))
                .firstOrNull()?.let { "@$q" to it }
        }.firstOrNull()
    }.toList()

    private val VERBS = setOf("order", "call", "book", "buy", "email", "text", "send", "pay", "finalize", "finish", "compare", "pick", "schedule", "ask",
        "get", "fix", "check", "review", "write", "renew", "cancel", "return", "submit", "sign", "print", "clean", "measure", "update", "follow", "remind",
        "make", "file", "prepare", "price", "paint", "hire", "move", "try", "draft", "share", "talk", "pick", "sort", "decide", "choose", "test", "look", "plan", "confirm", "reply", "respond", "meet", "visit", "drop", "bring", "install", "replace", "research", "find", "set")
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
        NEW_SPACE.find(raw)?.let { m ->
            val name = nameCase(m.groupValues[1].trim().trim('"', '“', '”', '.', ':'))
            if (name.isNotBlank()) return Parsed(raw, false, Kind.SPACE, name.take(80), null, null, null, people(raw, s).filter { !name.contains(it.title, true) },
                target = find(name, s)?.takeIf { it.type == ObjType.PROJECT && it.title.equals(name, true) })
        }
        // where you said to put it — taken out of the line before anything else reads it
        var body = raw
        var target: KObject? = null; var newSpace: String? = null
        ADD_TO.find(body)?.let { m ->
            val name = m.groupValues[1].trim().trimEnd('.', ',', '!')
            if (name.split(Regex("\\s+")).size <= 6) {
                target = find(name, s); if (target == null) newSpace = nameCase(name)
                body = body.removeRange(m.range).trim()
            }
        }
        // "@Ankle Surgery" for something that isn't in Atlas yet: the capitalised words after @ (or the one word)
        val resolved = mentions(body, s).map { it.first }
        MENTION.findAll(body).firstOrNull { m -> resolved.none { r -> m.value.startsWith(r) } }?.let { m ->
            if (target == null && newSpace == null) {
                val ws = m.groupValues[1].split(Regex("\\s+"))
                val run = if (ws[0].first().isUpperCase()) ws.takeWhile { it.first().isUpperCase() } else ws.take(1)
                val token = "@" + run.joinToString(" ")
                newSpace = nameCase(run.joinToString(" "))
                body = body.replace(token, " ").replace(Regex("\\s{2,}"), " ").trim()
            }
        }
        val explicit = target != null || newSpace != null
        val ments = mentions(body, s)
        val q = !explicit && isQuestion(body)
        val ppl = people(body, s)
        val sp = (target as KObject?)?.takeIf { it.type == ObjType.PROJECT } ?: if (explicit) null else space(body, s, ppl, currentSpace)
        val dt = if (q) null else date(body, today)
        var title = body
        dt?.let { title = title.replace(it.first, " ") }
        ments.forEach { (token, o) -> title = title.replace(token, if (o.type == ObjType.PERSON) o.firstName else " ") }
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
        title = title.replaceFirstChar { it.uppercase() }.ifBlank { if (explicit) "" else raw }
        val mentioned = ments.map { it.second }.distinctBy { it.id }
        val tgt = target as KObject?
        return Parsed(raw, q, if (title.isBlank() && newSpace != null) Kind.SPACE else kind, if (title.isBlank() && newSpace != null) newSpace!! else title.take(160), dt?.second, dt?.first?.trim(),
            sp ?: mentioned.firstOrNull { it.type == ObjType.PROJECT }, (ppl + mentioned.filter { it.type == ObjType.PERSON } + listOfNotNull(tgt?.takeIf { it.type == ObjType.PERSON })).distinctBy { it.id },
            (mentioned + listOfNotNull(tgt)).filter { it.type != ObjType.PERSON && it.type != ObjType.PROJECT }.distinctBy { it.id },
            target = tgt, newSpace = if (title.isBlank()) null else newSpace)
    }

    /** Words worth searching for in a question ("what could delay demolition?" → "delay demolition"). */
    fun searchTerms(question: String): String =
        Regex("[\\p{L}\\p{N}']+").findAll(question.lowercase()).map { it.value }.filter { it !in QWORDS && it.length > 2 && it !in STOP && it !in setOf("could", "would", "should", "about", "there", "anything", "something", "need", "left", "before", "after", "next", "still") }
            .joinToString(" ")

    // ------------------------------------------------------------ intent (design 0.7)
    /** What a line in the Atlas box is for. Decided by code in microseconds; the model is only called for ASK_WORLD and CHAT. */
    enum class Intent { FIND, CAPTURE, ASK_MINE, ASK_WORLD, CHAT }

    /** Words that make a question about the person's own things rather than about the world. */
    private val MINE = setOf("my", "mine", "late", "overdue", "due", "owe", "owes", "owed", "waiting", "left", "todo", "tasks", "task", "today", "tonight",
        "tomorrow", "week", "weekend", "schedule", "agenda", "plate", "slipped", "remind", "reminders", "upcoming", "deadline", "deadlines", "atlas")
    private val CAPTURE_PREFIX = Regex("^(task|todo|to do|idea|note|remember|remind me|don'?t forget)\\b[:,]?", RegexOption.IGNORE_CASE)

    /**
     * Routes a line. [hits] = Atlas's search for the line (for a question, for its content words); [inChat] = the last
     * thing Atlas said was a general answer, so a follow-up ("and at sunset?", "tell me more") continues that conversation.
     */
    fun intent(text: String, s: Snapshot, hits: List<com.jmr.atlas.data.Search.Hit>, inChat: Boolean, touchesMine: Boolean = false): Intent {
        val raw = text.trim()
        if (raw.isEmpty()) return Intent.FIND
        val p = parse(raw, s)
        val ws = com.jmr.atlas.data.Search.words(raw)
        if (p.target != null || p.newSpace != null || p.kind == Kind.SPACE || CAPTURE_PREFIX.containsMatchIn(raw) || (!p.question && p.due != null)) return Intent.CAPTURE
        if (p.question) {
            val mine = touchesMine || ws.any { it in MINE } || p.people.isNotEmpty() || p.mentions.isNotEmpty() || hits.any { it.inTitle && it.score >= 7.0 }
            return when { mine -> Intent.ASK_MINE; inChat -> Intent.CHAT; else -> Intent.ASK_WORLD }
        }
        if (inChat) return Intent.CHAT
        if (p.kind == Kind.TASK || p.kind == Kind.IDEA) return Intent.CAPTURE
        return if (ws.size <= 4 && (hits.isNotEmpty() || ws.size <= 2)) Intent.FIND else Intent.CAPTURE
    }
}
