package com.jmr.atlas.ui.objects

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Repository
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas

/** Side pane on wide screens: header, then Context / Sources / Activity as in the target design. */
@Composable
fun InspectorPane(vm: AtlasViewModel, snap: Snapshot, id: String?, modifier: Modifier = Modifier) {
    val o = id?.let { snap.objects[it] }?.takeIf { !it.trashed }
    Surface(modifier.fillMaxHeight().testTag("inspector"), shape = RoundedCornerShape(28.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border)) {
        if (o == null) { UniverseSummary(vm, snap); return@Surface }
        var tab by rememberSaveable(o.id) { mutableIntStateOf(0) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            CoverImage(o, snap)
            Header(vm, snap, o, large = true)
            if (o.body.isNotBlank()) Text(o.body, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(14.dp))
            Segmented(listOf("Context", "Sources", "Activity"), tab) { tab = it }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> { WhyNow(vm, snap, o, limit = 5); NextSteps(vm, snap, o) }
                1 -> SourcesBlock(vm, snap, o)
                else -> ActivityBlock(snap, o)
            }
        }
    }
}

/** Phone: the focused object in brief. The peek shows the header and the two most pressing reasons. */
@Composable
fun InspectorSheet(vm: AtlasViewModel, snap: Snapshot, id: String?) {
    val o = id?.let { snap.objects[it] }?.takeIf { !it.trashed }
    if (o == null) { Box(Modifier.heightIn(max = 560.dp)) { UniverseSummary(vm, snap) }; return }
    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).testTag("inspector")) {
        Header(vm, snap, o, large = false)
        Spacer(Modifier.height(8.dp))
        WhyNow(vm, snap, o, limit = 6, compact = true)
        if (o.body.isNotBlank()) { SectionLabel("About"); Text(o.body, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText) }
        NextSteps(vm, snap, o)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton({ vm.open(o.id) }, Modifier.fillMaxWidth()) { Text("Open full page") }
    }
}

/** Title, one subtitle line (type · key fact · connections) and the actions. No repeated chips. */
@Composable
fun Header(vm: AtlasViewModel, snap: Snapshot, o: KObject, large: Boolean) {
    val n = snap.relationsOf(o.id).count { it.review == Review.CONFIRMED }
    val proposed = snap.relationsOf(o.id).count { it.review == Review.PROPOSED }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!large) { ObjAvatar(o, 48.dp); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(o.title, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, maxLines = if (large) 3 else 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            Text(typeLine(o, snap) + " · " + plural(n, "connection"), style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!large) IconButton({ vm.open(o.id) }, Modifier.semantics { contentDescription = "Open full page" }) { Icon(Icons.Outlined.OpenInFull, null) }
        ObjectMenu(vm, o)
    }
    if (proposed > 0 || o.origin == Origin.EXTRACTED || o.origin == Origin.AI) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (proposed > 0) Pill("${plural(proposed, "connection")} to review", Icons.Outlined.Rule, Atlas.PrimaryContainer, Atlas.DeepPlum) { vm.open(o.id) }
        if (o.origin == Origin.EXTRACTED || o.origin == Origin.AI) Pill(o.origin.label, null, Atlas.SurfaceSecondary, Atlas.SecondaryText)
    }
}

/** Only shown when there is a real picture — no decorative banners. */
@Composable
private fun CoverImage(o: KObject, snap: Snapshot) {
    val img = remember(snap, o.id) { if (o.type == ObjType.IMAGE) o else snap.neighbours(o.id, false).map { it.first }.firstOrNull { it.type == ObjType.IMAGE && it.vaultFile.isNotBlank() } }
    val thumb = rememberThumb(img, 900) ?: return
    Image(thumb, null, Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
    Spacer(Modifier.height(12.dp))
}

@Composable
fun Segmented(labels: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(Atlas.SurfaceSecondary).padding(4.dp)) {
        labels.forEachIndexed { i, l ->
            val on = i == sel
            Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(CircleShape).background(if (on) Atlas.PrimaryContainer else Color.Transparent)
                .selectable(on, role = androidx.compose.ui.semantics.Role.Tab) { onSel(i) }.testTag("tab_$l"), contentAlignment = Alignment.Center) {
                Text(l, style = MaterialTheme.typography.labelLarge, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
            }
        }
    }
}

/** Tabs wrap onto a second line instead of hiding off-screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapTabs(labels: List<String>, sel: Int, onSel: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, l ->
            FilterChip(i == sel, { onSel(i) }, { Text(l) }, shape = CircleShape, modifier = Modifier.testTag("tab_$l"),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Atlas.PrimaryContainer, selectedLabelColor = Atlas.DeepPlum))
        }
    }
}

@Composable
fun ObjectMenu(vm: AtlasViewModel, o: KObject) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, Modifier.semantics { contentDescription = "More actions for ${o.title}" }) { Icon(Icons.Outlined.MoreVert, null) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Open page") }, { open = false; vm.open(o.id) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) })
            DropdownMenuItem({ Text("Show on map") }, { open = false; vm.focusOn(o.id) }, leadingIcon = { Icon(Icons.Outlined.Hub, null) })
            DropdownMenuItem({ Text("Edit") }, { open = false; vm.show(Overlay.Edit(o.id)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
            DropdownMenuItem({ Text("Connect…") }, { open = false; vm.show(Overlay.Connect(o.id)) }, leadingIcon = { Icon(Icons.Outlined.AddLink, null) })
            DropdownMenuItem({ Text(if (o.pinned) "Unpin" else "Pin") }, { open = false; vm.repo.update(o.copy(pinned = !o.pinned), if (o.pinned) "Unpinned" else "Pinned") },
                leadingIcon = { Icon(Icons.Outlined.PushPin, null) })
            DropdownMenuItem({ Text("Move to trash") }, { open = false; vm.repo.trash(o.id); vm.say("“${o.title}” moved to trash."); if (vm.focus.value == o.id) vm.focusBack() },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Atlas.Error) })
        }
    }
}

// ================================================================ blocks
/** “Why this matters now”: rule-based reasons. A reason about a task is the task itself, checkable. */
@Composable
fun WhyNow(vm: AtlasViewModel, snap: Snapshot, o: KObject, limit: Int, compact: Boolean = false) {
    val lines = remember(snap, o.id) { vm.repo.context(o.id) }.take(limit)
    Surface(shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary, modifier = Modifier.testTag("why_now")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Atlas.Primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Why this matters now", style = MaterialTheme.typography.titleSmall)
            }
            if (lines.isEmpty()) Text("Nothing pressing right now.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(vertical = 6.dp))
            lines.forEach { l ->
                val task = l.sources.singleOrNull()?.let { snap.objects[it] }?.takeIf { it.type == ObjType.TASK && it.id != o.id }
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Show source") { l.sources.firstOrNull()?.let { vm.focusOn(it) } },
                    verticalAlignment = Alignment.CenterVertically) {
                    if (task != null) Checkbox(task.done, { vm.repo.toggleDone(task.id) }, Modifier.size(36.dp).semantics { contentDescription = "Complete ${task.title}" },
                        colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal))
                    else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Icon(contextIcon(l.text), null, tint = Atlas.DeepTeal, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(6.dp))
                    val parts = l.text.split(" — ", limit = 2)
                    Text(androidx.compose.ui.text.buildAnnotatedString {
                        append(parts[0])
                        if (parts.size > 1) {
                            append(" — ")
                            withStyle(androidx.compose.ui.text.SpanStyle(color = if (parts[1].startsWith("was due")) Atlas.Error else Atlas.SecondaryText)) { append(parts[1]) }
                        }
                    }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = Atlas.DeepPlum)
                }
            }
        }
    }
}

private fun contextIcon(t: String) = when {
    t.contains("was due") -> Icons.Outlined.ErrorOutline; t.contains("— due") || t.startsWith("Target") -> Icons.Outlined.Event
    t.startsWith("Waiting") -> Icons.Outlined.HourglassEmpty; t.startsWith("Next milestone") -> Icons.Outlined.Flag
    t.startsWith("New this week") -> Icons.Outlined.FiberNew; t.contains("proposed") -> Icons.Outlined.Rule; else -> Icons.Outlined.Info
}

/** Open tasks not already shown above. */
@Composable
fun NextSteps(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    if (o.type == ObjType.TASK) return
    val shown = remember(snap, o.id) { vm.repo.context(o.id).flatMap { it.sources }.toSet() }
    val tasks = snap.neighbours(o.id, false).map { it.first }.filter { it.type == ObjType.TASK && !it.done && it.id !in shown }.sortedBy { it.due ?: java.time.LocalDate.MAX }
    SectionLabel(if (shown.any { snap.objects[it]?.type == ObjType.TASK }) "Other next steps" else "Next steps") {
        TextButton({ vm.show(Overlay.Create(ObjType.TASK, o.id)) }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add task") }
    }
    if (tasks.isEmpty()) Text("No other open tasks.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
    tasks.take(6).forEach { t -> ObjRow(t, snap, sub = t.due?.let { relDate(it) } ?: "No date", onToggle = { vm.repo.toggleDone(t.id) }) { vm.focusOn(t.id) } }
}

/** Where this knowledge comes from: the original file, the capture, quoted evidence, and the material connected to it. */
@Composable
fun SourcesBlock(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    if (o.vaultFile.isNotBlank()) OutlinedButton({ platform.openFile(o) }, Modifier.padding(bottom = 8.dp)) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Open original") }
    snap.captures.firstOrNull { it.noteId == o.id || it.attachmentId == o.id }?.let { c ->
        SectionLabel("Captured ${ago(c.created)}")
        Text(if (c.text.isBlank()) "File only" else c.text, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
    }
    val evidence = snap.relationsOf(o.id).filter { it.evidence.isNotBlank() }
    if (evidence.isNotEmpty()) SectionLabel("Why it's connected")
    evidence.forEach { r ->
        val other = snap.objects[r.other(o.id)] ?: return@forEach
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { vm.focusOn(other.id) }.padding(vertical = 6.dp)) {
            Text("${r.label(o.id)} ${other.title}", style = MaterialTheme.typography.labelLarge)
            Text(r.evidence, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
    }
    val material = snap.neighbours(o.id, false).map { it.first }.filter { it.type in setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.NOTE, ObjType.LINK) }
    if (material.isNotEmpty()) SectionLabel("Documents and notes · ${material.size}")
    material.forEach { m -> ObjRow(m, snap) { vm.focusOn(m.id) } }
    if (o.vaultFile.isBlank() && evidence.isEmpty() && material.isEmpty() && snap.captures.none { it.noteId == o.id || it.attachmentId == o.id })
        Text("Created directly — no documents or captures behind it yet.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
}

@Composable
fun ActivityBlock(snap: Snapshot, o: KObject) {
    val acts = snap.activityOf(o.id)
    if (acts.isEmpty()) Text("No activity yet.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
    acts.take(40).forEach { a ->
        Row(Modifier.padding(vertical = 6.dp)) {
            Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(Atlas.Orchid)); Spacer(Modifier.width(12.dp))
            Column { Text(a.text, style = MaterialTheme.typography.bodyMedium); Text(ago(a.at), style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText) }
        }
    }
}

@Composable
fun UniverseSummary(vm: AtlasViewModel, snap: Snapshot) {
    val pending = snap.pending().size
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("My Universe", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("${plural(snap.live.size, "item")} · ${plural(snap.relations.count { it.review == Review.CONFIRMED }, "connection")}", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Spacer(Modifier.height(12.dp))
        if (pending > 0) Card(Modifier.fillMaxWidth(), color = Atlas.PrimaryContainer, onClick = { vm.go(Route.Review) }) {
            Text("${plural(pending, "suggestion")} to review", style = MaterialTheme.typography.titleSmall)
        }
        SectionLabel("Focus on")
        val hubs = (snap.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED } + snap.ofType(ObjType.PERSON)).sortedByDescending { snap.relationsOf(it.id).size }.take(6)
        if (hubs.isEmpty()) Text("Capture a thought or add a project — the map grows from there.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        hubs.forEach { h -> ObjRow(h, snap, sub = "${typeLine(h)} · ${plural(snap.relationsOf(h.id).size, "connection")}") { vm.focusOn(h.id) } }
    }
}
