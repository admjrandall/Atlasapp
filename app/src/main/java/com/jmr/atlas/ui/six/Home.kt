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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
        Spacer(Modifier.height(if (wide) 22.dp else 18.dp))
        CoreStrip(vm, snap, today)
        Spacer(Modifier.height(if (wide) 22.dp else 18.dp))
        JourneysSection(vm, snap, wide)
        Spacer(Modifier.height(if (wide) 26.dp else 20.dp))
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1f)) { Week(vm, rows, todays, ticked) }
            Column(Modifier.width(268.dp)) { SpacesPanel(vm, snap, today) }
        } else {
            SpaceChips(vm, snap, today)
            Spacer(Modifier.height(20.dp))
            Week(vm, rows, todays, ticked)
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
    // three or more slipped tasks are one row that opens a quick sort-out, not a wall of red
    if (late.count { !it.task.done } >= 3) SlippedRow(late.size, late.first().task.title) { vm.go(Route.Space(Board.Kind.TASKS.key)) }
    else late.forEach { AgendaRow(vm, it, ticked) }
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

// ------------------------------------------------------------------ core kinds and spaces (design 0.7)

@Composable
private fun SlippedRow(n: Int, oldest: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(Atlas.LateBg).clickable(role = Role.Button, onClickLabel = "Sort out slipped tasks", onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp).testTag("slipped"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("$n slipped", style = T.row.copy(fontWeight = FontWeight.Bold), color = Atlas.LateInk)
            Text("Oldest: ${oldest.removePrefix("Expect ")}", style = T.meta, color = Atlas.LateInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Sort out", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.LateInk)
    }

private data class Core(val kind: Board.Kind?, val title: String, val icon: ImageVector)
private val CORE = listOf(
    Core(Board.Kind.PEOPLE, "People", Icons.Outlined.People), Core(Board.Kind.TASKS, "Tasks", Icons.Outlined.CheckCircle),
    Core(Board.Kind.FILES, "Files", Icons.Outlined.Description), Core(Board.Kind.NOTES, "Ideas & notes", Icons.Outlined.Lightbulb), Core(null, "Spaces", Icons.Outlined.Folder),
)

/** The core kinds in one strip that scrolls sideways, each with its count — so Now stays short however much you keep. */
@Composable
private fun CoreStrip(vm: AtlasViewModel, snap: Snapshot, today: LocalDate) {
    val wide = LocalWide.current
    val late = remember(snap) { snap.ofType(ObjType.TASK).count { !it.done && !Board.isWaiting(it) && Board.effective(it)?.isBefore(today) == true } }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("core_strip"), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CORE.forEach { c ->
            val n = when (c.kind) { null -> Board.allSpaces(snap).size; else -> count(snap, c.kind) }
            val sub = if (c.kind == Board.Kind.TASKS && late > 0) "$late slipped" else "$n"
            Column(Modifier.width(if (wide) 150.dp else 112.dp).height(if (wide) 96.dp else 90.dp).shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Atlas.Surface)
                .clickable(role = Role.Button, onClickLabel = "Open ${c.title}") { if (c.kind == null) vm.go(Route.Space(Board.SPACES_KEY)) else vm.go(Route.Space(c.kind.key)) }
                .padding(14.dp).testTag("core_${c.title}"), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(c.icon, null, Modifier.size(22.dp), tint = Atlas.DeepPlum)
                Column {
                    Text(c.title, style = T.tileTitle.copy(fontSize = if (wide) 16.sp else 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, style = T.meta.copy(fontSize = 12.sp), color = if (sub.endsWith("slipped")) Atlas.CoralText else Atlas.SecondaryText)
                }
            }
        }
    }
}

private fun count(s: Snapshot, k: Board.Kind) = when (k) {
    Board.Kind.PEOPLE -> s.ofType(ObjType.PERSON).size
    Board.Kind.FILES -> s.live.count { it.type.isFile }
    Board.Kind.NOTES -> s.live.count { it.type == ObjType.NOTE || it.type == ObjType.IDEA || it.type == ObjType.DECISION }
    Board.Kind.TASKS -> s.ofType(ObjType.TASK).count { !it.done }
    Board.Kind.EVERYTHING -> s.live.size
}

/** Fold open: up to four pinned spaces as tiles, a few more with something this week, then All spaces. */
@Composable
private fun SpacesPanel(vm: AtlasViewModel, snap: Snapshot, today: LocalDate) {
    val pinned = remember(snap) { Board.pinned(snap, today) }
    val active = remember(snap) { Board.active(snap, today, pinned.map { it.first.id }.toSet()).take(3) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Eyebrow("Pinned", Modifier.weight(1f))
        if (pinned.any { it.second }) Text("busiest first", style = T.meta.copy(fontSize = 12.sp), color = Atlas.Hint)
        Text("+ New space", style = T.metaStrong.copy(fontSize = 13.sp), color = Atlas.Primary,
            modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Chip).clickable(role = Role.Button) { vm.openCatch("New space ") }
                .padding(horizontal = 14.dp).wrapContentHeight(Alignment.CenterVertically).testTag("home_new_space"))
    }
    Spacer(Modifier.height(10.dp))
    if (pinned.isEmpty()) Text("Spaces you’re busy in show here. Tap + New space to make one.", style = T.meta, color = Atlas.SecondaryText)
    pinned.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (p, _) -> SpaceTile(vm, snap, p, today, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Atlas.Surface).padding(horizontal = 14.dp)) {
        active.forEach { p ->
            val (line, late) = Board.spaceLine(snap, p, today)
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { vm.projectSpace(p.id) }.padding(vertical = 8.dp).testTag("space_${p.title}"), verticalAlignment = Alignment.CenterVertically) {
                SpaceMark(snap, p, 26.dp); Spacer(Modifier.width(10.dp))
                Text(p.title, style = T.tileTitle.copy(fontSize = 14.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(line, style = T.meta.copy(fontSize = 12.sp), color = if (late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
            }
            HorizontalDivider(color = Atlas.Hairline)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { vm.go(Route.Space(Board.SPACES_KEY)) }.testTag("all_spaces"), verticalAlignment = Alignment.CenterVertically) {
            Text("All ${Board.allSpaces(snap).size} spaces", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.Primary)
        }
    }
}

@Composable
private fun SpaceTile(vm: AtlasViewModel, snap: Snapshot, p: KObject, today: LocalDate, modifier: Modifier) {
    val (line, late) = remember(snap, p.id) { Board.spaceLine(snap, p, today) }
    Column(modifier.shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Atlas.Surface).clickable(role = Role.Button, onClickLabel = "Open ${p.title}") { vm.projectSpace(p.id) }
        .padding(12.dp).testTag("cover"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            SpaceMark(snap, p, 34.dp); Spacer(Modifier.weight(1f))
            if (late) Box(Modifier.size(8.dp).clip(CircleShape).background(Atlas.Coral).semantics { contentDescription = "has a late task" })
        }
        Text(p.title, style = T.tileTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(line, style = T.meta.copy(fontSize = 12.sp), color = if (late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Cover screen: pinned spaces as chips, one thumb away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpaceChips(vm: AtlasViewModel, snap: Snapshot, today: LocalDate) {
    val pinned = remember(snap) { Board.pinned(snap, today) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Eyebrow("Spaces", Modifier.weight(1f))
        Text("All ${Board.allSpaces(snap).size}", style = T.metaStrong.copy(fontSize = 13.sp), color = Atlas.Primary,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { vm.go(Route.Space(Board.SPACES_KEY)) }.padding(horizontal = 8.dp, vertical = 6.dp).testTag("all_spaces"))
        Text("+ New space", style = T.metaStrong.copy(fontSize = 13.sp), color = Atlas.Primary,
            modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Chip).clickable(role = Role.Button) { vm.openCatch("New space ") }
                .padding(horizontal = 14.dp).wrapContentHeight(Alignment.CenterVertically).testTag("home_new_space"))
    }
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pinned.forEach { (p, _) ->
            val late = remember(snap, p.id) { Board.spaceLine(snap, p, today).second }
            Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.Surface).clickable(role = Role.Button) { vm.projectSpace(p.id) }
                .padding(start = 6.dp, end = 12.dp).testTag("space_${p.title}"), verticalAlignment = Alignment.CenterVertically) {
                SpaceMark(snap, p, 30.dp); Spacer(Modifier.width(8.dp))
                Text(p.title, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum, maxLines = 1)
                if (late) { Spacer(Modifier.width(6.dp)); Box(Modifier.size(8.dp).clip(CircleShape).background(Atlas.Coral)) }
            }
        }
        if (pinned.isEmpty()) Text("Tap + New space to make one.", style = T.meta, color = Atlas.SecondaryText)
    }
}

/** Home's journeys (approved board J-Home): each one in its own small animated shape with one status line; "+ New journey". */
@Composable
private fun JourneysSection(vm: AtlasViewModel, snap: Snapshot, wide: Boolean) {
    val js = remember(snap) { snap.ofType(ObjType.PROJECT).filter { !it.trashed }.mapNotNull { p -> Journeys.of(snap, p.id)?.let { p to it } }.sortedByDescending { it.first.updated } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Journeys", style = T.band, color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
        Text("+ New journey", style = T.metaStrong.copy(fontSize = 13.sp), color = Color.White, modifier = Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(Atlas.Primary)
            .clickable(role = Role.Button) { vm.go(Route.JourneyStart(null)) }.padding(horizontal = 14.dp).wrapContentHeight(Alignment.CenterVertically).testTag("home_new_journey"))
    }
    if (js.isEmpty()) { Text("Give any space a shape: a trail for a project, a tree for someone you love, a room to remember someone by.", style = T.meta, color = Atlas.SecondaryText,
        modifier = Modifier.padding(top = 6.dp)); return }
    val cols = if (wide) 4 else 2
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        js.chunked(cols).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { (p, j) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClickLabel = "Open ${p.title}") { vm.go(Route.Journey(p.id)) }
                    .padding(vertical = 10.dp, horizontal = 6.dp).testTag("home_journey"), horizontalAlignment = Alignment.CenterHorizontally) {
                    com.jmr.atlas.ui.journey.Glyph(j.shape, 72.dp)
                    Text(p.title, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(com.jmr.atlas.ui.journey.subtitle(snap, j), style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        } }
    }
}

