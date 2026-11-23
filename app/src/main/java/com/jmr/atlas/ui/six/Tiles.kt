package com.jmr.atlas.ui.six

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.combinedClickable
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

/**
 * A card on a board (0.7.1): each one looks like what it is — a person is their face and name on a pill, a file is its
 * format badge (red PDF, blue DOC) and name, a photo is the photo, a task has its tick box, an idea its bulb, a date its
 * calendar leaf. Tap opens it (a file opens in its app); long-press shows what it's linked to. While links are showing,
 * a tap moves the selection instead.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Tile(vm: AtlasViewModel, o: KObject, today: LocalDate, state: TileState, modifier: Modifier, anchor: Boolean = false, compactAnchor: Boolean = false,
         place: (String, LayoutCoordinates) -> Unit = { _, _ -> }) {
    val s = vm.repo.snap
    val platform = com.jmr.atlas.LocalPlatform.current
    val bring = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val margin = with(LocalDensity.current) { (if (LocalWide.current) 120.dp else 440.dp).toPx() }   // narrow: room below for the cards it links to
    LaunchedEffect(state == TileState.Selected) { if (state == TileState.Selected && size != IntSize.Zero) bring.bringIntoView(Rect(0f, -40f, size.width.toFloat(), size.height + margin)) }
    val alpha by animateFloatAsState(if (state == TileState.Faded) 0.18f else 1f, label = "fade")
    val scale by animateFloatAsState(if (state == TileState.Selected) 1.03f else 1f, label = "lift")
    val tint = stateTint(o, today)
    val isPerson = o.type == ObjType.PERSON && !anchor
    val shape = when {
        isPerson || (anchor && compactAnchor) -> RoundedCornerShape(percent = 50)
        anchor -> RoundedCornerShape(28.dp)
        o.type == ObjType.IDEA -> RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)      // a thought bubble's tail
        else -> RoundedCornerShape(20.dp)
    }
    val bg = tint?.first ?: if (o.type == ObjType.IDEA) Atlas.IdeaPaper else Atlas.Surface
    val ring = when (state) {
        TileState.Selected -> Modifier.border(3.dp, Atlas.Primary, shape)
        TileState.Ringed -> Modifier.border(3.dp, Atlas.Coral, shape)
        TileState.Related -> Modifier.border(2.dp, Atlas.Primary.copy(alpha = 0.45f), shape)
        else -> Modifier
    }
    val exploring = vm.boardSel.collectAsState().value != null
    val openIt: () -> Unit = { openThing(vm, platform, o) }
    Box(modifier.onGloballyPositioned { place(o.id, it); size = it.size }.bringIntoViewRequester(bring).scale(scale).alpha(alpha)
        .then(if (state == TileState.Selected) Modifier.shadow(14.dp, shape) else Modifier)
        .clip(shape).background(bg).then(ring)
        .combinedClickable(role = Role.Button, onClickLabel = if (exploring) "Show its links" else openLabel(o), onLongClickLabel = "Show what it's linked to",
            onClick = { if (exploring) vm.select(o.id) else openIt() }, onLongClick = { vm.select(o.id) })
        .semantics { selected = state == TileState.Selected; contentDescription = "${o.type.label}: ${o.title}"; stateDescription = humanLabel(o, s, today).first }
        .testTag("tile")) {
        when {
            anchor -> AnchorContent(o, s, today, compactAnchor)
            isPerson -> PersonContent(o, s, today)
            o.type == ObjType.IMAGE -> ImageContent(o)
            o.type == ObjType.DOCUMENT || o.type == ObjType.LINK -> FileContent(o)
            o.type == ObjType.TASK -> TaskContent(vm, o, s, today, tint?.second)
            o.type == ObjType.EVENT -> EventContent(o)
            else -> Content(o, s, today)
        }
        // files, links and photos open straight away, so their details sit behind a ⋮
        if (o.type.isFile && !anchor) Box(Modifier.align(Alignment.TopEnd).size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Details of ${o.title}") { vm.go(com.jmr.atlas.ui.Route.Item(o.id)) }.testTag("tile_more"), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Outlined.MoreVert, "Details", Modifier.size(18.dp), tint = if (o.type == ObjType.IMAGE) Color.White else Atlas.SecondaryText)
        }
        val links = LocalLinkCounts.current[o.id] ?: 0
        if (links > 0 && (state == TileState.Normal || state == TileState.Ringed) && !compactAnchor && !isPerson)
            Row(Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Atlas.Chip).padding(horizontal = 6.dp, vertical = 2.dp)
                .semantics { contentDescription = "Linked to $links on this board" }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, Modifier.size(12.dp), tint = Atlas.Primary)
                Spacer(Modifier.width(3.dp))
                Text("$links", style = T.metaStrong.copy(fontSize = 11.sp), color = Atlas.Primary)
            }
    }
}

private fun openLabel(o: KObject) = when (o.type) { ObjType.DOCUMENT, ObjType.IMAGE -> "Open the file"; ObjType.LINK -> "Open the link"; else -> "Open" }

/** Tap on anything: a file opens in its app, a link in the browser, everything else on its page. */
fun openThing(vm: AtlasViewModel, platform: com.jmr.atlas.Platform, o: KObject) = when {
    (o.type == ObjType.DOCUMENT || o.type == ObjType.IMAGE) && o.vaultFile.isNotBlank() -> platform.openFile(o)
    o.type == ObjType.LINK && o.url.isNotBlank() -> platform.launch(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(o.url)))
    else -> vm.open(o.id)
}

@Composable
private fun PersonContent(p: KObject, s: Snapshot, today: LocalDate) {
    val (line, lc) = personLine(p, s, today)
    Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Face(p, if (LocalWide.current) 64.dp else 56.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title, style = T.tileTitle.copy(fontSize = 16.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.role.ifBlank { p.org.ifBlank { "Person" } }, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = T.metaStrong.copy(fontSize = 12.sp), color = lc, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FileContent(o: KObject) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val k = FileKind.of(o)
    val sub = if (o.type == ObjType.LINK) o.site.ifBlank { o.url.substringAfter("://").substringBefore('/') }.removePrefix("www.") else FileKind.facts(o)
    // narrow cards (cover screen): badge on top, the whole name under it — like a file manager's grid
    if (maxWidth < 210.dp && maxHeight >= 110.dp) Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)) {
        FileBadge(k, 38.dp); Spacer(Modifier.height(6.dp))
        Text(FileKind.name(o), style = T.tileTitle.copy(fontSize = 14.sp, lineHeight = 17.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(sub, style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    } else Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 30.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FileBadge(k, 46.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(FileKind.name(o), style = T.tileTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(if (o.type == ObjType.LINK) o.site.ifBlank { o.url.substringAfter("://").substringBefore('/') }.removePrefix("www.") + (if (o.readMinutes > 0) " · ${o.readMinutes} min" else "")
                else listOf(FileKind.facts(o), o.kind.takeIf { it.isNotBlank() && it != "Other" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TaskContent(vm: AtlasViewModel, o: KObject, s: Snapshot, today: LocalDate, ink: Color?) {
    val (label, lc0) = tileLabel(o, s, today)
    val lc = ink ?: lc0
    Row(Modifier.fillMaxSize().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(role = Role.Checkbox, onClickLabel = if (o.done) "Mark not done" else "Mark done") { vm.repo.toggleDone(o.id) }
            .semantics { contentDescription = if (o.done) "Done" else "Not done" }.testTag("tile_check"), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (o.done) Atlas.DoneInk else Color.White).border(2.dp, if (o.done) Atlas.DoneInk else lc.copy(alpha = 0.7f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center) { if (o.done) Icon(androidx.compose.material.icons.Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color.White) }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(label, style = T.tileLabel, color = lc, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(o.title, style = T.tileTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = if (o.done) Atlas.DoneInk else Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (o.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
            }
            Footer(o, s)
        }
    }
}

@Composable
private fun EventContent(o: KObject) {
    val d = o.eventDate
    Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(48.dp).clip(RoundedCornerShape(12.dp)).background(Atlas.Chip), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(d?.month?.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())?.uppercase() ?: "—", style = T.tileLabel.copy(fontSize = 10.sp), color = Color.White,
                modifier = Modifier.fillMaxWidth().background(Atlas.DeepPlum).padding(vertical = 2.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(d?.dayOfMonth?.toString() ?: "?", style = T.band.copy(fontSize = 20.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(o.title, style = T.tileTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = Atlas.DeepPlum, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
    val (label, lc) = tileLabel(o, s, today)
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ic = if (o.kind == "Voice note") Icons.Outlined.Mic else com.jmr.atlas.ui.theme.icon(o.type)
            val ideaInk = if (o.type == ObjType.IDEA) Atlas.IdeaInk else lc
            Icon(ic, null, Modifier.size(16.dp), tint = ideaInk); Spacer(Modifier.width(6.dp))
            Text(label, style = T.tileLabel, color = ideaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val title = if (o.type == ObjType.NOTE && o.kind == "Voice note") "“${o.body.ifBlank { o.title }}”" else o.title
        Column {
            Text(title, style = if (o.type == ObjType.IDEA || o.type == ObjType.NOTE && o.kind == "Voice note") T.tileQuote.copy(fontSize = 16.sp, lineHeight = 20.sp) else T.tileTitle,
                color = Atlas.DeepPlum, maxLines = if (o.type == ObjType.NOTE && o.body.isNotBlank()) 1 else if (LocalWide.current) 2 else 3, overflow = TextOverflow.Ellipsis)
            if (o.type == ObjType.NOTE && o.kind != "Voice note" && o.body.isNotBlank())
                Text(o.body.replace('\n', ' '), style = T.meta.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Atlas.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
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
    Box(Modifier.fillMaxSize().background(Atlas.Chip)) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else FileBadge(o, 44.dp, Modifier.align(Alignment.Center))
        Box(Modifier.fillMaxWidth().height(36.dp).align(Alignment.TopCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))))
        Text(o.title, style = T.metaStrong, color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 9.dp, vertical = 5.dp))
    }
}
