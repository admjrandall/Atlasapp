package com.jmr.atlas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.cards.PersonPhoto
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Home (design P10): today's briefing, what needs you, what you're waiting on, and what you just captured. */
@Composable
fun NowScreen(vm: AtlasViewModel, snap: Snapshot, modifier: Modifier = Modifier) {
    val events by vm.sync.events.collectAsState()
    val today = LocalDate.now()
    val b = remember(snap, events) { Briefing.build(snap, events, today) }
    val platform = LocalPlatform.current
    val hour = LocalTime.now().hour
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 120.dp).testTag("now")) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = MaterialTheme.typography.bodyLarge, color = Atlas.SecondaryText)
                Text(when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            }
            IconButton({ vm.go(Route.Settings) }, Modifier.semantics { contentDescription = "Settings" }.testTag("nav_Settings")) { Icon(Icons.Outlined.Tune, null, tint = Atlas.DeepPlum) }
        }
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = Atlas.PrimaryContainer, modifier = Modifier.fillMaxWidth().testTag("briefing")) {
            Text(bold(b.headline), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.titleMedium.lineHeight), color = Atlas.DeepPlum, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
        }
        if (snap.live.isEmpty()) {
            Empty("Start by capturing something", "A thought, a voice note, a scan, a link. Tap + below or use the home-screen widget.")
            return@Column
        }

        if (b.today.isNotEmpty()) {
            SectionLabel("Today on your calendar")
            b.today.take(4).forEach { e ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { platform.launch(Calendars.viewIntent(e.eventId)) }.heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (e.allDay) "All day" else Briefing.time(e.begin), style = MaterialTheme.typography.labelLarge, color = Atlas.DeepTeal, modifier = Modifier.width(76.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (e.location.isNotBlank()) Text(e.location, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1)
                    }
                }
            }
        }

        SectionLabel("Needs you") { if (b.needsYou.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Text("${b.needsYou.size}", style = MaterialTheme.typography.labelLarge) } }
        if (b.needsYou.isEmpty()) Text("Nothing overdue or due today.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 8.dp))
        b.needsYou.take(6).forEach { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.open(item.task.id) }.heightIn(min = 60.dp).padding(vertical = 4.dp).testTag("needs_row"), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(false, { vm.repo.toggleDone(item.task.id); vm.say("Done: ${item.task.title}") { vm.repo.toggleDone(item.task.id) } }, Modifier.semantics { contentDescription = "Complete ${item.task.title}" })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.task.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.context, style = MaterialTheme.typography.bodySmall, color = if (item.late) Atlas.Error else Atlas.SecondaryText, maxLines = 1)
                }
            }
        }
        if (b.needsYou.size > 6) TextButton({ vm.top(Route.Library(ObjType.TASK)) }) { Text("All ${b.needsYou.size}") }

        if (b.waiting.isNotEmpty()) {
            SectionLabel("Waiting on")
            b.waiting.take(4).forEach { (t, p) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.open(t.id) }.heightIn(min = 60.dp).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (p != null) PersonPhoto(p, 44.dp) else Box(Modifier.size(44.dp).clip(CircleShape).background(Atlas.CoolMint), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Schedule, null, tint = Atlas.DeepTeal) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title.removePrefix("Expect ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text((t.due ?: t.deadline)?.let { d -> if (d.isBefore(today)) "Expected ${relDate(d).lowercase()} · follow up" else "Expected ${relDate(d).let { s -> if (s.length <= 9) s else shortDate(d) }}" } ?: "No date",
                            style = MaterialTheme.typography.bodySmall, color = if ((t.due ?: t.deadline)?.isBefore(today) == true) Atlas.Error else Atlas.SecondaryText)
                    }
                }
            }
        }

        val recent = snap.captures.filter { System.currentTimeMillis() - it.created < 48 * 3_600_000L }.take(2)
        if (recent.isNotEmpty()) {
            SectionLabel("Just captured")
            recent.forEach { cap ->
                val src = (cap.noteId ?: cap.attachmentId)?.let { snap.objects[it] } ?: return@forEach
                Surface(onClick = { vm.open(src.id) }, shape = RoundedCornerShape(24.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("just_captured")) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (src.kind == "Voice note") Icons.Outlined.Mic else com.jmr.atlas.ui.theme.icon(src.type), null, tint = Atlas.Primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("${src.kind.ifBlank { src.type.label }} · ${Briefing.time(cap.created)}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (src.type == ObjType.NOTE) "“${src.body.take(180)}”" else src.title, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        CaptureResult(vm, snap, cap, inSheet = false)
                    }
                }
            }
        }
        val pending = snap.pending().size
        if (pending > 0 && recent.none { c -> snap.pending().any { it.captureId == c.id } }) TextButton({ vm.go(Route.Review) }) { Text("${plural(pending, "suggestion")} to review →") }
    }
}

/** Bolds the counts in the briefing headline (“1 overdue, 2 due today”). */
private fun bold(s: String) = buildAnnotatedString {
    val rx = Regex("\\b(\\d+ overdue|\\d+ due today|\\d+)\\b")
    var i = 0
    rx.findAll(s).forEach { m -> append(s.substring(i, m.range.first)); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.value) }; i = m.range.last + 1 }
    append(s.substring(i))
}
