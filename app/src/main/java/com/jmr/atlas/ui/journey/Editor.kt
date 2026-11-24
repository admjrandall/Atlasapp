package com.jmr.atlas.ui.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.*
import com.jmr.atlas.model.Shape
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.cards.rememberPreview
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import java.time.LocalDate

/** Shape and parts (spec §4.9, §4.12, §4.14): rename, reorder, add, remove parts; change shape (items stay); template; sources; lock; the day. */
@Composable
fun Editor(vm: AtlasViewModel, snap: Snapshot, space: KObject, j: Journey, acc: Color, onDone: () -> Unit, onToast: (Pair<String, (() -> Unit)?>) -> Unit) {
    fun save(nj: Journey) = vm.repo.setJourney(nj)
    Column(Modifier.fillMaxSize().background(Color(0xFFFAF8FC)).then(BlockTaps).statusBarsPadding().testTag("journey_editor")) {
        Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Shape and parts", style = T.band.copy(fontSize = 20.sp), color = Atlas.DeepPlum); Text(space.title, style = T.meta, color = Atlas.SecondaryText) }
            Text("Done", style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White, modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(acc)
                .clickable(role = Role.Button) { onDone() }.padding(horizontal = 18.dp, vertical = 12.dp).testTag("editor_done"))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Label("${j.shape.partNoun.replaceFirstChar { it.uppercase() }}s · move with the arrows, tap a name to rename")
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            j.parts.forEachIndexed { i, p ->
                var name by remember(p.id) { mutableStateOf(p.name) }
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(start = 12.dp, end = 4.dp).testTag("edit_part"),
                    verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(name, { name = it.take(40); save(j.copy(parts = j.parts.map { x -> if (x.id == p.id) x.copy(name = name) else x })) },
                        Modifier.weight(1f).testTag("edit_name_$i"), textStyle = T.body.copy(color = Atlas.DeepPlum), singleLine = true, cursorBrush = SolidColor(acc))
                    Text(stateWord(j.shape, p.state), style = T.meta.copy(fontSize = 11.sp), color = Atlas.SecondaryText, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Change state") { save(j.copy(parts = j.parts.map { x -> if (x.id == p.id) x.copy(state = nextState(j.shape, x.state)) else if (nextState(j.shape, p.state) == PartState.HERE && x.state == PartState.HERE) x.copy(state = PartState.LATER) else x })) }
                        .padding(8.dp))
                    IconBtn(Icons.Outlined.KeyboardArrowUp, "Move up", i > 0) { save(j.copy(parts = j.parts.toMutableList().apply { add(i - 1, removeAt(i)) })) }
                    IconBtn(Icons.Outlined.KeyboardArrowDown, "Move down", i < j.parts.lastIndex) { save(j.copy(parts = j.parts.toMutableList().apply { add(i + 1, removeAt(i)) })) }
                    IconBtn(Icons.Outlined.Close, "Remove ${p.name}", true) {
                        save(j.copy(parts = j.parts.filter { it.id != p.id })); onToast("Removed ${p.name} · its items stay in the space" to { save(j) }) }
                }
            }
            Text("+ Add ${j.shape.partNoun}", style = T.body.copy(fontSize = 14.sp), color = Atlas.Primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                .clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFD9CCEB), RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { save(j.copy(parts = j.parts + Part(newId(), "New ${j.shape.partNoun}", PartState.LATER, j.parts.size))) }.padding(vertical = 13.dp).testTag("edit_add"))
        }
        if (j.shape == Shape.COUNTDOWN || j.shape == Shape.ORBIT) {
            Label("The day")
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7L, 30L, 90L, 180L).forEach { d -> Chip("In $d days", space.targetDate == LocalDate.now().plusDays(d), acc) { vm.repo.update(space.copy(targetDate = LocalDate.now().plusDays(d))) } }
            }
            Text(space.targetDate?.let { "Set for ${it.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}. Change it exactly on the space’s page." } ?: "Not set yet.",
                style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Label("Change shape · your items all stay")
        FlowRowCompat(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Shape.entries.forEach { sh -> Chip(sh.label, sh == j.shape, acc, "shape_${sh.name}") { if (sh != j.shape) { save(Journeys.reshape(j, sh)); onToast("Now a ${sh.label}" to { save(j) }) } } }
        }
        if (j.shape == Shape.REMEMBER) FlowRowCompat(Modifier.padding(horizontal = 16.dp)) {
            Shape.REMEMBER.styles.forEach { st -> Chip(Shape.styleLabels[st]!!, st == j.style, acc) { save(j.copy(style = st)) } } }
        Label("Sources it can use")
        val src = Sources.of(snap, space.id)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (src.isEmpty()) Text("Nothing in this space yet.", style = T.meta, color = Atlas.SecondaryText)
            src.forEach { sc -> val on = sc.kind.name !in j.hidden
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(role = Role.Checkbox) {
                    save(j.copy(hidden = if (on) j.hidden + sc.kind.name else j.hidden - sc.kind.name)) }.padding(horizontal = 14.dp).testTag("source_${sc.kind.name}"), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (on) acc else Color(0xFFD9D2E6)), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White) }
                    Spacer(Modifier.width(12.dp)); Text(sc.label, style = T.body.copy(fontSize = 15.sp), color = Atlas.DeepPlum)
                } }
        }
        Label("More")
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row_("Save as a template", "keeps the ${j.shape.partNoun}s, not your items", "save_template") { vm.repo.saveTemplate(space.title, j); onToast("Saved “${space.title}” as a template" to null) }
            val locked = space.id in vm.settings.lockedJourneys
            Row_(if (locked) "Unlock with fingerprint" else "Lock with fingerprint", if (locked) "on for this journey" else "off", "toggle_lock") {
                vm.settings.lockedJourneys = if (locked) vm.settings.lockedJourneys - space.id else vm.settings.lockedJourneys + space.id }
            Row_("Back to the board", "removes the journey; the space and everything in it stay", "remove_journey") {
                vm.repo.clearJourney(space.id); vm.back() }
        }
        }
    }
}

private fun stateWord(s: Shape, st: PartState) = when (st) {
    PartState.HERE -> if (s.finishes) "here" else "now"; PartState.DONE -> if (s == Shape.TREE) "fruit" else "done"; PartState.NEXT -> "next"
    PartState.OUT -> "ruled out"; PartState.LATER -> if (s == Shape.TREE) "bud" else "later"
}
private fun nextState(s: Shape, st: PartState): PartState = when (st) {
    PartState.LATER -> PartState.HERE; PartState.HERE -> PartState.DONE; PartState.DONE -> if (s == Shape.CROSSROADS) PartState.OUT else PartState.LATER
    PartState.OUT -> PartState.LATER; PartState.NEXT -> PartState.HERE
}

@Composable private fun Label(t: String) = Text(t.uppercase(), style = T.tileLabel, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
@Composable private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) =
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (enabled) Atlas.SecondaryText else Atlas.Border) }
@Composable fun Chip(t: String, on: Boolean, acc: Color, tag: String = "", onClick: () -> Unit) =
    Text(t, style = T.meta.copy(fontSize = 13.sp), color = if (on) Color.White else Atlas.DeepPlum, modifier = Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(18.dp))
        .background(if (on) acc else Color.White).clickable(role = Role.Button) { onClick() }.padding(horizontal = 12.dp, vertical = 9.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier))
@Composable private fun Row_(t: String, d: String, tag: String, onClick: () -> Unit) =
    Column(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(role = Role.Button) { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp).testTag(tag)) {
        Text(t, style = T.body.copy(fontSize = 15.sp), color = Atlas.DeepPlum); Text(d, style = T.meta, color = Atlas.SecondaryText) }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable fun FlowRowCompat(modifier: Modifier, content: @Composable () -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }

/** Finishing / cycling (spec §4.11): the journey folds into a memory, a decision settles, a ring closes, the day arrives. */
@Composable
fun Finish(vm: AtlasViewModel, snap: Snapshot, space: KObject, j: Journey, acc: Color, onClose: () -> Unit) {
    val photos = JourneyItems.inSpace(snap, space.id).filter { it.type == ObjType.IMAGE }.take(3)
    val chosen = j.parts.firstOrNull { it.state != PartState.OUT && it.state != PartState.DONE } ?: j.parts.firstOrNull { it.state == PartState.DONE }
    val n = JourneyItems.inSpace(snap, space.id).size
    val (kick, title, text, btn) = when (j.shape) {
        Shape.CROSSROADS -> listOf("Decided", chosen?.name ?: "Decided", "Chosen from ${j.parts.size} options. The others stay here, ruled out, if you ever want to look back.", "Settle it")
        Shape.ORBIT -> listOf("Cycle complete", "${j.here?.name ?: "This cycle"} is done", "The ring closes and a new one starts, already knowing what to watch for.", "Start the next cycle")
        Shape.COUNTDOWN -> listOf("The day", "${space.title}, celebrated", "Everything here turns into an album you can come back to.", "Keep it as an album")
        Shape.VOYAGE -> listOf("Trip complete", space.title, "$n things from the trip. The islands fold into an album, the best photos first.", "Keep it as an album")
        Shape.CLIMB -> listOf("Summit", "${space.title}, done", "${j.parts.size} camps got you here. The climb becomes a memory.", "Keep it as a memory")
        else -> listOf("Journey complete", "${space.title} is done", "${j.parts.size} ${j.shape.partNoun}s and $n things. It folds into a memory you can come back to.", "Keep it as a memory")
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFFAF8FC)).statusBarsPadding().padding(horizontal = 24.dp).padding(top = 40.dp).then(BlockTaps).testTag("journey_finish"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val grow = remember { Animatable(0f) }; LaunchedEffect(Unit) { grow.animateTo(1f, tween(500)) }
        Box(Modifier.size(300.dp, 240.dp)) {
            photos.forEachIndexed { i, o -> val (img, _) = rememberPreview(o)
                if (img != null) androidx.compose.foundation.Image(img, null, Modifier.offset(x = listOf(20, 120, 70)[i].dp, y = listOf(30, 0, 90)[i].dp).size(170.dp, 150.dp)
                    .scale(grow.value).rotate(listOf(-6f, 4f, -2f)[i]).shadow(14.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).border(5.dp, Color.White, RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop) }
            if (photos.isEmpty()) Icon(Icons.Outlined.AutoAwesome, null, Modifier.align(Alignment.Center).size(72.dp).scale(grow.value), tint = acc)
        }
        Text(kick.uppercase(), style = T.eyebrow, color = acc, modifier = Modifier.padding(top = 18.dp))
        Text(title, style = T.titleCompact, color = Atlas.DeepPlum, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Text(text, style = T.body, color = Atlas.SecondaryText, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(btn, style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White, modifier = Modifier.padding(top = 22.dp).heightIn(min = 50.dp).clip(RoundedCornerShape(25.dp)).background(acc)
            .clickable(role = Role.Button) {
                when (j.shape) {
                    Shape.CROSSROADS -> chosen?.let { c -> vm.repo.setJourney(j.copy(parts = j.parts.map { if (it.id == c.id) it.copy(state = PartState.DONE) else it.copy(state = PartState.OUT) })) }
                    Shape.ORBIT -> vm.repo.setJourney(j.copy(parts = listOf(Part(newId(), "Next cycle", PartState.HERE, 0)) + j.parts.map { it.copy(state = PartState.DONE) }))
                    else -> { vm.repo.setJourney(j.copy(parts = j.parts.map { it.copy(state = PartState.DONE) })); vm.repo.update(space.copy(projectStatus = ProjectStatus.COMPLETED)) }
                }
                onClose()
            }.padding(horizontal = 26.dp, vertical = 15.dp).testTag("finish_confirm"))
        Text("Not yet", style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button) { onClose() }.padding(horizontal = 18.dp, vertical = 12.dp))
    }
}
