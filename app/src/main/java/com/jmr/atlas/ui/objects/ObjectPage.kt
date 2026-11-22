package com.jmr.atlas.ui.objects

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.text.selection.SelectionContainer
import com.jmr.atlas.ui.screens.SuggestionCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate

enum class Tab(val label: String) {
    OVERVIEW("Overview"), CONNECTIONS("Connections"), FILES("Files"), TASKS("Tasks"), TIMELINE("Timeline"), ACTIVITY("Activity"),
    NOTES("Notes"), PROJECTS("Projects"), DOCUMENTS("Documents"), PREVIEW("Preview"), SUMMARY("Text"), ACTIONS("Actions"),
    RELATED("Related"), SOURCES("Sources"), CONTEXT("Context")
}

fun pageTabs(t: ObjType): List<Tab> = when (t) {
    ObjType.PROJECT -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.FILES, Tab.TASKS, Tab.TIMELINE, Tab.ACTIVITY)
    ObjType.PERSON -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.NOTES, Tab.PROJECTS, Tab.DOCUMENTS, Tab.ACTIVITY)
    ObjType.DOCUMENT, ObjType.IMAGE -> listOf(Tab.PREVIEW, Tab.SUMMARY, Tab.CONNECTIONS, Tab.ACTIONS, Tab.ACTIVITY)
    ObjType.IDEA -> listOf(Tab.OVERVIEW, Tab.RELATED, Tab.SOURCES, Tab.ACTIONS)
    ObjType.COLLECTION -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.ACTIVITY)
    else -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.SOURCES, Tab.ACTIVITY)
}

fun sheetTabs(t: ObjType): List<Tab> = when (t) {
    ObjType.PROJECT -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.FILES, Tab.TASKS)
    ObjType.PERSON -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.PROJECTS, Tab.DOCUMENTS)
    ObjType.DOCUMENT, ObjType.IMAGE -> listOf(Tab.PREVIEW, Tab.SUMMARY, Tab.CONNECTIONS)
    else -> listOf(Tab.OVERVIEW, Tab.CONNECTIONS, Tab.ACTIVITY)
}

@Composable
fun DetailScreen(vm: AtlasViewModel, snap: Snapshot, id: String) {
    val o = snap.objects[id]
    if (o == null) { Empty("Not found", "This item was permanently deleted."); return }
    val tabs = pageTabs(o.type)
    var tab by rememberSaveable(id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().testTag("detail")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vm.back() }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
            Spacer(Modifier.weight(1f))
            IconButton({ vm.focusOn(o.id) }, Modifier.semantics { contentDescription = "Show on map" }) { Icon(Icons.Outlined.Hub, null) }
            IconButton({ vm.show(Overlay.Edit(o.id)) }, Modifier.semantics { contentDescription = "Edit" }) { Icon(Icons.Outlined.Edit, null) }
            ObjectMenu(vm, o)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).widthIn(max = 760.dp)) {
            if (o.trashed) Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Atlas.Peach) {
                Text("In trash", style = MaterialTheme.typography.titleSmall)
                Row { TextButton({ vm.repo.restore(o.id) }) { Text("Restore") }; TextButton({ vm.show(Overlay.ConfirmPurge(o.id)) }) { Text("Delete forever", color = Atlas.Error) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ObjAvatar(o, 64.dp); Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(typeLine(o, snap), style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
                }
            }
            Spacer(Modifier.height(12.dp)); HeaderChips(o, snap); Spacer(Modifier.height(16.dp))
            ScrollTabs(tabs.map { it.label }, tab) { tab = it }
            Spacer(Modifier.height(12.dp))
            TabBody(vm, snap, o, tabs[tab], compact = false)
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun TabBody(vm: AtlasViewModel, snap: Snapshot, o: KObject, tab: Tab, compact: Boolean) {
    val nb = snap.neighbours(o.id, true)
    fun of(vararg t: ObjType) = nb.filter { it.first.type in t }
    when (tab) {
        Tab.OVERVIEW -> Overview(vm, snap, o, compact)
        Tab.CONTEXT -> ContextBlock(vm, snap, o, true)
        Tab.CONNECTIONS -> Connections(vm, snap, o, nb)
        Tab.FILES -> Related(vm, snap, o, of(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.NOTE), "No files or notes connected.", setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.NOTE))
        Tab.TASKS -> {
            Row { FilledTonalButton({ vm.show(Overlay.Create(ObjType.TASK, o.id)) }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add task") } }
            val tasks = of(ObjType.TASK).sortedWith(compareBy({ it.first.done }, { it.first.due ?: LocalDate.MAX }))
            if (tasks.isEmpty()) Text("No tasks connected.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
            tasks.forEach { (t, r) -> ObjRow(t, snap, proposed = r.review == Review.PROPOSED, onToggle = { vm.repo.toggleDone(t.id) }) { vm.open(t.id) } }
        }
        Tab.TIMELINE -> TimelineTab(vm, snap, o)
        Tab.ACTIVITY -> ActivityBlock(snap, o)
        Tab.NOTES -> Related(vm, snap, o, of(ObjType.NOTE), "No notes mention this person yet. Capture one — Atlas will suggest the link.", setOf(ObjType.NOTE))
        Tab.PROJECTS -> Related(vm, snap, o, of(ObjType.PROJECT, ObjType.COLLECTION), "Not connected to any project.", setOf(ObjType.PROJECT, ObjType.COLLECTION))
        Tab.DOCUMENTS -> Related(vm, snap, o, of(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK), "No documents connected.", setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK))
        Tab.PREVIEW -> Preview(o)
        Tab.SUMMARY -> {
            Text("Text read on this phone", style = MaterialTheme.typography.titleSmall)
            Text("Atlas doesn't summarize with AI in this version. This is the text recognized in the original, word for word.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            Spacer(Modifier.height(8.dp))
            SelectionContainer { Text(o.extracted.ifBlank { "No readable text was found." }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = Atlas.DeepPlum) }
        }
        Tab.ACTIONS -> Actions(vm, snap, o)
        Tab.RELATED -> Related(vm, snap, o, nb, "Nothing related yet.", null)
        Tab.SOURCES -> SourcesBlock(vm, snap, o)
    }
}

@Composable
private fun Overview(vm: AtlasViewModel, snap: Snapshot, o: KObject, compact: Boolean) {
    val ctx = LocalContext.current
    if (o.body.isNotBlank()) Text(o.body, style = MaterialTheme.typography.bodyLarge)
    else TextButton({ vm.show(Overlay.Edit(o.id)) }) { Text("Add a description") }
    val facts = buildList {
        if (o.type == ObjType.PERSON) { if (o.role.isNotBlank()) add("Role" to o.role); if (o.org.isNotBlank()) add("Organization" to o.org) }
        o.startDate?.let { add("Started" to relDate(it)) }; o.targetDate?.let { add("Target" to relDate(it)) }
        if (o.type == ObjType.TASK) { add("Due" to (o.due?.let(::relDate) ?: "No date")); add("Priority" to o.priority.label); if (o.blocked) add("Blocked" to "Yes") }
        o.eventDate?.let { add("Date" to relDate(it)) }
    }
    if (facts.isNotEmpty()) Column(Modifier.padding(top = 12.dp)) {
        facts.forEach { (k, v) -> Row(Modifier.padding(vertical = 3.dp)) { Text(k, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.width(110.dp)); Text(v, style = MaterialTheme.typography.bodyMedium) } }
    }
    if (o.type == ObjType.PERSON && (o.email.isNotBlank() || o.phone.isNotBlank())) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (o.email.isNotBlank()) AssistChip({ runCatching { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + o.email))) } }, { Text(o.email) }, leadingIcon = { Icon(Icons.Outlined.Mail, null) })
        if (o.phone.isNotBlank()) AssistChip({ runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + o.phone))) } }, { Text(o.phone) }, leadingIcon = { Icon(Icons.Outlined.Call, null) })
    }
    if (o.type == ObjType.LINK && o.url.isNotBlank()) AssistChip({ runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) } }, { Text("Open link") },
        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) }, modifier = Modifier.padding(top = 8.dp))
    if (o.type == ObjType.TASK) Row(Modifier.padding(top = 8.dp)) {
        FilledTonalButton({ vm.repo.toggleDone(o.id) }) { Icon(if (o.done) Icons.Outlined.Undo else Icons.Outlined.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (o.done) "Reopen" else "Complete") }
    }
    Spacer(Modifier.height(16.dp))
    ContextBlock(vm, snap, o, showNextSteps = o.type != ObjType.TASK)
    if (compact) {
        val nb = snap.neighbours(o.id, false).map { it.first }
        val ppl = nb.count { it.type == ObjType.PERSON }; val docs = nb.count { it.type in setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.NOTE) }; val tasks = nb.count { it.type == ObjType.TASK && !it.done }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) { nb.filter { it.type == ObjType.PERSON }.take(3).forEach { ObjAvatar(it, 30.dp, modifier = Modifier.border(2.dp, Atlas.Surface, androidx.compose.foundation.shape.CircleShape)) } }
            Spacer(Modifier.width(10.dp))
            Text("$ppl ${if (ppl == 1) "person" else "people"} · $docs ${if (docs == 1) "file" else "files"} · $tasks open ${if (tasks == 1) "task" else "tasks"}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
    }
}

@Composable
private fun Connections(vm: AtlasViewModel, snap: Snapshot, o: KObject, nb: List<Pair<KObject, Relation>>) {
    Row { FilledTonalButton({ vm.show(Overlay.Connect(o.id)) }, Modifier.testTag("connect_btn")) { Icon(Icons.Outlined.AddLink, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Connect") } }
    val proposed = nb.filter { it.second.review == Review.PROPOSED }
    if (proposed.isNotEmpty()) {
        SectionLabel("Proposed — not yet facts")
        proposed.forEach { (x, r) ->
            ObjRow(x, snap, sub = "${r.label(o.id)} · ${r.origin.label}", proposed = true, trailing = {
                IconButton({ vm.repo.approve(r.id) }, Modifier.semantics { contentDescription = "Confirm connection to ${x.title}" }) { Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal) }
                IconButton({ vm.repo.reject(r.id) }, Modifier.semantics { contentDescription = "Reject connection to ${x.title}" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.Error) }
            }) { vm.focusOn(x.id) }
        }
    }
    val confirmed = nb.filter { it.second.review == Review.CONFIRMED }
    if (confirmed.isEmpty() && proposed.isEmpty()) Text("No connections yet.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
    confirmed.groupBy { it.second.label(o.id) }.forEach { (label, l) ->
        SectionLabel("$label · ${l.size}")
        l.forEach { (x, r) ->
            ObjRow(x, snap, onToggle = { vm.repo.toggleDone(x.id) }, trailing = {
                IconButton({ vm.repo.unlink(r.id); vm.say("Disconnected ${x.title}.") }, Modifier.semantics { contentDescription = "Disconnect ${x.title}" }) { Icon(Icons.Outlined.LinkOff, null, tint = Atlas.SecondaryText) }
            }) { vm.focusOn(x.id) }
        }
    }
}

@Composable
private fun Related(vm: AtlasViewModel, snap: Snapshot, o: KObject, l: List<Pair<KObject, Relation>>, empty: String, types: Set<ObjType>?) {
    Row { FilledTonalButton({ vm.show(Overlay.Connect(o.id, types)) }) { Icon(Icons.Outlined.AddLink, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Connect") } }
    if (l.isEmpty()) Text(empty, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
    l.forEach { (x, r) -> ObjRow(x, snap, proposed = r.review == Review.PROPOSED, onToggle = { vm.repo.toggleDone(x.id) }) { vm.focusOn(x.id) } }
}

@Composable
private fun Preview(o: KObject) {
    val platform = LocalPlatform.current
    val thumb = rememberThumb(o, 1400)
    when {
        thumb != null -> Image(thumb, o.title, Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.FillWidth)
        o.vaultFile.isBlank() -> Text(if (o.origin == Origin.EXAMPLE) "Example document — there is no file behind it." else "No original file is stored for this item.",
            style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        else -> Card(Modifier.fillMaxWidth(), color = Atlas.PowderBlue) {
            Text(o.mime.substringAfter('/').uppercase() + " · " + size(o.sizeBytes), style = MaterialTheme.typography.titleSmall)
            Text("Stored encrypted on this phone. Opening makes a temporary decrypted copy for the viewer app; it is deleted on next launch.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
    }
    if (o.vaultFile.isNotBlank()) Button({ platform.openFile(o) }, Modifier.padding(top = 12.dp)) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Open original") }
}

@Composable
private fun Actions(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val cap = snap.captures.firstOrNull { it.attachmentId == o.id || it.noteId == o.id }
    val pending = cap?.let { c -> snap.pending().filter { it.captureId == c.id } }.orEmpty()
    if (pending.isNotEmpty()) {
        SectionLabel("Suggested from this item")
        pending.forEach { s -> SuggestionCard(vm, snap, s) }
    }
    SectionLabel("Do something with it")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton({ vm.show(Overlay.Create(ObjType.TASK, o.id, if (o.type == ObjType.IDEA) "Try: ${o.title}" else "")) }) { Text("New task") }
        OutlinedButton({ vm.show(Overlay.Connect(o.id, setOf(ObjType.PROJECT, ObjType.COLLECTION))) }) { Text("Add to project") }
    }
}

@Composable
private fun TimelineTab(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val today = LocalDate.now()
    val nb = snap.neighbours(o.id, false).map { it.first }
    data class E(val date: LocalDate, val title: String, val kind: String, val id: String)
    val planned = buildList {
        nb.filter { it.type == ObjType.EVENT && it.eventDate != null }.forEach { add(E(it.eventDate!!, it.title, "Milestone", it.id)) }
        nb.filter { it.type == ObjType.TASK && !it.done && it.due != null }.forEach { add(E(it.due!!, it.title, "Task due", it.id)) }
        o.targetDate?.let { add(E(it, "Target date", "Project", o.id)) }; o.startDate?.let { add(E(it, "Started", "Project", o.id)) }
    }.sortedBy { it.date }
    val recorded = nb.filter { it.type == ObjType.TASK && it.done && it.completedAt != null }.map {
        E(java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), it.title, "Completed", it.id) }
    Row { FilledTonalButton({ vm.show(Overlay.Create(ObjType.EVENT, o.id)) }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add milestone") } }
    SectionLabel("Planned and past dates")
    if (planned.isEmpty()) Text("No dates yet.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
    planned.forEach { e ->
        val past = e.date < today
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { vm.focusOn(e.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (past) Atlas.DeepTeal else Atlas.PrimaryContainer).border(2.dp, if (past) Atlas.DeepTeal else Atlas.Primary, androidx.compose.foundation.shape.CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(e.title, style = MaterialTheme.typography.bodyLarge); Text("${e.kind} · ${relDate(e.date)}${if (past) " · past" else ""}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
        }
    }
    if (recorded.isNotEmpty()) {
        SectionLabel("Recorded")
        recorded.sortedByDescending { it.date }.forEach { e -> Text("✓ ${e.title} · ${relDate(e.date)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp)) }
    }
}
