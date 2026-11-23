package com.jmr.atlas.ui.six

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.customActions
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Search
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.Turn
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.LocalDate

/**
 * The Atlas panel above the input row (design 0.7): one place where you talk to Atlas.
 * Header: who's answering and what it's doing. Body: the conversation, or — while you type — what Atlas reads your
 * line as (results for a name, the sentence for a task, or a note that a question is coming).
 */
@Composable
fun AtlasPanel(vm: AtlasViewModel, showClear: Boolean, maxHeight: Dp = 560.dp, onSwipeDown: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val working by vm.working.collectAsState()
    val drag = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 90.dp.toPx() }
    fun release() { scope.launch { if (onSwipeDown != null && drag.value > threshold) { onSwipeDown(); drag.snapTo(0f) } else drag.animateTo(0f) } }
    // pulling down past the top of the conversation (or on the header) folds it away
    val pull = remember(onSwipeDown) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (drag.value > 0f && available.y < 0f) { val use = maxOf(available.y, -drag.value); scope.launch { drag.snapTo(drag.value + use) }; return androidx.compose.ui.geometry.Offset(0f, use) }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (onSwipeDown == null || available.y <= 0f || source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero
                scope.launch { drag.snapTo(drag.value + available.y * 0.6f) }; return available
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (drag.value > 0f) { release(); return available }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).graphicsLayer { translationY = drag.value; alpha = 1f - (drag.value / (threshold * 3)).coerceIn(0f, 0.5f) }
        .shadow(14.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(Atlas.Background).nestedScroll(pull).testTag("atlas_panel")) {
        if (onSwipeDown != null) Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Atlas.Border))
        }
        Row(Modifier.fillMaxWidth().then(if (onSwipeDown != null) Modifier.draggable(androidx.compose.foundation.gestures.rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) } },
                androidx.compose.foundation.gestures.Orientation.Vertical, onDragStopped = { release() }).semantics { customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction("Hide the conversation") { onSwipeDown(); true }) } else Modifier)
            .padding(start = 20.dp, end = 10.dp, top = if (onSwipeDown != null) 2.dp else 10.dp, bottom = 8.dp).testTag("panel_header"), verticalAlignment = Alignment.CenterVertically) {
            Icon(Sparkle, null, Modifier.size(18.dp), tint = Atlas.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Atlas", style = T.band.copy(fontSize = 17.sp), color = Atlas.DeepPlum)
            Spacer(Modifier.width(10.dp))
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Atlas.Chip).padding(horizontal = 10.dp, vertical = 4.dp).testTag("atlas_status"), verticalAlignment = Alignment.CenterVertically) {
                if (working != null) { CircularProgressIndicator(Modifier.size(11.dp), strokeWidth = 1.5.dp, color = Atlas.Primary); Spacer(Modifier.width(6.dp)) }
                Text(working ?: if (vm.brainReady()) vm.brain.where else "Atlas only · no model", style = T.meta.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = Atlas.SecondaryText, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (showClear) Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = "Clear the conversation") { vm.clearThread() }.padding(horizontal = 12.dp).testTag("clear_thread"),
                contentAlignment = Alignment.Center) { Text("Clear", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary) }
            Box(Modifier.size(40.dp).clip(CircleShape).background(Atlas.Chip).clickable(role = Role.Button, onClickLabel = "Close") { vm.closeCatch() }.semantics { contentDescription = "Close" },
                contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Atlas.DeepPlum) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Atlas.Hairline))
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

// ------------------------------------------------------------------ the conversation

@Composable
fun ThreadView(vm: AtlasViewModel, s: Snapshot, turns: List<Turn>) {
    val scroll = rememberScrollState()
    LaunchedEffect(turns.size, turns.lastOrNull()) { scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.fillMaxWidth().verticalScroll(scroll).testTag("thread"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        turns.forEach { t ->
            when (t) {
                is Turn.You -> YouBubble(t)
                is Turn.Did -> DidCard(vm, s, t)
                is Turn.Mine -> MineView(vm, s, t)
                is Turn.World -> WorldView(vm, s, t, latest = t.id == turns.lastOrNull { it is Turn.World }?.id)
            }
        }
    }
}

private fun guessMime(name: String) = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: ""

@Composable
private fun YouBubble(t: Turn.You) = Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
    Column(Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).background(Atlas.DeepPlum).padding(horizontal = 16.dp, vertical = 10.dp)) {
        t.files.take(4).forEach { n ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                FileBadge(FileKind.of(guessMime(n), n), 26.dp); Spacer(Modifier.width(10.dp))
                Text(n, style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (t.files.size > 4) Text("+${t.files.size - 4} more", style = T.meta.copy(fontSize = 12.sp), color = Color.White.copy(alpha = 0.7f))
        if (t.text.isNotBlank()) Text(t.text, style = T.body.copy(fontSize = 16.sp, lineHeight = 22.sp), color = Color.White, modifier = Modifier.padding(top = if (t.files.isEmpty()) 0.dp else 4.dp))
    }
}

@Composable
private fun DidCard(vm: AtlasViewModel, s: Snapshot, t: Turn.Did) {
    val first = t.ids.firstOrNull()?.let { s.objects[it] }?.takeIf { !it.trashed }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("did"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (t.undone) Atlas.Hairline else Atlas.CoolMint), contentAlignment = Alignment.Center) {
            Icon(if (t.undone) Icons.Outlined.Undo else Icons.Outlined.Check, null, Modifier.size(16.dp), tint = if (t.undone) Atlas.SecondaryText else Atlas.DeepTeal) }
        Spacer(Modifier.width(12.dp))
        Text(if (t.undone) "Undone — ${t.text.replaceFirstChar { it.lowercase() }}" else t.text, style = T.metaStrong.copy(fontSize = 15.sp), color = if (t.undone) Atlas.SecondaryText else Atlas.DeepPlum,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        if (!t.undone && t.undo != null) Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { vm.undoTurn(t.id) }.padding(horizontal = 12.dp).testTag("undo_turn"),
            contentAlignment = Alignment.Center) { Text("Undo", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary) }
        if (!t.undone && first != null) Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Open ${first.title}") { vm.openHit(first.id) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.Hint) }
    }
}

@Composable
private fun MineView(vm: AtlasViewModel, s: Snapshot, a: Turn.Mine) = Column(Modifier.fillMaxWidth().testTag("answer"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(a.text, style = T.heroTitle.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = Atlas.DeepPlum)
    a.ids.mapNotNull { s.objects[it] }.take(6).forEachIndexed { i, o ->
        val (label, lc) = humanLabel(o, s, LocalDate.now())
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) { vm.openHit(o.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
            Text("${i + 1}", style = T.title.copy(fontSize = 20.sp, lineHeight = 24.sp), color = if (lc == Atlas.CoralText) Atlas.CoralText else Atlas.Primary, modifier = Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(o.title.removePrefix("Expect ").replaceFirstChar { it.uppercase() }, style = T.tileTitle, color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(label, style = T.meta, color = lc)
            }
        }
    }
    if (a.refining) Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Atlas.Primary); Spacer(Modifier.width(8.dp))
        Text("Checking with the model on this phone…", style = T.meta, color = Atlas.SecondaryText)
    }
    a.note?.let { Text(it, style = T.meta, color = Atlas.SecondaryText) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AtlasDid(if (a.byAi) "Answered by the model on this phone, from your things" else "Put together by Atlas from your things", Modifier.weight(1f))
        if (a.spaceId != null && a.ids.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Pill("Show on the board", dark = true, tag = "show_on_board") { vm.showOnBoard(a) }
        }
    }
}

@Composable
private fun WorldView(vm: AtlasViewModel, s: Snapshot, t: Turn.World, latest: Boolean = true) = Column(Modifier.fillMaxWidth().testTag("world"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    when {
        t.thinking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Atlas.Primary); Spacer(Modifier.width(10.dp))
            Text("Thinking on this phone…", style = T.body, color = Atlas.SecondaryText)
        }
        t.noModel -> {
            Text(t.text, style = T.body.copy(fontSize = 16.sp, lineHeight = 23.sp), color = Atlas.DeepPlum)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Set up AI", dark = true, tag = "setup_ai") { vm.closeCatch(); vm.go(Route.AiSettings) }
                Pill("Save the question as a note") { vm.saveWorld(t.id, null, null) }
            }
        }
        else -> {
            Text(t.text, style = T.body.copy(fontSize = 16.sp, lineHeight = 23.sp), color = Atlas.DeepPlum)
            if (!t.saved && latest) Row(Modifier.horizontalScroll(rememberScrollState()).testTag("save_offers"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                t.offers.mapNotNull { s.objects[it] }.filter { !it.trashed }.forEach { sp -> SpaceOffer("Save to ${sp.title}", sp) { vm.saveWorld(t.id, sp, null) } }
                DashedPill("+ New space") { vm.setCatch("add to ") }
                Pill("Keep as a note") { vm.saveWorld(t.id, null, null) }
            }
            if (latest) AtlasDid("Answered on this phone · can be wrong · doesn’t know recent events")
        }
    }
}

// ------------------------------------------------------------------ while typing

/** A name or a few words: results as you type, grouped by kind, best match first. Saving is offered last. */
@Composable
fun FindResults(vm: AtlasViewModel, s: Snapshot, text: String, hits: List<Search.Hit>) {
    var only by remember(text) { mutableStateOf<Search.Group?>(null) }
    val groups = remember(hits) { hits.groupBy { Search.group(it.o.type) } }
    val order = listOf(Search.Group.PEOPLE, Search.Group.SPACES, Search.Group.TASKS, Search.Group.FILES, Search.Group.NOTES)
    val platform = LocalPlatform.current
    // people on the phone who aren't in Atlas yet (integrations: contacts are read on demand, never copied wholesale)
    var canRead by remember { mutableStateOf(vm.sync.contacts.canRead()) }
    val phone by produceState(emptyList<com.jmr.atlas.integrations.Contacts.Hit>(), text, canRead) {
        value = if (canRead && text.isNotBlank()) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.sync.contacts.search(text.trim(), 6) } else emptyList()
    }
    val fresh = phone.filter { h -> s.ofType(ObjType.PERSON).none { it.contactKey == h.lookupKey || it.title.equals(h.name, true) } }
    val nameLike = Search.words(text).size in 1..3 && text.none { it.isDigit() }
    val contactsPart: @Composable () -> Unit = {
        if (fresh.isNotEmpty()) Column(Modifier.testTag("phone_contacts"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ON YOUR PHONE", style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 4.dp))
            fresh.forEach { h -> ContactRow(vm, h) }
        } else if (!canRead && nameLike) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Atlas.Surface)
            .clickable(role = Role.Button) { platform.request(listOf(android.Manifest.permission.READ_CONTACTS)) { canRead = it } }.padding(horizontal = 14.dp, vertical = 12.dp).testTag("allow_contacts"),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Contacts, null, Modifier.size(20.dp), tint = Atlas.Primary); Spacer(Modifier.width(12.dp))
            Text("Search your phone contacts too", style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
            Text("Allow", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary)
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("find_results"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (hits.isEmpty()) {
            Text(if (fresh.isEmpty()) "Nothing in Atlas matches “${text.trim()}”." else "Not in Atlas yet — but on your phone:", style = T.body, color = Atlas.SecondaryText)
            contactsPart()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Save as a task", dark = true) { vm.saveLine(text, com.jmr.atlas.capture.Sentence.Kind.TASK) }
                Pill("Save as a note") { vm.saveLine(text, com.jmr.atlas.capture.Sentence.Kind.NOTE) }
            }
            return@Column
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("All ${hits.size}", only == null) { only = null }
            order.filter { groups[it] != null }.forEach { g -> FilterChip("${g.label} ${groups[g]!!.size}", only == g) { only = if (only == g) null else g } }
        }
        val best = hits.first().takeIf { only == null && it.inTitle && (it.o.type == ObjType.PERSON || it.o.type == ObjType.PROJECT) }
        if (best != null) BestMatch(vm, s, best.o, text) { intent -> platform.launch(intent) }
        order.filter { g -> (only == null || only == g) && groups[g] != null }.forEach { g ->
            val list = groups[g]!!.filter { it.o.id != best?.o?.id }
            if (list.isEmpty()) return@forEach
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(g.label.uppercase(), style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
                    if (only == null && list.size > 3) Text("All ${list.size}", style = T.metaStrong.copy(fontSize = 13.sp), color = Atlas.Primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { only = g }.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).padding(horizontal = 12.dp)) {
                    (if (only == null) list.take(3) else list).forEachIndexed { i, h ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Atlas.Hairline))
                        HitRow(vm, s, h, text)
                    }
                }
            }
        }
        contactsPart()
        DashedPill("Not what you meant? Save “${text.trim().take(40)}” as a note") { vm.saveLine(text, com.jmr.atlas.capture.Sentence.Kind.NOTE) }
    }
}

@Composable
private fun BestMatch(vm: AtlasViewModel, s: Snapshot, o: KObject, q: String, launch: (Intent) -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Atlas.Surface).padding(16.dp).testTag("best_match"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (o.type == ObjType.PERSON) Face(o, 56.dp) else SpaceMark(s, o, 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(highlight(o.title, q), style = T.title.copy(fontSize = 22.sp, lineHeight = 26.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle(s, o), style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val phone = o.phone.ifBlank { s.detailsOf(o.id, DetailKind.PHONE).firstOrNull()?.value.orEmpty() }
            val email = o.email.ifBlank { s.detailsOf(o.id, DetailKind.EMAIL).firstOrNull()?.value.orEmpty() }
            Action("Open", dark = true, Modifier.weight(1f)) { vm.openHit(o.id) }
            if (o.type == ObjType.PERSON) {
                Action("Call", enabled = phone.isNotBlank(), modifier = Modifier.weight(1f)) { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                Action("Text", enabled = phone.isNotBlank(), modifier = Modifier.weight(1f)) { launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))) }
                Action("Email", enabled = email.isNotBlank(), modifier = Modifier.weight(1f)) { launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }
            }
        }
    }
}

@Composable
private fun HitRow(vm: AtlasViewModel, s: Snapshot, h: Search.Hit, q: String) {
    val o = h.o
    val (label, lc) = humanLabel(o, s, LocalDate.now())
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open ${o.title}") { vm.openHit(o.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        TypeMark(s, o, 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(highlight(o.title, q), style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(h.why ?: label, style = T.meta.copy(fontSize = 12.sp), color = if (h.why != null) Atlas.SecondaryText else lc, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Also in Atlas": what Atlas already has about a sentence you're about to save. */
@Composable
fun AlsoInAtlas(vm: AtlasViewModel, s: Snapshot, hits: List<Search.Hit>) {
    if (hits.isEmpty()) return
    Column(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Atlas.Background).padding(horizontal = 16.dp, vertical = 12.dp).testTag("also_in_atlas"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ALSO IN ATLAS", style = T.eyebrow, color = Atlas.SecondaryText)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            hits.take(4).forEach { h ->
                Row(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(Atlas.Surface).clickable(role = Role.Button, onClickLabel = "Open ${h.o.title}") { vm.openHit(h.o.id) }
                    .padding(start = 6.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypeMark(s, h.o, 28.dp); Spacer(Modifier.width(8.dp))
                    Text(h.o.title, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                }
            }
        }
    }
}

/** A question is coming: say who'll answer it, and offer the Ask button where the eyes are. */
@Composable
fun QuestionHint(vm: AtlasViewModel, mine: Boolean, inChat: Boolean) {
    val ready = vm.brainReady()
    Row(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp).testTag("question_hint"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(when {
            mine -> "A question about your things — Atlas answers from them."
            inChat -> "Continuing the conversation."
            ready -> "A general question — the model on this phone will answer."
            else -> "A general question — needs the on-device model."
        }, style = T.meta.copy(fontSize = 15.sp), color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
        SendButton("Ask") { vm.send() }
    }
}

// ------------------------------------------------------------------ small parts

/** Type is an icon (design 0.7, H): people are their face, spaces their own color and icon, everything else one ink icon on a quiet chip. */
@Composable
fun TypeMark(s: Snapshot, o: KObject, size: Dp) = when (o.type) {
    ObjType.PERSON -> Face(o, size)
    ObjType.PROJECT, ObjType.COLLECTION -> SpaceMark(s, o, size)
    ObjType.DOCUMENT, ObjType.LINK -> Box(Modifier.size(size), contentAlignment = Alignment.Center) { FileBadge(o, size * 0.92f) }
    ObjType.IMAGE -> {
        val th = com.jmr.atlas.ui.components.rememberThumb(o, 160)
        if (th != null) androidx.compose.foundation.Image(th, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        else Box(Modifier.size(size), contentAlignment = Alignment.Center) { FileBadge(o, size * 0.92f) }
    }
    else -> Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(Atlas.Chip), contentAlignment = Alignment.Center) {
        Icon(com.jmr.atlas.ui.theme.icon(o.type), null, Modifier.size(size * 0.52f), tint = Atlas.DeepPlum)
    }
}

/** A space's mark: the color and icon you chose (or one Atlas picked from its name). */
@Composable
fun SpaceMark(s: Snapshot, o: KObject, size: Dp) {
    val look = com.jmr.atlas.ui.theme.SpaceLook.of(s, o)
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(look.bg), contentAlignment = Alignment.Center) {
        Icon(look.icon, null, Modifier.size(size * 0.52f), tint = look.ink)
    }
}

private fun subtitle(s: Snapshot, o: KObject): String = when (o.type) {
    ObjType.PERSON -> listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Person" } +
        (com.jmr.atlas.data.Briefing.projectOf(s, o)?.let { " · in ${it.title}" } ?: "")
    else -> "Space · " + com.jmr.atlas.data.Board.members(s, o.id).size.let { if (it == 1) "1 thing" else "$it things" }
}

private fun highlight(title: String, q: String) = buildAnnotatedString {
    val ws = Search.words(q).filter { it.length >= 2 }
    var i = 0
    val low = title.lowercase()
    while (i < title.length) {
        val w = ws.firstOrNull { low.startsWith(it, i) && (i == 0 || !low[i - 1].isLetterOrDigit()) }
        if (w != null) { withStyle(SpanStyle(background = Atlas.PrimaryContainer)) { append(title.substring(i, i + w.length)) }; i += w.length }
        else { append(title[i]); i++ }
    }
}

@Composable
private fun FilterChip(text: String, on: Boolean, onClick: () -> Unit) =
    Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Atlas.DeepPlum else Atlas.Surface).border(1.5.dp, if (on) Atlas.DeepPlum else Atlas.Border, RoundedCornerShape(18.dp))
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = text + if (on) ", selected" else "" }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = if (on) Color.White else Atlas.DeepPlum)
    }

@Composable
fun Pill(text: String, dark: Boolean = false, tag: String = "", onClick: () -> Unit) =
    Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).background(if (dark) Atlas.DeepPlum else Atlas.Surface).border(if (dark) 0.dp else 1.5.dp, Atlas.Border, RoundedCornerShape(14.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier), contentAlignment = Alignment.Center) {
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = if (dark) Color.White else Atlas.Primary, maxLines = 1)
    }

@Composable
private fun SpaceOffer(text: String, sp: KObject, onClick: () -> Unit) {
    val look = com.jmr.atlas.ui.theme.SpaceLook.of(null, sp)
    Row(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.Surface).border(1.5.dp, Atlas.Border, RoundedCornerShape(14.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(start = 6.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(look.bg), contentAlignment = Alignment.Center) { Icon(look.icon, null, Modifier.size(15.dp), tint = look.ink) }
        Spacer(Modifier.width(8.dp))
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary, maxLines = 1)
    }
}

@Composable
fun DashedPill(text: String, onClick: () -> Unit) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
    Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp))
        .drawBehind { drawRoundRect(Atlas.Orchid, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = dash)) }
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Action(text: String, dark: Boolean = false, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    Box(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(if (dark) Atlas.DeepPlum else Atlas.Chip).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = T.metaStrong.copy(fontSize = 15.sp), color = when { dark -> Color.White; enabled -> Atlas.DeepPlum; else -> Atlas.Hint })
    }

@Composable
fun SendButton(label: String, onClick: () -> Unit) =
    Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Primary).clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(start = 18.dp, end = 14.dp).testTag("preview_send"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.ArrowUpward, null, Modifier.size(16.dp), tint = Color.White)
    }


@Composable
private fun ContactRow(vm: AtlasViewModel, h: com.jmr.atlas.integrations.Contacts.Hit) {
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Atlas.Surface).clickable(role = Role.Button, onClickLabel = "Add ${h.name} to Atlas and open") {
        scope.launch {
            val id = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.sync.addContact(h.lookupKey) }
            if (id != null) { vm.closeCatch(); vm.catchField.clearText(); vm.open(id); vm.say("${h.name} added from your contacts.") } else vm.say("Couldn't read that contact.")
        }
    }.padding(horizontal = 12.dp, vertical = 10.dp).testTag("contact_row"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Atlas.PrimaryContainer), contentAlignment = Alignment.Center) {
            Text(h.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }, style = T.metaStrong, color = Atlas.Primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(h.name, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(h.sub.ifBlank { "Phone contact" }, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Add", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
