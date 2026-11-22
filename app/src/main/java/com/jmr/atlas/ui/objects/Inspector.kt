package com.jmr.atlas.ui.objects

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Repository
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.accent
import com.jmr.atlas.ui.theme.icon

/** Side pane on wide screens (Fold inner, tablet). Context / Sources / Activity, modelled on the target design. */
@Composable
fun InspectorPane(vm: AtlasViewModel, snap: Snapshot, id: String?, modifier: Modifier = Modifier) {
    val o = id?.let { snap.objects[it] }?.takeIf { !it.trashed }
    Surface(modifier.fillMaxHeight().testTag("inspector"), shape = RoundedCornerShape(28.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border)) {
        if (o == null) { UniverseSummary(vm, snap); return@Surface }
        var tab by rememberSaveable(o.id) { mutableIntStateOf(0) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
            Cover(o, snap, vm, 150.dp)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 6.dp)) {
                Text(o.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                HeaderChips(o, snap)
                if (o.body.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(o.body, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, maxLines = 5, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(14.dp))
                Segmented(listOf("Context", "Sources", "Activity"), tab) { tab = it }
                Spacer(Modifier.height(12.dp))
                when (tab) {
                    0 -> ContextBlock(vm, snap, o, showNextSteps = true)
                    1 -> SourcesBlock(vm, snap, o)
                    else -> ActivityBlock(snap, o)
                }
            }
        }
    }
}

/** Persistent bottom sheet on phones: the focused object's page in brief. */
@Composable
fun InspectorSheet(vm: AtlasViewModel, snap: Snapshot, id: String?) {
    val o = id?.let { snap.objects[it] }?.takeIf { !it.trashed }
    if (o == null) { Box(Modifier.heightIn(max = 560.dp)) { UniverseSummary(vm, snap) }; return }
    val tabs = sheetTabs(o.type)
    var tab by rememberSaveable(o.id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).testTag("inspector")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ObjAvatar(o, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(o.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(typeLine(o, snap), style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ vm.open(o.id) }, Modifier.semantics { contentDescription = "Open full page" }) { Icon(Icons.Outlined.OpenInFull, null) }
            ObjectMenu(vm, o)
        }
        Spacer(Modifier.height(12.dp))
        ScrollTabs(tabs.map { it.label }, tab) { tab = it }
        Spacer(Modifier.height(8.dp))
        TabBody(vm, snap, o, tabs[tab], compact = true)
    }
}

@Composable
fun Cover(o: KObject, snap: Snapshot, vm: AtlasViewModel, height: androidx.compose.ui.unit.Dp) {
    val img = remember(snap, o.id) { if (o.type == ObjType.IMAGE) o else snap.neighbours(o.id, false).map { it.first }.firstOrNull { it.type == ObjType.IMAGE && it.vaultFile.isNotBlank() } }
    val thumb = rememberThumb(img, 900)
    val a = accent(o.type)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(20.dp))
        .background(if (o.type == ObjType.PROJECT) Brush.linearGradient(listOf(Atlas.Primary, Atlas.DeepPlum)) else Brush.linearGradient(listOf(a.container, Atlas.SurfaceSecondary)))) {
        if (thumb == null) Canvas(Modifier.fillMaxSize()) {
            // a quiet constellation: the object's real connection count drawn as nodes around it
            val n = snap.relationsOf(o.id).size.coerceIn(3, 18)
            val c = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val rnd = java.util.Random(o.id.hashCode().toLong())
            val line = if (o.type == ObjType.PROJECT) Color.White.copy(alpha = .22f) else a.content.copy(alpha = .18f)
            val dot = if (o.type == ObjType.PROJECT) Atlas.Orchid else a.content.copy(alpha = .45f)
            repeat(n) { i ->
                val ang = (i * 2 * Math.PI / n) + rnd.nextDouble() * .4
                val r = size.minDimension * (0.32f + rnd.nextFloat() * 0.5f)
                val p = c + androidx.compose.ui.geometry.Offset((kotlin.math.cos(ang) * r * 1.6f).toFloat(), (kotlin.math.sin(ang) * r).toFloat())
                drawLine(line, c, p, 1.5.dp.toPx()); drawCircle(dot, (3 + rnd.nextInt(4)).dp.toPx(), p)
            }
        }
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(icon(o.type), null, tint = if (o.type == ObjType.PROJECT) Color.White.copy(alpha = .85f) else a.content, modifier = Modifier.align(Alignment.Center).size(56.dp))
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            FilledTonalIconButton({ vm.open(o.id) }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Atlas.Surface.copy(alpha = .9f)),
                modifier = Modifier.semantics { contentDescription = "Open full page" }) { Icon(Icons.Outlined.OpenInFull, null) }
            Spacer(Modifier.width(6.dp))
            Surface(shape = CircleShape, color = Atlas.Surface.copy(alpha = .9f)) { ObjectMenu(vm, o) }
        }
    }
}

@Composable
fun HeaderChips(o: KObject, snap: Snapshot) {
    val n = snap.relationsOf(o.id).count { it.review == Review.CONFIRMED }
    val p = snap.relationsOf(o.id).count { it.review == Review.PROPOSED }
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val status = when (o.type) { ObjType.PROJECT -> o.projectStatus?.label; ObjType.TASK -> if (o.done) "Done" else if (o.blocked) "Blocked" else "Open"; else -> null }
        Pill(listOfNotNull(o.type.label, status).joinToString(" · "), icon(o.type), Atlas.PrimaryContainer, Atlas.DeepPlum)
        Pill("$n ${if (n == 1) "connection" else "connections"}" + if (p > 0) " · $p proposed" else "", Icons.Outlined.Hub, Atlas.SurfaceSecondary, Atlas.DeepPlum)
        if (o.origin != Origin.USER) Pill(o.origin.label, null, Atlas.SurfaceSecondary, Atlas.SecondaryText)
    }
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

@Composable
fun ScrollTabs(labels: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        IconButton({ open = true }, Modifier.semantics { contentDescription = "More actions for ${o.title}" }) { Icon(Icons.Outlined.MoreHoriz, null) }
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
@Composable
fun ContextBlock(vm: AtlasViewModel, snap: Snapshot, o: KObject, showNextSteps: Boolean) {
    val lines = remember(snap, o.id) { vm.repo.context(o.id) }
    Surface(shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Atlas.Primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("Atlas Context", style = MaterialTheme.typography.titleSmall)
            }
            Text("Why this matters now", style = MaterialTheme.typography.labelLarge, color = Atlas.DeepPlum, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            if (lines.isEmpty()) Text("Nothing pressing. Atlas only speaks up when something connected gives a reason — a deadline, a new file, a pending review.",
                style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            lines.forEach { l ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Show source") { l.sources.firstOrNull()?.let { vm.focusOn(it) } }
                    .padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(contextIcon(l.text), null, tint = Atlas.DeepTeal, modifier = Modifier.size(18.dp).padding(top = 2.dp)); Spacer(Modifier.width(10.dp))
                    Text(l.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
            if (lines.isNotEmpty()) Text("Rule-based, from the items shown. Tap a line to see its source.", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                color = Atlas.SecondaryText, modifier = Modifier.padding(top = 6.dp))
        }
    }
    if (showNextSteps) NextSteps(vm, snap, o)
}

private fun contextIcon(t: String) = when {
    t.contains("overdue") -> Icons.Outlined.ErrorOutline; t.startsWith("Next deadline") || t.startsWith("Target") -> Icons.Outlined.Event
    t.startsWith("Waiting") -> Icons.Outlined.HourglassEmpty; t.startsWith("Upcoming") -> Icons.Outlined.Flag
    t.startsWith("New this week") -> Icons.Outlined.FiberNew; t.contains("proposed") -> Icons.Outlined.Rule; else -> Icons.Outlined.Info
}

@Composable
fun NextSteps(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val tasks = snap.neighbours(o.id, false).map { it.first }.filter { it.type == ObjType.TASK && !it.done }.sortedBy { it.due ?: java.time.LocalDate.MAX }
    SectionLabel("Next steps") { TextButton({ vm.show(Overlay.Create(ObjType.TASK, o.id)) }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("Add") } }
    if (tasks.isEmpty()) Text("No open tasks connected.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
    tasks.take(6).forEach { t -> ObjRow(t, snap, onToggle = { vm.repo.toggleDone(t.id) }) { vm.focusOn(t.id) } }
}

@Composable
fun SourcesBlock(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    Text("${o.origin.label} · ${ago(o.created)}", style = MaterialTheme.typography.bodyMedium)
    if (o.vaultFile.isNotBlank()) OutlinedButton({ platform.openFile(o) }, Modifier.padding(top = 8.dp)) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Open original") }
    val derived = snap.relationsOf(o.id).filter { it.type == RelType.DERIVED_FROM || it.evidence.isNotBlank() }
    if (derived.isNotEmpty()) SectionLabel("Evidence")
    derived.forEach { r ->
        val other = snap.objects[r.other(o.id)] ?: return@forEach
        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { vm.focusOn(other.id) }) {
            Text("${r.label(o.id)} ${other.title}", style = MaterialTheme.typography.labelLarge)
            if (r.evidence.isNotBlank()) Text(r.evidence, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
            Text(r.origin.label + if (r.review == Review.PROPOSED) " · proposed" else "", style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
        }
    }
    snap.captures.firstOrNull { it.noteId == o.id || it.attachmentId == o.id }?.let { c ->
        SectionLabel("Original capture")
        Text(if (c.text.isBlank()) "(file only)" else c.text, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Text(c.status.label + " · " + ago(c.created), style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
    }
    if (derived.isEmpty() && o.vaultFile.isBlank() && snap.captures.none { it.noteId == o.id || it.attachmentId == o.id })
        Text("No external source — this was created directly.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
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
        Text("${snap.live.size} items · ${snap.relations.count { it.review == Review.CONFIRMED }} connections", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Spacer(Modifier.height(16.dp))
        if (pending > 0) Card(Modifier.fillMaxWidth(), color = Atlas.PrimaryContainer, onClick = { vm.go(Route.Review) }) {
            Text("$pending suggestions to review", style = MaterialTheme.typography.titleSmall)
            Text("Atlas proposed people, links and tasks from your captures. Nothing is connected until you accept.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
        SectionLabel("Pick something to focus on")
        val hubs = (snap.ofType(ObjType.PROJECT).filter { it.projectStatus != ProjectStatus.ARCHIVED } + snap.ofType(ObjType.PERSON)).sortedByDescending { snap.relationsOf(it.id).size }.take(6)
        if (hubs.isEmpty()) Text("Capture a thought, or create a project or person — the map grows from there.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        hubs.forEach { h -> ObjRow(h, snap, sub = "${typeLine(h)} · ${snap.relationsOf(h.id).size} connections") { vm.focusOn(h.id) } }
        val ins = remember(snap) { vm.repo.insights() }
        if (ins.isNotEmpty()) {
            SectionLabel("Insights") { TextButton({ vm.setMode(MapMode.INSIGHTS) }) { Text("All") } }
            ins.take(3).forEach { i -> Text("• ${i.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
        }
    }
}
