package com.jmr.atlas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jmr.atlas.AtlasApp
import com.jmr.atlas.capture.Readers
import com.jmr.atlas.capture.Sentence
import com.jmr.atlas.data.AiMode
import com.jmr.atlas.data.Board
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.data.ExampleWorkspace
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.integrations.Web
import com.jmr.atlas.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Destinations (design 0.6): Home → a Space → an item's full page. Encoded as strings so the back stack survives process death. */
sealed interface Route {
    val key: String
    data object Home : Route { override val key = "home" }
    /** A project ("p:<id>") or a special space ("people", "files", "notes", "tasks", "all"). */
    data class Space(val space: String) : Route { override val key = "space:$space" }
    data class Item(val id: String) : Route { override val key = "obj:$id" }
    data object Review : Route { override val key = "review" }
    data object Settings : Route { override val key = "settings" }
    data object Integrations : Route { override val key = "integrations" }
    data object AiSettings : Route { override val key = "ai" }
    data object Security : Route { override val key = "security" }
    data object Trash : Route { override val key = "trash" }
    data class History(val id: String) : Route { override val key = "hist:$id" }

    companion object {
        private val fixed by lazy { listOf(Home, Review, Settings, Integrations, AiSettings, Security, Trash).associateBy { it.key } }
        fun parse(s: String): Route? = fixed[s] ?: when {
            s.startsWith("space:") -> Space(s.removePrefix("space:"))
            s.startsWith("obj:") -> Item(s.removePrefix("obj:"))
            s.startsWith("hist:") -> History(s.removePrefix("hist:"))
            else -> null
        }
    }
}

/** Sheets and dialogs that remain in 0.6 (all secondary; creating things happens in the input box). */
sealed interface Overlay {
    data class Edit(val id: String) : Overlay
    data class Create(val type: ObjType?, val linkTo: String? = null, val title: String = "") : Overlay
    data class Connect(val id: String, val types: Set<ObjType>? = null) : Overlay
    data class ConfirmPurge(val id: String) : Overlay
    data object RemoveExample : Overlay
    data class AddField(val id: String) : Overlay
    data class PostUpdate(val id: String) : Overlay
    data class AddLine(val id: String, val kind: DetailKind) : Overlay
    data class TaskDates(val id: String) : Overlay
    data class AddContact(val linkTo: String? = null) : Overlay
    data class Block(val id: String) : Overlay
    data object BriefingTime : Overlay
    data class RenameBand(val projectId: String, val key: String, val current: String) : Overlay
}

/** What the input box shows above itself after you ask something. */
sealed interface AskState {
    data object Idle : AskState
    data object Thinking : AskState
    /** [byAi]: written by the on-device model from [ids]; otherwise put together by Atlas's own rules. */
    data class Answer(val question: String, val text: String, val ids: List<String>, val byAi: Boolean, val spaceId: String?) : AskState
}

class AtlasViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val atlas = app as AtlasApp
    val repo = atlas.repo
    val sync = atlas.sync
    val ai = atlas.ai
    val snap = repo.state
    val settings get() = repo.settings

    private val _stack = MutableStateFlow(saved.get<String>("stack")?.split("|")?.mapNotNull { Route.parse(it) }?.ifEmpty { null } ?: listOf<Route>(Route.Home))
    val stack = _stack.asStateFlow()

    private val _onboarded = MutableStateFlow(repo.onboarded)
    val onboarded = _onboarded.asStateFlow()

    val overlay = MutableStateFlow<Overlay?>(null)
    fun show(o: Overlay?) { overlay.value = o }

    /** Results of actions (never decorative). An optional Undo runs the given block. */
    data class Msg(val text: String, val undo: (() -> Unit)? = null)
    val messages = MutableStateFlow<Msg?>(null)
    fun say(m: String, undo: (() -> Unit)? = null) { messages.value = Msg(m, undo) }

    val busy = MutableStateFlow<String?>(null)
    val aiBusy = MutableStateFlow<String?>(null)

    // ---------------------------------------------------------------- navigation
    private fun persist() { saved["stack"] = _stack.value.joinToString("|") { it.key } }
    val route get() = _stack.value.last()
    fun go(r: Route) { if (route == r) return; _stack.value = _stack.value.filter { it != r } + r; clearBoard(); persist() }
    fun home() { _stack.value = listOf(Route.Home); clearBoard(); persist() }
    fun back(): Boolean {
        if (catchOpen.value) { closeCatch(); return true }
        if (boardSel.value != null) { boardSel.value = null; return true }
        if (_stack.value.size > 1) { _stack.value = _stack.value.dropLast(1); clearBoard(); persist(); return true }
        return false
    }
    fun open(id: String) {
        val o = repo.snap.objects[id] ?: return
        go(if (o.type == ObjType.PROJECT) Route.Space("p:$id") else Route.Item(id))
    }
    /** Opens the space an item lives in with the item selected (threads drawn), or its page if it has no space. */
    fun reveal(id: String) {
        val s = repo.snap; val o = s.objects[id] ?: return
        if (o.type == ObjType.PROJECT) { go(Route.Space("p:$id")); return }
        val p = Briefing.projectOf(s, o)
        if (p != null && Board.members(s, p.id).any { it.id == id }) { go(Route.Space("p:${p.id}")); boardSel.value = id } else go(Route.Item(id))
    }
    fun projectSpace(id: String) = go(Route.Space("p:$id"))

    fun finishOnboarding(example: Boolean) {
        if (example) ExampleWorkspace.seed(repo)
        repo.onboarded = true; _onboarded.value = true
        sync.start()
    }

    // ---------------------------------------------------------------- board
    val boardSel = MutableStateFlow<String?>(null)
    /** Items ringed on a board after "Show on the board", with the line explaining why. */
    val highlight = MutableStateFlow<Pair<String, Set<String>>?>(null)
    fun select(id: String?) { boardSel.value = if (boardSel.value == id) null else id; if (id != null) highlight.value = null }
    private fun clearBoard() { boardSel.value = null; highlight.value = null }

    fun renameBand(projectId: String, key: String, name: String) {
        val clean = name.replace(Regex("[\\p{Cntrl}]"), " ").trim().take(40)
        val existing = repo.snap.detailsOf(projectId, DetailKind.BAND).firstOrNull { it.label == key }
        if (clean.isEmpty()) { existing?.let { repo.deleteDetail(it.id) }; return }
        repo.putDetail((existing ?: Detail(objectId = projectId, kind = DetailKind.BAND, label = key)).copy(value = clean, origin = Origin.USER))
    }

    private val named = HashSet<String>()
    /** With on-device AI on, bands that still have their default name get a topic name. Runs once per band set; you can rename any. */
    fun nameBands(projectId: String) {
        if (settings.aiMode != AiMode.ON_DEVICE || ai.status.value != Ai.Status.Ready) return
        val s = repo.snap; val p = s.objects[projectId] ?: return
        val todo = Board.bands(s, projectId).filter { it.nameOrigin == Origin.AUTO && it.items.size >= 2 && it.key != Board.PEOPLE_BAND }
        val sig = projectId + todo.joinToString { it.key + it.items.size }
        if (todo.isEmpty() || !named.add(sig)) return
        viewModelScope.launch {
            runCatching { ai.nameBands(p.title, todo.map { b -> b.items.map { it.title } }) }.onSuccess { names ->
                todo.zip(names).forEach { (b, n) ->
                    if (n != null && repo.snap.detailsOf(projectId, DetailKind.BAND).none { it.label == b.key }) repo.putDetail(Detail(objectId = projectId, kind = DetailKind.BAND, label = b.key, value = n, origin = Origin.AI))
                }
            }
        }
    }

    // ---------------------------------------------------------------- the input box: catch or ask
    val catchOpen = MutableStateFlow(false)
    val catchText = MutableStateFlow("")
    val kindOverride = MutableStateFlow<Sentence.Kind?>(null)
    /** Parts of the preview you tapped off (space id / person ids) before saving. */
    val dropped = MutableStateFlow<Set<String>>(emptySet())
    val dueOff = MutableStateFlow(false)
    val attachOpen = MutableStateFlow(false)
    val ask = MutableStateFlow<AskState>(AskState.Idle)
    private var askJob: Job? = null

    fun openCatch(prefill: String = "") { catchText.value = prefill; kindOverride.value = null; dropped.value = emptySet(); dueOff.value = false; attachOpen.value = false; ask.value = AskState.Idle; catchOpen.value = true }
    fun closeCatch() { catchOpen.value = false; askJob?.cancel(); ask.value = AskState.Idle; attachOpen.value = false }
    fun setCatch(t: String) { catchText.value = t; if (ask.value !is AskState.Idle) ask.value = AskState.Idle; kindOverride.value = null; dropped.value = emptySet(); dueOff.value = false }
    fun cycleKind(current: Sentence.Kind) { val k = Sentence.Kind.entries; kindOverride.value = k[(k.indexOf(current) + 1) % k.size] }
    fun toggleDrop(id: String) { dropped.value = dropped.value.let { if (id in it) it - id else it + id } }

    /** The space you're looking at, used as the default home for what you type. */
    val currentProject: String? get() = (route as? Route.Space)?.space?.takeIf { it.startsWith("p:") }?.removePrefix("p:")
        ?: (route as? Route.Item)?.id?.let { id -> repo.snap.objects[id]?.let { Briefing.projectOf(repo.snap, it)?.id } }

    fun parsed(text: String = catchText.value): Sentence.Parsed = Sentence.parse(text, repo.snap, LocalDate.now(), currentProject)

    fun send() {
        val text = catchText.value.trim(); if (text.isEmpty()) return
        val p = parsed(text)
        if (p.question) { askNow(text); return }
        save(p)
    }

    private fun save(p: Sentence.Parsed) {
        val kind = kindOverride.value ?: p.kind
        val space = p.space?.takeIf { it.id !in dropped.value }
        val people = p.people.filter { it.id !in dropped.value }
        val due = if (dueOff.value) null else p.due
        val created: String? = runCatching {
            when (kind) {
                Sentence.Kind.NOTE -> {
                    val capId = repo.captureText(p.raw)
                    val noteId = repo.snap.captures.firstOrNull { it.id == capId }?.noteId
                    noteId?.let { n -> space?.let { repo.link(n, it.id, RelType.BELONGS_TO) }; people.forEach { repo.link(n, it.id, RelType.DISCUSSED_WITH) } }
                    viewModelScope.launch(Dispatchers.Default) { processCapture(capId) }
                    noteId
                }
                else -> repo.create(KObject(type = kind.type, title = p.title, body = if (p.title != p.raw) p.raw else "", due = if (kind == Sentence.Kind.TASK) due else null),
                    listOfNotNull(space?.let { it.id to RelType.BELONGS_TO }) + people.map { it.id to RelType.RELATED_TO })
            }
        }.getOrElse { say("Couldn't save — ${it.message}"); null }
        closeCatch(); catchText.value = ""
        created?.let { id -> say("Saved" + (space?.let { " to ${it.title}" } ?: " to Today")) { repo.trash(id) } }
    }

    private fun askNow(question: String) {
        askJob?.cancel()
        ask.value = AskState.Thinking
        askJob = viewModelScope.launch {
            val s = repo.snap
            val (basis, rule) = withContext(Dispatchers.Default) { gather(question, s) }
            val useAi = settings.aiMode == AiMode.ON_DEVICE && ai.status.value == Ai.Status.Ready && basis.isNotEmpty()
            val result = if (useAi) runCatching { ai.answer(question, basis, s) }.getOrNull()?.takeIf { it.text.isNotBlank() } else null
            val spaceId = commonProject(s, result?.ids?.ifEmpty { null } ?: rule.second)
            ask.value = if (result != null) AskState.Answer(question, result.text, result.ids.ifEmpty { basis.take(4).map { it.id } }, true, spaceId)
            else AskState.Answer(question, rule.first, rule.second, false, spaceId)
        }
    }

    private fun commonProject(s: Snapshot, ids: List<String>): String? =
        ids.mapNotNull { id -> s.objects[id]?.let { if (it.type == ObjType.PROJECT) it else Briefing.projectOf(s, it) }?.id }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

    /**
     * Atlas's own answer (no model): reads the question for a milestone ("before demolition"), lateness, what's due,
     * or what you're waiting on; otherwise it's a search. Returns the items to show and one plain sentence.
     */
    private fun gather(question: String, s: Snapshot, today: LocalDate = LocalDate.now()): Pair<List<KObject>, Pair<String, List<String>>> {
        val q = question.lowercase()
        val terms = Sentence.searchTerms(question)
        val words = terms.split(" ").filter { it.length > 3 }
        val open = s.ofType(ObjType.TASK).filter { !it.done }
        val event = s.ofType(ObjType.EVENT).filter { e -> e.eventDate != null && !e.eventDate.isBefore(today) && words.any { w -> e.title.contains(w.removeSuffix("s"), true) } }.minByOrNull { it.eventDate!! }
        val found = runCatching { repo.search(terms.ifBlank { question }) }.getOrNull()
        val searchHits = found?.let { it.exact + it.connected.map { c -> c.first } }.orEmpty().distinctBy { it.id }
        fun names(l: List<KObject>) = l.take(3).joinToString(", ") { it.title.removePrefix("Expect ") }
        if (event != null) {
            val p = Briefing.projectOf(s, event)
            val pool = if (p != null) Board.members(s, p.id).filter { it.type == ObjType.TASK } else open
            val risky = pool.filter { t -> !t.done && (Board.effective(t)?.let { !it.isAfter(event.eventDate) } == true || Board.isWaiting(t)) }
                .sortedWith(compareBy({ if (Board.effective(it)?.isBefore(today) == true) 0 else 1 }, { Board.effective(it) ?: LocalDate.MAX }))
            val late = risky.count { !Board.isWaiting(it) && Board.effective(it)?.isBefore(today) == true }
            val text = when {
                risky.isEmpty() -> "Nothing open is due before ${event.title.lowercase()} (${Board.shortDate(event.eventDate!!)})."
                late > 0 -> "${risky.size} open ${if (risky.size == 1) "thing is" else "things are"} due before ${event.title.lowercase()} on ${Board.shortDate(event.eventDate!!)} — $late already late."
                else -> "${risky.size} open ${if (risky.size == 1) "thing is" else "things are"} due before ${event.title.lowercase()} on ${Board.shortDate(event.eventDate!!)}."
            }
            return (risky + event + searchHits).distinctBy { it.id } to (text to risky.map { it.id }.ifEmpty { listOf(event.id) })
        }
        val scoped = Sentence.space(question, s, Sentence.people(question, s), null)?.let { p -> Board.members(s, p.id).map { it.id }.toSet() }
        fun inScope(l: List<KObject>) = if (scoped == null) l else l.filter { it.id in scoped }
        val late = inScope(open.filter { !Board.isWaiting(it) && Board.effective(it)?.isBefore(today) == true }).sortedBy { Board.effective(it) }
        val waiting = inScope(open.filter { Board.isWaiting(it) })
        val dueSoon = inScope(open.filter { !Board.isWaiting(it) && Board.effective(it)?.let { d -> !d.isBefore(today) && ChronoUnit.DAYS.between(today, d) <= 7 } == true }).sortedBy { Board.effective(it) }
        val rule: Pair<String, List<String>> = when {
            Regex("\\b(late|overdue|slipp|behind|missed)").containsMatchIn(q) -> (if (late.isEmpty()) "Nothing is late." else "${late.size} late: ${names(late)}.") to late.map { it.id }
            Regex("\\b(waiting|owe|owes)").containsMatchIn(q) -> (if (waiting.isEmpty()) "You're not waiting on anyone." else "Waiting on ${waiting.size}: ${names(waiting)}.") to waiting.map { it.id }
            Regex("\\b(due|this week|today|tomorrow|next)\\b").containsMatchIn(q) -> (if (dueSoon.isEmpty() && late.isEmpty()) "Nothing is due this week." else listOfNotNull(
                late.takeIf { it.isNotEmpty() }?.let { "${it.size} late" }, dueSoon.takeIf { it.isNotEmpty() }?.let { "${it.size} due this week: ${names(it)}" }).joinToString("; ") + ".") to (late + dueSoon).map { it.id }
            searchHits.isEmpty() -> "Nothing in Atlas matches “${terms.ifBlank { question }}”." to emptyList()
            else -> (if (searchHits.size == 1) "Found 1 thing." else "Found ${searchHits.size} things.") to searchHits.take(6).map { it.id }
        }
        val basis = (rule.second.mapNotNull { s.objects[it] } + searchHits).distinctBy { it.id }
        return basis to rule
    }

    /** "Show them on the board": opens the space the answer is about and rings the items it used. */
    fun showOnBoard(a: AskState.Answer) {
        val p = a.spaceId ?: return
        closeCatch()
        go(Route.Space("p:$p"))
        highlight.value = a.question to a.ids.toSet()
    }

    // ---------------------------------------------------------------- capture pipeline (original first, then on-device processing)
    fun captureText(text: String, from: String = "", voice: Boolean = false) {
        val url = Web.soleUrl(text)
        if (url != null) { captureLink(url, text.replace(url, "").trim(), from); return }
        val id = runCatching { repo.captureText(text, from, voice) }.getOrElse { say("Couldn't save — ${it.message}"); return }
        viewModelScope.launch(Dispatchers.Default) { processCapture(id); announce(id) }
    }

    fun captureLink(url: String, comment: String = "", from: String = "") {
        val id = runCatching { repo.captureLink(url, comment, from) }.getOrElse { say("Couldn't save that link."); return }
        repo.snap.captures.firstOrNull { it.id == id }?.attachmentId?.let { sync.preview(it) }
        viewModelScope.launch(Dispatchers.Default) { processCapture(id); announce(id) }
    }

    /** After a share or widget capture: one line saying where it went, with Undo. */
    private fun announce(captureId: String) {
        val s = repo.snap; val cap = s.captures.firstOrNull { it.id == captureId } ?: return
        val objId = cap.noteId ?: cap.attachmentId ?: return
        val where = s.neighbours(objId, false).map { it.first }.firstOrNull { it.type == ObjType.PROJECT }
        val pending = s.pending().count { it.captureId == captureId }
        say(listOfNotNull("Saved" + (where?.let { " to ${it.title}" } ?: " to Today"), if (pending > 0) "$pending to confirm" else null).joinToString(" · ")) { repo.trash(objId) }
    }

    private suspend fun processCapture(id: String) {
        busy.value = id
        val n = runCatching { repo.process(id).size }.getOrElse { repo.markFailed(id); -1 }
        if (n >= 0) runAiOn(id)
        busy.value = null
        if (n < 0) say("Saved. Reading it failed — the original is kept.")
    }

    /** On-device model, only if you turned it on and the model is downloaded. Its proposals always wait for you. */
    private suspend fun runAiOn(captureId: String) {
        if (settings.aiMode != AiMode.ON_DEVICE || ai.status.value != Ai.Status.Ready) return
        val cap = repo.snap.captures.firstOrNull { it.id == captureId } ?: return
        val text = listOfNotNull(cap.text.takeIf { it.isNotBlank() }, cap.attachmentId?.let { repo.snap.objects[it]?.extracted }).joinToString("\n").trim()
        if (text.length < 12) return
        aiBusy.value = captureId
        runCatching { ai.extract(text, repo.snap) }.onSuccess { repo.addProposals(captureId, it.map { s -> s.copy(captureId = captureId) }) }
            .onFailure { say("On-device AI couldn't read this one. Atlas's rules still did.") }
        aiBusy.value = null
    }

    fun summarize(id: String) {
        val o = repo.snap.objects[id] ?: return
        val text = listOf(o.extracted, o.body).filter { it.isNotBlank() }.joinToString("\n")
        if (text.isBlank()) { say("There's no text to summarize yet."); return }
        viewModelScope.launch {
            aiBusy.value = id
            runCatching { ai.summarize(text) }.onSuccess { repo.update(repo.snap.objects[id]!!.copy(summary = it), "Summarized on this phone") }
                .onFailure { say("The on-device model couldn't run: ${it.message?.take(60)}") }
            aiBusy.value = null
        }
    }

    fun captureUri(uri: Uri, mimeHint: String? = null, comment: String = "", from: String = "") {
        val ctx = getApplication<Application>()
        val space = currentProject
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Readers.read(ctx, uri, mimeHint) }
            if (r == null) { say("Couldn't read that file."); return@launch }
            val capId = withContext(Dispatchers.IO) { runCatching { repo.captureFile(r.bytes, r.mime, r.name, comment) }.getOrNull() }
            if (capId == null) { say("Couldn't save that file."); return@launch }
            busy.value = capId
            val docId = repo.snap.captures.firstOrNull { it.id == capId }?.attachmentId
            val text = withContext(Dispatchers.Default) { runCatching { Readers.text(ctx, r) }.getOrDefault("") }
            if (docId != null) { repo.setExtracted(docId, text); space?.let { repo.link(docId, it, RelType.BELONGS_TO) } }
            withContext(Dispatchers.Default) { processCapture(capId) }
            announce(capId)
        }
    }

    /** Photo chosen in Atlas for a person: encrypted into the vault; also written to the phone contact when sync is on. */
    fun setPhoto(personId: String, uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()?.takeIf { it.size < 15_000_000 } ?: run { say("Couldn't read that photo."); return@launch }
            val p = repo.snap.objects[personId] ?: return@launch
            if (p.photo.startsWith("vault:")) repo.files.delete(p.photo.removePrefix("vault:"))
            val name = repo.files.store(bytes)
            repo.update(p.copy(photo = "vault:$name"), "Photo changed")
        }
    }

    fun savePerson(p: KObject, details: List<Detail>, custom: List<Detail>, isNew: Boolean, linkTo: String?): String {
        val id = if (isNew) repo.create(p, listOfNotNull(linkTo?.let { it to RelType.RELATED_TO })) else { repo.update(p); p.id }
        repo.replaceDetails(id, com.jmr.atlas.integrations.Sync.CONTACT_KINDS, details)
        repo.replaceDetails(id, setOf(DetailKind.CUSTOM), custom.filter { it.value.isNotBlank() })
        viewModelScope.launch(Dispatchers.IO) { sync.pushPerson(id) }
        return id
    }
}
