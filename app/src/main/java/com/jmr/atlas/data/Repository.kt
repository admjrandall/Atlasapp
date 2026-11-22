package com.jmr.atlas.data

import android.content.Context
import com.jmr.atlas.capture.Extractor
import com.jmr.atlas.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Single source of truth. Every mutation is one SQLite transaction followed by a fresh immutable Snapshot.
 * Thread-safe: writes are serialized on this object.
 */
class Repository(context: Context, dbName: String? = "atlas.db") {
    private val app = context.applicationContext
    val db = AtlasDb(app, dbName)
    val files = SecureFiles(app)
    private val prefs = app.getSharedPreferences(if (dbName == null) "atlas_mem" else "atlas", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(db.loadAll())
    val state: StateFlow<Snapshot> = _state.asStateFlow()
    val snap get() = _state.value

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) { prefs.edit().putBoolean("onboarded", v).apply() }

    private fun refresh() { _state.value = db.loadAll() }

    @Synchronized private fun <T> write(block: AtlasDb.(android.database.sqlite.SQLiteDatabase) -> T): T {
        val r = db.tx { db.block(this) }
        refresh(); return r
    }

    private fun AtlasDb.log(d: android.database.sqlite.SQLiteDatabase, objectId: String, kind: String, text: String) =
        with(d) { putActivity(Activity(objectId = objectId, kind = kind, text = text)) }

    // ================================================================ objects
    fun create(o: KObject, linkTo: List<Pair<String, RelType>> = emptyList()): String = write { d ->
        with(d) { putObject(o) }
        log(d, o.id, "created", "${o.type.label} created · ${o.origin.label}")
        linkTo.forEach { (other, t) -> if (snap.objects[other] != null || other == o.id) with(d) { putRelation(Relation(sourceId = o.id, targetId = other, type = t, origin = o.origin)) } }
        o.id
    }

    fun update(o: KObject, note: String? = null) = write { d ->
        val before = snap.objects[o.id]
        with(d) { putObject(o.copy(updated = System.currentTimeMillis())) }
        val what = note ?: when {
            before == null -> "Saved"
            before.title != o.title -> "Renamed to “${o.title}”"
            before.projectStatus != o.projectStatus -> "Status → ${o.projectStatus?.label}"
            before.due != o.due -> "Due date → ${o.due ?: "none"}"
            else -> "Edited"
        }
        log(d, o.id, "edited", what)
    }

    fun toggleDone(id: String) {
        val t = snap.objects[id] ?: return
        val done = !t.done
        update(t.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null), if (done) "Completed" else "Reopened")
    }

    fun trash(id: String) = write { d ->
        snap.objects[id]?.let { with(d) { putObject(it.copy(trashed = true, updated = System.currentTimeMillis())) }; log(d, id, "trashed", "Moved to trash") }
    }
    fun restore(id: String) = write { d ->
        snap.objects[id]?.let { with(d) { putObject(it.copy(trashed = false, updated = System.currentTimeMillis())) }; log(d, id, "restored", "Restored from trash") }
    }
    /** Permanent. Relationships, activity and pending suggestions cascade; the encrypted original is deleted too. */
    fun purge(id: String) {
        val o = snap.objects[id] ?: return
        write { d -> with(d) { deleteObject(id) } }
        if (o.vaultFile.isNotBlank()) files.delete(o.vaultFile)
    }
    fun emptyTrash() = snap.objects.values.filter { it.trashed }.forEach { purge(it.id) }

    // ================================================================ relationships
    /** Returns false when the two are already connected that way (no duplicate edges). */
    fun link(a: String, b: String, type: RelType = RelType.RELATED_TO, origin: Origin = Origin.USER, review: Review = Review.CONFIRMED, evidence: String = ""): Boolean {
        if (a == b) return false
        val exists = snap.relations.any { (it.sourceId == a && it.targetId == b || it.sourceId == b && it.targetId == a) && it.type == type && it.review != Review.REJECTED }
        if (exists) return false
        return write { d ->
            val ok = with(d) { putRelation(Relation(sourceId = a, targetId = b, type = type, origin = origin, review = review, evidence = evidence)) }
            if (ok) {
                val an = snap.objects[a]?.title.orEmpty(); val bn = snap.objects[b]?.title.orEmpty()
                log(d, a, "linked", "${type.label} $bn"); log(d, b, "linked", "${type.inverse} $an")
            }
            ok
        }
    }
    fun unlink(relId: String) = write { d ->
        snap.relations.firstOrNull { it.id == relId }?.let { r ->
            with(d) { deleteRelation(relId) }
            log(d, r.sourceId, "unlinked", "Removed connection to ${snap.objects[r.targetId]?.title}")
            log(d, r.targetId, "unlinked", "Removed connection to ${snap.objects[r.sourceId]?.title}")
        }
    }
    fun approve(relId: String) = write { d ->
        snap.relations.firstOrNull { it.id == relId }?.let { with(d) { updateRelation(it.copy(review = Review.CONFIRMED)) }; log(d, it.sourceId, "linked", "Confirmed connection to ${snap.objects[it.targetId]?.title}") }
    }
    fun reject(relId: String) = write { d ->
        snap.relations.firstOrNull { it.id == relId }?.let { with(d) { updateRelation(it.copy(review = Review.REJECTED)) } }
    }

    // ================================================================ capture
    /** Step 1 (always first): persist the original text as a Note. Nothing is lost if processing fails. */
    fun captureText(text: String): String {
        val clean = text.trim()
        require(clean.isNotEmpty())
        val note = KObject(type = ObjType.NOTE, title = titleFrom(clean), body = clean)
        val cap = Capture(text = clean, noteId = note.id)
        write { d -> with(d) { putObject(note); putCapture(cap) }; log(d, note.id, "created", "Captured") }
        return cap.id
    }

    /** Step 1 for files: encrypt the original into the vault and create its Document/Image before any OCR. */
    fun captureFile(bytes: ByteArray, mime: String, name: String, comment: String = ""): String {
        val vault = files.store(bytes)
        val type = if (mime.startsWith("image/")) ObjType.IMAGE else ObjType.DOCUMENT
        val doc = KObject(type = type, title = name.substringBeforeLast('.').ifBlank { type.label }.take(80), body = comment.trim(), mime = mime, vaultFile = vault, sizeBytes = bytes.size.toLong())
        val cap = Capture(text = comment.trim(), noteId = null, attachmentId = doc.id)
        write { d -> with(d) { putObject(doc); putCapture(cap) }; log(d, doc.id, "created", "Captured · original encrypted on this phone") }
        return cap.id
    }

    /** Step 2: attach text read on-device (OCR) to the original. */
    fun setExtracted(objectId: String, text: String) {
        val o = snap.objects[objectId] ?: return
        update(o.copy(extracted = text.trim()), if (text.isBlank()) "No readable text found" else "Text read on this phone (${text.length} characters)")
    }

    fun markFailed(captureId: String) = write { d -> snap.captures.firstOrNull { it.id == captureId }?.let { with(d) { putCapture(it.copy(status = CaptureStatus.FAILED)) } } }

    /** Step 3: propose connections. Proposals land in the review inbox; nothing is linked until accepted. */
    fun process(captureId: String, today: LocalDate = LocalDate.now()): List<Suggestion> {
        val cap = snap.captures.firstOrNull { it.id == captureId } ?: return emptyList()
        val source = listOfNotNull(cap.text.takeIf { it.isNotBlank() }, cap.attachmentId?.let { snap.objects[it]?.extracted }).joinToString("\n")
        val proposals = runCatching { Extractor.extract(source, snap, today) }.getOrElse { markFailed(captureId); return emptyList() }
        val ids = proposals.associate { it.key to newId() }
        val sugs = proposals.map { p ->
            Suggestion(id = ids[p.key]!!, captureId = captureId, kind = p.kind, objType = p.objType, title = p.title, targetId = p.targetId, relType = p.relType,
                evidence = p.evidence, due = p.due, linkTo = p.linkKeys.mapNotNull { ids[it] })
        }
        write { d ->
            with(d) {
                sugs.forEach { putSuggestion(it) }
                putCapture(cap.copy(status = if (sugs.isEmpty()) CaptureStatus.NOTHING_FOUND else CaptureStatus.READY_FOR_REVIEW))
            }
        }
        return sugs
    }

    /** Accepting is transactional: object + every implied relationship, or nothing. Never creates a duplicate person. */
    fun accept(sugId: String): String? {
        val s = snap.suggestions.firstOrNull { it.id == sugId && it.status == SuggestionStatus.PENDING } ?: return null
        val cap = snap.captures.firstOrNull { it.id == s.captureId } ?: return null
        val sourceId = cap.noteId ?: cap.attachmentId
        val result = write { d ->
            val ev = s.evidence
            val objId: String = when (s.kind) {
                SuggestionKind.LINK_EXISTING -> s.targetId!!
                SuggestionKind.NEW_PERSON -> snap.ofType(ObjType.PERSON).firstOrNull { it.title.equals(s.title, true) }?.id
                    ?: KObject(type = ObjType.PERSON, title = s.title, origin = Origin.EXTRACTED).also { with(d) { putObject(it) }; log(d, it.id, "created", "Person created from a capture") }.id
                SuggestionKind.NEW_TASK -> KObject(type = ObjType.TASK, title = s.title, due = s.due, origin = Origin.EXTRACTED).also { with(d) { putObject(it) }; log(d, it.id, "created", "Task created from a capture") }.id
                SuggestionKind.NEW_EVENT -> KObject(type = ObjType.EVENT, title = s.title, eventDate = s.due, origin = Origin.EXTRACTED).also { with(d) { putObject(it) }; log(d, it.id, "created", "Milestone created from a capture") }.id
            }
            fun rel(a: String?, b: String?, t: RelType) { if (a != null && b != null && a != b) with(d) { putRelation(Relation(sourceId = a, targetId = b, type = t, origin = Origin.EXTRACTED, evidence = ev)) } }
            when (s.kind) {
                SuggestionKind.NEW_TASK, SuggestionKind.NEW_EVENT -> rel(objId, sourceId, RelType.DERIVED_FROM)
                else -> rel(sourceId, objId, s.relType)
            }
            // cross-links with sibling suggestions already accepted (task ↔ person/project)
            val siblings = snap.suggestions.filter { it.captureId == s.captureId && it.status == SuggestionStatus.ACCEPTED && it.targetId != null }
            siblings.filter { it.id in s.linkTo || s.id in it.linkTo }.forEach { sib ->
                val (task, other) = if (s.kind == SuggestionKind.NEW_TASK || s.kind == SuggestionKind.NEW_EVENT) objId to sib.targetId else sib.targetId to objId
                val t = if (snap.objects[other]?.type == ObjType.PERSON || (other == objId && s.kind == SuggestionKind.NEW_PERSON)) RelType.RELATED_TO else RelType.BELONGS_TO
                rel(task, other, t)
            }
            // a newly mentioned person is connected to the project the same capture is about
            if (s.kind == SuggestionKind.NEW_PERSON || (s.kind == SuggestionKind.LINK_EXISTING && s.objType == ObjType.PERSON)) {
                siblings.filter { snap.objects[it.targetId]?.type == ObjType.PROJECT }.forEach { rel(objId, it.targetId, RelType.RELATED_TO) }
            }
            if (s.kind == SuggestionKind.LINK_EXISTING && s.objType == ObjType.PROJECT) {
                siblings.filter { snap.objects[it.targetId]?.type == ObjType.PERSON && it.kind != SuggestionKind.LINK_EXISTING }.forEach { rel(it.targetId, objId, RelType.RELATED_TO) }
            }
            with(d) { putSuggestion(s.copy(status = SuggestionStatus.ACCEPTED, targetId = objId)) }
            log(d, objId, "linked", "Accepted from capture: ${s.kind.label.lowercase()}")
            objId
        }
        settle(s.captureId)
        return result
    }

    fun rejectSuggestion(sugId: String) {
        val s = snap.suggestions.firstOrNull { it.id == sugId } ?: return
        write { d -> with(d) { putSuggestion(s.copy(status = SuggestionStatus.REJECTED)) } }
        settle(s.captureId)
    }

    fun acceptAll(captureId: String) {
        // people and projects first so tasks can connect to them
        snap.pending().filter { it.captureId == captureId }.sortedBy { if (it.kind == SuggestionKind.NEW_TASK || it.kind == SuggestionKind.NEW_EVENT) 1 else 0 }.forEach { accept(it.id) }
    }
    fun rejectAll(captureId: String) = snap.pending().filter { it.captureId == captureId }.forEach { rejectSuggestion(it.id) }

    private fun settle(captureId: String) {
        val all = snap.suggestions.filter { it.captureId == captureId }
        if (all.any { it.status == SuggestionStatus.PENDING }) return
        val cap = snap.captures.firstOrNull { it.id == captureId } ?: return
        write { d -> with(d) { putCapture(cap.copy(status = if (all.any { it.status == SuggestionStatus.ACCEPTED }) CaptureStatus.CONNECTED else CaptureStatus.NOTHING_FOUND)) } }
    }

    // ================================================================ search
    data class SearchResult(val exact: List<KObject>, val connected: List<Pair<KObject, KObject>>)

    /** Exact = keyword matches (FTS). Connected = confirmed neighbours of the top matches, with the match that leads there. */
    fun search(q: String): SearchResult {
        if (q.isBlank()) return SearchResult(emptyList(), emptyList())
        val s = snap
        val fts = db.keyword(q)
        val terms = AtlasDb.tokens(q)
        val titleHits = s.live.filter { o -> o.title.contains(q.trim(), true) || (terms.isNotEmpty() && terms.all { o.title.contains(it, true) }) }.map { it.id }
        val scored = (fts.keys + titleHits).distinct().mapNotNull { id -> s.objects[id]?.takeIf { !it.trashed }?.let { it to ((fts[id] ?: 0f) + if (id in titleHits) 1f else 0f) } }
            .sortedWith(compareByDescending<Pair<KObject, Float>> { it.second }.thenByDescending { it.first.updated })
        val exact = scored.map { it.first }
        val ex = exact.map { it.id }.toSet()
        val connected = exact.take(5).flatMap { m -> s.neighbours(m.id, includeProposed = false).map { it.first to m } }
            .filter { it.first.id !in ex }.distinctBy { it.first.id }.take(20)
        return SearchResult(exact, connected)
    }

    // ================================================================ insights & context (rule-based, always sourced)
    data class Insight(val key: String, val title: String, val detail: String, val sources: List<String>, val scope: String, val at: Long = System.currentTimeMillis())

    fun insights(today: LocalDate = LocalDate.now(), scope: String? = null): List<Insight> {
        val s = snap
        val inScope: (KObject) -> Boolean = if (scope == null) { _ -> true } else { o -> o.id == scope || s.neighbours(scope, false).any { it.first.id == o.id } }
        val scopeName = scope?.let { s.objects[it]?.title } ?: "Everything"
        val open = s.ofType(ObjType.TASK).filter { !it.done && inScope(it) }
        val out = mutableListOf<Insight>()
        open.filter { it.due != null && it.due < today }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("overdue", "${l.size} overdue ${if (l.size == 1) "task" else "tasks"}", l.sortedBy { it.due }.take(3).joinToString(" · ") { "${it.title} (due ${it.due})" }, l.map { it.id }, scopeName)
        }
        open.filter { it.due != null && !it.due.isBefore(today) && !it.due.isAfter(today.plusDays(3)) }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("soon", "Due in the next 3 days", l.sortedBy { it.due }.joinToString(" · ") { it.title }, l.map { it.id }, scopeName)
        }
        open.filter { it.title.startsWith("Expect ", true) }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("waiting", "Waiting on ${l.size}", l.joinToString(" · ") { it.title.removePrefix("Expect ") }, l.map { it.id }, scopeName)
        }
        if (scope == null) {
            val caps = s.captures.filter { c -> s.pending().any { it.captureId == c.id } }
            if (caps.isNotEmpty()) out += Insight("review", "${s.pending().size} suggestions to review", "From ${caps.size} ${if (caps.size == 1) "capture" else "captures"}. Suggestions are not facts until you accept them.", caps.mapNotNull { it.noteId ?: it.attachmentId }, scopeName)
        }
        s.ofType(ObjType.PROJECT).filter { it.projectStatus == ProjectStatus.ACTIVE && inScope(it) }
            .filter { p -> s.neighbours(p.id, false).none { it.first.type == ObjType.TASK && !it.first.done } }
            .forEach { p -> out += Insight("nonext:${p.id}", "${p.title} has no next step", "Active project with no open task connected to it.", listOf(p.id), scopeName) }
        s.live.filter { (it.type == ObjType.DOCUMENT || it.type == ObjType.IMAGE) && inScope(it) && s.relationsOf(it.id).isEmpty() }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("orphans", "${l.size} unconnected ${if (l.size == 1) "file" else "files"}", l.take(3).joinToString(" · ") { it.title }, l.map { it.id }, scopeName)
        }
        if (scope == null) {
            s.ofType(ObjType.PERSON).groupBy { it.title.lowercase().replace(Regex("[^a-z ]"), "").trim() }.values.filter { it.size > 1 }.forEach { g ->
                out += Insight("dup:${g.first().title.lowercase()}", "Possible duplicate: ${g.first().title}", "${g.size} people share this name. Open them to merge or tell them apart.", g.map { it.id }, scopeName)
            }
        }
        val dismissed = prefs.getStringSet("dismissed", emptySet())!!
        return out.filter { sig(it) !in dismissed }
    }
    private fun sig(i: Insight) = i.key + ":" + i.sources.sorted().joinToString(",").hashCode()
    fun dismiss(i: Insight) { prefs.edit().putStringSet("dismissed", prefs.getStringSet("dismissed", emptySet())!! + sig(i)).apply(); _state.value = _state.value.copy() }

    data class ContextLine(val text: String, val sources: List<String>)

    /** “Why this matters now”: deterministic, each line names the objects it came from. */
    fun context(id: String, today: LocalDate = LocalDate.now()): List<ContextLine> {
        val s = snap; val o = s.objects[id] ?: return emptyList()
        val nb = s.neighbours(id, false).map { it.first }
        val out = mutableListOf<ContextLine>()
        val tasks = (nb + listOf(o)).filter { it.type == ObjType.TASK && !it.done }
        tasks.filter { it.due != null && it.due < today }.forEach { out += ContextLine("“${it.title}” is overdue (due ${fmt(it.due!!, today)}).", listOf(it.id)) }
        tasks.filter { it.due != null && !it.due.isBefore(today) }.minByOrNull { it.due!! }?.let { out += ContextLine("Next deadline: “${it.title}” ${fmt(it.due!!, today)}.", listOf(it.id)) }
        tasks.filter { it.title.startsWith("Expect ", true) }.forEach { t -> out += ContextLine("Waiting on: ${t.title.removePrefix("Expect ")}${t.due?.let { " (" + fmt(it, today) + ")" } ?: ""}.", listOf(t.id)) }
        o.targetDate?.let { if (!it.isBefore(today)) out += ContextLine("Target date ${fmt(it, today)}.", listOf(o.id)) }
        nb.filter { it.type == ObjType.EVENT && it.eventDate != null && !it.eventDate.isBefore(today) }.minByOrNull { it.eventDate!! }
            ?.let { out += ContextLine("Upcoming milestone: ${it.title} ${fmt(it.eventDate!!, today)}.", listOf(it.id)) }
        val weekAgo = System.currentTimeMillis() - 7 * 86_400_000L
        nb.filter { it.created > weekAgo && it.type in setOf(ObjType.NOTE, ObjType.DOCUMENT, ObjType.IMAGE, ObjType.PERSON) && it.origin != Origin.EXAMPLE }
            .sortedByDescending { it.created }.take(2).forEach { out += ContextLine("New this week: ${it.type.label.lowercase()} “${it.title}”.", listOf(it.id)) }
        val proposed = s.relationsOf(id).filter { it.review == Review.PROPOSED }
        if (proposed.isNotEmpty()) out += ContextLine("${proposed.size} proposed ${if (proposed.size == 1) "connection" else "connections"} waiting for your review.", proposed.map { it.other(id) })
        if (o.type == ObjType.PROJECT && o.projectStatus == ProjectStatus.ACTIVE && tasks.isEmpty()) out += ContextLine("Active, but no open task is connected — add a next step.", listOf(o.id))
        return out.distinctBy { it.text }.take(5)
    }

    private fun fmt(d: LocalDate, today: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when {
            days == 0L -> "today"; days == 1L -> "tomorrow"; days == -1L -> "yesterday"
            days in 2..6 -> "on " + d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            days < 0 -> "${-days} days ago"
            else -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
        }
    }

    // ================================================================ example workspace, export, reset
    fun clearExample() {
        val ids = snap.objects.values.filter { it.origin == Origin.EXAMPLE }.map { it.id }
        write { d -> with(d) { ids.forEach { deleteObject(it) } } }
    }

    fun wipeAll() { snap.objects.values.filter { it.vaultFile.isNotBlank() }.forEach { files.delete(it.vaultFile) }; db.wipe(); refresh() }

    /** Readable JSON of everything except encrypted file bytes (files are listed by name). */
    fun exportJson(): String {
        val s = snap
        fun o2j(o: KObject) = JSONObject().apply {
            put("id", o.id); put("type", o.type.name); put("title", o.title); put("body", o.body); put("created", o.created); put("updated", o.updated)
            put("origin", o.origin.name); put("trashed", o.trashed)
            if (o.role.isNotBlank()) put("role", o.role); if (o.org.isNotBlank()) put("org", o.org); if (o.email.isNotBlank()) put("email", o.email); if (o.phone.isNotBlank()) put("phone", o.phone)
            o.projectStatus?.let { put("status", it.name) }; o.startDate?.let { put("start", it.toString()) }; o.targetDate?.let { put("target", it.toString()) }
            o.due?.let { put("due", it.toString()) }; if (o.type == ObjType.TASK) { put("done", o.done); put("priority", o.priority.name); put("blocked", o.blocked) }
            if (o.mime.isNotBlank()) put("mime", o.mime); if (o.url.isNotBlank()) put("url", o.url); if (o.extracted.isNotBlank()) put("extractedText", o.extracted)
            o.eventDate?.let { put("date", it.toString()) }
        }
        return JSONObject().apply {
            put("format", "atlas-export"); put("version", 1); put("exported", System.currentTimeMillis())
            put("objects", JSONArray(s.objects.values.map(::o2j)))
            put("relationships", JSONArray(s.relations.map { r -> JSONObject().apply { put("source", r.sourceId); put("target", r.targetId); put("type", r.type.name); put("review", r.review.name); put("origin", r.origin.name); put("evidence", r.evidence) } }))
            put("captures", JSONArray(s.captures.map { c -> JSONObject().apply { put("id", c.id); put("text", c.text); put("created", c.created); put("status", c.status.name) } }))
        }.toString(2)
    }

    companion object {
        fun titleFrom(text: String): String {
            val line = text.lineSequence().first().trim()
            val sentence = line.split(Regex("(?<=[.!?])\\s")).first()
            return (if (sentence.length <= 60) sentence else sentence.take(57).substringBeforeLast(' ') + "…").removeSuffix(".")
        }
    }
}
