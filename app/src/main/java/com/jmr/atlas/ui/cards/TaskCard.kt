package com.jmr.atlas.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.components.Pill
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.components.relDate
import com.jmr.atlas.ui.components.shortDate
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    val today = LocalDate.now()
    Column(cardModifier("task_card")) {
        CardTopBar(vm, o)
        if (!LocalEmbedded.current) Row(verticalAlignment = Alignment.Top) {
            Checkbox(o.done, { vm.repo.toggleDone(o.id) }, Modifier.size(48.dp).semantics { contentDescription = if (o.done) "Mark not done" else "Complete" }.testTag("task_check"),
                colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal, uncheckedColor = Atlas.Primary))
            Spacer(Modifier.width(8.dp))
            Text(o.title, style = MaterialTheme.typography.headlineSmall, textDecoration = if (o.done) TextDecoration.LineThrough else null, color = if (o.done) Atlas.SecondaryText else Atlas.DeepPlum, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("task_chips")) {
            val dates = { vm.show(Overlay.TaskDates(o.id)) }
            Pill("Plan: " + (o.due?.let { relDate(it, today) } ?: "Someday"), Icons.Outlined.WbSunny, Atlas.SurfaceSecondary, Atlas.DeepPlum, onClick = dates)
            o.deadline?.let { Pill("Deadline: ${dayDate(it)}", Icons.Outlined.Flag, Atlas.Peach, Atlas.CoralText, onClick = dates) } ?: Pill("Deadline", Icons.Outlined.Flag, Atlas.SurfaceSecondary, Atlas.SecondaryText, onClick = dates)
            Pill(o.remindAt?.let { "Remind " + remindLabel(it, today) } ?: "Remind", Icons.Outlined.NotificationsNone, Atlas.SurfaceSecondary, if (o.remindAt != null) Atlas.DeepPlum else Atlas.SecondaryText, onClick = dates)
            if (o.priority != Priority.NONE) Pill(o.priority.label, Icons.Outlined.ErrorOutline, if (o.priority == Priority.HIGH) Atlas.PrimaryContainer else Atlas.SurfaceSecondary, Atlas.DeepPlum, onClick = dates)
            Pill(if (o.repeat == Repeat.NONE) "Repeat" else o.repeat.label, Icons.Outlined.Repeat, Atlas.SurfaceSecondary, if (o.repeat == Repeat.NONE) Atlas.SecondaryText else Atlas.DeepPlum, onClick = dates)
            if (o.blocked) Pill("Blocked", Icons.Outlined.Block, Atlas.Peach, Atlas.CoralText, onClick = dates)
        }

        // ---- where it belongs
        val rels = snap.neighbours(o.id, false)
        val project = rels.firstOrNull { it.first.type == ObjType.PROJECT }?.first
        val people = rels.filter { it.first.type == ObjType.PERSON }.map { it.first }
        val blocks = rels.filter { it.second.type == RelType.DEPENDS_ON }
        Spacer(Modifier.height(14.dp))
        if (!LocalEmbedded.current) Box16 {
            var any = false
            project?.let { any = true; FieldRow(Icons.Outlined.FolderOpen, it.title, "Project", onClick = { vm.open(it.id) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.SecondaryText) } }
            people.forEach { p -> if (any) Divider16(); any = true; FieldRow(Icons.Outlined.Person, p.title, "With", onClick = { vm.open(p.id) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.SecondaryText) } }
            blocks.forEach { (b, r) -> if (any) Divider16(); any = true; FieldRow(Icons.Outlined.Layers, b.title, r.label(o.id), onClick = { vm.open(b.id) }) }
            if (any) Divider16()
            FieldRow(Icons.Outlined.AddLink, "Connect to a project, person or task", "", valueColor = Atlas.Primary, onClick = { vm.show(Overlay.Connect(o.id, setOf(ObjType.PROJECT, ObjType.PERSON, ObjType.TASK))) })
        }

        // ---- steps
        val steps = snap.detailsOf(o.id, DetailKind.STEP)
        Section("Steps", trailing = if (steps.isNotEmpty()) "${steps.count { it.flag }} of ${steps.size}" else null)
        Box16 {
            steps.forEach { s ->
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(s.flag, { vm.repo.toggleStep(s.id) }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal))
                    Column(Modifier.weight(1f)) {
                        Text(s.value, style = MaterialTheme.typography.titleSmall, textDecoration = if (s.flag) TextDecoration.LineThrough else null, color = if (s.flag) Atlas.SecondaryText else Atlas.DeepPlum)
                        if (s.flag && s.extra.isNotBlank()) Text("Done ${runCatching { shortDate(LocalDate.parse(s.extra)) }.getOrDefault("")}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                        else if (s.label.isNotBlank()) Text(s.label, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                    }
                    IconButton({ vm.repo.deleteDetail(s.id) }, Modifier.semantics { contentDescription = "Remove step" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.SecondaryText, modifier = Modifier.size(18.dp)) }
                }
            }
            FieldRow(Icons.Outlined.Add, "Add step", "", valueColor = Atlas.Primary, onClick = { vm.show(Overlay.AddLine(o.id, DetailKind.STEP)) })
        }

        Section("Notes")
        Surface(onClick = { vm.show(Overlay.Edit(o.id)) }, shape = RoundedCornerShape(20.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth()) {
            Text(o.body.ifBlank { "Add notes" }, style = MaterialTheme.typography.bodyLarge, color = if (o.body.isBlank()) Atlas.SecondaryText else Atlas.DeepPlum, modifier = Modifier.padding(16.dp))
        }

        // ---- source capture
        val src = rels.firstOrNull { it.second.type == RelType.DERIVED_FROM }?.first
        if (src != null) {
            Section("Where it came from")
            val date = shortDate(java.time.Instant.ofEpochMilli(src.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            Quote("“${src.body.ifBlank { src.title }.take(220)}”", "${src.kind.ifBlank { src.type.label }} · $date") { vm.open(src.id) }
        }
        Spacer(Modifier.height(14.dp))
        if (o.calendarEventId != null) OutlinedButton({ platform.launch(Calendars.viewIntent(o.calendarEventId)) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Event, null); Spacer(Modifier.width(8.dp)); Text("Open in calendar")
        } else OutlinedButton({ vm.show(Overlay.Block(o.id)) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("block_time")) {
            Icon(Icons.Outlined.Event, null); Spacer(Modifier.width(8.dp)); Text("Block time in calendar")
        }
        CollapsedFooter(vm, snap, o)
    }
}

fun remindLabel(ms: Long, today: LocalDate): String {
    val dt = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    val d = dt.toLocalDate()
    return (if (d == today) "" else relDate(d, today) + " ") + Briefing.time(ms)
}
