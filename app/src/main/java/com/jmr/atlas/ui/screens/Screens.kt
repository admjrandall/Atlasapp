package com.jmr.atlas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.objects.Segmented
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.icon
import java.time.LocalDate

@Composable
fun PageHeader(title: String, vm: AtlasViewModel, showBack: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton({ vm.back() }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } else Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

// ================================================================ capture (design C09: a sheet over whatever you were doing)
@Composable
fun CaptureContent(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    var text by rememberSaveable { mutableStateOf("") }
    val busy by vm.busy.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val last by vm.lastCapture.collectAsState()
    var more by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (last == null) runCatching { fr.requestFocus() } }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("capture")) {
        Text("Capture", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 128.dp).focusRequester(fr).testTag("capture_field"),
            placeholder = { Text("What happened, what you decided, what to remember…") },
            shape = RoundedCornerShape(22.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Atlas.Primary, unfocusedBorderColor = Atlas.Primary.copy(alpha = 0.6f)))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundSource(Icons.Outlined.Mic, "Voice note") { platform.dictate { t -> vm.captureText(t, voice = true) } }
            RoundSource(Icons.Outlined.PhotoCamera, "Scan or photo") { platform.scan() }
            RoundSource(Icons.Outlined.Description, "File") { platform.pickFile() }
            RoundSource(Icons.Outlined.Link, "Paste copied link") { platform.clipboardUrl()?.let { vm.captureLink(it) } ?: vm.say("No link on the clipboard.") }
            Spacer(Modifier.weight(1f))
            Button({ vm.captureText(text); text = "" }, enabled = text.isNotBlank() && busy == null, modifier = Modifier.heightIn(min = 52.dp).testTag("capture_save"),
                colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary)) { Text("Save", style = MaterialTheme.typography.titleSmall) }
        }
        if (busy != null || aiBusy != null) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp))
            Text(if (aiBusy != null) "Saved. Reading it with the on-device model…" else "Saved. Looking for connections…", style = MaterialTheme.typography.bodyMedium)
        }
        val lastCap = last?.let { id -> snap.captures.firstOrNull { it.id == id } }
        if (lastCap != null && busy == null) CaptureResult(vm, snap, lastCap, inSheet = true)
        Text("Or add directly", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ObjType.PROJECT, ObjType.PERSON, ObjType.TASK, ObjType.IDEA).forEach { t ->
                Pill(t.label, icon(t), Atlas.SurfaceSecondary, Atlas.DeepPlum, Modifier.heightIn(min = 44.dp).testTag("add_${t.name}")) { vm.show(if (t == ObjType.PERSON) Overlay.AddContact() else Overlay.Create(t)) }
            }
            Box {
                Pill("More", Icons.Outlined.MoreVert, Atlas.SurfaceSecondary, Atlas.DeepPlum, Modifier.heightIn(min = 44.dp)) { more = true }
                DropdownMenu(more, { more = false }) {
                    listOf(ObjType.NOTE, ObjType.LINK, ObjType.EVENT, ObjType.COLLECTION, ObjType.DECISION).forEach { t ->
                        DropdownMenuItem({ Text(t.label) }, { more = false; vm.show(Overlay.Create(t)) }, leadingIcon = { Icon(icon(t), null) })
                    }
                    DropdownMenuItem({ Text("Photo from gallery") }, { more = false; platform.pickImage() }, leadingIcon = { Icon(Icons.Outlined.Image, null) })
                }
            }
        }
    }
}

@Composable
private fun RoundSource(ic: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(Atlas.SurfaceSecondary).clickable(onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Icon(ic, null, tint = Atlas.Primary)
    }
}

/** What happened to a capture: automatic connections (with Undo) and anything new waiting for review. Used by the sheet and Now. */
@Composable
fun CaptureResult(vm: AtlasViewModel, snap: Snapshot, cap: Capture, inSheet: Boolean) {
    val sugs = snap.suggestions.filter { it.captureId == cap.id }
    val auto = sugs.filter { it.status == SuggestionStatus.ACCEPTED && it.kind == SuggestionKind.LINK_EXISTING && snap.activityOf(it.targetId ?: "").any { a -> a.text.startsWith("Connected automatically") } }
    val pend = sugs.filter { it.status == SuggestionStatus.PENDING }
    if (auto.isNotEmpty()) {
        Text("Connected automatically", style = MaterialTheme.typography.labelLarge, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            auto.forEach { s -> snap.objects[s.targetId]?.let { o -> Pill(o.title, icon(o.type), Atlas.PrimaryContainer, Atlas.DeepPlum, Modifier.testTag("auto_chip")) { vm.open(o.id) } } }
            IconButton({ auto.forEach { vm.repo.undoAuto(it.id) }; vm.say("Automatic connections undone.") }, Modifier.semantics { contentDescription = "Undo automatic connections" }.testTag("undo_auto")) {
                Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = Atlas.Primary)
            }
        }
    }
    if (pend.isEmpty()) {
        if (auto.isEmpty() && inSheet) Text(when (cap.status) { CaptureStatus.FAILED -> "Saved. Reading it failed — the original is kept."; CaptureStatus.CONNECTED -> "Saved and connected."; else -> "Saved. Nothing to connect." },
            style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 14.dp))
    } else if (inSheet) {
        SectionLabel("${plural(pend.size, "new thing")} to review") { TextButton({ vm.repo.acceptAll(cap.id) }, Modifier.testTag("accept_all")) { Text("Add all") } }
        pend.forEach { SuggestionCard(vm, snap, it) }
    } else TextButton({ vm.go(Route.Review) }, contentPadding = PaddingValues(0.dp)) { Text("${plural(pend.size, "new item")} to review →", style = MaterialTheme.typography.titleSmall) }
}


/** One proposal: what it is, the words it came from, and two choices. */
@Composable
fun SuggestionCard(vm: AtlasViewModel, snap: Snapshot, s: Suggestion) {
    val existing = s.targetId?.let { snap.objects[it] }
    val (badge, title, action) = when (s.kind) {
        SuggestionKind.LINK_EXISTING -> Triple("Connect to ${(existing?.type ?: s.objType)?.label?.lowercase() ?: "item"}", s.title, "Connect")
        SuggestionKind.NEW_PERSON -> Triple("New person", s.title, "Add")
        SuggestionKind.NEW_TASK -> Triple("New task", s.title, "Add")
        SuggestionKind.NEW_EVENT -> Triple("Possible milestone", s.title, "Add")
    }
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("suggestion")) {
        Pill(badge, s.objType?.let { icon(it) }, Atlas.PrimaryContainer, Atlas.DeepPlum)
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        s.due?.let { Text("${if (s.kind == SuggestionKind.NEW_EVENT) "Around" else "Due"} ${dayDate(it)}", style = MaterialTheme.typography.bodySmall, color = Atlas.DeepTeal) }
        Text(s.evidence, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton({ vm.repo.rejectSuggestion(s.id) }, Modifier.testTag("reject")) { Text("Dismiss") }
            FilledTonalButton({ vm.repo.accept(s.id) }, Modifier.testTag("accept")) { Text(action) }
        }
    }
}

// ================================================================ review inbox
@Composable
fun ReviewScreen(vm: AtlasViewModel, snap: Snapshot) {
    val byCap = snap.pending().groupBy { it.captureId }
    Column(Modifier.fillMaxSize().testTag("review")) {
        PageHeader("Review", vm)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (byCap.isEmpty()) item { Empty("All caught up", "What Atlas finds in your captures waits here until you add or dismiss it.") }
            byCap.forEach { (capId, sugs) ->
                val cap = snap.captures.firstOrNull { it.id == capId }
                item(key = capId) {
                    val src = cap?.let { (it.noteId ?: it.attachmentId)?.let { id -> snap.objects[id] } }
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
                        Text("“" + (cap?.text?.ifBlank { src?.title } ?: "").take(200) + "”", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cap?.let { ago(it.created) } ?: "", style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
                            TextButton({ vm.repo.rejectAll(capId) }) { Text("Dismiss all") }; TextButton({ vm.repo.acceptAll(capId) }) { Text("Add all") }
                        }
                    }
                }
                items(sugs, key = { it.id }) { SuggestionCard(vm, snap, it) }
            }
        }
    }
}

@Composable
fun TimelineScreen(vm: AtlasViewModel, snap: Snapshot) {
    val today = LocalDate.now()
    data class E(val date: LocalDate, val o: KObject, val kind: String, val planned: Boolean)
    val entries = buildList {
        snap.ofType(ObjType.EVENT).filter { it.eventDate != null }.forEach { add(E(it.eventDate!!, it, "Milestone", it.eventDate >= today)) }
        snap.ofType(ObjType.TASK).filter { !it.done && it.due != null }.forEach { add(E(it.due!!, it, "Task due", true)) }
        snap.ofType(ObjType.TASK).filter { it.done && it.completedAt != null }.forEach { add(E(java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), it, "Completed", false)) }
        snap.ofType(ObjType.PROJECT).forEach { p -> p.targetDate?.let { add(E(it, p, "Target date", it >= today)) }; p.startDate?.let { add(E(it, p, "Started", false)) } }
        snap.live.filter { it.type in setOf(ObjType.NOTE, ObjType.DOCUMENT, ObjType.IMAGE) }.forEach { add(E(java.time.Instant.ofEpochMilli(it.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), it, "Captured", false)) }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().testTag("timeline")) {
        PageHeader("Timeline", vm)
        Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).widthIn(max = 420.dp)) { Segmented(listOf("Planned", "Happened"), tab) { tab = it } }
        val l = if (tab == 0) entries.filter { it.planned }.sortedBy { it.date } else entries.filter { !it.planned }.sortedByDescending { it.date }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (l.isEmpty()) item { Empty(if (tab == 0) "Nothing planned" else "Nothing yet", "Milestones, due dates and captures show up here.") }
            l.groupBy { it.date.withDayOfMonth(1) }.forEach { (m, es) ->
                item(key = "m$m$tab") { SectionLabel(m.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))) }
                items(es, key = { it.o.id + it.kind + it.date }) { e -> ObjRow(e.o, snap, sub = "${e.kind} · ${relDate(e.date)}") { vm.open(e.o.id) } }
            }
        }
    }
}

// ================================================================ search
@Composable
fun SearchScreen(vm: AtlasViewModel, snap: Snapshot, initial: String) {
    var q by rememberSaveable { mutableStateOf(initial) }
    val res = remember(q, snap) { vm.repo.search(q) }
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()
    val events by vm.sync.events.collectAsState()
    val phone by produceState(emptyList<com.jmr.atlas.integrations.Contacts.Hit>(), q) { value = if (q.length >= 2) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.sync.contacts.search(q, 6) } else emptyList() }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial.isEmpty()) runCatching { fr.requestFocus() } }
    Column(Modifier.fillMaxSize().testTag("search")) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr).testTag("search_field"),
                placeholder = { Text("Search everything, contacts and calendar") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape,
                trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }, Modifier.semantics { contentDescription = "Clear" }) { Icon(Icons.Outlined.Close, null) }
                    else IconButton({ platform.dictate { q = it } }, Modifier.semantics { contentDescription = "Search by voice" }) { Icon(Icons.Outlined.Mic, null) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = {}))
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (q.isBlank()) item { Empty("Search everything", "People, projects, notes, text inside photos and PDFs, saved pages, your phone contacts and your calendar.") }
            if (q.isNotBlank() && res.exact.isEmpty() && phone.isEmpty()) item { Empty("No matches", "Atlas searches titles, notes, people's details and text read from photos, PDFs and saved pages.") }
            if (res.exact.isNotEmpty()) {
                item { SectionLabel("Matches · ${res.exact.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.exact.take(40), key = { "e" + it.id }) { o -> ObjRow(o, snap, sub = snippet(o, q), onToggle = { vm.repo.toggleDone(o.id) }) { vm.open(o.id) } }
            }
            if (res.connected.isNotEmpty()) {
                item { SectionLabel("Connected to those · ${res.connected.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.connected, key = { "c" + it.first.id }) { (o, via) -> ObjRow(o, snap, sub = "${o.type.label} · via ${via.title}") { vm.open(o.id) } }
            }
            val outside = phone.filter { h -> snap.ofType(ObjType.PERSON).none { it.contactKey == h.lookupKey } }
            if (outside.isNotEmpty()) {
                item { SectionLabel("In your phone contacts · ${outside.size}", Modifier.padding(horizontal = 8.dp)) }
                items(outside, key = { "p" + it.lookupKey }) { h ->
                    ListItem({ Text(h.name) }, supportingContent = { Text(h.sub.ifBlank { "Phone contact · tap to add to Atlas" }) },
                        leadingContent = { Box(Modifier.size(40.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Contacts, null, tint = Atlas.Primary) } },
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { scope.launch { val id = withContext(kotlinx.coroutines.Dispatchers.IO) { vm.sync.addContact(h.lookupKey) }; id?.let { vm.open(it); vm.say("${h.name} added to Atlas.") } } })
                }
            }
            val cal = events.filter { q.length >= 2 && (it.title.contains(q, true) || it.location.contains(q, true)) }
            if (cal.isNotEmpty()) {
                item { SectionLabel("On your calendar · ${cal.size}", Modifier.padding(horizontal = 8.dp)) }
                items(cal.take(8), key = { "e" + it.instanceId }) { e ->
                    ListItem({ Text(e.title) }, supportingContent = { Text(com.jmr.atlas.ui.cards.eventWhen(e, LocalDate.now())) },
                        leadingContent = { Icon(Icons.Outlined.Event, null, tint = Atlas.DeepTeal) },
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { platform.launch(com.jmr.atlas.integrations.Calendars.viewIntent(e.eventId)) })
                }
            }
        }
    }
}

/** A short excerpt around the first hit, trimmed to whole words. */
fun snippet(o: KObject, q: String): String {
    val term = q.trim().split(Regex("\\s+")).firstOrNull { it.length > 1 } ?: return typeLine(o)
    val src = listOf(o.body, o.extracted).firstOrNull { it.contains(term, true) }?.replace('\n', ' ') ?: return typeLine(o)
    val i = src.indexOf(term, ignoreCase = true)
    var start = maxOf(0, i - 32); var end = minOf(src.length, i + term.length + 60)
    if (start > 0) { val sp = src.indexOf(' ', start); start = if (sp in 0 until i) sp + 1 else i }
    if (end < src.length) { val sp = src.lastIndexOf(' ', end); if (sp > i + term.length) end = sp }
    return (if (start > 0) "…" else "") + src.substring(start, end).trim() + if (end < src.length) "…" else ""
}

// ================================================================ insights
@Composable
fun InsightsView(vm: AtlasViewModel, snap: Snapshot, focusId: String?, modifier: Modifier = Modifier) {
    var scoped by rememberSaveable { mutableStateOf(focusId != null) }
    val focus = focusId?.let { snap.objects[it] }
    val list = remember(snap, scoped, focusId) { vm.repo.insights(scope = if (scoped) focusId else null) }
    LazyColumn(modifier.fillMaxSize().testTag("insights"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp)) {
        if (focus != null) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(scoped, { scoped = true }, { Text(focus.title) }); FilterChip(!scoped, { scoped = false }, { Text("Everything") }) }
        }
        if (list.isEmpty()) item { Empty("Nothing needs attention", "No overdue tasks, stalled projects, unconnected files or open reviews.") }
        items(list, key = { it.key }) { i ->
            Card(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("insight")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(i.title, style = MaterialTheme.typography.titleSmall)
                        if (i.detail.isNotBlank()) Text(i.detail, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                    }
                    IconButton({ vm.repo.dismiss(i) }, Modifier.semantics { contentDescription = "Dismiss ${i.title}" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.SecondaryText) }
                }
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    i.sources.take(6).mapNotNull { snap.objects[it] }.forEach { s ->
                        val label = s.title + (if (s.type == ObjType.TASK && !s.done) s.due?.let { " · " + relDate(it) } ?: "" else "")
                        Pill(label, icon(s.type), Atlas.SurfaceSecondary, Atlas.DeepPlum) { if (i.key == "review") vm.go(Route.Review) else vm.open(s.id) }
                    }
                    if (i.sources.size > 6) Text("+${i.sources.size - 6}", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText)
                }
            }
        }
    }
}

// ================================================================ trash
@Composable
fun TrashScreen(vm: AtlasViewModel, snap: Snapshot) {
    val l = snap.objects.values.filter { it.trashed }.sortedByDescending { it.updated }
    Column(Modifier.fillMaxSize().testTag("trash")) {
        PageHeader("Trash", vm) { if (l.isNotEmpty()) TextButton({ vm.repo.emptyTrash(); vm.say("Trash emptied.") }) { Text("Empty trash", color = Atlas.Error) } }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            if (l.isEmpty()) item { Empty("Trash is empty", "Deleted items stay here until you empty it.") }
            items(l, key = { it.id }) { o ->
                ObjRow(o, snap, trailing = {
                    TextButton({ vm.repo.restore(o.id) }) { Text("Restore") }
                    IconButton({ vm.show(Overlay.ConfirmPurge(o.id)) }, Modifier.semantics { contentDescription = "Delete ${o.title} forever" }) { Icon(Icons.Outlined.DeleteForever, null, tint = Atlas.Error) }
                }) { vm.open(o.id) }
            }
        }
    }
}

