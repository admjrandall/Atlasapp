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
import androidx.compose.material.icons.automirrored.outlined.ViewList
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

// ================================================================ capture (a sheet over whatever you were doing)
@Composable
fun CaptureContent(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    var text by rememberSaveable { mutableStateOf("") }
    val busy by vm.busy.collectAsState()
    val last by vm.lastCapture.collectAsState()
    var more by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("capture")) {
        Text("Capture", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("capture_field"),
            placeholder = { Text("What happened, what you decided, what to remember…") },
            shape = RoundedCornerShape(20.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SourceButton(Icons.Outlined.Image, "Photo") { platform.pickImage() }
            SourceButton(Icons.Outlined.DocumentScanner, "Scan") { platform.scan() }
            SourceButton(Icons.Outlined.AttachFile, "File") { platform.pickFile() }
            Spacer(Modifier.weight(1f))
            Button({ vm.captureText(text); text = "" }, enabled = text.isNotBlank() && busy == null, modifier = Modifier.testTag("capture_save")) { Text("Save") }
        }
        if (busy != null) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp))
            Text("Saved. Looking for connections…", style = MaterialTheme.typography.bodyMedium)
        }
        val lastCap = last?.let { id -> snap.captures.firstOrNull { it.id == id } }
        if (lastCap != null && busy == null) {
            val pend = snap.pending().filter { it.captureId == lastCap.id }
            if (pend.isEmpty()) Text(when (lastCap.status) { CaptureStatus.CONNECTED -> "Saved and connected."; CaptureStatus.FAILED -> "Saved. Reading it failed — the original is kept."; else -> "Saved. Nothing to connect." },
                style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 16.dp))
            else {
                SectionLabel("Atlas found ${plural(pend.size, "thing")} to connect") { TextButton({ vm.repo.acceptAll(lastCap.id) }, Modifier.testTag("accept_all")) { Text("Add all") } }
                pend.forEach { SuggestionCard(vm, snap, it) }
            }
        }
        SectionLabel("Or add")
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(ObjType.PROJECT, ObjType.PERSON, ObjType.TASK, ObjType.IDEA, ObjType.LINK).forEach { t ->
                AssistChip({ vm.show(Overlay.Create(t)) }, { Text(t.label) }, leadingIcon = { Icon(icon(t), null, Modifier.size(18.dp)) })
            }
            Box {
                AssistChip({ more = true }, { Text("More") }, trailingIcon = { Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp)) })
                DropdownMenu(more, { more = false }) {
                    listOf(ObjType.NOTE, ObjType.EVENT, ObjType.COLLECTION, ObjType.DECISION).forEach { t ->
                        DropdownMenuItem({ Text(t.label) }, { more = false; vm.show(Overlay.Create(t)) }, leadingIcon = { Icon(icon(t), null) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceButton(ic: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.Primary, modifier = Modifier.size(20.dp)) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
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

// ================================================================ lists
@Composable
fun ObjectsScreen(vm: AtlasViewModel, snap: Snapshot, type: ObjType, selected: String?, onSelect: ((String) -> Unit)?) {
    val today = LocalDate.now()
    var filter by rememberSaveable(type) { mutableIntStateOf(0) }
    var grid by rememberSaveable(type) { mutableStateOf(false) }
    val filters: List<Pair<String, (KObject) -> Boolean>> = when (type) {
        ObjType.TASK -> listOf<Pair<String, (KObject) -> Boolean>>("Open" to { o -> !o.done }, "Overdue" to { o -> !o.done && o.due != null && o.due < today },
            "Today" to { o -> !o.done && o.due == today }, "Upcoming" to { o -> !o.done && o.due != null && o.due > today },
            "Blocked" to { o -> !o.done && o.blocked }, "Done" to { o -> o.done })
        ObjType.PROJECT -> listOf<Pair<String, (KObject) -> Boolean>>("All" to { _ -> true }) + ProjectStatus.entries.map { s -> s.label to { o: KObject -> o.projectStatus == s } }
        ObjType.DOCUMENT -> listOf<Pair<String, (KObject) -> Boolean>>("All" to { _ -> true }, "PDFs" to { o -> o.mime == "application/pdf" }, "Images" to { o -> o.type == ObjType.IMAGE },
            "Notes" to { o -> o.type == ObjType.NOTE }, "Links" to { o -> o.type == ObjType.LINK }, "Unconnected" to { o -> snap.relationsOf(o.id).isEmpty() })
        else -> emptyList()
    }
    val base = when (type) { ObjType.DOCUMENT -> snap.live.filter { it.type in setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.NOTE, ObjType.LINK) }; ObjType.IDEA -> snap.live.filter { it.type == ObjType.IDEA || it.type == ObjType.DECISION }; else -> snap.ofType(type) }
    val list = base.filter { filters.getOrNull(filter)?.second?.invoke(it) ?: true }.let { l ->
        when (type) { ObjType.TASK -> l.sortedWith(compareBy<KObject> { it.done }.thenBy { it.due ?: LocalDate.MAX }); ObjType.PERSON -> l.sortedBy { it.title.lowercase() }
            else -> l.sortedWith(compareByDescending<KObject> { it.pinned }.thenByDescending { it.updated }) }
    }
    val click: (KObject) -> Unit = { o -> if (onSelect != null) onSelect(o.id) else vm.open(o.id) }
    val title = when (type) { ObjType.DOCUMENT -> "Documents"; ObjType.IDEA -> "Ideas"; else -> type.plural }
    Column(Modifier.fillMaxSize().testTag("list_${type.name}")) {
        PageHeader(title, vm, showBack = false) {
            if (type == ObjType.DOCUMENT) IconButton({ grid = !grid }, Modifier.semantics { contentDescription = if (grid) "Show as list" else "Show as grid" }) {
                Icon(if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView, null)
            }
        }
        if (filters.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { i, (l, f) -> FilterChip(filter == i, { filter = i }, { Text("$l ${base.count(f)}") }, shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Atlas.PrimaryContainer), modifier = Modifier.testTag("filter_$l")) }
        }
        if (list.isEmpty()) Empty("Nothing here", when (type) { ObjType.TASK -> "Add a task, or capture a note that mentions one."; ObjType.DOCUMENT -> "Add a photo, scan or file — it's stored encrypted on this phone."
            else -> "Add one, or capture a note that mentions it." })
        else if (grid) LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { o ->
                Card(onClick = { click(o) }) {
                    ObjAvatar(o, 96.dp); Spacer(Modifier.height(8.dp))
                    Text(o.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(typeLine(o), style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1)
                }
            }
        } else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            items(list, key = { it.id }) { o ->
                val sel = o.id == selected
                val n = snap.relationsOf(o.id).size
                ObjRow(o, snap, Modifier.background(if (sel) Atlas.PrimaryContainer.copy(alpha = .6f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp)),
                    sub = typeLine(o) + if (n > 0) " · " + plural(n, "connection") else "", onToggle = { vm.repo.toggleDone(o.id) }) { click(o) }
            }
            item { Spacer(Modifier.height(96.dp)) }
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
        PageHeader("Timeline", vm, showBack = false)
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
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial.isEmpty()) runCatching { fr.requestFocus() } }
    Column(Modifier.fillMaxSize().testTag("search")) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vm.back() }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
            OutlinedTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr).testTag("search_field"),
                placeholder = { Text("Search people, projects, documents…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape,
                trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }, Modifier.semantics { contentDescription = "Clear" }) { Icon(Icons.Outlined.Close, null) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = {}))
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (q.isNotBlank() && res.exact.isEmpty()) item { Empty("No matches", "Atlas searches titles, notes, people's details and text read from photos and PDFs.") }
            if (res.exact.isNotEmpty()) {
                item { SectionLabel("Matches · ${res.exact.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.exact.take(40), key = { "e" + it.id }) { o -> ObjRow(o, snap, sub = snippet(o, q), onToggle = { vm.repo.toggleDone(o.id) }) { vm.open(o.id) } }
            }
            if (res.connected.isNotEmpty()) {
                item { SectionLabel("Connected to those · ${res.connected.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.connected, key = { "c" + it.first.id }) { (o, via) -> ObjRow(o, snap, sub = "${o.type.label} · via ${via.title}") { vm.open(o.id) } }
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

// ================================================================ settings & trash
@Composable
fun SettingsScreen(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    var confirmWipe by remember { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val examples = snap.objects.values.count { it.origin == Origin.EXAMPLE }
    val trashed = snap.objects.values.count { it.trashed }
    Column(Modifier.fillMaxSize().testTag("settings")) {
        PageHeader("Settings", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).widthIn(max = 760.dp)) {
            SectionLabel("Privacy")
            Card(Modifier.fillMaxWidth(), color = Atlas.SurfaceSecondary) {
                Fact(Icons.Outlined.WifiOff, "Nothing leaves this phone", "Atlas has no internet access.")
                Fact(Icons.Outlined.Lock, "Files are encrypted", "Photos, scans and documents are locked to this phone.")
                Fact(Icons.Outlined.CloudOff, "Not in Android backup", "Use Export below to keep your own copy.")
                TextButton({ details = !details }, contentPadding = PaddingValues(0.dp)) { Text(if (details) "Hide details" else "Details") }
                if (details) Text(
                    "• No INTERNET permission in this build.\n• Originals: AES-256-GCM, key in Android Keystore (${if (vm.repo.files.keystoreBacked) "hardware-backed where available" else "test key"}).\n" +
                        "• Text recognition: ML Kit, bundled model, on device. Suggestions: on-device rules. No AI providers.\n• Excluded from cloud backup and device transfer.\n" +
                        "• Not yet: app lock and an encrypted database file. Your screen lock protects notes and other text.",
                    style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            }
            SectionLabel("Your data")
            SettingRow(Icons.Outlined.Download, "Export everything", "A readable JSON copy. Files are listed, not included.") { platform.export(vm.repo.exportJson()) }
            SettingRow(Icons.Outlined.Delete, "Trash", if (trashed == 0) "Empty" else plural(trashed, "item")) { vm.go(Route.Trash) }
            if (examples > 0) SettingRow(Icons.Outlined.CleaningServices, "Remove example workspace", plural(examples, "example item")) { vm.repo.clearExample(); vm.focusOn(null); vm.say("Example workspace removed.") }
            SettingRow(Icons.Outlined.DeleteForever, "Erase everything", "All items, connections and files on this phone.", danger = true) { confirmWipe = true }
            SectionLabel("About")
            Text("Atlas 0.4 · No ads, analytics or tracking.\nRoboto Flex © Google (SIL Open Font License 1.1). ML Kit © Google.",
                style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            Spacer(Modifier.height(80.dp))
        }
    }
    if (confirmWipe) AlertDialog({ confirmWipe = false }, title = { Text("Erase everything?") }, text = { Text("All items, connections, captures and files are permanently deleted from this phone.") },
        confirmButton = { TextButton({ vm.repo.wipeAll(); vm.focusOn(null); confirmWipe = false; vm.say("Everything erased.") }) { Text("Erase", color = Atlas.Error) } },
        dismissButton = { TextButton({ confirmWipe = false }) { Text("Cancel") } })
}

@Composable
private fun Fact(ic: ImageVector, t: String, d: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(ic, null, tint = Atlas.Primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
        Column { Text(t, style = MaterialTheme.typography.titleSmall); Text(d, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
    }
}

@Composable
private fun SettingRow(ic: ImageVector, t: String, d: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(ic, null, tint = if (danger) Atlas.Error else Atlas.Primary); Spacer(Modifier.width(16.dp))
        Column { Text(t, style = MaterialTheme.typography.titleSmall, color = if (danger) Atlas.Error else Atlas.DeepPlum); Text(d, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
    }
}

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

// ================================================================ onboarding
@Composable
fun Onboarding(vm: AtlasViewModel) {
    Box(Modifier.fillMaxSize().background(Atlas.Background).windowInsetsPadding(WindowInsets.safeDrawing).testTag("onboarding"), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AtlasMark(88.dp)
            Spacer(Modifier.height(20.dp))
            Text("ATLAS", style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)))
            Spacer(Modifier.height(8.dp))
            Text("Your people, projects, files and ideas — connected.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Point(Icons.Outlined.Bolt, "Capture anything — a thought, photo, scan or PDF.")
            Point(Icons.Outlined.Hub, "Atlas suggests who and what it relates to. You decide.")
            Point(Icons.Outlined.Lock, "Everything stays on this phone.")
            Spacer(Modifier.height(24.dp))
            Button({ vm.finishOnboarding(true) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("start_example")) { Text("Explore an example") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton({ vm.finishOnboarding(false) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("start_empty")) { Text("Start empty") }
            Text("You can remove the example any time.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun Point(ic: ImageVector, t: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.Primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp)); Text(t, style = MaterialTheme.typography.bodyMedium)
    }
}
