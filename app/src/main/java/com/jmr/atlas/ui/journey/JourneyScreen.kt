package com.jmr.atlas.ui.journey

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.model.Shape
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.cards.rememberPreview
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A journey (approved boards J-Trail … J-Countdown, J-Room/Garden/Letter): the shape full screen, parts you tap, a sheet
 * with the part's bundles (→ Lens) and an Atlas line, adding with Undo, the shape-and-parts editor, and finishing.
 */
@Composable
fun JourneyScreen(vm: AtlasViewModel, snap: Snapshot, spaceId: String) {
    val space = snap.objects[spaceId]?.takeIf { !it.trashed }
    val j = space?.let { Journeys.of(snap, spaceId) }
    if (space == null || j == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val platform = LocalPlatform.current
    val locked = spaceId in vm.settings.lockedJourneys
    var unlocked by remember(spaceId) { mutableStateOf(!locked) }
    if (!unlocked) { LockedGate(space.title) { platform.authenticate { unlocked = true } }; return }

    var sel by remember(spaceId) { mutableStateOf<String?>(j.here?.id) }
    var screen by remember(spaceId) { mutableStateOf("home") }
    var lens by remember(spaceId) { mutableStateOf<Bundle?>(null) }
    var toast by remember { mutableStateOf<Pair<String, (() -> Unit)?>?>(null) }
    val spots = remember(j) { Layout.spots(j) }
    val acc = Look.accent(j.shape)
    // adding (§4.8): something new arrives in the space while you're here → Atlas proposes where it goes
    val entered = remember(spaceId) { System.currentTimeMillis() }
    val places = JourneyItems.places(snap, spaceId)
    val arrived = JourneyItems.inSpace(snap, spaceId).filter { it.created >= entered && it.id !in places }.maxByOrNull { it.created }
    var dismissed by remember { mutableStateOf(setOf<String>()) }
    val pending = arrived?.takeIf { it.id !in dismissed && screen == "home" && lens == null }

    BackHandler(screen != "home") { screen = "home" }
    val cover = screen != "home" || lens != null || pending != null
    LaunchedEffect(cover) { vm.journeyCover.value = cover }
    DisposableEffect(Unit) { onDispose { vm.journeyCover.value = false } }
    Box(Modifier.fillMaxSize().background(Look.bg(j.shape, j.style)).testTag("journey")) {
        // the scene fits the space above the part sheet: scaled by width or height, whichever is tighter, and centred
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = if (j.shape == Shape.REMEMBER) 104.dp else 64.dp, bottom = 236.dp), contentAlignment = Alignment.TopCenter) {
            val k = minOf(maxWidth / SW, maxHeight / SH)
            val dx = (maxWidth - k * SW) / 2
            SceneArt(j, spots, Modifier.width(maxWidth).height(k * SH), with(androidx.compose.ui.platform.LocalDensity.current) { dx.toPx() })
            Box(Modifier.width(k * SW).height(k * SH)) {
                if (j.shape == Shape.REMEMBER && j.style == "LETTER") Letter(snap, j, spots, k)
                if (j.shape == Shape.SHELF) ShelfRows(snap, j, spots, k)
                if (j.shape == Shape.ORBIT || j.shape == Shape.COUNTDOWN) Center(space, j, k)
                spots.forEachIndexed { i, s -> PartNode(snap, j, s, i, k, s.part.id == sel, acc) { sel = if (sel == s.part.id) null else s.part.id } }
                if (j.shape == Shape.CROSSROADS) Gate(j, k, acc) { sel = GATE }
                if (j.shape == Shape.RIVER) Tributaries(snap, j, k) { lens = it }
            }
        }
        // header
        if (j.progress != null) Box(Modifier.fillMaxWidth().height(4.dp).background(acc.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(j.progress!!.coerceIn(0f, 1f)).height(4.dp).background(acc).testTag("journey_progress")) }
        Row(Modifier.statusBarsPadding().padding(start = 4.dp, end = 10.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Back") { vm.back() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Atlas.SecondaryText) }
            Column(Modifier.weight(1f)) {
                Text(space.title, style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle(snap, j), style = T.meta, color = Atlas.SecondaryText, maxLines = 1)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)).clickable(role = Role.Button, onClickLabel = "Edit this journey") { screen = "edit" }
                .testTag("journey_edit"), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Edit, "Edit this journey", tint = Atlas.DeepPlum) }
        }
        if (j.shape == Shape.REMEMBER) StyleTabs(j.style, Modifier.statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp)) { st -> vm.repo.setJourney(j.copy(style = st)) }
        // part sheet
        Box(Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = 92.dp)) {
            PartSheet(vm, snap, j, sel, acc, onLens = { lens = it }, onFinish = { screen = "done" }, onToast = { toast = it })
        }
        toast?.let { (m, undo) ->
            LaunchedEffect(m) { kotlinx.coroutines.delay(5000); toast = null }
            Row(Modifier.statusBarsPadding().padding(top = 70.dp, start = 24.dp, end = 24.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.DeepPlum)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("journey_toast"), verticalAlignment = Alignment.CenterVertically) {
                Text(m, style = T.meta.copy(fontSize = 13.sp), color = Color.White, modifier = Modifier.weight(1f))
                if (undo != null) Text("Undo", style = T.metaStrong, color = Color.White, modifier = Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f)).clickable(role = Role.Button) { undo(); toast = null }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        pending?.let { item -> AddSheet(snap, j, item, acc, onCancel = { dismissed = dismissed + item.id }) { part ->
            vm.repo.placeInPart(spaceId, item.id, part.id); sel = part.id; dismissed = dismissed + item.id
            toast = "Added to ${part.name}" to { vm.repo.replaceDetails(spaceId, setOf(DetailKind.PLACE), snap.detailsOf(spaceId, DetailKind.PLACE).filter { it.label != item.id }) }
        } }
        AnimatedVisibility(screen == "edit", enter = fadeIn(), exit = fadeOut()) { Editor(vm, snap, space, j, acc, onDone = { screen = "home" }, onToast = { toast = it }) }
        AnimatedVisibility(screen == "done", enter = fadeIn(), exit = fadeOut()) { Finish(vm, snap, space, j, acc) { screen = "home" } }
        lens?.let { b -> Lens(vm, snap, "${space.title} · ${partOf(snap, j, b)?.name ?: j.shape.label}", b) { lens = null } }
    }
}

private const val GATE = "__gate"

private fun partOf(s: Snapshot, j: Journey, b: Bundle): Part? = j.parts.firstOrNull { p -> JourneyItems.inPart(s, j, p.id).any { it.id == b.items.firstOrNull()?.id } }

fun subtitle(s: Snapshot, j: Journey): String {
    val here = j.here; val n = j.parts.size; val idx = j.parts.indexOf(here) + 1
    val count = JourneyItems.inSpace(s, j.spaceId).size
    return when (j.shape) {
        Shape.TRAIL -> if (here != null) "Trail · stop $idx of $n · ${((j.progress ?: 0f) * 100).toInt()}% of the way" else "Trail · $n stops"
        Shape.REMEMBER -> "${j.styleLabel} · $count memories"
        Shape.TREE -> "Living Tree · $count things · grows as they do"
        Shape.CROSSROADS -> "Crossroads · ${j.parts.count { it.state != PartState.OUT }} left"
        Shape.RIVER -> "River · ${listOf("curious", "learning the basics", "confident", "could teach it")[(((j.parts.count { it.state == PartState.DONE }).toFloat() / maxOf(1, n)) * 3.99f).toInt()]}"
        Shape.ORBIT -> "Orbit · comes around again"
        Shape.SHELF -> "Shelf · $count things · ${n} shelves"
        Shape.VOYAGE -> "Voyage · before · during · after"
        Shape.CLIMB -> "Climb · camp $idx of $n"
        Shape.COUNTDOWN -> "Countdown · ${n} lanes"
    }
}

@Composable
private fun StyleTabs(style: String, modifier: Modifier, onPick: (String) -> Unit) = Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Shape.REMEMBER.styles.forEach { st ->
        val on = st == style
        Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Atlas.Primary else Color.White.copy(alpha = 0.8f))
            .clickable(role = Role.Tab) { onPick(st) }.testTag("style_$st"), contentAlignment = Alignment.Center) {
            Text(Shape.styleLabels[st]!!, style = T.metaStrong.copy(fontSize = 12.sp), color = if (on) Color.White else Atlas.DeepPlum)
        }
    }
}

@Composable
private fun PartNode(s: Snapshot, j: Journey, sp: Spot, i: Int, k: Dp, on: Boolean, acc: Color, onTap: () -> Unit) {
    val p = sp.part
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(1f, 1.9f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "p")
    val w = k * sp.w; val h = k * sp.h
    val thumb = JourneyItems.inPart(s, j, p.id).firstOrNull { it.type == ObjType.IMAGE }
    val (img, _) = if (!sp.hit && thumb != null && j.shape in setOf(Shape.TREE, Shape.VOYAGE, Shape.CROSSROADS)) rememberPreview(thumb) else (null to 0)
    Box(Modifier.offset(k * sp.x - w / 2, k * sp.y - h / 2).size(w, h)) {
        if (!sp.hit && p.state == PartState.HERE) Box(Modifier.matchParentSize().scale(pulse.value).clip(CircleShape).border(2.dp, acc.copy(alpha = (1.9f - pulse.value) / 0.9f * 0.6f), CircleShape))
        val (bg, border) = when {
            sp.hit -> Color.Transparent to (if (on) acc else Color.Transparent)
            p.state == PartState.HERE -> Color.White to acc
            p.state == PartState.DONE -> acc to Color.White
            p.state == PartState.OUT -> Color(0xFFD5D9E2) to Color.White
            p.state == PartState.NEXT -> Color.White to Color(0xFFB9A3DA)
            else -> Color(0xFFF1ECF8) to Color(0xFFDCCFEE)
        }
        Box(Modifier.matchParentSize()
            .then(if (!sp.hit) Modifier.shadow(if (on) 10.dp else 3.dp, CircleShape) else Modifier)
            .clip(if (sp.hit) RoundedCornerShape(18.dp) else CircleShape).background(bg)
            .border(if (sp.hit) 2.dp else if (p.state == PartState.HERE) 5.dp else 3.dp, border, if (sp.hit) RoundedCornerShape(18.dp) else CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Open ${p.name}") { onTap() }.testTag("part_$i")) {
            if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
        if (on && !sp.hit) Box(Modifier.matchParentSize().clip(CircleShape).border(3.dp, acc.copy(alpha = 0.35f), CircleShape))
        if (sp.label != LabelAt.NONE) {
            val al = when (sp.label) { LabelAt.RIGHT -> Alignment.CenterStart; LabelAt.LEFT -> Alignment.CenterEnd; LabelAt.ABOVE -> Alignment.TopCenter; else -> Alignment.BottomCenter }
            val off = when (sp.label) { LabelAt.RIGHT -> Modifier.offset(x = w + 8.dp); LabelAt.LEFT -> Modifier.offset(x = -(w + 8.dp)); LabelAt.ABOVE -> Modifier.offset(y = -(h / 2 + 22.dp)); else -> Modifier.offset(y = h / 2 + 22.dp) }
            Column(Modifier.align(al).then(off).wrapContentSize(unbounded = true), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(p.name, style = T.metaStrong.copy(fontSize = 12.sp), color = Atlas.DeepPlum, maxLines = 1, modifier = Modifier.background(Look.bg(j.shape, j.style).copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp))
                stateNote(j, p, i)?.let { Text(it, style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText, maxLines = 1) }
            }
        }
    }
}

private fun stateNote(j: Journey, p: Part, i: Int): String? = when (j.shape) {
    Shape.TRAIL, Shape.CLIMB -> when (p.state) { PartState.HERE -> "you are here"; PartState.DONE -> "done"; else -> null }
    Shape.TREE -> when (p.state) { PartState.HERE -> "growing"; PartState.DONE -> "fruit"; PartState.LATER -> "an idea"; else -> null }
    Shape.CROSSROADS -> when (p.state) { PartState.OUT -> "ruled out"; PartState.DONE -> "chosen"; else -> null }
    Shape.VOYAGE -> if (p.state == PartState.HERE) "today" else null
    Shape.REMEMBER -> if (j.style == "ROOM") keepsakeFor(p.name).name.lowercase().replaceFirstChar { it.uppercase() }.let { if (it == "Box") null else "the ${it.lowercase()}" } else null
    else -> if (p.state == PartState.HERE) "now" else null
}

@Composable
private fun Gate(j: Journey, k: Dp, acc: Color, onTap: () -> Unit) {
    val chosen = j.parts.firstOrNull { it.state == PartState.DONE }
    val pulse = rememberInfiniteTransition(label = "gate").animateFloat(1f, 1.06f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")
    Box(Modifier.offset(k * SW / 2 - 39.dp, k * 520f - 39.dp).size(78.dp).scale(pulse.value).clip(CircleShape).background(if (chosen != null) acc else Color.White)
        .border(4.dp, acc, CircleShape).clickable(role = Role.Button, onClickLabel = "Decide") { onTap() }.testTag("gate"), contentAlignment = Alignment.Center) {
        Text(chosen?.name ?: "Decide", style = T.metaStrong.copy(fontSize = 12.sp), color = if (chosen != null) Color.White else acc, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Center(space: KObject, j: Journey, k: Dp) {
    val days = space.targetDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    Column(Modifier.offset(k * SW / 2 - 70.dp, k * 300f - 34.dp).width(140.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (days != null && days >= 0) {
            Text("$days", style = T.title.copy(fontSize = if (j.shape == Shape.COUNTDOWN) 44.sp else 24.sp), color = if (j.shape == Shape.COUNTDOWN) Atlas.DeepPlum else Atlas.Primary)
            Text("days to ${space.targetDate!!.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))}", style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, textAlign = TextAlign.Center)
        } else Text(if (j.shape == Shape.COUNTDOWN) "Set the day in Edit" else j.here?.name ?: "", style = T.meta, color = Atlas.SecondaryText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Letter(s: Snapshot, j: Journey, spots: List<Spot>, k: Dp) {
    val space = s.objects[j.spaceId]
    Column(Modifier.offset(k * 18f + 24.dp, k * 96f + 18.dp).width(k * 354f - 48.dp)) {
        Text("Dear ${space?.title?.removePrefix("Remembering ")?.substringBefore(' ')?.trim(',', '.', ';', ':') ?: ""},", style = T.band.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = Color(0xFF3B2E2A))
    }
    if (spots.isEmpty()) Text("Add a memory with the box below and I’ll write the first chapter from it.", style = T.meta.copy(fontSize = 14.sp), color = Color(0xFF6E5F57),
        modifier = Modifier.offset(k * 18f + 24.dp, k * 96f + 64.dp).width(k * 354f - 48.dp))
    spots.forEachIndexed { i, sp ->
        val items = JourneyItems.inPart(s, j, sp.part.id)
        val line = items.firstOrNull { it.type == ObjType.NOTE }?.body?.take(110) ?: items.firstOrNull()?.title ?: "Add a memory to write this chapter."
        val photo = items.firstOrNull { it.type == ObjType.IMAGE }
        Row(Modifier.offset(k * 18f + 24.dp, k * (sp.y - sp.h / 2) + 6.dp).width(k * 354f - 48.dp)) {
            Text(buildRoman(i + 1) + ". ", style = T.tileQuote, color = Atlas.Primary)
            Column(Modifier.weight(1f)) {
                Text(sp.part.name, style = T.tileQuote, color = Color(0xFF3B2E2A))
                Text(line, style = T.meta.copy(fontSize = 14.sp, lineHeight = 19.sp), color = Color(0xFF3B2E2A), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (photo != null) { val (img, _) = rememberPreview(photo); if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(78.dp, 60.dp).rotate(if (i % 2 == 0) 5f else -4f)
                .border(4.dp, Color.White).shadow(4.dp), contentScale = ContentScale.Crop) }
        }
    }
}
private fun buildRoman(n: Int) = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII").getOrElse(n - 1) { "$n" }

@Composable
private fun ShelfRows(s: Snapshot, j: Journey, spots: List<Spot>, k: Dp) = spots.forEach { sp ->
    val items = JourneyItems.inPart(s, j, sp.part.id).take(5)
    Text(sp.part.name.uppercase(), style = T.tileLabel, color = Color(0xFF6E5F57), modifier = Modifier.offset(k * 34f, k * (sp.y - sp.h / 2) + 4.dp))
    Row(Modifier.offset(k * 34f, k * (sp.y - sp.h / 2) + 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { o -> val (img, _) = rememberPreview(o)
            Box(Modifier.size(58.dp, minOf(k * sp.h - 40.dp, 72.dp)).clip(RoundedCornerShape(10.dp)).background(Color.White)) {
                if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else Text(o.title, style = T.meta.copy(fontSize = 10.sp), modifier = Modifier.padding(4.dp), maxLines = 4)
            } }
        if (items.isEmpty()) Text("Empty shelf", style = T.meta, color = Color(0xFF6E5F57))
    }
}

@Composable
private fun Tributaries(s: Snapshot, j: Journey, k: Dp, onLens: (Bundle) -> Unit) {
    val all = j.parts.flatMap { JourneyItems.bundles(s, j, it.id) }.filter { it.key in setOf("LINKS", "PEOPLE", "NOTES") }.distinctBy { it.key }.take(3)
    val at = listOf(Triple(30f, 220f, false), Triple(360f, 270f, true), Triple(30f, 400f, false))
    all.forEachIndexed { i, b -> val (x, y, right) = at[i]
        Row(Modifier.offset(k * x - (if (right) 140.dp else 8.dp), k * y - 12.dp).width(150.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Open ${b.title}") { onLens(b) }.padding(4.dp), horizontalArrangement = if (right) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically) {
            if (!right) { Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF9DBFE2))); Spacer(Modifier.width(8.dp)) }
            Text(b.title, style = T.metaStrong.copy(fontSize = 12.sp), color = Atlas.DeepPlum, maxLines = 1)
            if (right) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF9DBFE2))) }
        }
    }
}

/** The sheet for the selected part: what it is, its bundles (→ Lens), Atlas's line, and the one action that moves the journey on. */
@Composable
private fun PartSheet(vm: AtlasViewModel, s: Snapshot, j: Journey, sel: String?, acc: Color, onLens: (Bundle) -> Unit, onFinish: () -> Unit, onToast: (Pair<String, (() -> Unit)?>) -> Unit) {
    val p = j.parts.firstOrNull { it.id == sel }
    Column(Modifier.fillMaxWidth().heightIn(max = 230.dp).shadow(10.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.97f))
        .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp).animateContentSize().testTag("part_sheet")) {
        if (sel == GATE) {
            val left = j.parts.filter { it.state != PartState.OUT }
            Text("THE GATE", style = T.tileLabel, color = Atlas.SecondaryText)
            Text("Ready to decide?", style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum)
            AtlasLine("${left.size} ${if (left.size == 1) "option" else "options"} left. Choose one and this Crossroads settles.", Modifier.padding(top = 8.dp))
            return@Column
        }
        if (p == null) { AtlasLine(defaultLine(s, j)); return@Column }
        val i = j.parts.indexOf(p)
        val items = JourneyItems.inPart(s, j, p.id)
        val bundles = JourneyItems.bundles(s, j, p.id)
        Text(kicker(j, p, i).uppercase(), style = T.tileLabel, color = Atlas.SecondaryText)
        Text(p.name, style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum)
        Text(if (items.isEmpty()) "Nothing here yet. Add with the box below." else bundles.joinToString(" · ") { it.title }, style = T.meta.copy(fontSize = 13.sp), color = Atlas.SecondaryText, maxLines = 2)
        if (bundles.isNotEmpty()) Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bundles.forEach { b -> Stack(b) { onLens(b) } }
        }
        AtlasLine(partLine(s, j, p, items), Modifier.padding(top = 10.dp))
        action(j, p, i)?.let { (label, change) ->
            Text(label, style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White, modifier = Modifier.padding(top = 10.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(acc)
                .clickable(role = Role.Button) {
                    val before = j
                    val next = change(j)
                    if (next == null) onFinish() else { vm.repo.setJourney(next); onToast("${p.name}: ${label.lowercase()}" to { vm.repo.setJourney(before) })
                        if (next.parts.all { it.state == PartState.DONE || it.state == PartState.OUT } && j.shape.finishes && j.shape != Shape.CROSSROADS) onFinish() }
                }.padding(horizontal = 18.dp, vertical = 12.dp).testTag("part_action"))
        }
    }
}

@Composable
private fun Stack(b: Bundle, onOpen: () -> Unit) {
    Row(Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4EFFA)).clickable(role = Role.Button, onClickLabel = "Open ${b.title}") { onOpen() }
        .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp).testTag("stack_${b.key}"), verticalAlignment = Alignment.CenterVertically) {
        Box { b.best.take(2).forEachIndexed { n, o -> val (img, _) = rememberPreview(o)
            Box(Modifier.offset(x = (n * 26).dp).size(38.dp).clip(RoundedCornerShape(11.dp)).border(2.dp, Color.White, RoundedCornerShape(11.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else Icon(icon(o.type), null, Modifier.size(18.dp), tint = Atlas.Primary)
            } } }
        Spacer(Modifier.width(if (b.best.size > 1) 76.dp else 50.dp))
        Column { Text(b.title.replaceFirstChar { it.uppercase() }, style = T.metaStrong, color = Atlas.DeepPlum, maxLines = 1)
            Text("${b.items.size} ${if (b.items.size == 1) "item" else "items"}", style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText) }
    }
}

private fun icon(t: ObjType) = when (t) {
    ObjType.PERSON -> Icons.Outlined.Person; ObjType.LINK -> Icons.Outlined.Link; ObjType.TASK -> Icons.Outlined.CheckBox; ObjType.EVENT -> Icons.Outlined.Event
    ObjType.DOCUMENT -> Icons.Outlined.Description; ObjType.IMAGE -> Icons.Outlined.Image; else -> Icons.Outlined.StickyNote2
}

private fun kicker(j: Journey, p: Part, i: Int): String = when (j.shape) {
    Shape.TRAIL -> when (p.state) { PartState.HERE -> "You are here · stop ${i + 1}"; PartState.DONE -> "Finished"; PartState.NEXT -> "Next stop"; else -> if (i == j.parts.lastIndex) "Last stop" else "Later" }
    Shape.TREE -> when (p.state) { PartState.HERE -> "Branch · growing"; PartState.DONE -> "Fruit · finished"; else -> "Bud · an idea" }
    Shape.REMEMBER -> when (j.style) { "GARDEN" -> "Planted in the garden"; "LETTER" -> "Chapter ${buildRoman(i + 1)}"; else -> keepsakeFor(p.name).let { if (it == Keepsake.BOX) "A keepsake" else "The ${it.name.lowercase()}" } }
    Shape.CROSSROADS -> when (p.state) { PartState.OUT -> "Ruled out"; PartState.DONE -> "Chosen"; else -> "Option" }
    Shape.RIVER -> when (p.state) { PartState.DONE -> "A stone you placed"; PartState.HERE -> "Where you are"; else -> "Downstream" }
    Shape.ORBIT -> if (p.state == PartState.HERE) "This cycle" else "Earlier cycle"
    Shape.SHELF -> "Shelf"
    Shape.VOYAGE -> when (p.state) { PartState.HERE -> "Island · today"; PartState.DONE -> "Island · visited"; else -> if (i == j.parts.lastIndex) "After" else "Island" }
    Shape.CLIMB -> when (p.state) { PartState.HERE -> "Camp ${i} · you are here"; PartState.DONE -> "Reached"; else -> if (i == j.parts.lastIndex) "Summit" else "Next camp" }
    Shape.COUNTDOWN -> "Lane"
}

private fun defaultLine(s: Snapshot, j: Journey): String {
    val n = JourneyItems.inSpace(s, j.spaceId).size
    return when (j.shape) {
        Shape.REMEMBER -> "Everything here came from your phone. Tap a ${j.shape.partNoun} to step into it, or tell me one only you remember."
        Shape.TREE -> "Finished branches turn into fruit. Tap a branch to see what’s growing."
        Shape.CROSSROADS -> "Each stream is an option. Ruled-out options dry up. Tap one, or the gate to decide."
        Shape.SHELF -> "Things you keep, not finish. Tap a shelf to see what’s on it."
        else -> if (n == 0) "Nothing here yet. Add photos, files, links or notes with the box below and I’ll place them." else "Tap a ${j.shape.partNoun} to see what’s in it."
    }
}

private fun partLine(s: Snapshot, j: Journey, p: Part, items: List<KObject>): String {
    val people = items.filter { it.type == ObjType.PERSON }.map { it.title.substringBefore(' ') }
    val open = items.count { it.type == ObjType.TASK && !it.done }
    val late = items.count { it.type == ObjType.TASK && !it.done && it.due?.isBefore(LocalDate.now()) == true }
    return when {
        items.isEmpty() -> "When you add something that belongs here, I’ll put it in ${p.name}."
        late > 0 -> "$late ${if (late == 1) "task is" else "tasks are"} late here. Want to open ${if (late == 1) "it" else "them"}?"
        open > 0 -> "$open open ${if (open == 1) "task" else "tasks"} before you can move on."
        people.isNotEmpty() -> "With ${people.take(3).joinToString(", ")}. Tap a pile to look through it."
        j.shape == Shape.REMEMBER -> "${items.size} ${if (items.size == 1) "memory" else "memories"} here. Add one only you have."
        else -> "${items.size} ${if (items.size == 1) "thing" else "things"} here. Tap a pile to look through it."
    }
}

/** The action for a part: label + new journey, or null journey = finish screen. Shapes that never finish get a quieter action. */
private fun action(j: Journey, p: Part, i: Int): Pair<String, (Journey) -> Journey?>? {
    fun set(f: (Int, Part) -> Part) = { jj: Journey -> jj.copy(parts = jj.parts.mapIndexed(f)) }
    val last = i == j.parts.lastIndex
    return when (j.shape) {
        Shape.TRAIL, Shape.CLIMB, Shape.VOYAGE -> when (p.state) {
            PartState.HERE -> if (last) "Finish the journey" to set { n, x -> if (n == i) x.copy(state = PartState.DONE) else x }
                else "Done · move on" to set { n, x -> when (n) { i -> x.copy(state = PartState.DONE); i + 1 -> x.copy(state = PartState.HERE); else -> x } }
            PartState.DONE -> null
            else -> "I’m here now" to set { n, x -> when { n == i -> x.copy(state = PartState.HERE); n < i -> x.copy(state = PartState.DONE); x.state == PartState.HERE -> x.copy(state = PartState.LATER); else -> x } }
        }
        Shape.TREE -> when (p.state) {
            PartState.LATER, PartState.NEXT -> "Grow this into a branch" to set { n, x -> if (n == i) x.copy(state = PartState.HERE) else x }
            PartState.HERE -> "Finish the branch" to set { n, x -> if (n == i) x.copy(state = PartState.DONE) else x }
            else -> null
        }
        Shape.CROSSROADS -> when (p.state) {
            PartState.OUT -> "Bring it back" to set { n, x -> if (n == i) x.copy(state = PartState.LATER) else x }
            PartState.DONE -> null
            else -> if (j.parts.count { it.state != PartState.OUT } <= 1) "Choose ${p.name}" to { _: Journey -> null }
                else "Rule out" to set { n, x -> if (n == i) x.copy(state = PartState.OUT) else x }
        }
        Shape.RIVER -> if (p.state != PartState.DONE) "I learned this" to set { n, x -> if (n == i) x.copy(state = PartState.DONE) else x } else null
        Shape.ORBIT -> if (p.state == PartState.HERE) "Close this cycle" to { _: Journey -> null } else null
        Shape.COUNTDOWN -> if (p.state != PartState.DONE) "Ready" to set { n, x -> if (n == i) x.copy(state = PartState.DONE) else x } else null
        Shape.REMEMBER, Shape.SHELF -> null
    }
}

@Composable
private fun AddSheet(s: Snapshot, j: Journey, item: KObject, acc: Color, onCancel: () -> Unit, onPut: (Part) -> Unit) {
    val guess = JourneyItems.suggestPart(j, item.title + " " + item.body) ?: return
    var pick by remember(item.id) { mutableStateOf(guess) }
    Box(Modifier.fillMaxSize().background(Color(0x59241C33)).clickable(role = Role.Button, onClickLabel = "Cancel") { onCancel() })
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White).then(BlockTaps)
            .navigationBarsPadding().padding(20.dp).testTag("add_sheet")) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDD5EA)))
            Text("You added", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val (img, _) = rememberPreview(item)
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(Atlas.Chip), contentAlignment = Alignment.Center) {
                    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) else Icon(icon(item.type), null, tint = Atlas.Primary) }
                Spacer(Modifier.width(12.dp))
                Column { Text(item.title, style = T.tileTitle, color = Atlas.DeepPlum, maxLines = 2); Text(kind(item), style = T.meta, color = Atlas.SecondaryText) }
            }
            AtlasLine("This fits ${guess.name}. I’d put it there${if (guess.state == PartState.HERE) ", where you are now" else ""}.", Modifier.padding(top = 16.dp))
            Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                j.parts.forEach { p -> val on = p.id == pick.id
                    Text(p.name, style = T.meta.copy(fontSize = 13.sp), color = if (on) Color.White else Atlas.DeepPlum, modifier = Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(17.dp))
                        .background(if (on) acc else Atlas.Chip).clickable(role = Role.RadioButton) { pick = p }.padding(horizontal = 12.dp, vertical = 9.dp)) }
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Put it in ${pick.name}", style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp)).background(acc).clickable(role = Role.Button) { onPut(pick) }.padding(vertical = 14.dp).testTag("add_put"))
                Text("Not now", style = T.meta.copy(fontSize = 14.sp), color = Atlas.DeepPlum, modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
                    .background(Atlas.Chip).clickable(role = Role.Button) { onCancel() }.padding(horizontal = 18.dp, vertical = 14.dp))
            }
        }
    }
}

@Composable
private fun LockedGate(title: String, unlock: () -> Unit) = Column(Modifier.fillMaxSize().background(Atlas.Background).padding(32.dp).testTag("journey_locked"),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Icon(Icons.Outlined.Fingerprint, null, Modifier.size(56.dp), tint = Atlas.Primary)
    Text(title, style = T.band, color = Atlas.DeepPlum, modifier = Modifier.padding(top = 16.dp))
    Text("This journey is locked.", style = T.body, color = Atlas.SecondaryText)
    Text("Unlock", style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White, modifier = Modifier.padding(top = 20.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
        .background(Atlas.Primary).clickable(role = Role.Button) { unlock() }.padding(horizontal = 28.dp, vertical = 14.dp))
    LaunchedEffect(Unit) { unlock() }
}
