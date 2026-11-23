package com.jmr.atlas.ui.six

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jmr.atlas.data.Board
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.SpaceLook
import com.jmr.atlas.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * All spaces (design 0.7, F): a filter, Pinned, This week, spaces that hold other spaces, the rest, and Quiet (nothing
 * touched in a month) folded away. Each space has its own color and icon — tap the mark to change it.
 */
@Composable
fun AllSpacesScreen(vm: AtlasViewModel, snap: Snapshot) {
    val today = LocalDate.now()
    var q by remember { mutableStateOf("") }
    var showQuiet by remember { mutableStateOf(false) }
    var lookFor by remember { mutableStateOf<KObject?>(null) }
    val all = remember(snap) { Board.allSpaces(snap) }
    val pinned = remember(snap) { Board.pinned(snap, today) }
    val pinnedIds = pinned.map { it.first.id }.toSet()
    val active = remember(snap) { Board.active(snap, today, pinnedIds) }
    val activeIds = active.map { it.id }.toSet()
    val autoIds = pinned.filter { it.second }.map { it.first.id }.toSet()
    val topLevel = all.filter { Board.parentOf(snap, it) == null }
    val holders = topLevel.filter { it.id !in pinnedIds && it.id !in activeIds && Board.inside(snap, it.id).isNotEmpty() }
    val quiet = topLevel.filter { it.id !in pinnedIds && it.id !in activeIds && it !in holders && Board.quiet(snap, it, today) }
    val rest = topLevel.filter { it.id !in pinnedIds && it.id !in activeIds && it !in holders && it !in quiet }

    Column(Modifier.fillMaxSize().testTag("all_spaces_screen")) {
        PageTop(vm, "Spaces", "${all.size} in all") {
            Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.DeepPlum).clickable(role = Role.Button) { vm.openCatch("New space ") }.padding(horizontal = 16.dp).testTag("new_space"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = Color.White); Spacer(Modifier.width(6.dp))
                Text("New space", style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White)
            }
        }
        FilterField(q, "Filter ${all.size} spaces") { q = it }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = pad, end = pad, top = 8.dp, bottom = 150.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (q.isNotBlank()) {
                val hits = all.filter { it.title.contains(q.trim(), true) || Board.inside(snap, it.id).any { c -> c.title.contains(q.trim(), true) } }
                item { Section("Matching · ${hits.size}", hits, snap, today, vm, pinnedIds, autoIds) { lookFor = it } }
                return@LazyColumn
            }
            if (pinned.isNotEmpty()) item { Section("Pinned · ${pinned.size}", pinned.map { it.first }, snap, today, vm, pinnedIds, autoIds) { lookFor = it } }
            if (active.isNotEmpty()) item { Section("This week · ${active.size}", active, snap, today, vm, pinnedIds, autoIds) { lookFor = it } }
            // spaces already listed above aren't repeated under their holder; the holder's line still names them
            holders.forEach { h -> item { Section("${h.title} · ${Board.inside(snap, h.id).size} inside", listOf(h) + Board.inside(snap, h.id).filter { it.id !in pinnedIds && it.id !in activeIds }, snap, today, vm, pinnedIds, autoIds, indentAfterFirst = true) { lookFor = it } } }
            if (rest.isNotEmpty()) item { Section("Other spaces · ${rest.size}", rest, snap, today, vm, pinnedIds, autoIds) { lookFor = it } }
            if (quiet.isNotEmpty()) item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Atlas.Surface)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { showQuiet = !showQuiet }.padding(horizontal = 16.dp).testTag("quiet"), verticalAlignment = Alignment.CenterVertically) {
                        Text("Quiet · ${quiet.size} untouched for a month", style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
                        Icon(if (showQuiet) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Atlas.SecondaryText)
                    }
                    if (showQuiet) quiet.forEach { p -> HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Atlas.Hairline); SpaceRow(vm, snap, p, today, p.id in pinnedIds && p.id !in autoIds, p.id in autoIds, false) { lookFor = p } }
                }
            }
        }
    }
    lookFor?.let { p -> LookPicker(vm, snap, p) { lookFor = null } }
}

@Composable
private fun Section(title: String, spaces: List<KObject>, snap: Snapshot, today: LocalDate, vm: AtlasViewModel, pinnedIds: Set<String>,
                    auto: Set<String> = emptySet(), indentAfterFirst: Boolean = false, onLook: (KObject) -> Unit) = Column {
    Text(title.uppercase(), style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Atlas.Surface)) {
        spaces.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Atlas.Hairline)
            SpaceRow(vm, snap, p, today, p.id in pinnedIds && p.id !in auto, p.id in auto, indentAfterFirst && i > 0) { onLook(p) }
        }
        if (spaces.isEmpty()) Text("Nothing matches.", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun SpaceRow(vm: AtlasViewModel, snap: Snapshot, p: KObject, today: LocalDate, pinned: Boolean, auto: Boolean, indent: Boolean, onLook: () -> Unit) {
    val (line, late) = remember(snap, p.id) { Board.spaceLine(snap, p, today) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClickLabel = "Open ${p.title}") { if (p.type == ObjType.PROJECT) vm.projectSpace(p.id) else vm.go(Route.Item(p.id)) }
        .padding(start = if (indent) 36.dp else 12.dp, end = 4.dp).testTag("space_row_${p.title}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onLook).semantics { contentDescription = "Change color and icon of ${p.title}" }, contentAlignment = Alignment.Center) { SpaceMark(snap, p, 38.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title, style = T.tileTitle.copy(fontSize = 16.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = T.meta.copy(fontSize = 13.sp), color = if (late) Atlas.CoralText else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (p.type == ObjType.PROJECT) {
            if (auto) Text("Auto", style = T.meta.copy(fontSize = 12.sp), color = Atlas.Hint, modifier = Modifier.padding(end = 2.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = if (pinned) "Unpin ${p.title}" else "Pin ${p.title}") { vm.repo.update(p.copy(pinned = !pinned)) }
                .semantics { contentDescription = if (pinned) "Pinned" else "Not pinned" }.testTag("pin_${p.title}"), contentAlignment = Alignment.Center) {
                Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null, Modifier.size(19.dp), tint = if (pinned) Atlas.Primary else Atlas.Hint)
            }
        } else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.Hint, modifier = Modifier.padding(end = 10.dp))
    }
}

/** Choose a space's color and icon. */
@Composable
fun LookPicker(vm: AtlasViewModel, snap: Snapshot, p: KObject, onDone: () -> Unit) {
    val cur = SpaceLook.of(snap, p)
    var color by remember { mutableStateOf(cur.color) }
    var icon by remember { mutableStateOf(cur.iconName) }
    Dialog(onDone) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Atlas.Background).padding(20.dp).testTag("look_picker"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val look = SpaceLook.make(color, icon)
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(look.bg), contentAlignment = Alignment.Center) { Icon(look.icon, null, Modifier.size(28.dp), tint = look.ink) }
                Spacer(Modifier.width(14.dp))
                Text(p.title, style = T.band, color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("COLOR", style = T.eyebrow, color = Atlas.SecondaryText)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpaceLook.COLORS.forEach { (name, c) ->
                    Box(Modifier.size(42.dp).clip(CircleShape).background(c.first).border(if (name == color) 3.dp else 0.dp, c.second, CircleShape)
                        .clickable(role = Role.RadioButton, onClickLabel = name) { color = name }.semantics { contentDescription = name + if (name == color) ", selected" else "" })
                }
            }
            Text("ICON", style = T.eyebrow, color = Atlas.SecondaryText)
            SpaceLook.ICONS.entries.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, v) ->
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (name == icon) Atlas.DeepPlum else Atlas.Surface)
                            .clickable(role = Role.RadioButton, onClickLabel = name) { icon = name }.semantics { contentDescription = name + if (name == icon) ", selected" else "" }, contentAlignment = Alignment.Center) {
                            Icon(v, null, Modifier.size(22.dp), tint = if (name == icon) Color.White else Atlas.DeepPlum)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill("Cancel") { onDone() }
                Spacer(Modifier.width(8.dp))
                Pill("Save", dark = true, tag = "look_save") {
                    vm.repo.replaceDetails(p.id, setOf(DetailKind.LOOK), listOf(Detail(objectId = p.id, kind = DetailKind.LOOK, value = "$color:$icon")))
                    onDone()
                }
            }
        }
    }
}

// ------------------------------------------------------------------ kinds as a list (design 0.7, G)

/**
 * A kind with lots of items reads as a list, not 20 bands of one: Recent first, then A–Z with a letter rail for
 * people, or This week / This month / Earlier for everything else. The filter narrows as you type.
 */
@Composable
fun KindList(vm: AtlasViewModel, snap: Snapshot, kind: Board.Kind, q: String) {
    val platform = com.jmr.atlas.LocalPlatform.current
    val items = remember(snap, kind) { kindItems(snap, kind) }
    val shown = remember(items, q) {
        if (q.isBlank()) items else vm.find(q).map { it.o.id }.toSet().let { ids -> items.filter { it.id in ids || it.title.contains(q.trim(), true) } }
    }
    val today = LocalDate.now()
    val people = kind == Board.Kind.PEOPLE
    val groups: List<Pair<String, List<KObject>>> = remember(shown) {
        if (people) shown.sortedBy { it.title.lowercase() }.groupBy { it.title.firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() }?.toString() ?: "#" }.toList()
        else {
            val now = System.currentTimeMillis()
            shown.sortedByDescending { it.updated }.groupBy { o -> val d = (now - o.updated) / 86_400_000; when { d < 7 -> "This week"; d < 31 -> "This month"; else -> "Earlier" } }.toList()
        }
    }
    val recent = remember(items) { items.sortedByDescending { it.updated }.take(6) }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // header index of each group in the lazy list (recent row is item 0)
    val headerIndex = remember(groups, q) { var i = if (q.isBlank()) 1 else 0; groups.associate { (k, l) -> val at = i; i += 1 + l.size; k to at } }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("kind_list"), state = state, contentPadding = PaddingValues(start = pad, end = if (people) 4.dp else pad, top = 4.dp, bottom = 150.dp)) {
            if (q.isBlank()) item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text("RECENT", style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        recent.forEach { o ->
                            Column(Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClickLabel = "Open ${o.title}") { openThing(vm, platform, o) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                TypeMark(snap, o, 56.dp); Spacer(Modifier.height(6.dp))
                                Text(if (people) o.title.split(" ").first() else o.title, style = T.meta.copy(fontSize = 13.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            groups.forEach { (k, list) ->
                item(key = "h_$k") { Text(k, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.Primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) }
                items(list, key = { it.id }) { o ->
                    val (label, lc) = humanLabel(o, snap, today)
                    Row(Modifier.fillMaxWidth().background(Atlas.Surface).clickable(role = Role.Button, onClickLabel = "Open ${o.title}") { openThing(vm, platform, o) }.heightIn(min = 60.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TypeMark(snap, o, 36.dp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(o.title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (people) listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { label } else label, style = T.meta.copy(fontSize = 12.sp),
                                color = if (people) Atlas.SecondaryText else lc, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = Atlas.Hairline)
                }
            }
            if (shown.isEmpty()) item { Text("Nothing matches “${q.trim()}”.", style = T.body, color = Atlas.SecondaryText, modifier = Modifier.padding(16.dp)) }
        }
        if (people && groups.size > 3) Column(Modifier.width(26.dp).padding(top = 8.dp, end = 4.dp).testTag("letter_rail"), horizontalAlignment = Alignment.CenterHorizontally) {
            ('A'..'Z').forEach { c ->
                val has = headerIndex.containsKey(c.toString())
                Text(c.toString(), style = T.metaStrong.copy(fontSize = 11.sp), color = if (has) Atlas.Primary else Atlas.Hairline,
                    modifier = Modifier.heightIn(min = 18.dp).clickable(enabled = has, role = Role.Button, onClickLabel = "Jump to $c") { scope.launch { headerIndex[c.toString()]?.let { state.scrollToItem(it) } } })
            }
        }
    }
}

fun kindItems(s: Snapshot, k: Board.Kind): List<KObject> = when (k) {
    Board.Kind.PEOPLE -> s.ofType(ObjType.PERSON)
    Board.Kind.FILES -> s.live.filter { it.type.isFile }
    Board.Kind.NOTES -> s.live.filter { it.type == ObjType.NOTE || it.type == ObjType.IDEA || it.type == ObjType.DECISION }
    Board.Kind.TASKS -> s.ofType(ObjType.TASK).filter { !it.done }
    Board.Kind.EVERYTHING -> s.live.filter { it.type != ObjType.COLLECTION }
}

// ------------------------------------------------------------------ shared page parts

@Composable
fun PageTop(vm: AtlasViewModel, title: String, sub: String, trailing: @Composable () -> Unit = {}) =
    Row(Modifier.fillMaxWidth().padding(horizontal = pad).padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", { vm.back() }, Modifier.testTag("back"))
        HomeButton(vm)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (LocalWide.current) T.title else T.titleCompact, color = Atlas.DeepPlum, maxLines = 1)
            if (sub.isNotBlank()) Text(sub, style = T.meta, color = Atlas.SecondaryText)
        }
        trailing()
    }

@Composable
fun FilterField(q: String, hint: String, onChange: (String) -> Unit) =
    Row(Modifier.padding(horizontal = pad).padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(Atlas.Surface).border(1.5.dp, Atlas.Border, RoundedCornerShape(16.dp))
        .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = Atlas.SecondaryText)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (q.isEmpty()) Text(hint, style = T.input.copy(fontSize = 16.sp), color = Atlas.Hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(q, onChange, Modifier.fillMaxWidth().testTag("filter"), singleLine = true, textStyle = T.input.copy(fontSize = 16.sp, color = Atlas.DeepPlum), cursorBrush = SolidColor(Atlas.Primary))
        }
        if (q.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Clear filter") { onChange("") }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, null, Modifier.size(16.dp), tint = Atlas.SecondaryText) }
    }
