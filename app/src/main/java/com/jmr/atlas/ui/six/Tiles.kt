package com.jmr.atlas.ui.six

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.material.icons.outlined.Link
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.data.Board
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.components.rememberThumb
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TileState { Normal, Selected, Related, Faded, Ringed }

/** How many other cards on this board each card is linked to; the board provides it so tiles can show a small badge. */
val LocalLinkCounts = androidx.compose.runtime.staticCompositionLocalOf<Map<String, Int>> { emptyMap() }

/** What a tile is called in one short label above its title ("LATE · 2 DAYS", "VOICE NOTE · 18 NOV"). */
fun tileLabel(o: KObject, s: Snapshot, today: LocalDate): Pair<String, Color> {
    fun date(ms: Long) = Board.shortDate(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()).uppercase()
    return when (o.type) {
        ObjType.TASK -> {
            val d = Board.effective(o)
            when {
                o.done -> "DONE" to Atlas.DeepTeal
                Board.isWaiting(o) -> ("WAITING" + (s.neighbours(o.id, false).map { it.first }.firstOrNull { it.type == ObjType.PERSON }?.let { " ON ${it.firstName.uppercase()}" } ?: "")) to Atlas.BlueText
                d != null && d.isBefore(today) -> "LATE · ${Board.lateBy(d, today).removeSuffix(" late").uppercase()}" to Atlas.CoralText
                o.blocked -> "BLOCKED" + (d?.let { " · DUE " + Board.whenShort(it, today).uppercase() } ?: "") to Atlas.CoralText
                d != null -> "DUE ${Board.whenShort(d, today).uppercase()}" to Atlas.DeepPlum
                else -> "TASK" to Atlas.SecondaryText
            }
        }
        ObjType.NOTE -> "${if (o.kind == "Voice note") "VOICE NOTE" else "NOTE"} · ${date(o.created)}" to Atlas.SecondaryText
        ObjType.IDEA -> "IDEA" to Atlas.SecondaryText
        ObjType.DECISION -> "DECIDED" to Atlas.DeepPlum
        ObjType.DOCUMENT -> listOfNotNull(o.kind.ifBlank { "Document" }.uppercase(), o.docDate?.let { Board.shortDate(it).uppercase() }).joinToString(" · ") to Atlas.SecondaryText
        ObjType.LINK -> (o.site.ifBlank { o.url.substringAfter("://").substringBefore('/') }.removePrefix("www.").uppercase().take(28)) to Atlas.SecondaryText
        ObjType.IMAGE -> "PHOTO" to Atlas.SecondaryText
        ObjType.EVENT -> (o.eventDate?.let { Board.shortDate(it).uppercase() } ?: "MILESTONE") to Atlas.SecondaryText
        ObjType.PERSON -> o.role.uppercase().take(24) to Atlas.SecondaryText
        else -> o.type.label.uppercase() to Atlas.SecondaryText
    }
}

/**
 * Color is state (design 0.7, H): coral for late or blocked, amber for waiting, green for done, white for everything
 * else. Returns background and ink, or null for plain white.
 */
fun stateTint(o: KObject, today: LocalDate): Pair<Color, Color>? {
    if (o.type != ObjType.TASK) return null
    val d = Board.effective(o)
    return when {
        o.done -> Atlas.DoneBg to Atlas.DoneInk
        Board.isWaiting(o) -> Atlas.WaitBg to Atlas.WaitInk
        o.blocked || (d != null && d.isBefore(today)) -> Atlas.LateBg to Atlas.LateInk
        else -> null
    }
}

/** The same label in sentence case for prose ("Waiting on Jamie", "Due 5 Dec", "Late · 1 day"). */
fun humanLabel(o: KObject, s: Snapshot, today: LocalDate): Pair<String, Color> {
    if (o.type != ObjType.TASK) return tileLabel(o, s, today).let { (t, c) -> t.lowercase().replaceFirstChar { it.uppercase() } to c }
    val d = Board.effective(o)
    return when {
        o.done -> "Done" to Atlas.DeepTeal
        Board.isWaiting(o) -> ("Waiting" + (s.neighbours(o.id, false).map { it.first }.firstOrNull { it.type == ObjType.PERSON }?.let { " on ${it.firstName}" } ?: "") + (o.due?.let { " · " + Board.whenShort(it, today) } ?: "")) to Atlas.BlueText
        d != null && d.isBefore(today) -> "Late · ${Board.lateBy(d, today).removeSuffix(" late")}" to Atlas.CoralText
        o.blocked -> "Blocked" + (d?.let { " · due " + Board.whenShort(it, today) } ?: "") to Atlas.CoralText
        d != null -> "Due ${Board.whenShort(d, today)}" to Atlas.SecondaryText
        else -> "No date" to Atlas.SecondaryText
    }
}

/** One line under a person's name: what they owe you, else their next thing, else how much is linked. */
fun personLine(p: KObject, s: Snapshot, today: LocalDate): Pair<String, Color> {
    val nb = s.neighbours(p.id, false).map { it.first }
    val owe = nb.filter { Board.isWaiting(it) && !it.done }.minByOrNull { it.due ?: LocalDate.MAX }
    if (owe != null) return ("Owes you " + Board.shortTitle(owe.title.removePrefix("Expect ").substringBefore(" from ")).lowercase() + (owe.due?.let { " · " + Board.whenShort(it, today) } ?: "")) to Atlas.BlueText
    val next = nb.filter { it.type == ObjType.TASK && !it.done && Board.effective(it) != null }.minByOrNull { Board.effective(it)!! }
    if (next != null) return (Board.shortTitle(next.title) + " · " + Board.whenShort(Board.effective(next)!!, today)) to (if (Board.effective(next)!!.isBefore(today)) Atlas.CoralText else Atlas.SecondaryText)
    val n = nb.count { it.type != ObjType.PROJECT }
    return (if (n == 1) "1 thing linked" else "$n things linked") to Atlas.SecondaryText
}

@Composable
fun Tile(vm: AtlasViewModel, o: KObject, today: LocalDate, state: TileState, modifier: Modifier, anchor: Boolean = false, compactAnchor: Boolean = false,
         place: (String, LayoutCoordinates) -> Unit = { _, _ -> }) {
    val s = vm.repo.snap
    val bring = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val margin = with(LocalDensity.current) { (if (LocalWide.current) 120.dp else 440.dp).toPx() }   // narrow: room below for the cards it links to
    // a tile selected from elsewhere (Home, an answer) scrolls into view, clear of the bar at the bottom
    LaunchedEffect(state == TileState.Selected) { if (state == TileState.Selected && size != IntSize.Zero) bring.bringIntoView(Rect(0f, -40f, size.width.toFloat(), size.height + margin)) }
    val alpha by animateFloatAsState(if (state == TileState.Faded) 0.18f else 1f, label = "fade")
    val scale by animateFloatAsState(if (state == TileState.Selected) 1.03f else 1f, label = "lift")
    val bg = stateTint(o, today)?.first ?: Atlas.Surface
    val shape = RoundedCornerShape(20.dp)
    val ring = when (state) {
        TileState.Selected -> Modifier.border(3.dp, Atlas.Primary, shape)
        TileState.Ringed -> Modifier.border(3.dp, Atlas.Coral, shape)
        TileState.Related -> Modifier.border(2.dp, Atlas.Primary.copy(alpha = 0.45f), shape)
        else -> Modifier
    }
    Box(modifier.onGloballyPositioned { place(o.id, it); size = it.size }.bringIntoViewRequester(bring).scale(scale).alpha(alpha)
        .then(if (state == TileState.Selected) Modifier.shadow(14.dp, shape) else Modifier)
        .clip(shape).background(bg).then(ring)
        .clickable(role = Role.Button, onClickLabel = if (state == TileState.Selected) "Deselect" else "Show connections") { vm.select(o.id) }
        .semantics { selected = state == TileState.Selected; contentDescription = "${o.type.label}: ${o.title}"; stateDescription = humanLabel(o, s, today).first }
        .testTag("tile")) {
        when {
            anchor -> AnchorContent(o, s, today, compactAnchor)
            o.type == ObjType.IMAGE -> ImageContent(o)
            else -> Content(o, s, today)
        }
        val links = LocalLinkCounts.current[o.id] ?: 0
        if (links > 0 && (state == TileState.Normal || state == TileState.Ringed) && !compactAnchor)
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Atlas.Chip).padding(horizontal = 6.dp, vertical = 2.dp)
                .semantics { contentDescription = "Linked to $links on this board" }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, Modifier.size(12.dp), tint = Atlas.Primary)
                Spacer(Modifier.width(3.dp))
                Text("$links", style = T.metaStrong.copy(fontSize = 11.sp), color = Atlas.Primary)
            }
    }
}

@Composable
private fun AnchorContent(p: KObject, s: Snapshot, today: LocalDate, compact: Boolean) {
    val (line, lc) = personLine(p, s, today)
    if (compact) Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Face(p, 40.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.role.ifBlank { "Person" }, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(line, style = T.metaStrong.copy(fontSize = 12.sp), color = lc, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 130.dp))
    } else Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Face(p, 60.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.title, style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(p.role.ifBlank { "Person" }, style = T.meta, color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(line, style = T.metaStrong.copy(fontSize = 12.sp), color = lc, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Content(o: KObject, s: Snapshot, today: LocalDate) {
    val (label, lc0) = tileLabel(o, s, today)
    val lc = stateTint(o, today)?.second ?: lc0
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        if (o.type == ObjType.PERSON) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Face(o, 40.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(o.role.ifBlank { o.org.ifBlank { "Person" } }, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val (line, c) = personLine(o, s, today)
            Text(line, style = T.metaStrong.copy(fontSize = 12.sp), color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (o.kind == "Voice note") Icons.Outlined.Mic else com.jmr.atlas.ui.theme.icon(o.type), null, Modifier.size(14.dp), tint = lc); Spacer(Modifier.width(5.dp))
            Text(label, style = T.tileLabel, color = lc, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val title = if (o.type == ObjType.NOTE && o.kind == "Voice note") "“${o.body.ifBlank { o.title }}”" else o.title
        Text(title, style = if (o.type == ObjType.IDEA || o.type == ObjType.NOTE && o.kind == "Voice note") T.tileQuote.copy(fontSize = 16.sp, lineHeight = 20.sp) else T.tileTitle,
            color = if (o.done) Atlas.DoneInk else Atlas.DeepPlum, maxLines = if (LocalWide.current) 2 else 3, overflow = TextOverflow.Ellipsis)
        Footer(o, s)
    }
}

/** The small extra line at the foot of a tile: steps done, a fact read from a document, a summary. */
@Composable
private fun Footer(o: KObject, s: Snapshot) {
    when (o.type) {
        ObjType.TASK -> {
            val steps = s.detailsOf(o.id, DetailKind.STEP)
            if (steps.isNotEmpty()) Row(Modifier.fillMaxWidth().semantics { contentDescription = "${steps.count { it.flag }} of ${steps.size} steps done" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.take(6).forEach { st -> Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (st.flag) Atlas.DeepTeal else Atlas.Border)) }
            } else Spacer(Modifier.height(1.dp))
        }
        ObjType.DOCUMENT -> {
            val fact = s.detailsOf(o.id, DetailKind.FACT).firstOrNull { it.type == FieldType.MONEY } ?: s.detailsOf(o.id, DetailKind.FACT).firstOrNull()
            if (fact != null) Text("${fact.label} ${fact.value}", style = T.metaStrong.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) else Spacer(Modifier.height(1.dp))
        }
        ObjType.LINK -> if (o.readMinutes > 0) Text("${o.readMinutes} min read", style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText) else Spacer(Modifier.height(1.dp))
        else -> Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun ImageContent(o: KObject) {
    val thumb = rememberThumb(o, 480)
    Box(Modifier.fillMaxSize().background(Atlas.Peach)) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(o.body.ifBlank { "Photo" }, style = T.meta, color = Atlas.CoralText, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(14.dp))
        Text(o.title, style = T.metaStrong, color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 9.dp, vertical = 5.dp))
    }
}
