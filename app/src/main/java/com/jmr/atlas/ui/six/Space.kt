package com.jmr.atlas.ui.six

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
    val links = remember(snap, selected) { selected?.let { s -> snap.neighbours(s, includeProposed = false).filter { it.first.id in onBoard && it.first.id != s }.associate { it.first.id to it.second } }.orEmpty() }
    val related = links.keys
    val ringed = hl?.second?.filter { it in onBoard }?.toSet()?.takeIf { it.isNotEmpty() && selected == null }
    val expanded = remember(key) { mutableStateMapOf<String, Boolean>() }
    val linkCounts = remember(snap, onBoard) { onBoard.associateWith { id -> snap.neighbours(id, includeProposed = false).count { it.first.id in onBoard && it.first.id != id } } }

    CompositionLocalProvider(LocalLinkCounts provides linkCounts) {
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
                    if (selected != null) Threads(rects, origin, selected, links)
                }
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

/**
 * Lines from the selected tile to each tile it's linked to. Each line is routed through the gutters between tiles
 * (the route that crosses the fewest other tiles wins), drawn with rounded elbows over a white halo so it stays readable
 * on any band, draws itself in, and carries what the link means ("assigned to", "blocks"). Links Atlas made are dashed.
 */
@Composable
private fun BoxScope.Threads(rects: Map<String, Rect>, origin: Offset, selected: String, links: Map<String, Relation>) {
    val a = rects[selected] ?: return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(selected, links.keys) { progress.snapTo(0f); progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = T.tileLabel.copy(fontSize = 11.sp, letterSpacing = 0.3.sp)
    val desc = links.values.joinToString(", ") { linkLabel(it, selected) ?: "linked" }
    Canvas(Modifier.matchParentSize().semantics { contentDescription = "Lines to ${links.size} connected items: $desc" }) {
        val dp = density.density
        val g = 10f * dp
        val sa = a.translate(-origin)
        val others = rects.filterKeys { it != selected }.mapValues { it.value.translate(-origin) }
        val labels = mutableListOf<Triple<List<Offset>, String, Color>>()
        links.entries.sortedBy { others[it.key]?.top ?: 0f }.forEachIndexed { i, (id, rel) ->
            val sb = others[id] ?: return@forEachIndexed
            val lane = ((i % 3) - 1) * 3f * dp
            val obstacles = others.filterKeys { it != id }.values.toList()
            val pts = bestRoute(sa, sb, obstacles, g, lane, (i % 3 - 1) * 12f * dp)
            val path = rounded(pts, 14f * dp)
            val color = when (rel.type) { RelType.DEPENDS_ON -> Atlas.CoralText; RelType.ASSIGNED_TO, RelType.DISCUSSED_WITH -> Atlas.DeepTeal; else -> Atlas.Primary }
            val pm = PathMeasure().apply { setPath(path, false) }
            val seg = Path().also { pm.getSegment(0f, pm.length * progress.value, it, true) }
            val dashed = rel.origin != Origin.USER
            drawPath(seg, color.copy(alpha = 0.14f), style = Stroke(16f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(seg, Color.White.copy(alpha = 0.95f), style = Stroke(8f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(seg, color, style = Stroke(2.75f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f * dp, 5f * dp)) else null))
            // ports: where the line leaves the selected tile and where it lands
            port(pts.first(), color, dp)
            if (progress.value > 0.97f) port(pts.last(), color, dp)
            // what the link means, on the longest straight run so it never sits on a corner
            linkLabel(rel, selected)?.let { labels += Triple(pts, it, color) }
        }
        // labels last, so no line or port ever sits on top of one; nudged apart when two would overlap
        if (progress.value > 0.6f) {
            val alpha = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
            val placed = mutableListOf<Rect>()
            labels.forEach { (pts, text, color) ->
                val lay = measurer.measure(text, labelStyle)
                val w = lay.size.width + 14f * dp; val h = lay.size.height + 6f * dp
                // centred on the line, or just to its left or right: whichever covers the least of the other cards
                fun covered(r: Rect) = others.values.sumOf { o -> val i = o.intersect(r); if (i.width > 0 && i.height > 0) (i.width * i.height).toDouble() else 0.0 } +
                    placed.sumOf { o -> val i = o.intersect(r); if (i.width > 0 && i.height > 0) 1e7 else 0.0 }
                // every straight run long enough to hold it; on each, centred or to either side; covering least text wins
                val spots = (0 until pts.size - 1).filter { (pts[it + 1] - pts[it]).getDistance() > 24f * dp }.ifEmpty { listOf(0) }.flatMap { k ->
                    val m = Offset((pts[k].x + pts[k + 1].x) / 2, (pts[k].y + pts[k + 1].y) / 2)
                    val offs = listOf(Offset(-w / 2, -h / 2), Offset(-w / 2, -h - 3f * dp), Offset(-w / 2, 3f * dp), Offset(-w + 4f * dp, -h / 2), Offset(-4f * dp, -h / 2))
                    offs.map { o -> Rect(m + o, Size(w, h)) }
                }
                var r = spots.minBy { covered(it) }
                var tries = 0
                while (placed.any { it.overlaps(r) } && tries++ < 6) r = r.translate(0f, h + 4f * dp)
                placed += r
                drawRoundRect(Color.White.copy(alpha = alpha), r.topLeft, r.size, CornerRadius(h / 2))
                drawRoundRect(color.copy(alpha = alpha), r.topLeft, r.size, CornerRadius(h / 2), style = Stroke(1.25f * dp))
                drawText(lay, color.copy(alpha = alpha), Offset(r.left + 7f * dp, r.top + 3f * dp))
            }
        }
    }
}

/** "assigned to", "blocks", "from" — read from the selected tile's side. Plain "related" links carry no label. */
private fun linkLabel(r: Relation, selected: String): String? {
    if (r.type == RelType.RELATED_TO) return null
    val out = r.sourceId == selected
    return when (r.type) {
        RelType.ASSIGNED_TO -> if (out) "assigned to" else "owns"
        RelType.DEPENDS_ON -> if (out) "waits on" else "blocks"
        RelType.DERIVED_FROM -> if (out) "from" else "source"
        RelType.DISCUSSED_WITH -> "discussed"
        RelType.BELONGS_TO, RelType.PART_OF -> if (out) "part of" else "includes"
        RelType.REFERENCES -> if (out) "cites" else "cited by"
        RelType.SUPPORTS -> if (out) "supports" else "backed by"
        RelType.FOLLOW_UP_TO -> if (out) "follows" else "followed by"
        RelType.DUPLICATE_OF -> "same as"
        RelType.RELATED_TO -> null
    }
}

private fun DrawScope.port(p: Offset, color: Color, dp: Float) {
    drawCircle(Color.White, 6.5f * dp, p); drawCircle(color, 4f * dp, p)
}

/** Candidate orthogonal routes between two tiles; the one crossing the fewest other tiles (then shortest) wins. */
private fun bestRoute(a: Rect, b: Rect, obstacles: List<Rect>, g: Float, lane: Float, spread: Float): List<Offset> {
    val c = mutableListOf<List<Offset>>()
    val ax = a.center.x + spread.coerceIn(-a.width / 3, a.width / 3); val bx = b.center.x
    val sameRow = kotlin.math.abs(a.center.y - b.center.y) < minOf(a.height, b.height) / 2
    if (sameRow) {
        val y = (maxOf(a.top, b.top) + minOf(a.bottom, b.bottom)) / 2 + lane
        if (b.left >= a.right) c += listOf(Offset(a.right, y), Offset(b.left, y)) else if (b.right <= a.left) c += listOf(Offset(a.left, y), Offset(b.right, y))
        val under = maxOf(a.bottom, b.bottom) + g / 2 + lane
        c += listOf(Offset(ax, a.bottom), Offset(ax, under), Offset(bx, under), Offset(bx, b.bottom))
        val over = minOf(a.top, b.top) - g / 2 - lane
        c += listOf(Offset(ax, a.top), Offset(ax, over), Offset(bx, over), Offset(bx, b.top))
    } else {
        val down = b.center.y > a.center.y
        val ay0 = if (down) a.bottom else a.top; val by0 = if (down) b.top else b.bottom
        val s = if (down) 1 else -1
        val y1 = ay0 + s * (g / 2 + lane); val y2 = by0 - s * (g / 2 - lane)
        c += listOf(Offset(ax, ay0), Offset(ax, y1), Offset(bx, y1), Offset(bx, by0))
        c += listOf(Offset(ax, ay0), Offset(ax, y2), Offset(bx, y2), Offset(bx, by0))
        for (x in listOf(b.left - g / 2 - lane, b.right + g / 2 + lane, a.left - g / 2 - lane, a.right + g / 2 + lane))
            c += listOf(Offset(ax, ay0), Offset(ax, y1), Offset(x, y1), Offset(x, y2), Offset(bx, y2), Offset(bx, by0))
        // side exit when the tiles don't share columns
        if (b.left >= a.right || b.right <= a.left) {
            val right = b.left >= a.right
            val sx = if (right) a.right else a.left; val ex = if (right) b.left else b.right
            val xm = (sx + ex) / 2 + lane
            c += listOf(Offset(sx, a.center.y + lane), Offset(xm, a.center.y + lane), Offset(xm, b.center.y), Offset(ex, b.center.y))
        }
    }
    /** A vertical first or last run leaves/enters through the side of the tile that faces the rest of the route. */
    fun snap(r: List<Offset>): List<Offset> {
        if (r.size < 3) return r
        val m = r.toMutableList()
        if (m[0].x == m[1].x && m[0].y in listOf(a.top, a.bottom)) {
            val y = if (m[1].y < a.center.y) a.top else a.bottom; m[0] = Offset(m[0].x, y)
        }
        val n = m.size - 1
        if (m[n].x == m[n - 1].x && m[n].y in listOf(b.top, b.bottom)) {
            val y = if (m[n - 1].y < b.center.y) b.top else b.bottom; m[n] = Offset(m[n].x, y)
        }
        return m
    }
    fun cost(r: List<Offset>): Float {
        var hits = 0; var len = 0f
        for (k in 0 until r.size - 1) {
            val p = r[k]; val q = r[k + 1]
            len += (q - p).getDistance()
            val seg = Rect(minOf(p.x, q.x) - 1f, minOf(p.y, q.y) - 1f, maxOf(p.x, q.x) + 1f, maxOf(p.y, q.y) + 1f)
            hits += obstacles.count { o -> o.deflate(3f).overlaps(seg) }
            // the route may touch its own two tiles only at its ports
            if (k > 0) hits += if (a.deflate(3f).overlaps(seg)) 1 else 0
            if (k < r.size - 2) hits += if (b.deflate(3f).overlaps(seg)) 1 else 0
        }
        return hits * 100_000f + len + r.size * 40f
    }
    return c.map { snap(it) }.minBy { cost(it) }
}

/** A polyline with rounded elbows. */
private fun rounded(pts: List<Offset>, radius: Float): Path {
    val p = pts.fold(mutableListOf<Offset>()) { acc, o -> if (acc.isEmpty() || (acc.last() - o).getDistance() > 0.5f) acc += o; acc }
    return Path().apply {
        if (p.isEmpty()) return@apply
        moveTo(p[0].x, p[0].y)
        for (i in 1 until p.size - 1) {
            val prev = p[i - 1]; val cur = p[i]; val next = p[i + 1]
            val d1 = (cur - prev).getDistance(); val d2 = (next - cur).getDistance()
            val r = minOf(radius, d1 / 2, d2 / 2)
            val inP = cur - (cur - prev) * (r / d1); val outP = cur + (next - cur) * (r / d2)
            lineTo(inP.x, inP.y); quadraticTo(cur.x, cur.y, outP.x, outP.y)
        }
        lineTo(p.last().x, p.last().y)
    }
}

private fun longestMid(pts: List<Offset>): Pair<Offset, Boolean> {
    var best = 0; var bl = -1f
    for (k in 0 until pts.size - 1) { val l = (pts[k + 1] - pts[k]).getDistance(); if (l > bl) { bl = l; best = k } }
    val p = pts[best]; val q = pts[best + 1]
    return Offset((p.x + q.x) / 2, (p.y + q.y) / 2) to (kotlin.math.abs(q.x - p.x) > kotlin.math.abs(q.y - p.y))
}

