package com.jmr.atlas.ui.journey

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.model.Shape
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.cards.rememberPreview
import com.jmr.atlas.ui.six.FileBadge
import com.jmr.atlas.ui.components.ObjAvatar
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Lens (approved board "Lens"): a bundle blooms into a swipeable fan of big tiles, best few first, near-duplicates in one
 * "+N" pile; tap a tile for the item viewer — photo, PDF with facts pulled out, link preview, note, person, task, date —
 * with back to the bundle, previous/next, an Atlas line about the item, and "Ask about this…".
 */
@Composable
fun Lens(vm: AtlasViewModel, snap: Snapshot, crumb: String, b: Bundle, onClose: () -> Unit) {
    var idx by remember(b.key) { mutableStateOf<Int?>(null) }
    var showAll by remember(b.key) { mutableStateOf(false) }
    val shown = if (showAll) b.items else b.best
    BackHandler { if (idx != null) idx = null else onClose() }
    Box(Modifier.fillMaxSize().background(Color(0xF0F3EEFA)).then(BlockTaps).testTag("lens")) {
        AnimatedContent(idx, label = "lens") { i ->
            if (i == null) Strip(snap, crumb, b, shown, showAll, { showAll = true }, onClose) { idx = it }
            else Viewer(vm, snap, b.title, shown, i, onBack = { idx = null }) { idx = it }
        }
    }
}

@Composable
private fun Strip(snap: Snapshot, crumb: String, b: Bundle, shown: List<KObject>, all: Boolean, showAll: () -> Unit, onClose: () -> Unit, open: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(crumb, style = T.meta, color = Atlas.SecondaryText)
                Text(b.title.replaceFirstChar { it.uppercase() }, style = T.band.copy(fontSize = 22.sp), color = Atlas.DeepPlum)
                Text(mix(b.items), style = T.meta, color = Atlas.SecondaryText)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable(role = Role.Button, onClickLabel = "Close") { onClose() }.testTag("lens_close"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, "Close", tint = Atlas.SecondaryText)
            }
        }
        AtlasLine(if (b.folded > 0 && !all) "Showing the best ${b.best.size}. ${b.folded} similar ${if (b.folded == 1) "one is" else "ones are"} tucked into one pile at the end."
            else "Everything in this pile, newest first.", Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
        LazyRow(Modifier.fillMaxWidth().height(360.dp), contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            itemsIndexed(shown, key = { _, o -> o.id }) { i, o ->
                Tile(o, Modifier.rotate(listOf(-2f, 1.5f, -1f, 2f, -1.5f)[i % 5]).clickable(role = Role.Button, onClickLabel = "Open ${o.title}") { open(i) }.testTag("lens_tile"))
            }
            if (b.folded > 0 && !all) item {
                Column(Modifier.width(170.dp).height(320.dp).clip(RoundedCornerShape(24.dp)).border(2.dp, Color(0xFFC9B5E6), RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "Show all") { showAll() }.padding(16.dp).testTag("lens_more"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("+${b.folded}", style = T.title.copy(fontSize = 30.sp), color = Atlas.Primary)
                    Text("similar ones, grouped · tap to see all", style = T.meta, color = Atlas.SecondaryText)
                }
            }
        }
        val people = b.items.flatMap { o -> snap.neighbours(o.id, includeProposed = false).map { it.first }.filter { it.type == ObjType.PERSON } }.distinctBy { it.id }.take(4)
        if (people.isNotEmpty()) Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            people.forEach { p -> Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                ObjAvatar(p, 26.dp); Spacer(Modifier.width(6.dp)); Text(p.title.substringBefore(' '), style = T.meta) } }
        }
        Spacer(Modifier.weight(1f))
        Text("Swipe through · tap to open", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 30.dp))
    }
}

@Composable
private fun Tile(o: KObject, modifier: Modifier) {
    val (img, _) = rememberPreview(o)
    Box(modifier.width(230.dp).height(320.dp).shadow(14.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(if (o.type == ObjType.NOTE || o.type == ObjType.IDEA) Color(0xFFFFFDF8) else Color.White)) {
        if (img != null) androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(Modifier.padding(20.dp)) {
            when (o.type) {
                ObjType.PERSON -> { ObjAvatar(o, 72.dp); Spacer(Modifier.height(12.dp)) }
                ObjType.DOCUMENT, ObjType.LINK -> { FileBadge(o, 56.dp); Spacer(Modifier.height(12.dp)) }
                else -> {}
            }
            Text(o.title, style = T.tileTitle, color = Atlas.DeepPlum, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val sub = sub(o); if (sub.isNotBlank()) Text(sub, style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 6.dp), maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
        Text(kind(o), style = T.metaStrong.copy(fontSize = 11.sp), color = Atlas.DeepPlum,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun Viewer(vm: AtlasViewModel, snap: Snapshot, bundleTitle: String, items: List<KObject>, i: Int, onBack: () -> Unit, go: (Int) -> Unit) {
    val o = items[i]; val n = items.size
    val platform = LocalPlatform.current
    Box(Modifier.fillMaxSize().statusBarsPadding().testTag("lens_viewer")) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 6.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClickLabel = "Back to the pile") { onBack() }.padding(horizontal = 8.dp).testTag("lens_back"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Atlas.SecondaryText); Spacer(Modifier.width(4.dp))
                    Text(bundleTitle.replaceFirstChar { it.uppercase() }, style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText, maxLines = 1)
                }
                Spacer(Modifier.weight(1f)); Text("${i + 1} of $n", style = T.meta, color = Atlas.SecondaryText)
            }
            Text(o.title, style = T.band, color = Atlas.DeepPlum, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta(o), style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(horizontal = 20.dp))
            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Body(snap, o) { platform.launch(it) } }
            AtlasLine(ai(snap, o), Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp))
            Row(Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(28.dp)).background(Color.White)
                .clickable(role = Role.Button, onClickLabel = "Ask about this") { vm.askAbout(o.id) }.padding(horizontal = 18.dp).testTag("lens_ask"), verticalAlignment = Alignment.CenterVertically) {
                Text("Ask about this…", style = T.input, color = Atlas.Hint, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Mic, null, tint = Atlas.Primary)
            }
        }
        if (n > 1) {
            NavBtn(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous item", Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) { go((i - 1 + n) % n) }
            NavBtn(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next item", Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) { go((i + 1) % n) }
        }
    }
}

@Composable
private fun Body(snap: Snapshot, o: KObject, launch: (Intent) -> Unit) {
    val (img, pages) = rememberPreview(o)
    when {
        o.type == ObjType.IMAGE && img != null -> androidx.compose.foundation.Image(img, o.title, Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.FillWidth)
        o.type == ObjType.DOCUMENT -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            if (img != null) androidx.compose.foundation.Image(img, o.title, Modifier.weight(0.58f).shadow(12.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.FillWidth)
            else Box(Modifier.weight(0.58f).height(260.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) { FileBadge(o, 72.dp) }
            Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PULLED OUT FOR YOU", style = T.tileLabel, color = Atlas.SecondaryText)
                val facts = snap.detailsOf(o.id, DetailKind.FACT).ifEmpty { snap.detailsOf(o.id, DetailKind.CUSTOM) }.take(4)
                if (facts.isEmpty()) Text(o.extracted.ifBlank { o.body }.take(240).ifBlank { if (pages > 0) "$pages pages" else "No text found yet" }, style = T.meta, color = Atlas.DeepPlum)
                facts.forEach { f -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(10.dp)) {
                    Text(f.label.ifBlank { "Fact" }, style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText)
                    Text(if (f.flag) "••••" else f.value, style = T.metaStrong, color = Atlas.DeepPlum) } }
            }
        }
        o.type == ObjType.LINK -> Column(Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White)) {
            if (img != null) androidx.compose.foundation.Image(img, null, Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
            Column(Modifier.padding(18.dp)) {
                Text(runCatching { Uri.parse(o.url).host.orEmpty().removePrefix("www.") }.getOrDefault(""), style = T.meta, color = Atlas.SecondaryText)
                Text(o.title, style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(top = 4.dp))
                if (o.body.isNotBlank()) Text(o.body.take(280), style = T.meta.copy(fontSize = 14.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(top = 8.dp))
                if (o.url.isNotBlank()) Text("Open in browser", style = T.metaStrong, color = Atlas.Primary, modifier = Modifier.padding(top = 14.dp).heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Atlas.Chip).clickable(role = Role.Button) { launch(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) }.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
        o.type == ObjType.PERSON -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ObjAvatar(o, 120.dp); Spacer(Modifier.height(12.dp))
            Text(listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · "), style = T.body, color = Atlas.SecondaryText)
            listOf(o.phone, o.email).filter { it.isNotBlank() }.forEach { Text(it, style = T.body, color = Atlas.DeepPlum, modifier = Modifier.padding(top = 6.dp)) }
        }
        else -> Column(Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(22.dp)) {
            Text(kind(o), style = T.meta, color = Atlas.SecondaryText)
            Text(o.body.ifBlank { o.title }, style = T.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(top = 10.dp), maxLines = 16, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NavBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) =
    Box(modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).clickable(role = Role.Button, onClickLabel = label) { onClick() }.testTag(if (label.startsWith("Next")) "lens_next" else "lens_prev"),
        contentAlignment = Alignment.Center) { Icon(icon, label, tint = Atlas.SecondaryText) }

@Composable
fun AtlasLine(text: String, modifier: Modifier = Modifier) = Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(15.dp).padding(top = 3.dp), tint = Atlas.Primary)
    Text(text, style = T.meta.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Atlas.DeepPlum)
}

private val dayFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFmt)
fun kind(o: KObject) = when (o.type) {
    ObjType.IMAGE -> "Photo"; ObjType.DOCUMENT -> if (o.mime == "application/pdf") "PDF" else "Document"; ObjType.LINK -> "Link"
    ObjType.NOTE -> "Note"; ObjType.IDEA -> "Idea"; ObjType.DECISION -> "Decision"; ObjType.PERSON -> "Person"; ObjType.TASK -> "Task"
    ObjType.EVENT -> "Date"; else -> o.type.label
}
private fun sub(o: KObject) = when (o.type) {
    ObjType.TASK -> listOfNotNull(if (o.done) "Done" else null, o.due?.format(dayFmt)?.let { "Due $it" }).joinToString(" · ")
    ObjType.EVENT -> o.eventDate?.format(dayFmt).orEmpty()
    ObjType.PERSON -> listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · ")
    else -> o.body.take(200)
}
private fun meta(o: KObject) = listOf(kind(o), day(o.created), if (o.sizeBytes > 0) "${(o.sizeBytes / 1024).coerceAtLeast(1)} KB" else "").filter { it.isNotBlank() }.joinToString(" · ")
private fun mix(items: List<KObject>) = items.groupBy { kind(it) }.map { (k, l) -> "${l.size} ${k.lowercase()}${if (l.size > 1 && !k.endsWith("s")) "s" else ""}" }.joinToString(" · ")
/** The Atlas line about an item: what it is linked to here, said plainly. Rules for now; the on-device model can refine it. */
private fun ai(s: Snapshot, o: KObject): String {
    val links = s.neighbours(o.id, includeProposed = false).map { it.first }.filter { it.type != ObjType.PROJECT }
    val people = links.filter { it.type == ObjType.PERSON }.map { it.title.substringBefore(' ') }
    val facts = s.detailsOf(o.id, DetailKind.FACT).size
    return when {
        o.type == ObjType.DOCUMENT && facts > 0 -> "I pulled $facts key ${if (facts == 1) "fact" else "facts"} out of this. Ask me anything about it."
        people.isNotEmpty() -> "Linked to ${people.take(3).joinToString(", ")}${if (people.size > 3) " and ${people.size - 3} more" else ""}."
        links.isNotEmpty() -> "Linked to ${links.size} other ${if (links.size == 1) "thing" else "things"} here."
        else -> "Added ${day(o.created)}. Ask me about it, or link it to someone."
    }
}

/** Takes part in hit testing so the screen underneath an overlay gets no taps; consumes nothing, so the overlay’s own buttons work. */
val BlockTaps: Modifier = Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }

