package com.jmr.atlas.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.components.Pill
import com.jmr.atlas.ui.components.ago
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.components.relDate
import com.jmr.atlas.ui.components.shortDate
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun healthColors(h: Health?) = when (h) {
    Health.ON_TRACK -> Atlas.CoolMint to Atlas.DeepTeal
    Health.AT_RISK -> Atlas.Peach to Atlas.CoralText
    Health.OFF_TRACK -> androidx.compose.ui.graphics.Color(0xFFFCE4E7) to Atlas.Error
    null -> Atlas.SurfaceSecondary to Atlas.SecondaryText
}

@Composable
fun CoverBand(seed: Int, a: androidx.compose.ui.graphics.Color = Atlas.Peach, b: androidx.compose.ui.graphics.Color = Atlas.CoolMint, c: androidx.compose.ui.graphics.Color = Atlas.PrimaryContainer) {
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        drawRect(a)
        val r = size.width
        drawCircle(c, r * 0.32f, Offset(r * (0.82f + (seed % 7) * 0.01f), size.height * 0.6f))
        drawCircle(b, r * 0.22f, Offset(r * 0.08f, size.height * 1.05f))
        drawRoundRect(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f), Offset(r * 0.38f, size.height * 0.22f), Size(r * 0.3f, size.height * 0.5f), CornerRadius(24f, 24f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val today = LocalDate.now()
    val nb = snap.neighbours(o.id, false).map { it.first }
    val tasks = nb.filter { it.type == ObjType.TASK }
    val done = tasks.count { it.done }
    val milestones = nb.filter { it.type == ObjType.EVENT && it.eventDate != null }
    val left = milestones.count { !it.eventDate!!.isBefore(today) }
    val updates = snap.detailsOf(o.id, DetailKind.UPDATE).sortedByDescending { it.created }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp).testTag("project_card")) {
        Box { CoverBand(o.id.hashCode()); CardTopBar(vm, o, overImage = true) }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(o.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                o.projectStatus?.let { Pill(it.label) }
                o.health?.let { h -> val (bg, fg) = healthColors(h); Pill(h.label, Icons.Outlined.Check, bg, fg) }
                o.targetDate?.let { t -> val wk = ChronoUnit.DAYS.between(today, t); Pill("Target ${shortDate(t)}" + if (wk > 0) " · ${Briefing.span(wk)}" else "", Icons.Outlined.Flag, Atlas.SurfaceSecondary, Atlas.DeepPlum) }
            }
            if (tasks.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row { Text("$done of ${tasks.size} tasks done", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f)); if (left > 0) Text("${left} milestone${if (left == 1) "" else "s"} left", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText) }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator({ done.toFloat() / tasks.size }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Atlas.DeepTeal, trackColor = Atlas.SurfaceSecondary, drawStopIndicator = {})
            }

            // ---- latest update (Linear-style)
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth().testTag("project_update")) {
                Column(Modifier.padding(16.dp)) {
                    val u = updates.firstOrNull()
                    if (u != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val h = Health.entries.firstOrNull { it.name == u.extra }; val (bg, fg) = healthColors(h)
                            Pill(h?.label ?: "Update", Icons.Outlined.Check, bg, fg); Spacer(Modifier.width(10.dp))
                            Text("Your update · ${ago(u.created)}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                        }
                        Spacer(Modifier.height(10.dp)); Text(u.value, style = MaterialTheme.typography.bodyLarge)
                    } else Text("No updates yet. A short status note each week keeps the project honest.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (updates.size > 1) TextButton({ vm.go(com.jmr.atlas.ui.Route.History(o.id)) }) { Text("Earlier updates (${updates.size - 1})") }
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton({ vm.show(Overlay.PostUpdate(o.id)) }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Atlas.PrimaryContainer, contentColor = Atlas.DeepPlum), modifier = Modifier.testTag("post_update")) { Text("Post update") }
                    }
                }
            }
            WhyNowBox(vm, snap, o, vm.sync.eventsFor(o, snap).firstOrNull()?.let { listOf(Icons.Outlined.Event to "${it.title} — ${eventWhen(it, today)}") }.orEmpty(),
                skip = { it.startsWith("Target date") || it.startsWith("Next milestone") })

            milestones.filter { !it.eventDate!!.isBefore(today) }.minByOrNull { it.eventDate!! }?.let { m ->
                Spacer(Modifier.height(12.dp))
                Surface(onClick = { vm.open(m.id) }, shape = RoundedCornerShape(22.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.CoolMint), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Flag, null, tint = Atlas.DeepTeal) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Next milestone", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                            Text(m.title, style = MaterialTheme.typography.titleSmall)
                            Text("${dayDate(m.eventDate!!)} · in ${Briefing.span(ChronoUnit.DAYS.between(today, m.eventDate).coerceAtLeast(0))}", style = MaterialTheme.typography.bodySmall, color = Atlas.DeepTeal)
                        }
                    }
                }
            }

            // ---- next up
            val open = tasks.filter { !it.done }.sortedWith(compareBy({ it.due ?: it.deadline ?: LocalDate.MAX }, { -it.priority.ordinal }))
            Section("Next up", action = "Add task") { vm.show(Overlay.Create(ObjType.TASK, o.id)) }
            if (open.isEmpty()) Text("No open tasks. What's the next step?", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
            open.take(5).forEach { t -> TaskLine(vm, snap, t) }
            if (open.size > 5) TextButton({ vm.go(com.jmr.atlas.ui.Route.Connections(o.id)) }) { Text("All ${open.size} open tasks") }

            // ---- people
            val people = nb.filter { it.type == ObjType.PERSON }
            Section("People and roles", action = "Add") { vm.show(Overlay.Connect(o.id, setOf(ObjType.PERSON))) }
            if (people.isEmpty()) Text("Who's involved? Connect people to see them here.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                people.forEach { p ->
                    Column(Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable { vm.open(p.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PersonPhoto(p, 56.dp); Spacer(Modifier.height(6.dp))
                        Text(p.firstName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Text(p.role.ifBlank { " " }, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ---- documents
            val docs = nb.filter { it.type == ObjType.DOCUMENT || it.type == ObjType.IMAGE || it.type == ObjType.LINK }.sortedWith(compareBy({ !it.pinned }, { -it.created }))
            if (docs.isNotEmpty()) {
                Section(if (docs.any { it.pinned }) "Pinned documents" else "Documents", trailing = if (docs.size > 3) "All ${docs.size}" else null)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    docs.take(6).forEach { d ->
                        Surface(onClick = { vm.open(d.id) }, shape = RoundedCornerShape(20.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.width(140.dp).height(132.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Atlas.PowderBlue), contentAlignment = Alignment.Center) { Icon(com.jmr.atlas.ui.theme.icon(d.type), null, tint = Atlas.BlueText, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.height(10.dp))
                                Text(d.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(com.jmr.atlas.ui.components.typeLine(d).substringAfter(" · ", d.type.label), style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ---- decisions
            val decisions = nb.filter { it.type == ObjType.DECISION }.sortedByDescending { it.created }
            Section("Decisions")
            Box16 {
                decisions.take(4).forEachIndexed { i, d -> if (i > 0) Divider16()
                    FieldRow(Icons.Outlined.Check, d.title, shortDate(java.time.Instant.ofEpochMilli(d.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), onClick = { vm.open(d.id) }) }
                if (decisions.isNotEmpty()) Divider16()
                FieldRow(Icons.Outlined.Add, "Record a decision", "", valueColor = Atlas.Primary, onClick = { vm.show(Overlay.Create(ObjType.DECISION, o.id)) })
            }
            YourFields(vm, o)
            CollapsedFooter(vm, snap, o)
        }
    }
}

@Composable
fun TaskLine(vm: AtlasViewModel, snap: Snapshot, t: KObject) {
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.open(t.id) }.heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(t.done, { vm.repo.toggleDone(t.id) }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val parts = listOfNotNull(
                t.deadline?.let { "Deadline ${relDate(it, today).let { s -> if (s.length > 10) shortDate(it) else s }}" },
                t.due?.takeIf { t.deadline == null }?.let { relDate(it, today) },
                "blocked".takeIf { t.blocked },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = if ((t.due ?: t.deadline)?.isBefore(today) == true) Atlas.Error else Atlas.SecondaryText)
        }
    }
}
