package com.jmr.atlas.data

import com.jmr.atlas.model.*
import kotlin.math.min

/**
 * Atlas's own search (design 0.7, "instant engine"). No model involved: an in-memory word index over every live
 * item — titles, notes, text read from photos and scans, summaries, a person's role, company, phone and email,
 * and their details — built once per snapshot.
 *
 * Matching: every query word must match (word beginnings, so "pri" finds Priya), with one typo allowed for words
 * of four letters or more. Ranking: title matches first, then type (people, spaces), how recently it changed and
 * how connected it is. Each hit says why it matched when the match isn't in the title.
 */
object Search {
    enum class Group(val label: String) { PEOPLE("People"), TASKS("Tasks"), FILES("Files"), NOTES("Ideas & notes"), SPACES("Spaces") }

    fun group(t: ObjType): Group = when (t) {
        ObjType.PERSON -> Group.PEOPLE
        ObjType.TASK, ObjType.EVENT -> Group.TASKS
        ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK -> Group.FILES
        ObjType.IDEA, ObjType.NOTE, ObjType.DECISION -> Group.NOTES
        ObjType.PROJECT, ObjType.COLLECTION -> Group.SPACES
    }

    data class Hit(val o: KObject, val score: Double, val why: String?, val inTitle: Boolean)

    class Index internal constructor(val snap: Snapshot, internal val entries: List<Entry>)

    internal class Entry(val o: KObject, val title: List<String>, val fields: List<Pair<String, String>>, val fieldWords: List<Set<String>>, val links: Int)

    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val STOP = setOf("the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "my", "me", "i", "is", "it", "at", "with", "about")

    fun words(s: String): List<String> = WORD.findAll(s.lowercase()).map { it.value }.toList()

    fun index(s: Snapshot): Index = Index(s, s.live.map { o ->
        val fields = buildList {
            if (o.body.isNotBlank()) add((if (o.type == ObjType.NOTE || o.type == ObjType.IDEA) "in the note" else "in the description") to o.body)
            if (o.extracted.isNotBlank()) add("in the text" to o.extracted)
            if (o.summary.isNotBlank()) add("in the summary" to o.summary)
            listOf(o.role to "role", o.org to "company", o.nickname to "nickname", o.kind to "kind", o.site to "site", o.author to "author").forEach { (v, l) -> if (v.isNotBlank()) add(l to v) }
            if (o.email.isNotBlank()) add("email" to o.email)
            if (o.phone.isNotBlank()) add("phone" to o.phone + " " + o.phone.filter(Char::isDigit))
            s.detailsOf(o.id).forEach { d ->
                if (d.value.isBlank() || d.kind == DetailKind.BAND) return@forEach
                val label = when (d.kind) { DetailKind.PHONE -> "phone"; DetailKind.EMAIL -> "email"; DetailKind.ADDRESS -> "address"; DetailKind.STEP -> "a step"
                    DetailKind.REMEMBER -> "things to remember"; DetailKind.UPDATE -> "an update"; DetailKind.FACT -> "a key fact"; else -> d.label.ifBlank { d.kind.label }.lowercase() }
                add(label to (if (d.kind == DetailKind.PHONE) d.value + " " + d.value.filter(Char::isDigit) else d.value))
            }
        }
        Entry(o, words(o.title), fields, fields.map { words(it.second).toSet() }, s.relationsOf(o.id).size)
    })

    /** One edit (insert, delete, substitute, or swap of neighbours) between a and b. */
    private fun withinOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0; var j = 0; var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                i + 1 < a.length && j + 1 < b.length && a[i] == b[j + 1] && a[i + 1] == b[j] -> { i += 2; j += 2 }
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    /** How well one query word matches one indexed word: 0 none, 1 typo, 2 beginning, 3 whole word. */
    private fun match(q: String, w: String): Int = when {
        w == q -> 3
        w.startsWith(q) -> 2
        q.length >= 4 && (withinOne(q, w) || (w.length > q.length && withinOne(q, w.take(q.length)))) -> 1
        else -> 0
    }

    fun find(ix: Index, query: String, limit: Int = 60, now: Long = System.currentTimeMillis()): List<Hit> {
        val qs = words(query).filter { it !in STOP }.ifEmpty { words(query) }
        if (qs.isEmpty()) return emptyList()
        val out = ArrayList<Hit>()
        for (e in ix.entries) {
            var score = 0.0; var allTitle = true; var why: String? = null
            var ok = true
            for (q in qs) {
                val t = e.title.maxOfOrNull { match(q, it) } ?: 0
                if (t > 0) { score += t * 2.0; continue }
                allTitle = false
                var best = 0; var where: Pair<String, String>? = null
                for ((k, f) in e.fields.withIndex()) {
                    val ws = e.fieldWords[k]
                    val m = if (q in ws) 3 else ws.maxOfOrNull { match(q, it) } ?: 0
                    if (m > best) { best = m; where = f }
                    if (best == 3) break
                }
                if (best == 0) { ok = false; break }
                score += best * 0.8
                if (why == null && where != null) why = snippet(where, q)
            }
            if (!ok) continue
            if (allTitle && e.title.size == qs.size && qs.zip(e.title).all { (a, b) -> b.startsWith(a) }) score += 3.0
            score += when (e.o.type) { ObjType.PERSON, ObjType.PROJECT -> 1.0; ObjType.TASK -> if (e.o.done) -1.0 else 0.4; else -> 0.0 }
            val days = (now - e.o.updated).coerceAtLeast(0) / 86_400_000.0
            score += 1.0 / (1.0 + days / 30.0) + min(e.links, 6) * 0.15
            out += Hit(e.o, score, if (allTitle) null else why, allTitle)
        }
        return out.sortedByDescending { it.score }.take(limit)
    }

    /** "found in the text: “…revised by Priya…”" */
    private fun snippet(f: Pair<String, String>, q: String): String {
        val (label, text) = f
        if (label in setOf("role", "company", "email", "phone", "nickname", "kind", "site", "author")) return "$label: ${text.substringBefore("  ").take(40).trim()}"
        val low = text.lowercase(); val at = low.indexOf(q).let { if (it < 0) 0 else it }
        val start = (at - 24).coerceAtLeast(0); val end = (at + q.length + 28).coerceAtMost(text.length)
        val cut = text.substring(start, end).replace(Regex("\\s+"), " ").trim()
        return "found $label: “${if (start > 0) "…" else ""}$cut${if (end < text.length) "…" else ""}”"
    }
}
