package com.jmr.atlas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jmr.atlas.AtlasApp
import com.jmr.atlas.capture.Readers
import com.jmr.atlas.data.ExampleWorkspace
import com.jmr.atlas.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Destinations. Encoded as strings so the back stack survives process death (SavedStateHandle). */
sealed interface Route {
    val key: String
    data object Universe : Route { override val key = "universe" }
    data class Objects(val type: ObjType) : Route { override val key = "list:${type.name}" }
    data class Detail(val id: String) : Route { override val key = "obj:$id" }
    data object Timeline : Route { override val key = "timeline" }
    data object Review : Route { override val key = "review" }
    data object Search : Route { override val key = "search" }
    data object Settings : Route { override val key = "settings" }
    data object Trash : Route { override val key = "trash" }

    companion object {
        fun parse(s: String): Route? = when {
            s == "universe" -> Universe; s == "timeline" -> Timeline; s == "review" -> Review
            s == "search" -> Search; s == "settings" -> Settings; s == "trash" -> Trash
            s.startsWith("list:") -> ObjType.entries.firstOrNull { it.name == s.removePrefix("list:") }?.let { Objects(it) }
            s.startsWith("obj:") -> Detail(s.removePrefix("obj:"))
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
}

class AtlasViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    val repo = (app as AtlasApp).repo
    val snap = repo.state

    private val _stack = MutableStateFlow(saved.get<String>("stack")?.split("|")?.mapNotNull { Route.parse(it) }?.ifEmpty { null } ?: listOf<Route>(Route.Universe))
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
    fun search(q: String) { query.value = q; go(Route.Search) }

    val overlay = MutableStateFlow<Overlay?>(null)
    fun show(o: Overlay?) { overlay.value = o }

    /** Messages for a snackbar (results of actions, never decorative). */
    val messages = MutableStateFlow<String?>(null)

    /** Capture currently being processed, for the capture screen's progress state. */
    val busy = MutableStateFlow<String?>(null)
    val lastCapture = MutableStateFlow<String?>(null)

    private fun persist() {
        saved["stack"] = _stack.value.joinToString("|") { it.key }
        saved["focus"] = _focus.value
        saved["fhist"] = focusHistory.joinToString("|") { it.orEmpty() }
        saved["mode"] = _mode.value.name
    }

    val route get() = _stack.value.last()
    fun go(r: Route) { _stack.value = if (r == Route.Universe) listOf(Route.Universe) else _stack.value.filter { it != r } + r; persist() }
    /** Top-level destinations replace the stack (rail / drawer). */
    fun top(r: Route) { _stack.value = if (r == Route.Universe) listOf(Route.Universe) else listOf(Route.Universe, r); persist() }
    fun back(): Boolean {
        if (_stack.value.size > 1) { _stack.value = _stack.value.dropLast(1); persist(); return true }
        if (focusHistory.isNotEmpty()) { _focus.value = focusHistory.removeLast(); persist(); return true }
        return false
    }
    fun canGoBack() = _stack.value.size > 1 || focusHistory.isNotEmpty()

    fun focusOn(id: String?) {
        if (id == _focus.value) return
        focusHistory.addLast(_focus.value); while (focusHistory.size > 30) focusHistory.removeFirst()
        _focus.value = id
        _stack.value = listOf(Route.Universe); persist()
    }
    fun focusBack() { if (focusHistory.isNotEmpty()) { _focus.value = focusHistory.removeLast(); persist() } }
    val hasFocusHistory get() = focusHistory.isNotEmpty()

    fun setMode(m: MapMode) { _mode.value = m; persist() }

    /** Opens an object: on the map when it is a hub type, otherwise its own page. */
    fun open(id: String) = go(Route.Detail(id))

    fun finishOnboarding(example: Boolean) {
        if (example) ExampleWorkspace.seed(repo)
        repo.onboarded = true; _onboarded.value = true
        if (example) repo.snap.live.firstOrNull { it.title == "Home Renovation" && it.origin == Origin.EXAMPLE }?.let { _focus.value = it.id; persist() }
    }

    fun say(m: String) { messages.value = m }

    // ---------------------------------------------------------------- capture pipeline (original first, then on-device processing)
    fun captureText(text: String) {
        val id = runCatching { repo.captureText(text) }.getOrElse { say("Couldn't save — ${it.message}"); return }
        lastCapture.value = id
        viewModelScope.launch(Dispatchers.Default) {
            busy.value = id
            val n = runCatching { repo.process(id).size }.getOrElse { repo.markFailed(id); -1 }
            busy.value = null
            if (n < 0) say("Saved. Reading it failed — the original is kept.")
        }
    }

    fun captureUri(uri: Uri, mimeHint: String? = null, comment: String = "") {
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
            val n = withContext(Dispatchers.Default) { runCatching { repo.process(capId).size }.getOrElse { repo.markFailed(capId); -1 } }
            busy.value = null
            if (overlay.value != Overlay.Capture) say(when {
                n < 0 -> "Saved “${r.name}”. Reading it failed — the original is kept."
                n == 0 -> "Saved “${r.name}”."
                else -> "Saved “${r.name}”. ${n} to review."
            })
        }
    }
}
