package com.jmr.atlas.ui.six

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.data.Board
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A space (design 0.6): a project board grouped into bands, or one of the special spaces (People, Files, Ideas & notes,
 * Tasks, Everything). Readable before you tap — each band says who and what it's about and how it's doing. Tap a tile
 * and lines draw to exactly what it's linked to while everything else fades; Open goes to the full page.
 */
@Composable
fun SpaceScreen(vm: AtlasViewModel, snap: Snapshot, key: String) {
    val wide = LocalWide.current
    val today = LocalDate.now()
    val project = if (key.startsWith("p:")) snap.objects[key.removePrefix("p:")]?.takeIf { !it.trashed } else null
    val kind = Board.kind(key)
    if (project == null && kind == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val bands = remember(snap, key) { if (project != null) Board.bands(snap, project.id, today) else Board.kindBands(snap, kind!!, today) }
    val sel by vm.boardSel.collectAsState()
    val hl by vm.highlight.collectAsState()
    if (project != null) LaunchedEffect(project.id, bands.size) { vm.nameBands(project.id) }

    val onBoard = remember(bands) { bands.flatMap { b -> b.items + listOfNotNull(b.anchor) }.map { it.id }.toSet() }
    val selected = sel?.takeIf { it in onBoard }
    val related = remember(snap, selected) { selected?.let { s -> snap.neighbours(s, includeProposed = false).map { it.first.id }.filter { it in onBoard && it != s }.toSet() }.orEmpty() }
    val ringed = hl?.second?.filter { it in onBoard }?.toSet()?.takeIf { it.isNotEmpty() && selected == null }
    val expanded = remember(key) { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxSize().testTag("space")) {
        SpaceHeader(vm, snap, project, kind, today, wide)
        if (ringed != null && hl != null) {
            Row(Modifier.padding(horizontal = pad).padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Atlas.DeepPlum).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Ringed: what Atlas used to answer “${hl!!.first.take(60)}”", style = T.meta.copy(fontSize = 14.sp), color = Atlas.Background, modifier = Modifier.weight(1f), maxLines = 2)
                Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.12f)).clickable(role = Role.Button) { vm.highlight.value = null }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center) { Text("Clear", style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White) }
            }
        }
        val rects = remember(key) { mutableStateMapOf<String, Rect>() }
        var origin by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = pad).padding(top = 12.dp, bottom = 150.dp)) {
                if (project != null && bands.isNotEmpty()) {
                    val anyAi = bands.any { it.nameOrigin == Origin.AI }
                    AtlasDid(when { anyAi && wide -> "Grouped and named by Atlas · tap a name to change it"; anyAi -> "Named by Atlas · tap a name to change it"
                        wide -> "Grouped by who and what they’re about · most urgent first"; else -> "Grouped by who and what · most urgent first" }, Modifier.padding(start = 4.dp, bottom = 10.dp))
                }
                if (bands.isEmpty()) Empty(project != null)
                Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.positionInRoot() }) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        bands.forEach { b ->
                            BandView(vm, b, project, wide, today, selected, related, ringed, expanded[b.key] == true, { expanded[b.key] = !(expanded[b.key] ?: false) }) { id, c ->
                                val p = c.positionInRoot(); rects[id] = Rect(p.x, p.y, p.x + c.size.width, p.y + c.size.height)
                            }
                        }
                    }
                    if (selected != null) Threads(rects, origin, selected, related)
                }
            }
        }
    }
    // the dark caption bar replaces the input box while something is selected (Root draws it)
}

@Composable
private fun Empty(isProject: Boolean) =
    Text(if (isProject) "Nothing here yet. Type below — what you catch while you’re in this space lands here." else "Nothing here yet.",
        style = T.body, color = Atlas.SecondaryText, modifier = Modifier.padding(vertical = 24.dp))

@Composable
private fun SpaceHeader(vm: AtlasViewModel, snap: Snapshot, project: KObject?, kind: Board.Kind?, today: LocalDate, wide: Boolean) {
    Column(Modifier.padding(horizontal = pad).padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", { vm.back() }, Modifier.testTag("back"))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(project?.title ?: kind!!.title, style = if (wide) T.title else T.titleCompact, color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = if (project != null) listOfNotNull(project.body.lineSequence().firstOrNull()?.trim()?.split(Regex("(?<=[.!?;])\\s")).orEmpty().firstOrNull()?.trim()?.trimEnd('.', ';')?.takeIf { it.isNotBlank() }?.let { if (it.length > 70) it.take(68).trimEnd() + "…" else it },
                    project.targetDate?.let { "finish by ${Board.shortDate(it)}" }).joinToString(" · ") else kind!!.blurb
                if (sub.isNotBlank()) Text(sub, style = T.meta, color = Atlas.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (project != null) RoundIcon(Icons.Outlined.Info, "Project details", { vm.go(Route.Item(project.id)) }, Modifier.testTag("project_details"), container = Color.Transparent, tint = Atlas.SecondaryText)
        }
        if (project != null) {
            val marks = remember(snap, project.id) { Board.timeline(snap, project, today) }
            if (marks.isNotEmpty()) { Spacer(Modifier.height(14.dp)); if (wide) Ribbon(marks, today) { id -> id?.let { vm.open(it) } } else RibbonLine(marks, today) }
        }
    }
}

/** The project's timeline as a ribbon: past darker, Today marked, milestones labelled where they fit. */
@Composable
private fun Ribbon(marks: List<Board.Mark>, today: LocalDate, onMark: (String?) -> Unit) {
    val start = marks.first().date; val end = marks.last().date
    val span = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1).toFloat()
    fun f(d: LocalDate) = (ChronoUnit.DAYS.between(start, d) / span).coerceIn(0f, 1f)
    val next = marks.firstOrNull { !it.past && !it.isTarget }
    BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp).testTag("ribbon")) {
        val w = maxWidth
        val todayF = f(today)
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Atlas.Hairline))
        Box(Modifier.padding(top = 14.dp).fillMaxWidth(todayF).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Atlas.DeepPlum))
        var lastEnd = -1f
        // the start label gives way to the first milestone when they'd collide
        val skipStart = marks.size > 1 && marks[0].isStart && (w * f(marks[1].date)).value < 124f
        marks.forEachIndexed { i, m ->
            val x = w * f(m.date)
            val isNext = m == next
            val dot = Modifier.offset(x = (x - 8.dp).coerceIn(0.dp, w - 16.dp), y = 8.dp).size(16.dp).clip(CircleShape)
            Box(when { m.past -> dot.background(Atlas.DeepPlum); isNext -> dot.background(Atlas.Coral); else -> dot.background(Atlas.Surface).border(3.dp, Atlas.DeepPlum, CircleShape) })
            val labelW = 112.dp
            val lx = when (i) { 0 -> 0.dp; marks.lastIndex -> w - labelW; else -> (x - 20.dp).coerceIn(0.dp, w - labelW) }
            val fits = !(i == 0 && skipStart) && (lx.value > lastEnd || i == marks.lastIndex && lx.value > lastEnd - 10)
            if (fits) {
                lastEnd = lx.value + labelW.value - 12
                Column(Modifier.offset(x = lx, y = 30.dp).width(labelW).clickable(enabled = m.id != null, role = Role.Button) { onMark(m.id) },
                    horizontalAlignment = if (i == marks.lastIndex) Alignment.End else Alignment.Start) {
                    Text(Board.shortDate(m.date), style = T.meta.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Atlas.SecondaryText)
                    Text(m.title, style = T.metaStrong.copy(fontSize = 12.sp, lineHeight = 15.sp), color = if (isNext) Atlas.CoralText else Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (today.isAfter(start) && today.isBefore(end)) Box(Modifier.offset(x = (w * todayF - 26.dp).coerceIn(0.dp, w - 56.dp), y = 2.dp).height(24.dp).clip(RoundedCornerShape(12.dp)).background(Atlas.Primary).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center) { Text("Today", style = T.metaStrong.copy(fontSize = 12.sp), color = Color.White) }
    }
}

/** On the cover screen the ribbon is a line of words: Today → the next two milestones. */
@Composable
private fun RibbonLine(marks: List<Board.Mark>, today: LocalDate) {
    val upcoming = marks.filter { !it.past }.take(2)
    if (upcoming.isEmpty()) return
    Text(buildString {
        append("Today")
        upcoming.forEach { append("  →  ").append(it.title).append(' ').append(Board.shortDate(it.date)) }
    }, style = T.metaStrong, color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 58.dp).testTag("ribbon"))
}

@Composable
private fun BandView(
    vm: AtlasViewModel, b: Board.Band, project: KObject?, wide: Boolean, today: LocalDate,
    selected: String?, related: Set<String>, ringed: Set<String>?, expanded: Boolean, onToggle: () -> Unit,
    place: (String, LayoutCoordinates) -> Unit,
) {
    val tint = Atlas.bands[b.tint % 4]
    val gap = 10.dp
    val tileH = if (wide) 104.dp else 116.dp
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(if (wide) 28.dp else 24.dp)).background(tint).padding(12.dp).testTag("band")) {
        val header: @Composable () -> Unit = {
            val renamable = b.renamable && project != null
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = renamable, role = Role.Button, onClickLabel = "Rename band") {
                vm.show(Overlay.RenameBand(project!!.id, b.key, b.name)) }, verticalAlignment = Alignment.CenterVertically) {
                Text(b.name, style = T.band, color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("band_name"))
                if (b.nameOrigin == Origin.AI) { Spacer(Modifier.width(6.dp)); Icon(Sparkle, "Named by Atlas", Modifier.size(14.dp), tint = Atlas.Primary) }
            }
        }
        val status: @Composable () -> Unit = { Text(b.status, style = T.metaStrong, color = if (b.late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        // unfolded: name and status on one line; cover screen: status under the name, so neither is cut off
        if (wide) Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { header(); status() }
        else Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp, top = 2.dp)) { header(); status() }
        val cap = if (wide) 8 else 6
        val items = if (expanded || b.items.size <= cap + 1) b.items else b.items.take(cap)
        val more = b.items.size - items.size
        fun state(id: String): TileState = when {
            selected != null -> if (id == selected) TileState.Selected else if (id in related) TileState.Related else TileState.Faded
            ringed != null -> if (id in ringed) TileState.Ringed else TileState.Faded
            else -> TileState.Normal
        }
        val anchor = b.anchor
        if (anchor != null && wide) {
            val cols = 2
            val rows = (items.size + (if (more > 0) 1 else 0) + cols - 1) / cols
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Tile(vm, anchor, today, state(anchor.id), Modifier.width(168.dp).height(tileH * rows.coerceAtLeast(1) + gap * (rows - 1).coerceAtLeast(0)), anchor = true, place = place)
                Grid(vm, items, more, cols, tileH, gap, today, ::state, onToggle, expanded, place, Modifier.weight(1f))
            }
        } else {
            if (anchor != null) { Tile(vm, anchor, today, state(anchor.id), Modifier.fillMaxWidth().height(64.dp), anchor = true, compactAnchor = true, place = place); Spacer(Modifier.height(gap)) }
            Grid(vm, items, more, if (wide) 3 else 2, tileH, gap, today, ::state, onToggle, expanded, place, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Grid(vm: AtlasViewModel, items: List<KObject>, more: Int, cols: Int, tileH: Dp, gap: Dp, today: LocalDate, state: (String) -> TileState,
                 onToggle: () -> Unit, expanded: Boolean, place: (String, LayoutCoordinates) -> Unit, modifier: Modifier) {
    val cells: List<KObject?> = items + if (more > 0 || expanded) listOf(null) else emptyList()
    if (cells.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        cells.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { o ->
                    if (o != null) Tile(vm, o, today, state(o.id), Modifier.weight(1f).height(tileH), place = place)
                    else MoreTile(if (expanded) "Show fewer" else "$more more", Modifier.weight(1f).height(tileH), onToggle)
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoreTile(text: String, modifier: Modifier, onClick: () -> Unit) =
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Atlas.Surface.copy(alpha = 0.55f)).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary)
    }

/** Lines from the selected tile to each tile it's linked to, edge to edge, drawn over the board. */
@Composable
private fun BoxScope.Threads(rects: Map<String, Rect>, origin: Offset, selected: String, related: Set<String>) {
    val a = rects[selected] ?: return
    val alpha by animateFloatAsState(1f, label = "threads")
    val density = LocalDensity.current
    val stroke = with(density) { 3.dp.toPx() }
    val dot = with(density) { 6.dp.toPx() }
    Canvas(Modifier.matchParentSize().semantics { contentDescription = "Lines to ${related.size} connected items" }) {
        val sa = a.translate(-origin)
        related.mapNotNull { rects[it]?.translate(-origin) }.forEach { sb ->
            val horizontal = sb.left >= sa.right - 1 || sb.right <= sa.left + 1
            val (p0, p1) = if (horizontal) {
                if (sb.left >= sa.right - 1) Offset(sa.right, sa.center.y) to Offset(sb.left, sb.center.y) else Offset(sa.left, sa.center.y) to Offset(sb.right, sb.center.y)
            } else {
                if (sb.top >= sa.bottom) Offset(sa.center.x, sa.bottom) to Offset(sb.center.x, sb.top) else Offset(sa.center.x, sa.top) to Offset(sb.center.x, sb.bottom)
            }
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                if (horizontal) { val mx = (p0.x + p1.x) / 2; cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y) } else { val my = (p0.y + p1.y) / 2; cubicTo(p0.x, my, p1.x, my, p1.x, p1.y) }
            }
            drawPath(path, Atlas.Primary.copy(alpha = alpha), style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawCircle(Color.White, dot + stroke / 2, p1); drawCircle(Atlas.Primary, dot - stroke / 3, p1)
            drawCircle(Color.White, dot + stroke / 2, p0); drawCircle(Atlas.Primary, dot - stroke / 3, p0)
        }
    }
}
