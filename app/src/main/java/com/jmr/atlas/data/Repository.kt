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
class Repository(context: Context, dbName: String? = "atlas.db", encrypted: Boolean = dbName != null) {
    private val app = context.applicationContext
    val db = if (encrypted && dbName != null) AtlasDb(app, dbName, DbCrypto.factory(app, dbName)) else AtlasDb(app, dbName)
    val encrypted = encrypted && dbName != null
    val files = SecureFiles(app)
    private val prefs = app.getSharedPreferences(if (dbName == null) "atlas_mem" else "atlas", Context.MODE_PRIVATE)
    val settings = Settings(prefs)

    private val _state = MutableStateFlow(db.loadAll())
    val state: StateFlow<Snapshot> = _state.asStateFlow()
    val snap get() = _state.value

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) { prefs.edit().putBoolean("onboarded", v).apply() }

    private fun refresh() { _state.value = db.loadAll() }

    @Synchronized private fun <T> write(block: AtlasDb.(androidx.sqlite.db.SupportSQLiteDatabase) -> T): T {
        val r = db.tx { db.block(this) }
        refresh(); return r
    }

    private fun AtlasDb.log(d: androidx.sqlite.db.SupportSQLiteDatabase, objectId: String, kind: String, text: String) =
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

    /** Completing a repeating task rolls it to its next date instead of closing it. */
    fun toggleDone(id: String, today: LocalDate = LocalDate.now()) {
        val t = snap.objects[id] ?: return
        val done = !t.done
        if (done && t.repeat != Repeat.NONE) {
            val base = t.due ?: t.deadline ?: today
            val next = nextDate(base, t.repeat, today)
            val shift = java.time.temporal.ChronoUnit.DAYS.between(base, next)
            update(t.copy(due = t.due?.plusDays(shift), deadline = t.deadline?.plusDays(shift), remindAt = t.remindAt?.plus(shift * 86_400_000L)),
                "Completed · next ${next}")
            return
        }
        update(t.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null), if (done) "Completed" else "Reopened")
    }

    fun complete(id: String) { if (snap.objects[id]?.done == false) toggleDone(id) }

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
    fun captureText(text: String, from: String = "", voice: Boolean = false): String {
        val clean = text.trim()
        require(clean.isNotEmpty())
        val note = KObject(type = ObjType.NOTE, title = titleFrom(clean), body = clean, kind = if (voice) "Voice note" else "")
        val cap = Capture(text = clean, noteId = note.id)
        write { d -> with(d) { putObject(note); putCapture(cap) }; log(d, note.id, "created", when { voice -> "Voice note, transcribed on this phone"; from.isNotBlank() -> "Shared from $from"; else -> "Captured" }) }
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

    /** A saved link: the URL becomes a Link item at once; its preview is fetched afterwards (Sync.preview). */
    fun captureLink(url: String, comment: String = "", from: String = ""): String {
        val link = KObject(type = ObjType.LINK, title = com.jmr.atlas.integrations.Web.host(url), url = url, body = comment.trim())
        val cap = Capture(text = comment.trim(), noteId = null, attachmentId = link.id)
        write { d -> with(d) { putObject(link); putCapture(cap) }; log(d, link.id, "created", if (from.isNotBlank()) "Saved from $from" else "Link saved") }
        return cap.id
    }

    /** Step 2: attach text read on-device (OCR) to the original. */
    fun setExtracted(objectId: String, text: String) {
        val o = snap.objects[objectId] ?: return
        val f = com.jmr.atlas.capture.Facts.extract(text, objectId)
        update(o.copy(extracted = text.trim(), docDate = o.docDate ?: f.docDate, kind = o.kind.ifBlank { f.kind }),
            if (text.isBlank()) "No readable text found" else "Text read on this phone (${text.length} characters)")
        if (f.facts.isNotEmpty() && snap.detailsOf(objectId, DetailKind.FACT).isEmpty()) replaceDetails(objectId, setOf(DetailKind.FACT), f.facts)
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
        // Known people and projects connect at once (logged, undoable). Anything new waits in To review.
        if (settings.autoConnect) sugs.filter { it.kind == SuggestionKind.LINK_EXISTING }.forEach { accept(it.id, auto = true) }
        return snap.suggestions.filter { it.captureId == captureId }
    }

    /** Proposals from the on-device model join the capture's review list. They are never auto-accepted. */
    fun addProposals(captureId: String, proposals: List<Suggestion>) {
        val cap = snap.captures.firstOrNull { it.id == captureId } ?: return
        val existing = snap.suggestions.filter { it.captureId == captureId }
        val fresh = proposals.filter { p -> existing.none { it.title.equals(p.title, true) && it.kind == p.kind } && (p.targetId == null || existing.none { it.targetId == p.targetId }) }
        if (fresh.isEmpty()) return
        write { d -> with(d) { fresh.forEach { putSuggestion(it.copy(captureId = captureId)) }; putCapture(cap.copy(status = CaptureStatus.READY_FOR_REVIEW)) } }
    }

    /** Undo an automatic connection: removes the edge and records the suggestion as rejected. */
    fun undoAuto(sugId: String) {
        val s = snap.suggestions.firstOrNull { it.id == sugId } ?: return
        val cap = snap.captures.firstOrNull { it.id == s.captureId } ?: return
        val source = cap.noteId ?: cap.attachmentId
        val rel = snap.relations.firstOrNull { r -> r.origin == Origin.AUTO && ((r.sourceId == source && r.targetId == s.targetId) || (r.targetId == source && r.sourceId == s.targetId)) }
        write { d -> with(d) { rel?.let { deleteRelation(it.id) }; putSuggestion(s.copy(status = SuggestionStatus.REJECTED)) }; s.targetId?.let { log(d, it, "unlinked", "Undid automatic connection") } }
    }

    /** Accepting is transactional: object + every implied relationship, or nothing. Never creates a duplicate person. */
    fun accept(sugId: String, auto: Boolean = false): String? {
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
            val relOrigin = if (auto) Origin.AUTO else Origin.EXTRACTED
            fun rel(a: String?, b: String?, t: RelType) { if (a != null && b != null && a != b) with(d) { putRelation(Relation(sourceId = a, targetId = b, type = t, origin = relOrigin, evidence = ev)) } }
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
            log(d, objId, "linked", if (auto) "Connected automatically from a capture" else "Accepted from capture: ${s.kind.label.lowercase()}")
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

    // ================================================================ details, custom fields, updates
    fun putDetail(dt: Detail, note: String? = null) = write { d -> with(d) { putDetail(dt) }; note?.let { log(d, dt.objectId, "edited", it) } }
    fun deleteDetail(id: String) = write { d -> snap.details.firstOrNull { it.id == id }?.let { with(d) { deleteDetail(id) } } }

    /** Replaces all details of the given kinds for one object (used by editors and contact sync) in one transaction. */
    /** Save a space's journey (shape + parts). The space's items and links are not touched. */
    fun setJourney(j: Journey, note: String? = null) = replaceDetails(j.spaceId, setOf(DetailKind.SHAPE, DetailKind.PART), Journeys.details(j), note)
    /** Put an item in a part of its space's journey. */
    fun placeInPart(spaceId: String, itemId: String, partId: String) =
        replaceDetails(spaceId, setOf(DetailKind.PLACE), JourneyItems.placeDetails(snap, spaceId, itemId, partId))
    /** Save a journey's skeleton (shape + part names, never items) as a template. Kept in settings, newest first. */
    fun saveTemplate(name: String, j: Journey) {
        val arr = org.json.JSONArray(settings.templatesJson)
        val keep = (0 until arr.length()).map { arr.getJSONObject(it) }.filter { it.optString("name") != Journeys.clean(name) }
        val out = org.json.JSONArray().put(org.json.JSONObject().put("name", Journeys.clean(name)).put("shape", j.shape.name)
            .put("parts", org.json.JSONArray(j.parts.map { it.name })))
        keep.forEach { out.put(it) }
        settings.templatesJson = out.toString()
    }
    fun templates(): List<Pair<String, Journey>> = runCatching {
        val arr = org.json.JSONArray(settings.templatesJson)
        (0 until arr.length()).mapNotNull { k ->
            val o = arr.getJSONObject(k); val sh = Shape.of(o.optString("shape")) ?: return@mapNotNull null
            val names = o.optJSONArray("parts")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
            o.optString("name") to Journey("", sh, sh.styles.firstOrNull().orEmpty(), names.mapIndexed { n, nm ->
                Part(newId(), nm, if (sh.finishes && n == 0) PartState.HERE else PartState.LATER, n) })
        }
    }.getOrDefault(emptyList())

    /** Remove a space's journey; the space and everything in it stay, shown on the board as before. */
    fun clearJourney(spaceId: String) = replaceDetails(spaceId, setOf(DetailKind.SHAPE, DetailKind.PART), emptyList(), "journey removed")

    fun replaceDetails(objectId: String, kinds: Set<DetailKind>, list: List<Detail>, note: String? = null) = write { d ->
        with(d) {
            snap.details.filter { it.objectId == objectId && it.kind in kinds }.forEach { deleteDetail(it.id) }
            list.forEachIndexed { i, x -> putDetail(x.copy(objectId = objectId, sort = i)) }
        }
        note?.let { log(d, objectId, "edited", it) }
    }

    fun toggleStep(id: String) {
        val st = snap.details.firstOrNull { it.id == id } ?: return
        putDetail(st.copy(flag = !st.flag, extra = if (!st.flag) LocalDate.now().toString() else ""), if (!st.flag) "Step done: ${st.value}" else null)
    }

    fun postUpdate(projectId: String, text: String, health: Health) {
        val p = snap.objects[projectId] ?: return
        write { d ->
            with(d) {
                putDetail(Detail(objectId = projectId, kind = DetailKind.UPDATE, value = text.trim(), extra = health.name))
                putObject(p.copy(health = health, updated = System.currentTimeMillis()))
            }
            log(d, projectId, "update", "Posted an update · ${health.label}")
        }
    }

    fun addFieldDef(f: FieldDef) = write { d -> with(d) { putFieldDef(f.copy(sort = snap.fieldDefs.count { it.objType == f.objType })) } }
    fun deleteFieldDef(id: String) = write { d -> with(d) {
        deleteFieldDef(id)
        snap.details.filter { it.kind == DetailKind.CUSTOM && it.extra == id && it.value.isBlank() }.forEach { deleteDetail(it.id) }
    } }

    /** Custom fields for an item: its own values plus empty slots for type-wide definitions it hasn't filled yet. */
    fun fieldsFor(o: KObject): List<Detail> {
        val own = snap.detailsOf(o.id, DetailKind.CUSTOM)
        val defs = snap.fieldDefs.filter { it.objType == o.type && own.none { d -> d.extra == it.id } }
            .map { Detail(objectId = o.id, kind = DetailKind.CUSTOM, label = it.label, type = it.type, flag = it.secret, extra = it.id) }
        return own + defs
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
            out += Insight("overdue", "${l.size} overdue ${if (l.size == 1) "task" else "tasks"}", "", l.sortedBy { it.due }.map { it.id }, scopeName)
        }
        open.filter { it.due != null && !it.due.isBefore(today) && !it.due.isAfter(today.plusDays(3)) }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("soon", "Due in the next 3 days", "", l.sortedBy { it.due }.map { it.id }, scopeName)
        }
        open.filter { it.title.startsWith("Expect ", true) }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("waiting", "Waiting on ${if (l.size == 1) "1 thing" else "${l.size} things"}", "", l.map { it.id }, scopeName)
        }
        if (scope == null) {
            val caps = s.captures.filter { c -> s.pending().any { it.captureId == c.id } }
            if (caps.isNotEmpty()) out += Insight("review", "${s.pending().size} suggestions to review", "From ${caps.size} ${if (caps.size == 1) "capture" else "captures"}", caps.mapNotNull { it.noteId ?: it.attachmentId }, scopeName)
        }
        s.ofType(ObjType.PROJECT).filter { it.projectStatus == ProjectStatus.ACTIVE && inScope(it) }
            .filter { p -> s.neighbours(p.id, false).none { it.first.type == ObjType.TASK && !it.first.done } }
            .forEach { p -> out += Insight("nonext:${p.id}", "${p.title} has no next step", "No open task is connected to this active project.", listOf(p.id), scopeName) }
        s.live.filter { (it.type == ObjType.DOCUMENT || it.type == ObjType.IMAGE) && inScope(it) && s.relationsOf(it.id).isEmpty() }.takeIf { it.isNotEmpty() }?.let { l ->
            out += Insight("orphans", "${l.size} unconnected ${if (l.size == 1) "file" else "files"}", "", l.map { it.id }, scopeName)
        }
        if (scope == null) {
            s.ofType(ObjType.PERSON).groupBy { it.title.lowercase().replace(Regex("[^a-z ]"), "").trim() }.values.filter { it.size > 1 }.forEach { g ->
                out += Insight("dup:${g.first().title.lowercase()}", "Possible duplicate: ${g.first().title}", "${g.size} people share this name.", g.map { it.id }, scopeName)
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
        tasks.filter { it.due != null && it.due < today }.forEach { out += ContextLine("${it.title} — was due ${fmt(it.due!!, today)}", listOf(it.id)) }
        tasks.filter { it.due != null && !it.due.isBefore(today) && !it.title.startsWith("Expect ", true) }.minByOrNull { it.due!! }
            ?.let { out += ContextLine("${it.title} — due ${fmt(it.due!!, today)}", listOf(it.id)) }
        tasks.filter { it.title.startsWith("Expect ", true) }.forEach { t -> out += ContextLine("Waiting on ${t.title.removePrefix("Expect ")}${t.due?.let { " — " + fmt(it, today) } ?: ""}", listOf(t.id)) }
        o.targetDate?.let { if (!it.isBefore(today)) out += ContextLine("Target date ${fmt(it, today)}", listOf(o.id)) }
        nb.filter { it.type == ObjType.EVENT && it.eventDate != null && !it.eventDate.isBefore(today) }.minByOrNull { it.eventDate!! }
            ?.let { out += ContextLine("Next milestone: ${it.title}, ${fmt(it.eventDate!!, today)}", listOf(it.id)) }
        val weekAgo = System.currentTimeMillis() - 7 * 86_400_000L
        nb.filter { it.created > weekAgo && it.type in setOf(ObjType.NOTE, ObjType.DOCUMENT, ObjType.IMAGE, ObjType.PERSON) && it.origin != Origin.EXAMPLE }
            .sortedByDescending { it.created }.take(2).forEach { out += ContextLine("New this week: ${it.title}", listOf(it.id)) }
        val proposed = s.relationsOf(id).filter { it.review == Review.PROPOSED }
        if (proposed.isNotEmpty()) out += ContextLine("${proposed.size} proposed ${if (proposed.size == 1) "connection" else "connections"} to review", proposed.map { it.other(id) })
        if (o.type == ObjType.PROJECT && o.projectStatus == ProjectStatus.ACTIVE && tasks.isEmpty()) out += ContextLine("Active, but no open task — add a next step", listOf(o.id))
        return out.distinctBy { it.text }.take(5)
    }

    private fun fmt(d: LocalDate, today: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when {
            days == 0L -> "today"; days == 1L -> "tomorrow"; days == -1L -> "yesterday"
            days in 2..6 -> "on " + d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            days < 0 -> "${-days} days ago"
            d.year == today.year -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
            else -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
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
            put("format", "atlas-export"); put("version", 2); put("exported", System.currentTimeMillis())
            put("objects", JSONArray(s.objects.values.map(::o2j)))
            put("relationships", JSONArray(s.relations.map { r -> JSONObject().apply { put("source", r.sourceId); put("target", r.targetId); put("type", r.type.name); put("review", r.review.name); put("origin", r.origin.name); put("evidence", r.evidence) } }))
            put("details", JSONArray(s.details.map { x -> JSONObject().apply { put("object", x.objectId); put("kind", x.kind.name); put("label", x.label); put("value", x.value); put("type", x.type.name); put("flag", x.flag); put("extra", x.extra) } }))
            put("fieldDefinitions", JSONArray(s.fieldDefs.map { f -> JSONObject().apply { put("objectType", f.objType.name); put("label", f.label); put("type", f.type.name); put("secret", f.secret) } }))
            put("captures", JSONArray(s.captures.map { c -> JSONObject().apply { put("id", c.id); put("text", c.text); put("created", c.created); put("status", c.status.name) } }))
        }.toString(2)
    }

    companion object {
        fun nextDate(from: LocalDate, r: Repeat, today: LocalDate): LocalDate {
            if (r == Repeat.NONE) return from
            fun step(d: LocalDate): LocalDate = when (r) {
                Repeat.DAILY -> d.plusDays(1); Repeat.WEEKLY -> d.plusWeeks(1); Repeat.MONTHLY -> d.plusMonths(1); Repeat.YEARLY -> d.plusYears(1)
                Repeat.WEEKDAYS -> { var x = d.plusDays(1); while (x.dayOfWeek.value >= 6) x = x.plusDays(1); x }
                Repeat.NONE -> d
            }
            var n = step(from); while (n.isBefore(today) || n == from) n = step(n)
            return n
        }
        fun titleFrom(text: String): String {
            val line = text.lineSequence().first().trim()
            val sentence = line.split(Regex("(?<=[.!?])\\s")).first()
            return (if (sentence.length <= 60) sentence else sentence.take(57).substringBeforeLast(' ') + "…").removeSuffix(".")
        }
    }
}
