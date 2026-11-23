package com.jmr.atlas.ui.cards

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.integrations.Contacts
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.components.Pill
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.components.relDate
import com.jmr.atlas.ui.components.shortDate
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    val events by vm.sync.events.collectAsState()
    val phones = snap.detailsOf(o.id, DetailKind.PHONE).ifEmpty { listOfNotNull(o.phone.takeIf { it.isNotBlank() }?.let { Detail(objectId = o.id, kind = DetailKind.PHONE, label = "Phone", value = it) }) }
    val emails = snap.detailsOf(o.id, DetailKind.EMAIL).ifEmpty { listOfNotNull(o.email.takeIf { it.isNotBlank() }?.let { Detail(objectId = o.id, kind = DetailKind.EMAIL, label = "Email", value = it) }) }
    val addresses = snap.detailsOf(o.id, DetailKind.ADDRESS)
    val sites = snap.detailsOf(o.id, DetailKind.WEBSITE)
    val social = snap.detailsOf(o.id, DetailKind.SOCIAL)
    val dates = snap.detailsOf(o.id, DetailKind.DATE)
    val nb = snap.neighbours(o.id, false).map { it.first }
    val projects = nb.filter { it.type == ObjType.PROJECT }
    val myEvents = remember(events, o, snap) { vm.sync.eventsFor(o, snap) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp).testTag("person_card")) {
        CardTopBar(vm, o)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                PersonPhoto(o, 120.dp)
                if (o.contactKey.isNotBlank()) Box(Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(Atlas.Surface).padding(3.dp).clip(CircleShape).background(Atlas.CoolMint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Call, "Linked to a phone contact", tint = Atlas.DeepTeal, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(o.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            if (o.nickname.isNotBlank()) Text("“${o.nickname}”", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
            listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = Atlas.SecondaryText) }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), itemVerticalAlignment = Alignment.CenterVertically) {
                if (o.role.isNotBlank()) Pill(o.role)
                projects.take(2).forEach { p -> Pill(p.title, Icons.Outlined.FolderOpen, Atlas.SurfaceSecondary, Atlas.DeepPlum) { vm.open(p.id) } }
            }
            Spacer(Modifier.height(6.dp))
            if (o.contactKey.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                Text(if (vm.settings.contactsSync) "Synced with your phone contact" else "Linked to your phone contact · read-only", style = MaterialTheme.typography.labelMedium, color = Atlas.DeepTeal)
            } else TextButton({ vm.show(Overlay.AddContact(o.id)) }, Modifier.testTag("link_contact")) { Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Connect to a phone contact") }
        }
        ActionRow(listOf(
            CardAction(Icons.Outlined.Call, "Call", phones.isNotEmpty()) { platform.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phones.first().value))) },
            CardAction(Icons.AutoMirrored.Outlined.Message, "Text", phones.isNotEmpty()) { platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phones.first().value))) },
            CardAction(Icons.Outlined.Email, "Email", emails.isNotEmpty()) { platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emails.first().value))) },
            CardAction(Icons.Outlined.Directions, "Directions", addresses.isNotEmpty()) { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(addresses.first().value)))) },
            CardAction(Icons.Outlined.Mic, "Note") { platform.dictate { t -> vm.captureText("${o.title}: $t", voice = true) } },
        ))
        WhyNowBox(vm, snap, o, myEvents.firstOrNull()?.let { listOf(Icons.Outlined.Event to "${it.title} — ${eventWhen(it, today)}") }.orEmpty())

        // ---- contact
        Section("Contact", trailing = if (o.contactKey.isNotBlank()) "✓ Phone contact" else "In Atlas only", trailingColor = if (o.contactKey.isNotBlank()) Atlas.DeepTeal else Atlas.SecondaryText)
        val hasContact = phones.isNotEmpty() || emails.isNotEmpty() || addresses.isNotEmpty() || sites.isNotEmpty() || dates.isNotEmpty() || social.isNotEmpty()
        Box16 {
            if (!hasContact) FieldRow(Icons.Outlined.Add, "Add phone, email, address", "Saved to the phone contact when sync is on", valueColor = Atlas.Primary, onClick = { vm.show(Overlay.Edit(o.id)) })
            var first = true
            fun sep(): Boolean { val f = first; first = false; return !f }
            phones.forEach { p -> if (sep()) Divider16()
                FieldRow(Icons.Outlined.Call, p.value, p.label, onClick = { platform.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p.value))) }) {
                    IconButton({ platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + p.value))) }, Modifier.semantics { contentDescription = "Text ${p.value}" }) { Icon(Icons.AutoMirrored.Outlined.Message, null, tint = Atlas.DeepPlum) }
                } }
            emails.forEach { e -> if (sep()) Divider16(); FieldRow(Icons.Outlined.Email, e.value, e.label, onClick = { platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + e.value))) }) }
            addresses.forEach { a -> if (sep()) Divider16(); FieldRow(Icons.Outlined.Place, a.value, a.label, onClick = { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a.value)))) }) }
            sites.forEach { w -> if (sep()) Divider16(); FieldRow(Icons.Outlined.Link, w.value.removePrefix("https://").removePrefix("http://"), w.label, onClick = { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url(w.value)))) }) }
            dates.forEach { d -> if (sep()) Divider16()
                val next = Contacts.nextOccurrence(d.value, today)
                val shown = next?.let { it.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d")) } ?: d.value
                val sub = next?.let { n -> val days = ChronoUnit.DAYS.between(today, n); " · " + if (days == 0L) "today" else "in " + Briefing.span(days) }.orEmpty()
                FieldRow(Icons.Outlined.Cake.takeIf { d.label.equals("Birthday", true) } ?: Icons.Outlined.CalendarMonth, shown, d.label + sub) }
            if (social.isNotEmpty() || hasContact) {
                FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    social.forEach { s -> Pill(s.label, Icons.Outlined.Link, if (s.label == "Instagram") Atlas.Peach else Atlas.PowderBlue, if (s.label == "Instagram") Atlas.CoralText else Atlas.BlueText) { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url(s.value)))) } }
                    Pill("Add social", Icons.Outlined.Add, Atlas.SurfaceSecondary, Atlas.DeepPlum) { vm.show(Overlay.Edit(o.id)) }
                }
            }
        }

        // ---- things to remember
        val remember = snap.detailsOf(o.id, DetailKind.REMEMBER)
        Section("Things to remember", action = "Add") { vm.show(Overlay.AddLine(o.id, DetailKind.REMEMBER)) }
        if (remember.isEmpty()) Text("Preferences, family, what matters to them — small things you'd want to recall.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        else Box16 {
            remember.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(Atlas.Orchid)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.value, style = MaterialTheme.typography.bodyLarge)
                        Text("${if (r.origin == Origin.USER) "You" else r.origin.label}, ${shortDate(java.time.Instant.ofEpochMilli(r.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                    }
                    IconButton({ vm.repo.deleteDetail(r.id) }, Modifier.size(36.dp).semantics { contentDescription = "Remove" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.SecondaryText, modifier = Modifier.size(18.dp)) }
                }
            }
        }

        // ---- open loops
        val loops = nb.filter { it.type == ObjType.TASK && !it.done }
        if (loops.isNotEmpty()) {
            Section("Open loops")
            Box16 {
                loops.sortedBy { it.due ?: it.deadline ?: LocalDate.MAX }.forEachIndexed { i, t ->
                    if (i > 0) Divider16()
                    val waiting = t.title.startsWith("Expect ", true)
                    FieldRow(if (waiting) Icons.Outlined.Schedule else Icons.Outlined.Check, t.title.removePrefix("Expect ").replaceFirstChar { it.uppercase() },
                        if (waiting) "Waiting on ${o.firstName}" else "You promised", onClick = { vm.open(t.id) }) {
                        (t.due ?: t.deadline)?.let { d -> Pill(relDate(d).take(9), container = if (d.isBefore(today)) Atlas.Peach else if (waiting) Atlas.CoolMint else Atlas.SurfaceSecondary, content = if (d.isBefore(today)) Atlas.CoralText else Atlas.DeepTeal) }
                    }
                }
            }
        }

        YourFields(vm, o)

        if (myEvents.isNotEmpty()) {
            Section("Upcoming")
            Box16 { myEvents.take(4).forEachIndexed { i, e -> if (i > 0) Divider16(); FieldRow(Icons.Outlined.Event, "${e.title} · ${eventWhen(e, today)}", "Calendar" + (e.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                onClick = { platform.launch(Calendars.viewIntent(e.eventId)) }) } }
        }

        val mentions = nb.filter { it.type in setOf(ObjType.NOTE, ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.IDEA) }.sortedByDescending { it.created }
        if (mentions.isNotEmpty()) {
            Section("Recent mentions", trailing = if (mentions.size > 3) "All ${mentions.size}" else null)
            Box16 { mentions.take(3).forEachIndexed { i, m -> if (i > 0) Divider16()
                val quote = if (m.type == ObjType.NOTE) "“${snippetAround(m.body, o.firstName)}”" else m.title
                FieldRow(com.jmr.atlas.ui.theme.icon(m.type).let { if (m.kind == "Voice note") Icons.Outlined.Mic else it }, quote,
                    "${m.kind.ifBlank { m.type.label }} · ${relDate(java.time.Instant.ofEpochMilli(m.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate()).lowercase()}", onClick = { vm.open(m.id) }) } }
        }
        CollapsedFooter(vm, snap, o)
    }
}

fun url(v: String) = if (v.startsWith("http", true)) v else "https://$v"

fun eventWhen(e: Calendars.Event, today: LocalDate): String {
    val d = e.day
    val day = when (ChronoUnit.DAYS.between(today, d)) { 0L -> "Today"; 1L -> "Tomorrow"; in 2..6 -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()); else -> dayDate(d) }
    return if (e.allDay) "$day · all day" else "$day ${Briefing.time(e.begin)}"
}

fun snippetAround(text: String, word: String, n: Int = 60): String {
    val i = text.indexOf(word, ignoreCase = true)
    if (i < 0) return text.take(n).let { if (text.length > n) "$it…" else it }
    val start = text.lastIndexOf(' ', (i - 15).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val s = text.substring(start).take(n).substringBeforeLast(' ')
    return (if (start > 0) "…" else "") + s + if (start + s.length < text.length) "…" else ""
}
