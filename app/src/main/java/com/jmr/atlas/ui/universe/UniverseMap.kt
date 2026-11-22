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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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
    val toggleTask: (String) -> Unit,
    val openFocus: () -> Unit,
    val pickFocus: () -> Unit,
    val back: (() -> Unit)?,
)

/** A placed neighbourhood: rectangle in dp relative to the map's top-left. */
private data class Place(val kind: SlotKind, val x: Float, val y: Float, val w: Float, val h: Float) {
    fun anchorTowards(cx: Float, cy: Float) = Offset(cx.coerceIn(x, x + w), cy.coerceIn(y, y + h))
}

@Composable
fun UniverseMap(snap: Snapshot, focusId: String?, compact: Boolean, actions: MapActions, modifier: Modifier = Modifier) {
    var listMode by rememberSaveable { mutableStateOf(false) }
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(focusId) { scale = 1f; panX = 0f; panY = 0f }
    val tf = rememberTransformableState { z, p, _ -> scale = (scale * z).coerceIn(0.6f, 2.5f); panX += p.x; panY += p.y }
    val focus = focusId?.let { snap.objects[it] }?.takeIf { !it.trashed }

    val focusPill: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actions.back != null) {
                FilledTonalIconButton(actions.back, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Atlas.Surface),
                    modifier = Modifier.semantics { contentDescription = "Previous focus" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                Spacer(Modifier.width(8.dp))
            }
            Pill(if (focus == null) "My Universe" else "Focus: ${focus.title}", Icons.Outlined.CenterFocusStrong, Atlas.Surface, Atlas.DeepPlum,
                Modifier.widthIn(max = 260.dp).testTag("focus_picker"), onClick = actions.pickFocus)
        }
    }
    val listToggle: @Composable () -> Unit = {
        FilledTonalIconButton({ listMode = !listMode }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Atlas.Surface),
            modifier = Modifier.testTag("list_toggle").semantics { contentDescription = if (listMode) "Show as map" else "Show as list" }) {
            Icon(if (listMode) Icons.Outlined.Hub else Icons.AutoMirrored.Outlined.ViewList, null)
        }
    }
    Column(modifier.fillMaxSize().testTag("universe")) {
        // wide: controls sit in their own strip so they never cover a neighbourhood
        if (!compact) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            focusPill()
            Spacer(Modifier.weight(1f))
            if (!listMode) listOf(Triple(Icons.Outlined.Remove, "Zoom out") { scale = (scale / 1.2f).coerceAtLeast(0.6f) },
                Triple(Icons.Outlined.Add, "Zoom in") { scale = (scale * 1.2f).coerceAtMost(2.5f) },
                Triple(Icons.Outlined.FitScreen, "Fit to screen") { scale = 1f; panX = 0f; panY = 0f }).forEach { (ic, d, act) ->
                IconButton(act, Modifier.semantics { contentDescription = d }) { Icon(ic, null, tint = Atlas.SecondaryText) }
            }
            listToggle()
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            if (listMode) UniverseList(snap, focus, actions, Modifier.fillMaxSize(), topPad = if (compact) 72.dp else 8.dp)
            else Box(Modifier.fillMaxSize().transformable(tf).pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; panX = 0f; panY = 0f }) }) {
                AnimatedContent(
                    targetState = focus?.id, label = "refocus",
                    transitionSpec = { (fadeIn(tween(280)) + scaleIn(tween(320), initialScale = 0.9f)) togetherWith (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.06f)) },
                ) { fid ->
                    val f = fid?.let { snap.objects[it] }
                    val slots = remember(snap, fid) { slotsFor(snap, fid) }
                    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = panX; translationY = panY }) {
                        if (compact) CompactMap(snap, f, slots, actions) else WideMap(snap, f, slots, actions)
                    }
                }
            }
            if (compact) {
                Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { focusPill() }
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { listToggle() }
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

// ============================================================ wide (Fold inner / tablet)
private val WIDE = listOf(
    SlotKind.PEOPLE to floatArrayOf(.03f, .02f, .45f, .29f),
    SlotKind.DOCUMENTS to floatArrayOf(.54f, .03f, .43f, .27f),
    SlotKind.TASKS to floatArrayOf(.015f, .365f, .345f, .26f),
    SlotKind.IDEAS to floatArrayOf(.64f, .365f, .345f, .26f),
    SlotKind.PROJECTS to floatArrayOf(.05f, .68f, .42f, .30f),
    SlotKind.TIMELINE to floatArrayOf(.53f, .69f, .44f, .27f),
)

@Composable
private fun WideMap(snap: Snapshot, focus: KObject?, slots: List<Slot>, a: MapActions) = Fitted(500f, 560f) { W, H ->
    val cx = W / 2; val cy = H / 2; val R = (min(W, H) * 0.125f).coerceIn(58f, 104f)
    val places = WIDE.map { (k, f) -> Place(k, f[0] * W, f[1] * H, f[2] * W, f[3] * H) }
    val byKind = slots.associateBy { it.kind }
    Box(Modifier.requiredSize(W.dp, H.dp)) {
        ConstellationCanvas(places.map { it to byKind[it.kind]!! }, cx, cy, R, blobs = true)
        places.forEach { p ->
            val slot = byKind[p.kind]!!
            Box(Modifier.offset(p.x.dp, p.y.dp).size(p.w.dp, p.h.dp).testTag("cluster_${p.kind.name}")) {
                Cluster(slot, focus, p.w, p.h, snap, a)
            }
        }
        FocalNode(focus, snap, R, Modifier.offset((cx - R).dp, (cy - R).dp), a)
    }
}

@Composable
private fun Cluster(slot: Slot, focus: KObject?, w: Float, h: Float, snap: Snapshot, a: MapActions) {
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ClusterHeader(slot, focus) { a.openCluster(slot.kind) }
        Spacer(Modifier.height(4.dp))
        val items = slot.items
        if (items.isEmpty()) {
            Text("None connected", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            TextButton({ a.openCluster(slot.kind) }) { Text("Connect or add") }
            return@Column
        }
        var shown = 0
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            shown = when (slot.kind) {
                SlotKind.PEOPLE, SlotKind.PROJECTS -> {
                    val n = ((w - 20) / 80).toInt().coerceIn(1, 4).coerceAtMost(items.size)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                        items.take(n).forEach { Node(it, if (slot.kind == SlotKind.PEOPLE) 48.dp else 52.dp, Modifier, a) }
                    }; n
                }
                SlotKind.DOCUMENTS -> {
                    val n = ((w - 20) / 78).toInt().coerceIn(1, 4).coerceAtMost(items.size)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items.take(n).forEach { FileTile(it, a) } }; n
                }
                SlotKind.TASKS -> {
                    val n = ((h - 72) / 40).toInt().coerceIn(1, 5).coerceAtMost(items.size)
                    Column(Modifier.fillMaxWidth()) { items.take(n).forEach { TaskLine(it, a, showDue = w >= 200) } }; n
                }
                SlotKind.IDEAS -> {
                    val cols = if (w >= 210) 2 else 1
                    val n = (if (h > 150) 2 * cols else cols).coerceAtMost(items.size)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = cols) {
                        items.take(n).forEach { IdeaCard(it, ((w - 20 - 6 * (cols - 1)) / cols).dp, a) }
                    }; n
                }
                SlotKind.TIMELINE -> { val win = timelineWindow(items, n = if (w < 230) 3 else 4); TimelineStrip(win, a); win.size }
            }
        }
        if (items.size > shown) Text("+${items.size - shown} more", style = MaterialTheme.typography.labelSmall, color = Atlas.Primary,
            modifier = Modifier.align(Alignment.End).offset(y = (-2).dp).clip(CircleShape).clickable { a.openCluster(slot.kind) }.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun ClusterHeader(slot: Slot, focus: KObject?, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Atlas.Surface, contentColor = Atlas.DeepPlum, border = BorderStroke(1.dp, slot.kind.accent.content.copy(alpha = .35f)),
        modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = "${slot.label(focus)}, ${slot.total}. Show all" }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(slot.kind.icon, null, tint = slot.kind.accent.content.takeIf { slot.kind != SlotKind.PROJECTS } ?: Atlas.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(slot.label(focus), style = MaterialTheme.typography.labelLarge)
            if (slot.total > 0) { Spacer(Modifier.width(6.dp)); Text("${slot.total}", style = MaterialTheme.typography.labelMedium, color = Atlas.SecondaryText) }
        }
    }
}

@Composable
private fun Node(it: SlotItem, size: Dp, modifier: Modifier, a: MapActions) {
    val o = it.obj
    Column(modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = "Focus") { a.focus(o.id) }.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        ObjAvatar(o, size, it.proposed, Modifier.border(2.dp, Atlas.Surface, CircleShape))
        Spacer(Modifier.height(2.dp))
        Text(o.title.let { t -> if (o.type == ObjType.PERSON) t.substringBefore(' ') else t }, style = MaterialTheme.typography.labelMedium, maxLines = if (o.type == ObjType.PERSON) 1 else 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        val sub = if (o.type == ObjType.PERSON) o.role else o.projectStatus?.label.orEmpty()
        if (sub.isNotBlank() || it.proposed) Text(if (it.proposed) "Proposed" else sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = if (it.proposed) Atlas.Primary else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FileTile(it: SlotItem, a: MapActions) {
    val o = it.obj
    val ext = when { o.mime == "application/pdf" -> "PDF"; o.mime.startsWith("image/") -> "IMG"; o.type == ObjType.NOTE -> "NOTE"; o.type == ObjType.LINK -> "LINK"; o.mime.startsWith("text/") -> "TXT"; else -> "FILE" }
    Column(Modifier.width(70.dp).clip(RoundedCornerShape(12.dp)).clickable { a.focus(o.id) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            ObjAvatar(o, 46.dp, it.proposed)
            Text(ext, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(if (ext == "PDF") Atlas.CoralText else Atlas.Primary).padding(horizontal = 4.dp, vertical = 1.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(o.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TaskLine(it: SlotItem, a: MapActions, showDue: Boolean) {
    val o = it.obj
    Row(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).clickable { a.focus(o.id) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(o.done, { a.toggleTask(o.id) }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal, uncheckedColor = Atlas.SecondaryText),
            modifier = Modifier.size(40.dp).semantics { contentDescription = if (o.done) "Reopen ${o.title}" else "Complete ${o.title}" })
        Text(o.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            color = if (o.done) Atlas.SecondaryText else Atlas.DeepPlum, textDecoration = if (o.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
        val overdue = !o.done && o.due != null && o.due < LocalDate.now()
        if (showDue && !o.done && o.due != null) Text(relDate(o.due), style = MaterialTheme.typography.labelSmall, color = if (overdue) Atlas.Error else Atlas.SecondaryText, maxLines = 1)
        else if (overdue) Icon(Icons.Outlined.ErrorOutline, "Overdue", tint = Atlas.Error, modifier = Modifier.size(16.dp))
        if (it.proposed) Icon(Icons.Outlined.HelpOutline, "Proposed", tint = Atlas.Primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IdeaCard(it: SlotItem, w: Dp, a: MapActions) {
    val o = it.obj
    Box(Modifier.width(w).heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.Peach.copy(alpha = .9f))
        .then(if (it.proposed) Modifier.dashedBorder(Atlas.Primary, false, 14.dp) else Modifier).clickable { a.focus(o.id) }.padding(10.dp)) {
        Text(o.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, color = Atlas.DeepPlum)
    }
}

@Composable
private fun TimelineStrip(items: List<SlotItem>, a: MapActions) {
    val today = LocalDate.now()
    Box(Modifier.fillMaxWidth().height(78.dp)) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val n = items.size.coerceAtLeast(1); val step = size.width / n; val y = size.height / 2
            drawLine(Atlas.DeepTeal.copy(alpha = .5f), Offset(step / 2, y), Offset(size.width - step / 2, y), 2.dp.toPx())
            items.forEachIndexed { i, s ->
                val x = step * i + step / 2; val past = (s.obj.eventDate ?: LocalDate.MAX) < today
                if (past) drawCircle(Atlas.DeepTeal, 7.dp.toPx(), Offset(x, y))
                else { drawCircle(Atlas.Surface, 7.dp.toPx(), Offset(x, y)); drawCircle(Atlas.Primary, 7.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx())) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 26.dp)) {
            items.forEach { s ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { a.focus(s.obj.id) }.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.obj.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    s.obj.eventDate?.let { Text(relDate(it), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = Atlas.SecondaryText, maxLines = 1) }
                }
            }
        }
    }
}

// ============================================================ compact (phone / Fold cover)
private val COMPACT_ANGLES = mapOf(SlotKind.PEOPLE to -122.0, SlotKind.DOCUMENTS to -58.0, SlotKind.TASKS to 180.0, SlotKind.IDEAS to 0.0, SlotKind.TIMELINE to 122.0, SlotKind.PROJECTS to 58.0)

@Composable
private fun CompactMap(snap: Snapshot, focus: KObject?, slots: List<Slot>, a: MapActions) = Fitted(320f, 420f) { W, H ->
    val cx = W / 2; val cy = H * 0.5f; val R = min(W * 0.17f, H * 0.15f).coerceIn(50f, 80f)
    val tile = if (W < 360) 74f else 82f
    val rx = W / 2 - tile / 2 - 10; val ry = H / 2 - tile / 2 - 16
    val byKind = slots.associateBy { it.kind }
    val places = COMPACT_ANGLES.map { (k, deg) ->
        val t = Math.toRadians(deg); val px = cx + (cos(t) * rx).toFloat(); val py = cy + (sin(t) * ry).toFloat()
        Place(k, px - tile / 2, py - tile / 2, tile, tile)
    }
    Box(Modifier.requiredSize(W.dp, H.dp)) {
        ConstellationCanvas(places.map { it to byKind[it.kind]!! }, cx, cy, R, blobs = false)
        // one representative per neighbourhood, on its connector
        places.forEachIndexed { i, p ->
            val first = byKind[p.kind]!!.items.firstOrNull() ?: return@forEachIndexed
            if (p.kind == SlotKind.TASKS || p.kind == SlotKind.TIMELINE) return@forEachIndexed
            val ax = p.x + p.w / 2; val ay = p.y + p.h / 2
            val mx = cx + (ax - cx) * 0.56f; val my = cy + (ay - cy) * 0.56f
            val len = hypot(ax - cx, ay - cy); val off = if (i % 2 == 0) 16f else -16f
            if (len * 0.56f - 21f < R + 10f || len * 0.44f - 21f < tile / 2) return@forEachIndexed   // would collide with the focus or the tile
            val nx = mx + (-(ay - cy) / len) * off; val ny = my + ((ax - cx) / len) * off
            Box(Modifier.offset((nx - 21).dp, (ny - 21).dp).size(42.dp).clip(CircleShape).clickable(onClickLabel = "Focus ${first.obj.title}") { a.focus(first.obj.id) }) {
                ObjAvatar(first.obj, 42.dp, first.proposed, Modifier.border(2.dp, Atlas.Surface, CircleShape))
            }
        }
        places.forEach { p ->
            val s = byKind[p.kind]!!
            Surface(onClick = { a.openCluster(p.kind) }, shape = RoundedCornerShape(24.dp), color = s.kind.accent.container.copy(alpha = if (s.kind == SlotKind.PROJECTS) 1f else .92f),
                border = BorderStroke(1.dp, Atlas.Surface), shadowElevation = 1.dp,
                modifier = Modifier.offset(p.x.dp, p.y.dp).size(p.w.dp).testTag("cluster_${p.kind.name}").semantics { contentDescription = "${s.label(focus)}, ${s.total}" }) {
                Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(s.kind.icon, null, tint = if (s.kind == SlotKind.PROJECTS) Atlas.Primary else s.kind.accent.content, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(s.label(focus).replace("Related ", "Related\n"), style = if (tile < 80) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, softWrap = s.kind == SlotKind.PROJECTS, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 15.sp)
                    if (s.kind != SlotKind.TIMELINE || s.total > 0) Text("${s.total}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = Atlas.SecondaryText)
                }
            }
        }
        FocalNode(focus, snap, R, Modifier.offset((cx - R).dp, (cy - R).dp), a)
    }
}

// ============================================================ shared drawing
@Composable
private fun ConstellationCanvas(places: List<Pair<Place, Slot>>, cx: Float, cy: Float, R: Float, blobs: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val d = density
        val c = Offset(cx * d, cy * d)
        // soft halo behind the focus
        drawCircle(Brush.radialGradient(listOf(Atlas.PrimaryContainer.copy(alpha = .55f), Color.Transparent), c, R * 3.2f * d), R * 3.2f * d, c)
        // decorative star dust (fixed seed — stable across recompositions)
        val rnd = java.util.Random(7)
        repeat(46) { drawCircle(if (it % 3 == 0) Atlas.DeepTeal.copy(alpha = .28f) else Atlas.Orchid.copy(alpha = .55f), (1.4f + rnd.nextFloat() * 2.2f) * d, Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)) }
        if (blobs) places.forEachIndexed { i, (p, s) ->
            val path = organic(p.x * d - 8 * d, p.y * d - 6 * d, (p.w + 16) * d, (p.h + 12) * d, i)
            drawPath(path, s.kind.accent.container.copy(alpha = if (s.kind == SlotKind.PROJECTS) .55f else .5f))
            drawPath(path, Atlas.Surface.copy(alpha = .9f), style = Stroke(1.5f * d))
        }
        places.forEachIndexed { i, (p, s) ->
            val anchor = if (blobs) p.anchorTowards(cx, cy).let { Offset(it.x * d, it.y * d) } else Offset((p.x + p.w / 2) * d, (p.y + p.h / 2) * d)
            val dir = anchor - c; val len = dir.getDistance(); if (len < 1f) return@forEachIndexed
            val u = dir / len; val start = c + u * (R + 4) * d
            val perp = Offset(-u.y, u.x) * (if (i % 2 == 0) 1f else -1f)
            val c1 = start + (anchor - start) * .35f + perp * len * .10f
            val c2 = start + (anchor - start) * .72f - perp * len * .06f
            val path = Path().apply { moveTo(start.x, start.y); cubicTo(c1.x, c1.y, c2.x, c2.y, anchor.x, anchor.y) }
            val allProposed = s.items.isNotEmpty() && s.items.all { it.proposed }
            val empty = s.items.isEmpty()
            drawPath(path, if (empty) Atlas.Border else Atlas.Orchid, style = Stroke(width = (if (empty) 1.2f else 2f) * d,
                pathEffect = if (allProposed || empty) PathEffect.dashPathEffect(floatArrayOf(7 * d, 5 * d)) else null, cap = StrokeCap.Round))
            // filament twigs with nodes, like neurons branching off the main connector
            if (!empty) {
                val mid = bez(start, c1, c2, anchor, .5f)
                val twig = mid + perp * 26f * d + u * 8f * d
                drawLine(Atlas.Orchid.copy(alpha = .6f), mid, twig, 1.2f * d)
                drawCircle(Atlas.DeepTeal.copy(alpha = .75f), 2.6f * d, twig)
                dot(bez(start, c1, c2, anchor, .5f), Atlas.Primary, 3.4f * d)
            }
            dot(anchor, if (empty) Atlas.Border else Atlas.DeepTeal, 4.2f * d)
        }
        // focus ring glow
        drawCircle(Atlas.Orchid.copy(alpha = .35f), (R + 12) * d, c)
        drawCircle(Atlas.PrimaryContainer.copy(alpha = .8f), (R + 6) * d, c)
    }
}

private fun DrawScope.dot(p: Offset, color: Color, r: Float) { drawCircle(Atlas.Surface, r + 2f * density, p); drawCircle(color, r, p) }
private fun bez(a: Offset, b: Offset, c: Offset, d: Offset, t: Float): Offset {
    val m = 1 - t
    return a * (m * m * m) + b * (3 * m * m * t) + c * (3 * m * t * t) + d * (t * t * t)
}

/** Squircle with gentle deterministic wobble: covers the rectangle's corners (content never spills) yet reads as organic. */
private fun organic(x: Float, y: Float, w: Float, h: Float, seed: Int): Path {
    val n = 18; val cx = x + w / 2; val cy = y + h / 2
    val pts = (0 until n).map { i ->
        val t = 2 * PI * i / n
        val ct = cos(t); val st = sin(t)
        val r = (abs(ct).pow(4) + abs(st).pow(4)).pow(-0.25)
        val j = 1.0 + 0.045 * sin(t * 3 + seed * 1.7) + 0.03 * cos(t * 5 + seed)
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
        modifier.size((R * 2).dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Atlas.Primary, Atlas.DeepPlum)))
            .border(3.dp, Atlas.Orchid, CircleShape)
            .clickable(onClickLabel = if (focus == null) "Choose a focus" else "Open ${focus.title}") { if (focus == null) a.pickFocus() else a.openFocus() }
            .testTag("focal"),
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Atlas.DeepPlum.copy(alpha = .78f)))))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp).padding(top = if (thumb != null) (R * 0.5f).dp else 0.dp)) {
            if (thumb == null) {
                if (focus == null) AtlasMark((R * 0.5f).dp) else Icon(icon(focus.type), null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size((R * 0.42f).dp))
                Spacer(Modifier.height(4.dp))
            }
            Text(focus?.title ?: "My Universe", color = Color.White, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = (if (R > 80) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall).copy(lineHeight = if (R > 80) 24.sp else 19.sp))
        }
    }
}

// ============================================================ accessible list alternative
@Composable
fun UniverseList(snap: Snapshot, focus: KObject?, a: MapActions, modifier: Modifier = Modifier, topPad: Dp = 72.dp) {
    val slots = remember(snap, focus?.id) { slotsFor(snap, focus?.id) }
    LazyColumn(modifier.testTag("universe_list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad, bottom = 120.dp)) {
        item {
            Text(focus?.title ?: "My Universe", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(if (focus == null) "Everything in Atlas, by kind" else "Everything connected to ${focus.title}", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        }
        slots.forEach { s ->
            item(key = "h" + s.kind) {
                SectionLabel("${s.label(focus)} · ${s.total}", Modifier.semantics { heading() }) { TextButton({ a.openCluster(s.kind) }) { Text(if (s.total == 0) "Add" else "Show all") } }
            }
            items(s.items.take(6), key = { s.kind.name + it.obj.id }) { it ->
                ObjRow(it.obj, snap, proposed = it.proposed, onToggle = { a.toggleTask(it.obj.id) }) { a.focus(it.obj.id) }
            }
        }
    }
}
