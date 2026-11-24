package com.jmr.atlas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jmr.atlas.AtlasApp
import com.jmr.atlas.capture.Readers
import com.jmr.atlas.capture.Sentence
import com.jmr.atlas.data.Search
import com.jmr.atlas.data.AiMode
import com.jmr.atlas.data.Board
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.data.ExampleWorkspace
import com.jmr.atlas.integrations.Brain
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.integrations.Web
import com.jmr.atlas.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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

/** One line in the Atlas conversation (design 0.7). The thread is short-lived: Clear empties it; what Atlas did lives in your spaces. */
sealed interface Turn {
    val id: String
    data class You(override val id: String = newId(), val text: String, val files: List<String> = emptyList()) : Turn
    /** Atlas changed something in Atlas. Undo stays on the card while it's in the thread. */
    data class Did(override val id: String = newId(), val text: String, val ids: List<String>, val undo: (() -> Unit)?, val undone: Boolean = false) : Turn
    /** An answer about your own things: Atlas's own sentence at once, maybe refined by the model. */
    data class Mine(override val id: String = newId(), val question: String, val text: String, val ids: List<String>, val byAi: Boolean, val spaceId: String?,
        val refining: Boolean = false, val note: String? = null) : Turn
    /** A general answer or conversational reply. [offers] = spaces it might belong to. */
    data class World(override val id: String = newId(), val question: String, val text: String = "", val thinking: Boolean = true, val noModel: Boolean = false,
        val offers: List<String> = emptyList(), val saved: Boolean = false) : Turn
}

class AtlasViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val atlas = app as AtlasApp
    val repo = atlas.repo
    val sync = atlas.sync
    val ai = atlas.ai
    /** The model behind Atlas (Gemma on this phone today; Apple's or your server's later). */
    val brain: Brain get() = ai
    fun brainReady() = settings.aiMode == AiMode.ON_DEVICE && brain.ready()
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

    /** Settings: load the sample workspace after setup (0.7.2). Only offered while none is loaded. */
    fun loadExample() {
        if (repo.snap.objects.values.any { it.origin == Origin.EXAMPLE }) return
        ExampleWorkspace.seed(repo); home(); say("Example workspace loaded. Remove it any time in Settings.")
    }

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
        if (!brainReady()) return
        val s = repo.snap; val p = s.objects[projectId] ?: return
        val todo = Board.bands(s, projectId).filter { it.nameOrigin == Origin.AUTO && it.items.size >= 2 && it.key != Board.PEOPLE_BAND }
        val sig = projectId + todo.joinToString { it.key + it.items.size }
        if (todo.isEmpty() || !named.add(sig)) return
        viewModelScope.launch {
            runCatching { brain.nameBands(p.title, todo.map { b -> b.items.map { it.title } }) }.onSuccess { names ->
                todo.zip(names).forEach { (b, n) ->
                    if (n != null && repo.snap.detailsOf(projectId, DetailKind.BAND).none { it.label == b.key }) repo.putDetail(Detail(objectId = projectId, kind = DetailKind.BAND, label = b.key, value = n, origin = Origin.AI))
                }
            }
        }
    }

    // ---------------------------------------------------------------- the input box: catch or ask
    /**
     * The text lives in a [TextFieldState] (snapshot state, edited synchronously by the keyboard) — never in a StateFlow,
     * which lags the keyboard and makes some keyboards drop or revert what you type.
     * Source: developer.android.com/develop/ui/compose/text/migrate-state-based (checked 2026-11-23).
     */
    val catchField = TextFieldState()
    val catchText: String get() = catchField.text.toString()
    val catchOpen = MutableStateFlow(false)
    val kindOverride = MutableStateFlow<Sentence.Kind?>(null)
    /** Parts of the preview you tapped off (space / people / context / mention ids) before saving. */
    val dropped = MutableStateFlow<Set<String>>(emptySet())
    val dueOff = MutableStateFlow(false)
    /** The item the line will be added to: the page you're on, or the tile selected on a board. */
    val context = MutableStateFlow<String?>(null)
    /** Items you picked with "Link to…". */
    val extraLinks = MutableStateFlow<List<String>>(emptyList())
    val attachOpen = MutableStateFlow(false)
    /** Photos and files waiting in the input box until you send, so a line like "Add to Ankle Surgery" can say where they go. */
    class Pending(val id: String, val read: com.jmr.atlas.capture.Readers.Read, val thumb: androidx.compose.ui.graphics.ImageBitmap?)
    val attachments = MutableStateFlow<List<Pending>>(emptyList())
    /** Spaces you asked for in "Link to…" that don't exist yet; made when you send. */
    val newLinks = MutableStateFlow<List<String>>(emptyList())
    val thread = MutableStateFlow<List<Turn>>(emptyList())
    /** What Atlas is doing right now, shown under its name ("Thinking on this phone…"). */
    val working = MutableStateFlow<String?>(null)
    private var askJob: Job? = null
    private var lastText = ""

    /** Opens the input box. Without [prefill] whatever you'd typed before is still there (a draft survives closing). */
    fun openCatch(prefill: String? = null) {
        if (prefill != null) catchField.setTextAndPlaceCursorAtEnd(prefill)
        else if (catchField.text.isNotEmpty()) catchField.edit { selectAll() }   // an old draft is selected, so typing replaces it
        catchOpenedAt = android.os.SystemClock.uptimeMillis()
        context.value = (route as? Route.Item)?.id ?: boardSel.value?.takeIf { route is Route.Space }
        resetParts(); attachOpen.value = false; catchOpen.value = true
    }
    /** Closes the box; the draft (text and attachments) stays for next time. Sending is what clears it. */
    fun closeCatch() { catchOpen.value = false; attachOpen.value = false }
    /** When the box last opened: taps on the backdrop right after (keyboard still sliding in) are ignored. */
    var catchOpenedAt = 0L; private set
    private fun resetParts() { kindOverride.value = null; dropped.value = emptySet(); dueOff.value = false; extraLinks.value = emptyList(); newLinks.value = emptyList(); lastText = catchText }

    /** A photo, scan or file picked from the input box waits there; picked from anywhere else it's saved straight away. */
    fun attachOrCapture(uri: Uri, mimeHint: String? = null) {
        if (!catchOpen.value) { captureUri(uri, mimeHint); return }
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { com.jmr.atlas.capture.Readers.read(ctx, uri, mimeHint) } ?: run { say("Couldn't read that file."); return@launch }
            val thumb = if (r.mime.startsWith("image/")) withContext(Dispatchers.IO) {
                runCatching {
                    val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(r.bytes, 0, r.bytes.size, o)
                    var n = 1; while (maxOf(o.outWidth, o.outHeight) / (n * 2) >= 160) n *= 2
                    android.graphics.BitmapFactory.decodeByteArray(r.bytes, 0, r.bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = n })?.asImageBitmap()
                }.getOrNull()
            } else null
            attachments.value = attachments.value + Pending(newId(), r, thumb)
        }
    }
    fun removeAttachment(id: String) { attachments.value = attachments.value.filterNot { it.id == id } }
    fun addNewLink(name: String) { val n = Sentence.nameCase(name); if (n.isNotBlank() && newLinks.value.none { it.equals(n, true) }) newLinks.value = newLinks.value + n }
    fun removeNewLink(name: String) { newLinks.value = newLinks.value - name }
    /** Called as you type: a new line forgets the choices you made for the old one and any answer shown. */
    fun textChanged(t: String) {
        if (t == lastText) return
        lastText = t
        kindOverride.value = null; dueOff.value = false
    }
    fun setCatch(t: String) { catchField.setTextAndPlaceCursorAtEnd(t); textChanged(t) }
    fun cycleKind(current: Sentence.Kind) { val k = Sentence.kindsFor(contextObj); kindOverride.value = k[(k.indexOf(current).coerceAtLeast(0) + 1) % k.size] }
    fun toggleDrop(id: String) { dropped.value = dropped.value.let { if (id in it) it - id else it + id } }
    fun addLink(id: String) { if (id != context.value && id !in extraLinks.value) extraLinks.value = extraLinks.value + id }
    fun removeLink(id: String) { extraLinks.value = extraLinks.value - id }

    val contextObj: KObject? get() = context.value?.let { repo.snap.objects[it] }?.takeIf { !it.trashed }

    /** The space you're looking at, used as the default home for what you type. */
    val currentProject: String? get() = (route as? Route.Space)?.space?.takeIf { it.startsWith("p:") }?.removePrefix("p:")
        ?: contextObj?.let { if (it.type == ObjType.PROJECT) it.id else Briefing.projectOf(repo.snap, it)?.id }

    fun parsed(text: String = catchText): Sentence.Parsed = Sentence.parse(text, repo.snap, LocalDate.now(), currentProject)

    /** The kind a line will be saved as: your choice, else what it reads as — a checklist step when you're on a task. */
    fun kindOf(p: Sentence.Parsed): Sentence.Kind = kindOverride.value
        ?: if (contextObj?.type == ObjType.TASK && p.due == null && p.kind != Sentence.Kind.IDEA && p.kind != Sentence.Kind.SPACE && p.target == null && p.newSpace == null && context.value !in dropped.value) Sentence.Kind.STEP else p.kind

    // ---------------------------------------------------------------- the instant engine (design 0.7)
    private var ixFor: Snapshot? = null
    private var ix: Search.Index? = null
    /** Atlas's own search index, rebuilt only when something changes. */
    fun searchIndex(): Search.Index { val s = repo.snap; if (s !== ixFor || ix == null) { ix = Search.index(s); ixFor = s }; return ix!! }
    fun find(text: String): List<Search.Hit> = if (text.isBlank()) emptyList() else Search.find(searchIndex(), text)

    /** True when the last thing Atlas said was a general answer, so a follow-up continues that conversation. */
    val inChat: Boolean get() = (thread.value.lastOrNull { it !is Turn.You } as? Turn.World)?.let { !it.noModel } == true

    fun intentOf(text: String): Sentence.Intent {
        val q = Sentence.isQuestion(text)
        val terms = if (q) Sentence.searchTerms(text).ifBlank { text } else text
        // a question that names one of your things ("what could delay demolition?") is about your world, even if
        // not every word matches; inside a space, questions are about that space
        val touches = q && (currentProject != null || Search.words(terms).filter { it.length >= 4 }.any { w -> find(w).any { it.inTitle && it.score >= 5.0 && it.o.type != ObjType.NOTE } })
        return Sentence.intent(text, repo.snap, find(terms), inChat, touches)
    }

    fun send() {
        val text = catchText.trim()
        if (attachments.value.isNotEmpty()) { saveFiles(if (text.isEmpty()) null else parsed(text)); return }
        if (text.isEmpty()) return
        val p = parsed(text)
        when (intentOf(text)) {
            Sentence.Intent.FIND -> find(text).firstOrNull()?.let { openHit(it.o.id) } ?: save(p.copy(kind = Sentence.Kind.NOTE))
            Sentence.Intent.CAPTURE -> if (inChat && p.title.isBlank() && (p.target != null || p.newSpace != null)) saveWorld(null, p.target, p.newSpace) else save(p)
            Sentence.Intent.ASK_MINE -> askMine(text)
            Sentence.Intent.ASK_WORLD, Sentence.Intent.CHAT -> askWorld(text)
        }
    }

    /** Save a line as exactly this kind (used when Atlas read it as a search but you meant to keep it). */
    fun saveLine(text: String, kind: Sentence.Kind) { val p = parsed(text.trim()); kindOverride.value = kind; save(p.copy(title = p.title.ifBlank { text.trim() })) }

    /** Open a search result from the box. */
    fun openHit(id: String) { closeCatch(); reveal(id) }

    fun clearThread() { askJob?.cancel(); working.value = null; thread.value = emptyList(); threadHidden.value = false }
    /** Swiped down: the conversation folds to a pill above the input; it's kept until Clear. */
    val threadHidden = MutableStateFlow(false)
    private fun push(vararg t: Turn) { thread.value = thread.value + t; threadHidden.value = false }
    private fun replace(id: String, f: (Turn) -> Turn) { thread.value = thread.value.map { if (it.id == id) f(it) else it } }
    private fun done(text: String, ids: List<String>, undo: (() -> Unit)?, said: String) {
        push(Turn.You(text = said, files = attachments.value.map { it.read.name }), Turn.Did(text = text, ids = ids, undo = undo))
        catchField.clearText(); attachments.value = emptyList(); lastText = ""; resetParts()
    }
    fun undoTurn(id: String) {
        val t = thread.value.firstOrNull { it.id == id } as? Turn.Did ?: return
        if (t.undone) return
        runCatching { t.undo?.invoke() }
        replace(id) { (it as Turn.Did).copy(undone = true) }
    }

    /** A general question or a reply in a conversation: answered by the model, with "save to…" offers underneath. */
    private fun askWorld(question: String) {
        catchField.clearText(); lastText = ""
        val turn = Turn.World(question = question)
        push(Turn.You(text = question), turn)
        if (!brainReady()) {
            replace(turn.id) { (it as Turn.World).copy(thinking = false, noModel = true, text = "I answer questions like this with the AI model on your phone, and it isn’t set up yet. It’s a one-time 2.6 GB download (or import the file), then it works offline.") }
            return
        }
        val history = thread.value.dropLast(2).flatMap { t -> when (t) { is Turn.You -> listOf(true to t.text); is Turn.World -> if (t.text.isNotBlank() && !t.noModel) listOf(false to t.text) else emptyList(); else -> emptyList() } }
        askJob?.cancel()
        askJob = viewModelScope.launch {
            working.value = "Thinking on this phone…"
            val text = withTimeoutOrNull(90_000) { runCatching { brain.chat(history, question) }.getOrNull() }?.takeIf { it.isNotBlank() }
            working.value = null
            val offers = if (text == null) emptyList() else spaceOffers(question + " " + text)
            replace(turn.id) { (it as Turn.World).copy(thinking = false, text = text ?: "The model didn’t answer in time. Try again, or ask something shorter.", offers = offers) }
        }
    }

    /** Spaces a general answer might belong to: ones whose name shares a word with the question or answer. */
    private fun spaceOffers(text: String): List<String> {
        val ws = Search.words(text).filter { it.length >= 4 }.toSet()
        return repo.snap.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED && Search.words(it.title).any { w -> w.length >= 4 && w in ws } }
            .sortedByDescending { it.updated }.take(2).map { it.id }
    }

    /** Save a general answer as a note: to a space, a new space, or on its own (Today). Uses the latest answer when [turnId] is null. */
    fun saveWorld(turnId: String?, space: KObject?, newSpace: String?) {
        val t = (turnId?.let { id -> thread.value.firstOrNull { it.id == id } } ?: thread.value.lastOrNull { it is Turn.World && it.text.isNotBlank() && !it.noModel }) as? Turn.World ?: return
        val made = newSpace?.let { makeSpace(Sentence.nameCase(it)) }
        val target = made ?: space
        val title = t.question.trim().replaceFirstChar { it.uppercase() }.take(80)
        val id = repo.create(KObject(type = ObjType.NOTE, title = title, body = t.text), listOfNotNull(target?.let { it.id to RelType.BELONGS_TO }))
        replace(t.id) { (it as Turn.World).copy(saved = true) }
        val where = target?.let { if (made != null) "Saved to ${it.title} (new space)" else "Saved to ${it.title}" } ?: "Saved as a note"
        push(Turn.Did(text = where, ids = listOf(id), undo = { repo.trash(id); made?.let { m -> repo.trash(m.id) } }))
        catchField.clearText(); lastText = ""
    }

    /** New spaces this line asks for (typed or picked), unless you crossed them out. */
    private fun spacesToMake(p: Sentence.Parsed?): List<String> =
        (listOfNotNull(p?.newSpace, p?.takeIf { it.kind == Sentence.Kind.SPACE && it.target == null && kindOverride.value == null }?.title?.takeIf { attachments.value.isNotEmpty() }) + newLinks.value)
            .distinctBy { it.lowercase() }.filter { "new:${it.lowercase()}" !in dropped.value }

    private fun makeSpace(name: String): KObject {
        repo.snap.live.firstOrNull { it.type == ObjType.PROJECT && it.title.equals(name, true) }?.let { return it }
        val o = KObject(type = ObjType.PROJECT, title = name, projectStatus = ProjectStatus.ACTIVE)
        repo.create(o, emptyList()); return repo.snap.objects[o.id] ?: o
    }

    /** Photos/files waiting in the box: saved as originals, linked where the line says (or to what's open), captioned by the rest. */
    private fun saveFiles(p: Sentence.Parsed?) {
        val drop = dropped.value
        val made = spacesToMake(p).map { makeSpace(it) }
        val ctx = contextObj?.takeIf { it.id !in drop }
        val links = (made + listOfNotNull(p?.target, ctx, p?.space?.takeIf { it.id !in drop }) + p?.people.orEmpty().filter { it.id !in drop } +
            p?.mentions.orEmpty().filter { it.id !in drop } + extraLinks.value.mapNotNull { repo.snap.objects[it] }).filter { it.id !in drop }.distinctBy { it.id }
        val caption = p?.takeIf { it.kind != Sentence.Kind.SPACE }?.title.orEmpty()
        val files = attachments.value
        val app = getApplication<Application>()
        val docs = files.mapNotNull { f ->
            val capId = runCatching { repo.captureFile(f.read.bytes, f.read.mime, f.read.name, caption) }.getOrNull() ?: return@mapNotNull null
            val docId = repo.snap.captures.firstOrNull { it.id == capId }?.attachmentId ?: return@mapNotNull null
            links.forEach { repo.link(docId, it.id, if (it.type == ObjType.PROJECT) RelType.BELONGS_TO else RelType.RELATED_TO) }
            viewModelScope.launch(Dispatchers.Default) {
                runCatching { repo.setExtracted(docId, com.jmr.atlas.capture.Readers.text(app, f.read)) }
                processCapture(capId)
            }
            docId
        }
        if (docs.isEmpty()) { say("Couldn't save that file."); return }
        val what = if (docs.size == 1) (if (files[0].read.mime.startsWith("image/")) "Photo" else "File") else "${docs.size} ${if (files.all { it.read.mime.startsWith("image/") }) "photos" else "files"}"
        val where = links.firstOrNull { it.type == ObjType.PROJECT }?.let { sp -> if (made.any { it.id == sp.id }) "added to ${sp.title} (new space)" else "added to ${sp.title}" }
            ?: links.firstOrNull()?.let { "linked to ${it.title}" } ?: "saved"
        done("$what $where".replaceFirstChar { it.uppercase() }, docs, { docs.forEach { repo.trash(it) }; made.forEach { repo.trash(it.id) } }, p?.raw.orEmpty())
    }

    private fun relFor(child: ObjType, parent: KObject) = when {
        parent.type == ObjType.PROJECT || parent.type == ObjType.COLLECTION -> RelType.BELONGS_TO
        parent.type == ObjType.PERSON && child == ObjType.NOTE -> RelType.DISCUSSED_WITH
        parent.type == ObjType.TASK && child == ObjType.TASK -> RelType.FOLLOW_UP_TO
        else -> RelType.RELATED_TO
    }

    private fun save(p: Sentence.Parsed) {
        val kind = kindOf(p)
        val drop = dropped.value
        val ctx = contextObj?.takeIf { it.id !in drop }
        val space = p.space?.takeIf { it.id !in drop }
        val people = p.people.filter { it.id !in drop }
        val links = (listOfNotNull(ctx, space) + people + p.mentions.filter { it.id !in drop } + extraLinks.value.mapNotNull { repo.snap.objects[it] }).distinctBy { it.id }
        val due = if (dueOff.value) null else p.due
        val result: Pair<String, () -> Unit>? = runCatching<Pair<String, () -> Unit>?> {
            if (kind == Sentence.Kind.SPACE) {
                if (p.target != null && kindOverride.value == null) { closeCatch(); go(Route.Space("p:${p.target.id}")); return }
                val sp = makeSpace(p.title)
                people.forEach { repo.link(it.id, sp.id, RelType.PART_OF) }
                return@runCatching "New space: ${sp.title}" to { repo.trash(sp.id) }
            }
            if (p.title.isBlank()) { say(if (p.target != null) "Type what to add to ${p.target.title}, or attach a photo or file." else "Type something to save."); return }
            val made = spacesToMake(p).map { makeSpace(it) }
            val links = (made + links).distinctBy { it.id }
            when {
                kind.addsToContext && ctx == null -> { say("Open the item you want to add this to, or pick another kind."); null }
                kind == Sentence.Kind.STEP -> {
                    val d = Detail(objectId = ctx!!.id, kind = DetailKind.STEP, value = p.title, sort = repo.snap.detailsOf(ctx.id, DetailKind.STEP).size)
                    repo.putDetail(d, "Step added"); "Step added to ${ctx.title}" to { repo.deleteDetail(d.id) }
                }
                kind == Sentence.Kind.REMEMBER -> {
                    val d = Detail(objectId = ctx!!.id, kind = DetailKind.REMEMBER, value = p.raw.replace(Regex("^(remember|note)[:,]?\\s*", RegexOption.IGNORE_CASE), "").replaceFirstChar { it.uppercase() })
                    repo.putDetail(d, "Added a thing to remember"); "Saved to ${ctx.firstName}’s things to remember" to { repo.deleteDetail(d.id) }
                }
                kind == Sentence.Kind.UPDATE -> {
                    repo.postUpdate(ctx!!.id, p.raw, ctx.health ?: Health.ON_TRACK)
                    val d = repo.snap.detailsOf(ctx.id, DetailKind.UPDATE).maxByOrNull { it.created }
                    "Update posted to ${ctx.title}" to { d?.let { repo.deleteDetail(it.id) } }
                }
                kind == Sentence.Kind.NOTE -> {
                    val capId = repo.captureText(p.raw.replace(Regex("@(?=\\S)"), ""))
                    val noteId = repo.snap.captures.firstOrNull { it.id == capId }?.noteId ?: error("note not saved")
                    links.forEach { repo.link(noteId, it.id, relFor(ObjType.NOTE, it)) }
                    viewModelScope.launch(Dispatchers.Default) { processCapture(capId) }
                    where(links, made) to { repo.trash(noteId); made.forEach { m -> repo.trash(m.id) } }
                }
                else -> {
                    val id = repo.create(KObject(type = kind.type, title = p.title, body = if (p.title != p.raw) p.raw else "", due = if (kind == Sentence.Kind.TASK) due else null),
                        links.map { it.id to relFor(kind.type, it) })
                    where(links, made) to { repo.trash(id); made.forEach { m -> repo.trash(m.id) } }
                }
            }
        }.getOrElse { say("Couldn't save — ${it.message}"); null }
        if (result != null) done(result.first, emptyList(), result.second, p.raw)
    }

    private fun where(links: List<KObject>, made: List<KObject> = emptyList()): String {
        val p = links.firstOrNull { it.type == ObjType.PROJECT }; val other = links.firstOrNull { it.type != ObjType.PROJECT }
        val pn = p?.let { if (made.any { m -> m.id == it.id }) "${it.title} (new space)" else it.title }
        return when { other != null && p != null -> "Saved to $pn, linked to ${other.title}"; other != null -> "Saved, linked to ${other.title}"; p != null -> "Saved to $pn"; else -> "Saved to Today" }
    }

    /** A question about your own things: Atlas's own answer at once, then the model refines it if it's on. */
    private fun askMine(question: String) {
        catchField.clearText(); lastText = ""
        askJob?.cancel()
        val s = repo.snap
        val (basis, rule) = runCatching { gather(question, s) }.getOrElse { emptyList<KObject>() to ("Couldn't search just now — ${it.message?.take(60)}" to emptyList()) }
        val canAi = brainReady() && basis.isNotEmpty()
        val turn = Turn.Mine(question = question, text = rule.first, ids = rule.second, byAi = false, spaceId = commonProject(s, rule.second), refining = canAi)
        push(Turn.You(text = question), turn)
        if (!canAi) return
        askJob = viewModelScope.launch {
            working.value = "Checking with the model on this phone…"
            val ai = withTimeoutOrNull(45_000) { runCatching { brain.answer(question, basis, s) }.getOrNull() }?.takeIf { it.text.isNotBlank() }
            working.value = null
            replace(turn.id) { cur -> (cur as Turn.Mine).let { if (ai != null) it.copy(text = ai.text, ids = ai.ids.ifEmpty { rule.second }, byAi = true, refining = false, spaceId = commonProject(s, ai.ids.ifEmpty { rule.second }) ?: it.spaceId)
                else it.copy(refining = false, note = "The model didn’t answer in time — this is Atlas’s own answer.") } }
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
    fun showOnBoard(a: Turn.Mine) {
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
        if (!brainReady()) return
        val cap = repo.snap.captures.firstOrNull { it.id == captureId } ?: return
        val text = listOfNotNull(cap.text.takeIf { it.isNotBlank() }, cap.attachmentId?.let { repo.snap.objects[it]?.extracted }).joinToString("\n").trim()
        if (text.length < 12) return
        aiBusy.value = captureId
        runCatching { brain.extract(text, repo.snap) }.onSuccess { repo.addProposals(captureId, it.map { s -> s.copy(captureId = captureId) }) }
            .onFailure { say("On-device AI couldn't read this one. Atlas's rules still did.") }
        aiBusy.value = null
    }

    fun summarize(id: String) {
        val o = repo.snap.objects[id] ?: return
        val text = listOf(o.extracted, o.body).filter { it.isNotBlank() }.joinToString("\n")
        if (text.isBlank()) { say("There's no text to summarize yet."); return }
        viewModelScope.launch {
            aiBusy.value = id
            runCatching { brain.summarize(text) }.onSuccess { repo.update(repo.snap.objects[id]!!.copy(summary = it), "Summarized on this phone") }
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
