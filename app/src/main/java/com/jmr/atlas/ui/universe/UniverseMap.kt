package com.jmr.atlas.ui.universe

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.icon
import java.time.LocalDate
import kotlin.math.*

class MapActions(
    val focus: (String?) -> Unit,
    val openCluster: (SlotKind) -> Unit,
    val addTo: (SlotKind) -> Unit,
    val toggleTask: (String) -> Unit,
    val openFocus: () -> Unit,
    val pickFocus: () -> Unit,
)

/** Zoom/pan and list mode live outside the map so the toolbar's “Map options” menu can drive them. */
class MapView {
    var scale by mutableFloatStateOf(1f)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)
    var list by mutableStateOf(false)
    fun reset() { scale = 1f; panX = 0f; panY = 0f }
    fun zoom(f: Float) { scale = (scale * f).coerceIn(0.6f, 2.5f) }
}

private data class Place(val kind: SlotKind, val x: Float, val y: Float, val w: Float, val h: Float) {
    fun anchorTowards(cx: Float, cy: Float) = Offset(cx.coerceIn(x, x + w), cy.coerceIn(y, y + h))
}

/** Size rules per form factor. Phone sizes keep every label ≥ 12 sp at the phone's own scale. */
private data class Spec(val phone: Boolean) {
    val node = if (phone) 56f else 76f
    val avatar = if (phone) 40.dp else 48.dp
    val file = if (phone) 40.dp else 46.dp
    val taskRow = if (phone) 44f else 40f
    val header = 44f
}

private val WIDE = listOf(
    SlotKind.PEOPLE to floatArrayOf(.03f, .02f, .45f, .28f),
    SlotKind.DOCUMENTS to floatArrayOf(.54f, .02f, .43f, .28f),
    SlotKind.TASKS to floatArrayOf(.01f, .34f, .35f, .31f),
    SlotKind.IDEAS to floatArrayOf(.64f, .34f, .35f, .31f),
    SlotKind.PROJECTS to floatArrayOf(.04f, .68f, .43f, .30f),
    SlotKind.TIMELINE to floatArrayOf(.53f, .68f, .44f, .30f),
)
private val PHONE = listOf(
    SlotKind.PEOPLE to floatArrayOf(.02f, .01f, .47f, .29f),
    SlotKind.DOCUMENTS to floatArrayOf(.51f, .01f, .47f, .29f),
    SlotKind.TASKS to floatArrayOf(.01f, .335f, .36f, .33f),
    SlotKind.IDEAS to floatArrayOf(.63f, .335f, .36f, .33f),
    SlotKind.PROJECTS to floatArrayOf(.02f, .70f, .47f, .29f),
    SlotKind.TIMELINE to floatArrayOf(.51f, .70f, .47f, .29f),
)

@Composable
fun UniverseMap(snap: Snapshot, focusId: String?, phone: Boolean, view: MapView, actions: MapActions, modifier: Modifier = Modifier) {
    LaunchedEffect(focusId) { view.reset() }
    val tf = rememberTransformableState { z, p, _ -> view.zoom(z); view.panX += p.x; view.panY += p.y }
    val focus = focusId?.let { snap.objects[it] }?.takeIf { !it.trashed }
    Box(modifier.fillMaxSize().clipToBounds().testTag("universe")) {
        if (view.list) UniverseList(snap, focus, actions, Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize().transformable(tf).pointerInput(Unit) { detectTapGestures(onDoubleTap = { view.reset() }) }) {
            AnimatedContent(
                targetState = focus?.id, label = "refocus",
                transitionSpec = { (fadeIn(tween(280)) + scaleIn(tween(320), initialScale = 0.9f)) togetherWith (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.06f)) },
            ) { fid ->
                val f = fid?.let { snap.objects[it] }
                val slots = remember(snap, fid) { slotsFor(snap, fid) }
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = view.scale; scaleY = view.scale; translationX = view.panX; translationY = view.panY }) {
                    MapLayout(snap, f, slots, Spec(phone), actions)
                }
            }
        }
    }
}

/** The map's overflow menu: every control has a written label (no bare icons). */
@Composable
fun MapMenu(view: MapView, hasFocus: Boolean, onPick: () -> Unit, onRoot: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, Modifier.testTag("map_menu").semantics { contentDescription = "Map options" }) { Icon(Icons.Outlined.MoreVert, null, tint = Atlas.DeepPlum) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Change focus…") }, { open = false; onPick() }, leadingIcon = { Icon(Icons.Outlined.CenterFocusStrong, null) })
            if (hasFocus) DropdownMenuItem({ Text("Show My Universe") }, { open = false; onRoot() }, leadingIcon = { Icon(Icons.Outlined.Hub, null) })
            DropdownMenuItem({ Text(if (view.list) "Show as map" else "Show as list") }, { open = false; view.list = !view.list },
                leadingIcon = { Icon(if (view.list) Icons.Outlined.Hub else Icons.AutoMirrored.Outlined.ViewList, null) }, modifier = Modifier.testTag("list_toggle"))
            if (!view.list) {
                HorizontalDivider()
                DropdownMenuItem({ Text("Zoom in") }, { view.zoom(1.2f) }, leadingIcon = { Icon(Icons.Outlined.ZoomIn, null) })
                DropdownMenuItem({ Text("Zoom out") }, { view.zoom(1 / 1.2f) }, leadingIcon = { Icon(Icons.Outlined.ZoomOut, null) })
                DropdownMenuItem({ Text("Fit to screen") }, { open = false; view.reset() }, leadingIcon = { Icon(Icons.Outlined.FitScreen, null) })
            }
        }
    }
}

/** Lays the map out at a minimum design size, then scales it down to fit — never clipped on narrow panes. */
@Composable
private fun Fitted(minW: Float, minH: Float, content: @Composable (W: Float, H: Float) -> Unit) = BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val aw = maxWidth.value; val ah = maxHeight.value
    val W = maxOf(aw, minW); val H = maxOf(ah, minH)
    val s = min(aw / W, ah / H).coerceAtMost(1f)
    Box(Modifier.requiredSize(W.dp, H.dp).graphicsLayer { scaleX = s; scaleY = s }) { content(W, H) }
}

@Composable
private fun MapLayout(snap: Snapshot, focus: KObject?, slots: List<Slot>, spec: Spec, a: MapActions) =
    Fitted(if (spec.phone) 340f else 500f, if (spec.phone) 480f else 540f) { W, H ->
        val cx = W / 2; val cy = H / 2
        val R = if (spec.phone) min(W * 0.125f, H * 0.11f).coerceIn(46f, 62f) else (min(W, H) * 0.12f).coerceIn(56f, 100f)
        val places = (if (spec.phone) PHONE else WIDE).map { (k, f) -> Place(k, f[0] * W, f[1] * H, f[2] * W, f[3] * H) }
        val byKind = slots.associateBy { it.kind }
        Box(Modifier.fillMaxSize()) {
            ConstellationCanvas(places.map { it to byKind[it.kind]!! }, cx, cy, R)
            places.forEach { p ->
                Box(Modifier.offset(p.x.dp, p.y.dp).size(p.w.dp, p.h.dp).testTag("cluster_${p.kind.name}")) { Cluster(byKind[p.kind]!!, focus, p.w, p.h, spec, a) }
            }
            FocalNode(focus, snap, R, Modifier.offset((cx - R).dp, (cy - R).dp), a)
        }
    }

@Composable
private fun Cluster(slot: Slot, focus: KObject?, w: Float, h: Float, spec: Spec, a: MapActions) {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ClusterHeader(slot, focus, short = w < 180) { if (slot.items.isEmpty()) a.addTo(slot.kind) else a.openCluster(slot.kind) }
        val items = slot.items
        if (items.isEmpty()) {
            TextButton({ a.addTo(slot.kind) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add", style = MaterialTheme.typography.labelMedium) }
            return@Column
        }
        Spacer(Modifier.height(4.dp))
        val inner = w - 16
        var shown = 0
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            shown = when (slot.kind) {
                SlotKind.PEOPLE, SlotKind.PROJECTS, SlotKind.DOCUMENTS -> {
                    // people need little room (first names); titles need enough width that words never break mid-word
                    val minW = if (slot.kind == SlotKind.PEOPLE) spec.node else if (spec.phone) 76f else 80f
                    val n = ((inner + 4) / (minW + 4)).toInt().coerceIn(1, 4).coerceAtMost(items.size)
                    val itemW = ((inner - 4 * (n - 1)) / n).coerceAtMost(minW * 1.35f)
                    val showSub = !spec.phone && h >= 175
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                        items.take(n).forEach { if (slot.kind == SlotKind.DOCUMENTS) FileTile(it, spec, itemW, a) else Node(it, spec, itemW, showSub, a) }
                    }; n
                }
                SlotKind.TASKS -> {
                    val n = ((h - spec.header - 24) / spec.taskRow).toInt().coerceIn(1, 5).coerceAtMost(items.size)
                    Column(Modifier.fillMaxWidth()) { items.take(n).forEach { TaskLine(it, spec, showDue = inner >= 190, a) } }; n
                }
                SlotKind.IDEAS -> {
                    val cols = if (inner >= 200) 2 else 1
                    val rows = ((h - spec.header - 24) / 58).toInt().coerceIn(1, 3)
                    val n = (rows * cols).coerceAtMost(items.size)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = cols) {
                        items.take(n).forEach { IdeaCard(it, ((inner - 6 * (cols - 1)) / cols).dp, a) }
                    }; n
                }
                SlotKind.TIMELINE -> { val win = timelineWindow(items, n = (inner / 78).toInt().coerceIn(2, 4)); TimelineStrip(win, a); win.size }
            }
        }
        if (items.size > shown) Text("+${items.size - shown} more", style = MaterialTheme.typography.labelSmall, color = Atlas.Primary,
            modifier = Modifier.align(Alignment.End).clip(CircleShape).clickable(onClickLabel = "Show all ${slot.label(focus)}") { a.openCluster(slot.kind) }.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun ClusterHeader(slot: Slot, focus: KObject?, short: Boolean, onClick: () -> Unit) {
    val empty = slot.items.isEmpty()
    Surface(onClick = onClick, shape = CircleShape, color = if (empty) Atlas.Surface.copy(alpha = .7f) else Atlas.Surface, contentColor = if (empty) Atlas.SecondaryText else Atlas.DeepPlum,
        border = BorderStroke(1.dp, slot.kind.accent.content.copy(alpha = if (empty) .15f else .35f)),
        modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = if (empty) "${slot.label(focus)}: none yet. Add" else "${slot.label(focus)}, ${slot.total}. Show all" }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(slot.kind.icon, null, tint = if (slot.kind == SlotKind.PROJECTS) Atlas.Primary else slot.kind.accent.content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (short) slot.kind.label else slot.label(focus), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!empty) { Spacer(Modifier.width(6.dp)); Text("${slot.total}", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText) }
        }
    }
}

@Composable
private fun Node(it: SlotItem, spec: Spec, w: Float, showSub: Boolean, a: MapActions) {
    val o = it.obj
    val size = if (o.type == ObjType.PERSON) spec.avatar else spec.avatar + 4.dp
    Column(Modifier.width(w.dp).clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = "Focus on ${o.title}") { a.focus(o.id) }.padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        ObjAvatar(o, size, it.proposed, Modifier.border(2.dp, Atlas.Surface, CircleShape))
        Spacer(Modifier.height(3.dp))
        Text(if (o.type == ObjType.PERSON) o.title.substringBefore(' ') else o.title, style = MaterialTheme.typography.labelMedium,
            maxLines = if (o.type == ObjType.PERSON) 1 else 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 15.sp)
        val sub = when { it.proposed -> "Proposed"; !showSub -> ""; o.type == ObjType.PERSON -> o.role; else -> "" }
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = if (it.proposed) Atlas.Primary else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

private fun ext(o: KObject) = when { o.mime == "application/pdf" -> "PDF"; o.mime.startsWith("image/") -> "IMG"; o.type == ObjType.NOTE -> "NOTE"; o.type == ObjType.LINK -> "LINK"; o.mime.startsWith("text/") -> "TXT"; else -> "FILE" }

@Composable
private fun FileTile(it: SlotItem, spec: Spec, w: Float, a: MapActions) {
    val o = it.obj; val e = ext(o)
    Column(Modifier.width(w.dp).clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = "Focus on ${o.title}") { a.focus(o.id) }.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            ObjAvatar(o, spec.file, it.proposed)
            Text(e, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).offset(5.dp, 4.dp)
                .clip(RoundedCornerShape(5.dp)).background(if (e == "PDF") Atlas.CoralText else Atlas.Primary).padding(horizontal = 4.dp, vertical = 1.dp).clearAndSetSemantics { })
        }
        Spacer(Modifier.height(5.dp))
        Text(o.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TaskLine(it: SlotItem, spec: Spec, showDue: Boolean, a: MapActions) {
    val o = it.obj
    val overdue = !o.done && o.due != null && o.due < LocalDate.now()
    Row(Modifier.fillMaxWidth().height(spec.taskRow.dp).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Focus on ${o.title}") { a.focus(o.id) },
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(o.done, { a.toggleTask(o.id) }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal, uncheckedColor = Atlas.SecondaryText),
            modifier = Modifier.size(40.dp).semantics { contentDescription = if (o.done) "Reopen ${o.title}" else "Complete ${o.title}" })
        Text(o.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = if (spec.phone) 2 else 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f), lineHeight = 16.sp, color = if (o.done) Atlas.SecondaryText else Atlas.DeepPlum,
            textDecoration = if (o.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
        if (showDue && !o.done && o.due != null) Text(relDate(o.due), style = MaterialTheme.typography.labelSmall, color = if (overdue) Atlas.Error else Atlas.SecondaryText, maxLines = 1)
        else if (overdue) Icon(Icons.Outlined.ErrorOutline, "Overdue", tint = Atlas.Error, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IdeaCard(it: SlotItem, w: Dp, a: MapActions) {
    val o = it.obj
    Box(Modifier.width(w).heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.Peach.copy(alpha = .9f))
        .then(if (it.proposed) Modifier.dashedBorder(Atlas.Primary, false, 14.dp) else Modifier).clickable(onClickLabel = "Focus on ${o.title}") { a.focus(o.id) }.padding(10.dp)) {
        Text(o.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, color = Atlas.DeepPlum, lineHeight = 16.sp)
    }
}

@Composable
private fun TimelineStrip(items: List<SlotItem>, a: MapActions) {
    val today = LocalDate.now()
    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val n = items.size.coerceAtLeast(1); val step = size.width / n; val y = size.height / 2
            drawLine(Atlas.DeepTeal.copy(alpha = .5f), Offset(step / 2, y), Offset(size.width - step / 2, y), 2.dp.toPx())
            items.forEachIndexed { i, s ->
                val x = step * i + step / 2; val past = (s.obj.eventDate ?: LocalDate.MAX) < today
                if (past) drawCircle(Atlas.DeepTeal, 7.dp.toPx(), Offset(x, y))
                else { drawCircle(Atlas.Surface, 7.dp.toPx(), Offset(x, y)); drawCircle(Atlas.Primary, 7.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx())) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            items.forEach { s ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "Focus on ${s.obj.title}") { a.focus(s.obj.id) }.padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.obj.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 15.sp)
                    s.obj.eventDate?.let { Text(it.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = Atlas.SecondaryText, maxLines = 1) }
                }
            }
        }
    }
}

// ============================================================ drawing
@Composable
private fun ConstellationCanvas(places: List<Pair<Place, Slot>>, cx: Float, cy: Float, R: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val d = density
        val c = Offset(cx * d, cy * d)
        drawCircle(Brush.radialGradient(listOf(Atlas.PrimaryContainer.copy(alpha = .55f), Color.Transparent), c, R * 3.2f * d), R * 3.2f * d, c)
        val rnd = java.util.Random(7)
        repeat(40) { drawCircle(if (it % 3 == 0) Atlas.DeepTeal.copy(alpha = .25f) else Atlas.Orchid.copy(alpha = .5f), (1.4f + rnd.nextFloat() * 2f) * d, Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)) }
        places.forEachIndexed { i, (p, s) ->
            val path = organic(p.x * d - 6 * d, p.y * d - 4 * d, (p.w + 12) * d, (p.h + 8) * d, i)
            val empty = s.items.isEmpty()
            drawPath(path, s.kind.accent.container.copy(alpha = if (empty) .18f else .5f))
            drawPath(path, Atlas.Surface.copy(alpha = .9f), style = Stroke(1.5f * d))
        }
        places.forEachIndexed { i, (p, s) ->
            val anchor = p.anchorTowards(cx, cy).let { Offset(it.x * d, it.y * d) }
            val dir = anchor - c; val len = dir.getDistance(); if (len < 1f) return@forEachIndexed
            val u = dir / len; val start = c + u * (R + 4) * d
            val perp = Offset(-u.y, u.x) * (if (i % 2 == 0) 1f else -1f)
            val c1 = start + (anchor - start) * .35f + perp * len * .10f
            val c2 = start + (anchor - start) * .72f - perp * len * .06f
            val path = Path().apply { moveTo(start.x, start.y); cubicTo(c1.x, c1.y, c2.x, c2.y, anchor.x, anchor.y) }
            val empty = s.items.isEmpty(); val allProposed = !empty && s.items.all { it.proposed }
            drawPath(path, if (empty) Atlas.Border else Atlas.Orchid, style = Stroke(width = (if (empty) 1.2f else 2f) * d,
                pathEffect = if (allProposed || empty) PathEffect.dashPathEffect(floatArrayOf(7 * d, 5 * d)) else null, cap = StrokeCap.Round))
            if (!empty) {
                val mid = bez(start, c1, c2, anchor, .5f)
                val twig = mid + perp * 22f * d + u * 6f * d
                drawLine(Atlas.Orchid.copy(alpha = .6f), mid, twig, 1.2f * d)
                drawCircle(Atlas.DeepTeal.copy(alpha = .75f), 2.4f * d, twig)
                dot(mid, Atlas.Primary, 3.2f * d)
            }
            dot(anchor, if (empty) Atlas.Border else Atlas.DeepTeal, 4f * d)
        }
        drawCircle(Atlas.Orchid.copy(alpha = .35f), (R + 11) * d, c)
        drawCircle(Atlas.PrimaryContainer.copy(alpha = .8f), (R + 5) * d, c)
    }
}

private fun DrawScope.dot(p: Offset, color: Color, r: Float) { drawCircle(Atlas.Surface, r + 2f * density, p); drawCircle(color, r, p) }
private fun bez(a: Offset, b: Offset, c: Offset, d: Offset, t: Float): Offset { val m = 1 - t; return a * (m * m * m) + b * (3 * m * m * t) + c * (3 * m * t * t) + d * (t * t * t) }

/** Squircle with gentle deterministic wobble: covers the rectangle's corners yet reads as organic. */
private fun organic(x: Float, y: Float, w: Float, h: Float, seed: Int): Path {
    val n = 18; val cx = x + w / 2; val cy = y + h / 2
    val pts = (0 until n).map { i ->
        val t = 2 * PI * i / n; val ct = cos(t); val st = sin(t)
        val r = (abs(ct).pow(4) + abs(st).pow(4)).pow(-0.25)
        val j = 1.0 + 0.04 * sin(t * 3 + seed * 1.7) + 0.025 * cos(t * 5 + seed)
        Offset((cx + ct * r * j * w / 2).toFloat(), (cy + st * r * j * h / 2).toFloat())
    }
    return Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n) {
            val p0 = pts[(i - 1 + n) % n]; val p1 = pts[i]; val p2 = pts[(i + 1) % n]; val p3 = pts[(i + 2) % n]
            val c1 = p1 + (p2 - p0) / 6f; val c2 = p2 - (p3 - p1) / 6f
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
        close()
    }
}

@Composable
private fun FocalNode(focus: KObject?, snap: Snapshot, R: Float, modifier: Modifier, a: MapActions) {
    val cover = remember(snap, focus?.id) {
        focus?.let { f -> if (f.type == ObjType.IMAGE) f else snap.neighbours(f.id, false).map { it.first }.firstOrNull { it.type == ObjType.IMAGE && it.vaultFile.isNotBlank() } }
    }
    val thumb = rememberThumb(cover, 600)
    Box(
        modifier.size((R * 2).dp).clip(CircleShape).background(Brush.linearGradient(listOf(Atlas.Primary, Atlas.DeepPlum))).border(3.dp, Atlas.Orchid, CircleShape)
            .clickable(onClickLabel = if (focus == null) "Choose a focus" else "Open ${focus.title}") { if (focus == null) a.pickFocus() else a.openFocus() }.testTag("focal"),
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Atlas.DeepPlum.copy(alpha = .78f)))))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp).padding(top = if (thumb != null) (R * 0.5f).dp else 0.dp)) {
            if (thumb == null) {
                if (focus == null) AtlasMark((R * 0.45f).dp) else Icon(icon(focus.type), null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size((R * 0.36f).dp))
                Spacer(Modifier.height(3.dp))
            }
            val title = focus?.title ?: "My Universe"
            val base = if (R > 70) 20f else 15f
            val longest = title.split(' ').maxOf { it.length }.coerceAtLeast(1)
            val fs = minOf(base, (R * 2 - 18) / (longest * 0.6f)).coerceAtLeast(11f)
            Text(title, color = Color.White, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = fs.sp, lineHeight = (fs * 1.2f).sp))
        }
    }
}

// ============================================================ accessible list alternative
@Composable
fun UniverseList(snap: Snapshot, focus: KObject?, a: MapActions, modifier: Modifier = Modifier) {
    val slots = remember(snap, focus?.id) { slotsFor(snap, focus?.id) }
    LazyColumn(modifier.testTag("universe_list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)) {
        item { Text(focus?.title ?: "My Universe", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }) }
        slots.forEach { s ->
            item(key = "h" + s.kind) {
                SectionLabel(if (s.total == 0) s.label(focus) else "${s.label(focus)} · ${s.total}", Modifier.semantics { heading() }) {
                    TextButton({ if (s.total == 0) a.addTo(s.kind) else a.openCluster(s.kind) }) { Text(if (s.total == 0) "Add" else "Show all") }
                }
            }
            items(s.items.take(6), key = { s.kind.name + it.obj.id }) { it ->
                ObjRow(it.obj, snap, proposed = it.proposed, onToggle = { a.toggleTask(it.obj.id) }) { a.focus(it.obj.id) }
            }
        }
    }
}
