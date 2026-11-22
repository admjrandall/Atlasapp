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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun PageHeader(title: String, vm: AtlasViewModel, sub: String? = null, showBack: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton({ vm.back() }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } else Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
        actions()
    }
}

// ================================================================ capture
@Composable
fun CaptureScreen(vm: AtlasViewModel, snap: Snapshot, showBack: Boolean) {
    val platform = LocalPlatform.current
    var text by rememberSaveable { mutableStateOf("") }
    val busy by vm.busy.collectAsState()
    val last by vm.lastCapture.collectAsState()
    val fr = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().testTag("capture")) {
        PageHeader("Capture", vm, "Saved first, understood second. Nothing is connected without your review.", showBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).widthIn(max = 760.dp)) {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp).focusRequester(fr).testTag("capture_field"),
                placeholder = { Text("A conversation, a decision, something to remember… e.g. “Talked with Jamie about replacing the kitchen cabinets. He will send me an updated estimate Friday.”") },
                shape = RoundedCornerShape(22.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SourceButton(Icons.Outlined.Image, "Photo") { platform.pickImage() }
                SourceButton(Icons.Outlined.DocumentScanner, "Scan") { platform.scan() }
                SourceButton(Icons.Outlined.AttachFile, "File") { platform.pickFile() }
                Spacer(Modifier.weight(1f))
                Button({ vm.captureText(text); text = "" }, enabled = text.isNotBlank() && busy == null, modifier = Modifier.testTag("capture_save")) {
                    Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save")
                }
            }
            if (busy != null) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp))
                Text("Saved. Reading and looking for connections on this phone…", style = MaterialTheme.typography.bodyMedium)
            }
            val lastCap = last?.let { id -> snap.captures.firstOrNull { it.id == id } }
            if (lastCap != null && busy == null) {
                val pend = snap.pending().filter { it.captureId == lastCap.id }
                SectionLabel("Just captured · ${lastCap.status.label}")
                if (pend.isEmpty()) Text(if (lastCap.status == CaptureStatus.CONNECTED) "All suggestions handled." else "Saved as-is. Nothing in it matched people, projects or dates.",
                    style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                else {
                    Row { TextButton({ vm.repo.acceptAll(lastCap.id) }, Modifier.testTag("accept_all")) { Text("Accept all ${pend.size}") }; TextButton({ vm.repo.rejectAll(lastCap.id) }) { Text("Dismiss all") } }
                    pend.forEach { SuggestionCard(vm, snap, it) }
                }
            }
            SectionLabel("Or create directly")
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ObjType.PROJECT, ObjType.PERSON, ObjType.TASK, ObjType.IDEA, ObjType.LINK, ObjType.EVENT, ObjType.COLLECTION, ObjType.DECISION).forEach { t ->
                    AssistChip({ vm.show(Overlay.Create(t)) }, { Text(t.label) }, leadingIcon = { Icon(icon(t), null, Modifier.size(18.dp)) })
                }
            }
            val recent = snap.captures.take(8).filter { it.id != last }
            if (recent.isNotEmpty()) {
                SectionLabel("Recent captures")
                recent.forEach { c ->
                    val src = (c.noteId ?: c.attachmentId)?.let { snap.objects[it] }
                    ObjRowOrText(src, snap, c, vm)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ObjRowOrText(src: KObject?, snap: Snapshot, c: Capture, vm: AtlasViewModel) {
    if (src != null) ObjRow(src, snap, sub = "${c.status.label} · ${ago(c.created)}") { vm.open(src.id) }
    else Text("${c.text.take(80)} · ${c.status.label}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun SourceButton(ic: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.Primary) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SuggestionCard(vm: AtlasViewModel, snap: Snapshot, s: Suggestion) {
    val existing = s.targetId?.let { snap.objects[it] }
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("suggestion")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(s.kind.label, s.objType?.let { icon(it) }, Atlas.PrimaryContainer, Atlas.DeepPlum)
            Spacer(Modifier.width(8.dp))
            if (s.kind == SuggestionKind.LINK_EXISTING && existing != null) Text(existing.type.label, style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText)
        }
        Spacer(Modifier.height(8.dp))
        Text(when (s.kind) { SuggestionKind.LINK_EXISTING -> "Connect to ${s.title}"; SuggestionKind.NEW_PERSON -> "Add ${s.title} as a person"; else -> s.title },
            style = MaterialTheme.typography.titleSmall)
        s.due?.let { Text("${if (s.kind == SuggestionKind.NEW_EVENT) "Around" else "Due"} ${relDate(it)} · $it", style = MaterialTheme.typography.bodySmall, color = Atlas.DeepTeal) }
        Text(s.evidence, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            TextButton({ vm.repo.rejectSuggestion(s.id) }, Modifier.testTag("reject")) { Text("Dismiss") }
            FilledTonalButton({ vm.repo.accept(s.id) }, Modifier.testTag("accept")) { Text("Accept") }
        }
    }
}

// ================================================================ review inbox
@Composable
fun ReviewScreen(vm: AtlasViewModel, snap: Snapshot) {
    val byCap = snap.pending().groupBy { it.captureId }
    Column(Modifier.fillMaxSize().testTag("review")) {
        PageHeader("Review", vm, "Suggestions are not facts until you accept them.")
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (byCap.isEmpty()) item { Empty("All caught up", "When you capture something, Atlas proposes people, links and tasks here for you to accept or dismiss.") }
            byCap.forEach { (capId, sugs) ->
                val cap = snap.captures.firstOrNull { it.id == capId }
                item(key = capId) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("From capture · ${cap?.let { ago(it.created) } ?: ""}", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText)
                            val src = cap?.let { (it.noteId ?: it.attachmentId)?.let { id -> snap.objects[id] } }
                            Text(cap?.text?.ifBlank { src?.title }?.take(240) ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            Row { TextButton({ vm.repo.acceptAll(capId) }) { Text("Accept all ${sugs.size}") }; TextButton({ vm.repo.rejectAll(capId) }) { Text("Dismiss all") } }
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
        ObjType.TASK -> listOf<Pair<String, (KObject) -> Boolean>>("Today" to { o -> !o.done && o.due != null && !o.due.isAfter(today) }, "Upcoming" to { o -> !o.done && o.due != null && o.due.isAfter(today) },
            "All open" to { o -> !o.done }, "Blocked" to { o -> !o.done && o.blocked }, "Completed" to { o -> o.done })
        ObjType.PROJECT -> listOf<Pair<String, (KObject) -> Boolean>>("All" to { _ -> true }) + ProjectStatus.entries.map { s -> s.label to { o: KObject -> o.projectStatus == s } }
        ObjType.DOCUMENT -> listOf<Pair<String, (KObject) -> Boolean>>("All" to { _: KObject -> true }, "PDFs" to { o -> o.mime == "application/pdf" }, "Images" to { o -> o.type == ObjType.IMAGE },
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
        PageHeader(title, vm, "${base.size} ${if (base.size == 1) "item" else "items"}", showBack = false) {
            if (type == ObjType.DOCUMENT) IconButton({ grid = !grid }, Modifier.semantics { contentDescription = if (grid) "Show as list" else "Show as grid" }) { Icon(if (grid) Icons.Outlined.ViewList else Icons.Outlined.GridView, null) }
            FilledTonalIconButton({ vm.show(Overlay.Create(if (type == ObjType.DOCUMENT) ObjType.NOTE else type)) }, Modifier.semantics { contentDescription = "New ${type.label.lowercase()}" }) { Icon(Icons.Outlined.Add, null) }
        }
        if (filters.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { i, (l, f) -> FilterChip(filter == i, { filter = i }, { Text("$l · ${base.count(f)}") }, shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Atlas.PrimaryContainer)) }
        }
        if (list.isEmpty()) Empty("Nothing here", when (type) { ObjType.TASK -> "Tasks you create or accept from captures appear here."; ObjType.DOCUMENT -> "Capture a photo, scan or file — it is saved encrypted on this phone."
            else -> "Create one, or capture a note that mentions it." })
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
                ObjRow(o, snap, Modifier.background(if (sel) Atlas.PrimaryContainer.copy(alpha = .6f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp)),
                    sub = typeLine(o) + " · ${snap.relationsOf(o.id).size} connections", onToggle = { vm.repo.toggleDone(o.id) }) { click(o) }
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
    var showPlanned by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().testTag("timeline")) {
        PageHeader("Timeline", vm, "Planned dates and what actually happened, kept apart", showBack = false)
        Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).widthIn(max = 420.dp)) { Segmented(listOf("Planned", "Recorded"), showPlanned) { showPlanned = it } }
        val l = if (showPlanned == 0) entries.filter { it.planned }.sortedBy { it.date } else entries.filter { !it.planned }.sortedByDescending { it.date }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (l.isEmpty()) item { Empty(if (showPlanned == 0) "Nothing planned" else "Nothing recorded yet", "Milestones, due dates and captures land here.") }
            l.groupBy { it.date.withDayOfMonth(1) }.forEach { (m, es) ->
                item(key = "m$m$showPlanned") { SectionLabel(m.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))) }
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
        PageHeader("Search", vm, "Exact matches, and what they're connected to")
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(fr).testTag("search_field"),
            placeholder = { Text("People, projects, words inside documents…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape,
            trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }, Modifier.semantics { contentDescription = "Clear" }) { Icon(Icons.Outlined.Close, null) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = {}))
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (q.isNotBlank() && res.exact.isEmpty()) item { Empty("No matches", "Atlas searches titles, notes, people's details and text read from photos and PDFs.") }
            if (res.exact.isNotEmpty()) {
                item { SectionLabel("Exact matches · ${res.exact.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.exact.take(40), key = { "e" + it.id }) { o -> ObjRow(o, snap, sub = snippet(o, q), onToggle = { vm.repo.toggleDone(o.id) }) { vm.open(o.id) } }
            }
            if (res.connected.isNotEmpty()) {
                item { SectionLabel("Connected · ${res.connected.size}", Modifier.padding(horizontal = 8.dp)) }
                items(res.connected, key = { "c" + it.first.id }) { (o, via) -> ObjRow(o, snap, sub = "${o.type.label} · via ${via.title}") { vm.open(o.id) } }
            }
        }
    }
}

private fun snippet(o: KObject, q: String): String {
    val term = q.trim().split(Regex("\\s+")).firstOrNull { it.length > 1 } ?: return typeLine(o)
    val src = listOf(o.body, o.extracted).firstOrNull { it.contains(term, true) } ?: return typeLine(o)
    val i = src.indexOf(term, ignoreCase = true)
    return (if (i > 30) "…" else "") + src.substring(maxOf(0, i - 30), minOf(src.length, i + 60)).replace('\n', ' ') + "…"
}

// ================================================================ insights
@Composable
fun InsightsView(vm: AtlasViewModel, snap: Snapshot, focusId: String?, modifier: Modifier = Modifier) {
    var scoped by rememberSaveable { mutableStateOf(focusId != null) }
    val focus = focusId?.let { snap.objects[it] }
    val list = remember(snap, scoped, focusId) { vm.repo.insights(scope = if (scoped) focusId else null) }
    LazyColumn(modifier.fillMaxSize().testTag("insights"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp)) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("Rules over your own data — each one lists the items it came from. No scores, no guesses.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            if (focus != null) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(scoped, { scoped = true }, { Text(focus.title) }); FilterChip(!scoped, { scoped = false }, { Text("Everything") })
            }
        }
        if (list.isEmpty()) item { Empty("Nothing needs attention", "No overdue tasks, stalled projects, unconnected files or open reviews.") }
        items(list, key = { it.key }) { i ->
            Card(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("insight")) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(i.title, style = MaterialTheme.typography.titleSmall)
                        Text(i.detail, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconButton({ vm.repo.dismiss(i) }, Modifier.semantics { contentDescription = "Dismiss ${i.title}" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.SecondaryText) }
                }
                Text("Sources", style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    i.sources.take(6).mapNotNull { snap.objects[it] }.forEach { s -> Pill(s.title, icon(s.type), Atlas.SurfaceSecondary, Atlas.DeepPlum, Modifier.widthIn(max = 220.dp)) { if (i.key == "review") vm.go(Route.Review) else vm.open(s.id) } }
                    if (i.sources.size > 6) Text("+${i.sources.size - 6}", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText)
                }
                Text("Scope: ${i.scope} · checked ${ago(i.at)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ================================================================ settings & trash
@Composable
fun SettingsScreen(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    var confirmWipe by remember { mutableStateOf(false) }
    val examples = snap.objects.values.count { it.origin == Origin.EXAMPLE }
    Column(Modifier.fillMaxSize().testTag("settings")) {
        PageHeader("Settings", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).widthIn(max = 760.dp)) {
            SectionLabel("Privacy")
            Card(Modifier.fillMaxWidth(), color = Atlas.SurfaceSecondary) {
                Fact(Icons.Outlined.WifiOff, "No internet access", "This version has no network permission. Nothing you capture can leave the phone through Atlas.")
                Fact(Icons.Outlined.Lock, "Originals encrypted", if (vm.repo.files.keystoreBacked) "Photos, scans and files are stored with AES-256-GCM; the key lives in Android Keystore." else "Test mode: Keystore unavailable, a local test key is in use.")
                Fact(Icons.Outlined.CloudOff, "Excluded from Android backup", "Atlas data is never copied by cloud backup or device transfer. Use Export to keep a copy.")
                Fact(Icons.Outlined.PhoneAndroid, "Understanding happens here", "Text recognition (ML Kit, bundled) and suggestions run on this phone. AI providers are not part of this version.")
                Fact(Icons.Outlined.Info, "Not yet in this version", "App lock and an encrypted database file arrive in a later phase. Use your phone's screen lock.")
            }
            SectionLabel("Your data")
            SettingRow(Icons.Outlined.Download, "Export everything (JSON)", "Readable copy of all items, connections and captures. Files are listed, not included.") { platform.export(vm.repo.exportJson()) }
            SettingRow(Icons.Outlined.Delete, "Trash", "${snap.objects.values.count { it.trashed }} items") { vm.go(Route.Trash) }
            if (examples > 0) SettingRow(Icons.Outlined.CleaningServices, "Remove example workspace", "$examples example items") { vm.repo.clearExample(); vm.focusOn(null); vm.say("Example workspace removed.") }
            SettingRow(Icons.Outlined.DeleteForever, "Erase everything", "Deletes all items, connections and encrypted originals on this phone.", danger = true) { confirmWipe = true }
            SectionLabel("About")
            Text("Atlas 0.3 · personal knowledge universe\nRoboto Flex © Google, SIL Open Font License 1.1. ML Kit text recognition & document scanner © Google.\nNo ads, analytics or telemetry.",
                style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            Spacer(Modifier.height(80.dp))
        }
    }
    if (confirmWipe) AlertDialog({ confirmWipe = false }, title = { Text("Erase everything?") }, text = { Text("All items, connections, captures and encrypted originals are permanently deleted from this phone.") },
        confirmButton = { TextButton({ vm.repo.wipeAll(); vm.focusOn(null); confirmWipe = false; vm.say("Everything erased.") }) { Text("Erase", color = Atlas.Error) } },
        dismissButton = { TextButton({ confirmWipe = false }) { Text("Cancel") } })
}

@Composable
private fun Fact(ic: androidx.compose.ui.graphics.vector.ImageVector, t: String, d: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Icon(ic, null, tint = Atlas.Primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
        Column { Text(t, style = MaterialTheme.typography.titleSmall); Text(d, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
    }
}

@Composable
private fun SettingRow(ic: androidx.compose.ui.graphics.vector.ImageVector, t: String, d: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(ic, null, tint = if (danger) Atlas.Error else Atlas.Primary); Spacer(Modifier.width(16.dp))
        Column { Text(t, style = MaterialTheme.typography.titleSmall, color = if (danger) Atlas.Error else Atlas.DeepPlum); Text(d, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
    }
}

@Composable
fun TrashScreen(vm: AtlasViewModel, snap: Snapshot) {
    val l = snap.objects.values.filter { it.trashed }.sortedByDescending { it.updated }
    Column(Modifier.fillMaxSize().testTag("trash")) {
        PageHeader("Trash", vm, "${l.size} items") { if (l.isNotEmpty()) TextButton({ vm.repo.emptyTrash(); vm.say("Trash emptied.") }) { Text("Empty", color = Atlas.Error) } }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            if (l.isEmpty()) item { Empty("Trash is empty", "Items you delete stay here until you empty it.") }
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
    Box(Modifier.fillMaxSize().background(Atlas.Background).testTag("onboarding"), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AtlasMark(88.dp)
            Spacer(Modifier.height(20.dp))
            Text("ATLAS", style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)))
            Spacer(Modifier.height(8.dp))
            Text("Your people, projects, files and ideas — connected, on this phone.", style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth(), color = Atlas.SurfaceSecondary) {
                Text("• Capture anything: a thought, a photo, a scan, a PDF.\n• Atlas keeps the original and proposes who and what it relates to.\n• You decide what becomes a connection. Proposals are never facts.\n• No internet permission. Nothing leaves the phone.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            Button({ vm.finishOnboarding(false) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("start_empty")) { Text("Start empty") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton({ vm.finishOnboarding(true) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("start_example")) { Text("Explore an example workspace") }
            Text("Example items are labeled and can be removed in Settings.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
