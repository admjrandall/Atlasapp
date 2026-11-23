package com.jmr.atlas.ui.six

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.data.Board
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.cards.CoverBand
import com.jmr.atlas.ui.components.dashedBorder
import com.jmr.atlas.ui.components.rememberThumb
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Home (design 0.6): one sentence, one list, your spaces. The settings icon sits quietly at the top right. */
@Composable
fun HomeScreen(vm: AtlasViewModel, snap: Snapshot) {
    val wide = LocalWide.current
    val today = LocalDate.now()
    val events by vm.sync.events.collectAsState()
    val sentence = remember(snap, today) { Briefing.sentence(snap, today) }
    val week = remember(snap, today) { Briefing.week(snap, today) }
    val todays = remember(events) { events.filter { it.day == today && !it.allDay && it.end > System.currentTimeMillis() }.sortedBy { it.begin }.take(3) }
    // rows you tick stay (struck through) until you leave, so a tap never makes the list jump
    val ticked = remember { mutableStateMapOf<String, Briefing.Agenda>() }
    val rows = remember(week, ticked.toMap()) { (week + ticked.values.filter { t -> week.none { it.task.id == t.task.id } }).sortedWith(compareBy({ if (it.late) 0 else 1 }, { Board.effective(it.task) })) }
    val sources = remember(snap) { snap.ofType(ObjType.TASK).count { !it.done } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = pad).padding(top = if (wide) 28.dp else 20.dp, bottom = 132.dp).testTag("home")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(today.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")), Modifier.weight(1f))
            RoundIcon(Icons.Outlined.Settings, "Settings", { vm.go(Route.Settings) }, Modifier.testTag("nav_Settings"), container = Color.Transparent, tint = Atlas.Hint)
        }
        Spacer(Modifier.height(if (wide) 14.dp else 8.dp))
        BriefSentence(sentence, wide) { id -> if (id.startsWith("space:")) vm.projectSpace(id.removePrefix("space:")) else vm.reveal(id) }
        Spacer(Modifier.height(12.dp))
        AtlasDid(if (sources == 0 && todays.isEmpty()) "Written on this phone" else "Written on this phone from ${if (sources == 1) "1 task" else "$sources tasks"}" + if (todays.isNotEmpty() || vm.settings.calendarInBriefing) " and your calendar" else "")
        val pending = snap.pending().size
        if (pending > 0) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Atlas.PrimaryContainer).clickable(role = Role.Button) { vm.go(Route.Review) }.padding(horizontal = 14.dp, vertical = 10.dp).testTag("to_confirm"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Sparkle, null, Modifier.size(16.dp), tint = Atlas.Primary); Spacer(Modifier.width(8.dp))
                Text("Atlas found ${if (pending == 1) "1 thing" else "$pending things"} to confirm", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum)
            }
        }
        Spacer(Modifier.height(if (wide) 30.dp else 24.dp))
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) { Week(vm, rows, todays, ticked) }
            Column(Modifier.width(252.dp)) { SpacesColumn(vm, snap, today) }
        } else {
            Week(vm, rows, todays, ticked)
            Spacer(Modifier.height(26.dp))
            SpacesRow(vm, snap, today)
        }
    }
}

@Composable
private fun BriefSentence(segs: List<Briefing.Seg>, wide: Boolean, onEntity: (String) -> Unit) {
    val style = if (wide) T.sentence else T.sentenceCompact
    val link = TextLinkStyles(SpanStyle(color = Atlas.Primary, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        segs.forEach { s ->
            if (s.id == null) append(s.text)
            else withLink(LinkAnnotation.Clickable(s.id, link) { onEntity(s.id) }) { append(s.text) }
        }
    }
    Text(text, style = style, color = Atlas.DeepPlum, modifier = Modifier.widthIn(max = 640.dp).testTag("brief"))
}

@Composable
private fun Week(vm: AtlasViewModel, rows: List<Briefing.Agenda>, todays: List<Calendars.Event>, ticked: MutableMap<String, Briefing.Agenda>) {
    Eyebrow("This week")
    Spacer(Modifier.height(6.dp))
    if (rows.isEmpty() && todays.isEmpty()) {
        Text("Nothing due this week. Anything you catch with a date lands here.", style = T.body, color = Atlas.SecondaryText, modifier = Modifier.padding(vertical = 12.dp))
        return
    }
    val late = rows.takeWhile { it.late }
    late.forEach { AgendaRow(vm, it, ticked) }
    val platform = com.jmr.atlas.LocalPlatform.current
    todays.forEach { e -> EventRow(e) { platform.launch(Calendars.viewIntent(e.eventId)) } }
    rows.drop(late.size).forEach { AgendaRow(vm, it, ticked) }
}

@Composable
private fun AgendaRow(vm: AtlasViewModel, a: Briefing.Agenda, ticked: MutableMap<String, Briefing.Agenda>) {
    val live = vm.repo.snap.objects[a.task.id] ?: a.task
    val done = live.done
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 6.dp).testTag("agenda_row"), verticalAlignment = Alignment.CenterVertically) {
            val box = Modifier.size(44.dp).clickable(role = Role.Checkbox, onClickLabel = if (done) "Mark not done" else "Mark done") {
                if (!done) ticked[a.task.id] = a
                vm.repo.toggleDone(a.task.id)
            }.semantics { contentDescription = a.task.title; stateDescription = if (done) "Done" else "Not done" }
            Box(box, contentAlignment = Alignment.Center) {
                val shape = if (a.waiting) CircleShape else RoundedCornerShape(10.dp)
                Box(Modifier.size(28.dp).clip(shape).then(
                    when { done -> Modifier.background(Atlas.DeepTeal); a.waiting -> Modifier.dashedBorder(Atlas.Hint, circle = true); else -> Modifier.border(2.dp, Atlas.Hint, shape) }),
                    contentAlignment = Alignment.Center) { if (done) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClickLabel = "Show") { vm.reveal(a.task.id) }.padding(vertical = 4.dp)) {
                Text(live.title, style = T.row, color = if (done) Atlas.Hint else Atlas.DeepPlum, textDecoration = if (done) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(a.whenText, a.space?.title).joinToString(" · "), style = T.meta, color = if (done) Atlas.Hint else if (a.late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = Atlas.Hairline)
    }
}


@Composable
private fun EventRow(e: Calendars.Event, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClickLabel = "Open in calendar", onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Event, null, tint = Atlas.BlueText, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = T.row, color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull("Today " + Briefing.time(e.begin), e.location.takeIf { it.isNotBlank() }, "calendar").joinToString(" · "), style = T.meta, color = Atlas.BlueText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = Atlas.Hairline)
    }
}

// ------------------------------------------------------------------ spaces
private data class Special(val kind: Board.Kind, val icon: ImageVector, val tint: Color)
private val SPECIAL = listOf(
    Special(Board.Kind.PEOPLE, Icons.Outlined.People, Atlas.PrimaryContainer), Special(Board.Kind.FILES, Icons.Outlined.Description, Atlas.PowderBlue),
    Special(Board.Kind.NOTES, Icons.Outlined.Lightbulb, Atlas.Peach), Special(Board.Kind.TASKS, Icons.Outlined.CheckCircle, Atlas.CoolMint),
    Special(Board.Kind.EVERYTHING, Icons.Outlined.GridView, Atlas.Chip),
)

private fun count(s: Snapshot, k: Board.Kind) = when (k) {
    Board.Kind.PEOPLE -> s.ofType(ObjType.PERSON).size
    Board.Kind.FILES -> s.live.count { it.type.isFile }
    Board.Kind.NOTES -> s.live.count { it.type == ObjType.NOTE || it.type == ObjType.IDEA || it.type == ObjType.DECISION }
    Board.Kind.TASKS -> s.ofType(ObjType.TASK).count { !it.done }
    Board.Kind.EVERYTHING -> s.live.size
}

@Composable
private fun SpacesColumn(vm: AtlasViewModel, snap: Snapshot, today: LocalDate) {
    Eyebrow("Spaces")
    Spacer(Modifier.height(10.dp))
    val spaces = Board.spaces(snap)
    spaces.take(2).forEach { p -> Cover(vm, snap, p, today, Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Atlas.Surface).padding(horizontal = 14.dp, vertical = 4.dp)) {
        val rest = spaces.drop(2)
        rest.forEach { p -> SpaceLine(p.title, Board.cover(snap, p, today).line, null, Atlas.bands[(p.id.hashCode() and 0x7fffffff) % 4]) { vm.projectSpace(p.id) }; HorizontalDivider(color = Atlas.Hairline) }
        SPECIAL.forEachIndexed { i, sp ->
            SpaceLine(sp.kind.title, "${count(snap, sp.kind)} · ${sp.kind.blurb.lowercase()}", sp.icon, sp.tint) { vm.go(Route.Space(sp.kind.key)) }
            if (i < SPECIAL.lastIndex) HorizontalDivider(color = Atlas.Hairline)
        }
    }
}

@Composable
private fun SpaceLine(title: String, sub: String, icon: ImageVector?, tint: Color, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp).testTag("space_$title"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tint), contentAlignment = Alignment.Center) { if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = Atlas.DeepPlum) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

@Composable
private fun SpacesRow(vm: AtlasViewModel, snap: Snapshot, today: LocalDate) {
    Eyebrow("Spaces")
    Spacer(Modifier.height(10.dp))
    val spaces = Board.spaces(snap)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        spaces.forEach { p -> Cover(vm, snap, p, today, Modifier.width(230.dp)) }
        if (spaces.isEmpty()) Text("Type “Plan the garden” below and make it a space, or open one from Everything.", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.width(260.dp))
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SPECIAL.forEach { sp ->
            Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Surface).clickable(role = Role.Button) { vm.go(Route.Space(sp.kind.key)) }
                .padding(start = 6.dp, end = 14.dp).testTag("space_${sp.kind.title}"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(sp.tint), contentAlignment = Alignment.Center) { Icon(sp.icon, null, Modifier.size(17.dp), tint = Atlas.DeepPlum) }
                Spacer(Modifier.width(8.dp))
                Text(sp.kind.title, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum)
            }
        }
    }
}

@Composable
private fun Cover(vm: AtlasViewModel, snap: Snapshot, p: KObject, today: LocalDate, modifier: Modifier) {
    val c = remember(snap, p.id) { Board.cover(snap, p, today) }
    val image = remember(snap, p.id) { Board.members(snap, p.id).firstOrNull { it.type == ObjType.IMAGE && it.vaultFile.isNotBlank() } }
    val thumb = rememberThumb(image, 480)
    Column(modifier.shadow(6.dp, RoundedCornerShape(26.dp), ambientColor = Atlas.DeepPlum.copy(alpha = 0.2f), spotColor = Atlas.DeepPlum.copy(alpha = 0.2f))
        .clip(RoundedCornerShape(26.dp)).background(Atlas.Surface).clickable(role = Role.Button, onClickLabel = "Open space") { vm.projectSpace(p.id) }.testTag("cover")) {
        Box(Modifier.fillMaxWidth().height(104.dp)) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) { CoverBand(p.id.hashCode()) }
            Row(Modifier.align(Alignment.BottomStart).offset(x = 14.dp, y = 18.dp)) {
                c.people.forEachIndexed { i, person -> Face(person, 38.dp, Modifier.offset(x = (-10 * i).dp), ring = Atlas.Surface) }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = if (c.people.isEmpty()) 14.dp else 26.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(p.title, style = T.cover, color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
            c.progress?.let { pr ->
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Atlas.Hairline)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pr).clip(RoundedCornerShape(3.dp)).background(Atlas.Primary))
                }
            }
            Text(c.line, style = T.meta, color = if (c.late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
