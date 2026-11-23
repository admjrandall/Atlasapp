package com.jmr.atlas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jmr.atlas.AtlasApp
import com.jmr.atlas.capture.Readers
import com.jmr.atlas.data.AiMode
import com.jmr.atlas.data.ExampleWorkspace
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.integrations.Web
import com.jmr.atlas.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Destinations. Encoded as strings so the back stack survives process death (SavedStateHandle). */
sealed interface Route {
    val key: String
    data object Now : Route { override val key = "now" }
    data object Universe : Route { override val key = "universe" }
    data object Search : Route { override val key = "search" }
    data class Library(val type: ObjType? = null) : Route { override val key = "lib:${type?.name.orEmpty()}" }
    data class Detail(val id: String) : Route { override val key = "obj:$id" }
    data object Timeline : Route { override val key = "timeline" }
    data object Review : Route { override val key = "review" }
    data object Settings : Route { override val key = "settings" }
    data object Integrations : Route { override val key = "integrations" }
    data object AiSettings : Route { override val key = "ai" }
    data object Security : Route { override val key = "security" }
    data object Trash : Route { override val key = "trash" }
    data class History(val id: String) : Route { override val key = "hist:$id" }
    data class Connections(val id: String) : Route { override val key = "conn:$id" }

    val top get() = this == Now || this == Universe || this == Search || this is Library

    companion object {
        private val fixed by lazy { listOf(Now, Universe, Search, Timeline, Review, Settings, Integrations, AiSettings, Security, Trash).associateBy { it.key } }
        fun parse(s: String): Route? = fixed[s] ?: when {
            s.startsWith("lib:") -> Library(ObjType.entries.firstOrNull { it.name == s.removePrefix("lib:") })
            s.startsWith("obj:") -> Detail(s.removePrefix("obj:"))
            s.startsWith("hist:") -> History(s.removePrefix("hist:"))
            s.startsWith("conn:") -> Connections(s.removePrefix("conn:"))
            else -> null
        }
    }
}

enum class MapMode { MAP, INSIGHTS }

/** Sheets and dialogs. One at a time; all of them perform real changes. */
sealed interface Overlay {
    data class Edit(val id: String) : Overlay
    data class Create(val type: ObjType?, val linkTo: String? = null, val title: String = "") : Overlay
    data class Connect(val id: String, val types: Set<ObjType>? = null) : Overlay
    data class Cluster(val focusId: String?, val kind: com.jmr.atlas.ui.universe.SlotKind) : Overlay
    data object FocusPicker : Overlay
    data class ConfirmPurge(val id: String) : Overlay
    data object Capture : Overlay
    data object RemoveExample : Overlay
    data class AddField(val id: String) : Overlay
    data class PostUpdate(val id: String) : Overlay
    data class AddLine(val id: String, val kind: DetailKind) : Overlay
    data class TaskDates(val id: String) : Overlay
    data class AddContact(val linkTo: String? = null) : Overlay
    data class Block(val id: String) : Overlay
    data object BriefingTime : Overlay
}

class AtlasViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val atlas = app as AtlasApp
    val repo = atlas.repo
    val sync = atlas.sync
    val ai = atlas.ai
    val snap = repo.state
    val settings get() = repo.settings

    private val _stack = MutableStateFlow(saved.get<String>("stack")?.split("|")?.mapNotNull { Route.parse(it) }?.ifEmpty { null } ?: listOf<Route>(Route.Now))
    val stack = _stack.asStateFlow()

    /** Universe focus + its own history, kept across fold/unfold and rotation. */
    private val _focus = MutableStateFlow(saved.get<String>("focus"))
    val focus = _focus.asStateFlow()
    private val focusHistory = ArrayDeque<String?>(saved.get<String>("fhist")?.split("|")?.map { it.ifEmpty { null } } ?: emptyList())

    private val _mode = MutableStateFlow(MapMode.valueOf(saved.get<String>("mode") ?: "MAP"))
    val mode = _mode.asStateFlow()

    private val _onboarded = MutableStateFlow(repo.onboarded)
    val onboarded = _onboarded.asStateFlow()

    val query = MutableStateFlow("")
    fun search(q: String) { query.value = q; top(Route.Search) }

    val overlay = MutableStateFlow<Overlay?>(null)
    fun show(o: Overlay?) { overlay.value = o }

    /** Messages for a snackbar (results of actions, never decorative). An optional Undo runs the given block. */
    data class Msg(val text: String, val undo: (() -> Unit)? = null)
    val messages = MutableStateFlow<Msg?>(null)
    fun say(m: String, undo: (() -> Unit)? = null) { messages.value = Msg(m, undo) }

    /** Capture currently being processed, for the capture sheet's progress state. */
    val busy = MutableStateFlow<String?>(null)
    val lastCapture = MutableStateFlow<String?>(null)
    val aiBusy = MutableStateFlow<String?>(null)

    private fun persist() {
        saved["stack"] = _stack.value.joinToString("|") { it.key }
        saved["focus"] = _focus.value
        saved["fhist"] = focusHistory.joinToString("|") { it.orEmpty() }
        saved["mode"] = _mode.value.name
    }

    val route get() = _stack.value.last()
    fun go(r: Route) { _stack.value = _stack.value.filter { it != r } + r; persist() }
    /** Top-level destinations replace the stack (bottom bar / rail). */
    fun top(r: Route) { _stack.value = if (r == Route.Now) listOf(Route.Now) else listOf(Route.Now, r); persist() }
    fun back(): Boolean {
        if (_stack.value.size > 1) { _stack.value = _stack.value.dropLast(1); persist(); return true }
        return false
    }
    fun canGoBack() = _stack.value.size > 1

    fun focusOn(id: String?) {
        if (id == _focus.value) { top(Route.Universe); return }
        focusHistory.addLast(_focus.value); while (focusHistory.size > 30) focusHistory.removeFirst()
        _focus.value = id
        top(Route.Universe)
    }
    fun focusBack() { if (focusHistory.isNotEmpty()) { _focus.value = focusHistory.removeLast(); persist() } }
    val hasFocusHistory get() = focusHistory.isNotEmpty()
    fun setMode(m: MapMode) { _mode.value = m; persist() }

    fun open(id: String) = go(Route.Detail(id))

    fun finishOnboarding(example: Boolean) {
        if (example) ExampleWorkspace.seed(repo)
        repo.onboarded = true; _onboarded.value = true
        if (example) repo.snap.live.firstOrNull { it.title == "Home Renovation" && it.origin == Origin.EXAMPLE }?.let { _focus.value = it.id; persist() }
        sync.start()
    }

    // ---------------------------------------------------------------- capture pipeline (original first, then on-device processing)
    fun captureText(text: String, from: String = "", voice: Boolean = false) {
        val url = Web.soleUrl(text)
        if (url != null) { captureLink(url, text.replace(url, "").trim(), from); return }
        val id = runCatching { repo.captureText(text, from, voice) }.getOrElse { say("Couldn't save — ${it.message}"); return }
        lastCapture.value = id
        viewModelScope.launch(Dispatchers.Default) { processCapture(id) }
    }

    fun captureLink(url: String, comment: String = "", from: String = "") {
        val id = runCatching { repo.captureLink(url, comment, from) }.getOrElse { say("Couldn't save that link."); return }
        lastCapture.value = id
        repo.snap.captures.firstOrNull { it.id == id }?.attachmentId?.let { sync.preview(it) }
        viewModelScope.launch(Dispatchers.Default) { processCapture(id) }
    }

    private suspend fun processCapture(id: String) {
        busy.value = id
        val n = runCatching { repo.process(id).size }.getOrElse { repo.markFailed(id); -1 }
        if (n >= 0) runAiOn(id)
        busy.value = null
        if (n < 0) say("Saved. Reading it failed — the original is kept.")
    }

    /** On-device model, only if you turned it on and the model is downloaded. Its proposals always go to review. */
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
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Readers.read(ctx, uri, mimeHint) }
            if (r == null) { say("Couldn't read that file."); return@launch }
            val capId = withContext(Dispatchers.IO) { runCatching { repo.captureFile(r.bytes, r.mime, r.name, comment) }.getOrNull() }
            if (capId == null) { say("Couldn't save that file."); return@launch }
            lastCapture.value = capId; busy.value = capId
            val docId = repo.snap.captures.firstOrNull { it.id == capId }?.attachmentId
            val text = withContext(Dispatchers.Default) { runCatching { Readers.text(ctx, r) }.getOrDefault("") }
            if (docId != null) repo.setExtracted(docId, text)
            withContext(Dispatchers.Default) { processCapture(capId) }
            if (overlay.value != Overlay.Capture) say("Saved “${r.name}”.")
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
