package com.jmr.atlas.ui.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.*
import com.jmr.atlas.model.Shape
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T

/**
 * Start a journey (approved board J-Start): for a space, "Here's how I'd shape it" (the pick + reason, every other shape one tap
 * away) → "Gathering what's yours" (sources, each a checkbox) → open. Brand new: name it, pick a shape (Atlas suggests one as you
 * type), or start from a template.
 */
@Composable
fun StartScreen(vm: AtlasViewModel, snap: Snapshot, spaceId: String?) {
    val space = spaceId?.let { snap.objects[it] }
    var step by remember(spaceId) { mutableStateOf(if (space == null) "new" else "shape") }
    val pick = remember(spaceId) { space?.let { ShapePick.pick(snap, it.id) } }
    var shape by remember(spaceId) { mutableStateOf(pick?.shape ?: Shape.TRAIL) }
    var style by remember(spaceId) { mutableStateOf("ROOM") }
    var off by remember(spaceId) { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F2FB)).statusBarsPadding().testTag("journey_start")) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Back") { if (step == "sources") step = "shape" else vm.back() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Atlas.SecondaryText) }
            Text(when (step) { "shape" -> "Choose a shape"; "sources" -> "Building"; else -> "New journey" }, style = T.tileTitle.copy(fontSize = 17.sp), color = Atlas.DeepPlum)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            when (step) {
                "shape" -> {
                    Text("Here’s how I’d shape it", style = T.titleCompact.copy(fontSize = 24.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(top = 8.dp))
                    Column(Modifier.padding(top = 14.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color.White).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(Look.bg(shape, style)), contentAlignment = Alignment.Center) { Glyph(shape, 150.dp) }
                        Text(shape.label.uppercase(), style = T.eyebrow, color = Atlas.Primary, modifier = Modifier.padding(top = 14.dp))
                        Text(space?.title ?: "", style = T.band, color = Atlas.DeepPlum)
                        Text(if (shape == pick?.shape) pick.reason else "${shape.label}: ${shape.time.lowercase()}.", style = T.body.copy(fontSize = 14.sp), color = Atlas.SecondaryText,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp).testTag("shape_reason"))
                    }
                    if (shape == Shape.REMEMBER) FlowRowCompat(Modifier.padding(top = 12.dp)) { Shape.REMEMBER.styles.forEach { st -> Chip(Shape.styleLabels[st]!!, st == style, Atlas.Primary) { style = st } } }
                    Text("Or pick another shape · everything still goes in", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    FlowRowCompat(Modifier) { Shape.entries.forEach { sh -> Chip(sh.label, sh == shape, Atlas.Primary, "pick_${sh.name}") { shape = sh } } }
                    Big("Use ${shape.label}", "use_shape") { step = "sources" }
                }
                "sources" -> {
                    val src = Sources.of(snap, space!!.id)
                    Text("Gathering what’s yours", style = T.titleCompact.copy(fontSize = 24.sp), color = Atlas.DeepPlum, modifier = Modifier.padding(top = 8.dp))
                    Text("Only from this space and your phone. Uncheck anything you don’t want in.", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (src.isEmpty()) Text("Nothing yet. That’s fine: things you add later are placed for you.", style = T.body, color = Atlas.SecondaryText)
                        src.forEach { sc -> val on = sc.kind.name !in off
                            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(role = Role.Checkbox) {
                                off = if (on) off + sc.kind.name else off - sc.kind.name }.padding(horizontal = 14.dp).testTag("src_${sc.kind.name}"), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (on) Atlas.Primary else Color(0xFFD9D2E6)), contentAlignment = Alignment.Center) {
                                    if (on) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White) }
                                Spacer(Modifier.width(12.dp))
                                Column { Text(sc.kind.title, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum); Text(sc.label, style = T.meta, color = Atlas.SecondaryText) }
                            } }
                    }
                    val fill = remember { Animatable(0f) }; LaunchedEffect(Unit) { fill.animateTo(1f, tween(2400)) }
                    Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE6DCF3))) {
                        Box(Modifier.fillMaxWidth(fill.value).height(6.dp).background(Atlas.Primary)) }
                    Big("Open ${space.title}", "open_journey") {
                        vm.repo.setJourney(Journeys.new(space.id, shape, style).copy(hidden = off), "made a ${shape.label}")
                        vm.goInstead(Route.Journey(space.id))
                    }
                }
                else -> {
                    val guess = remember(name) { if (name.isBlank()) null else ShapePick.pickText(name) }
                    LaunchedEffect(guess?.shape) { guess?.let { shape = it.shape } }
                    Text("Name it", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp))
                    BasicTextField(name, { name = it.take(60) }, Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 14.dp).testTag("new_name"), textStyle = T.input.copy(color = Atlas.DeepPlum), singleLine = true,
                        decorationBox = { inner -> if (name.isEmpty()) Text("Kitchen remodel, Ellie, Oregon trip…", style = T.input, color = Atlas.Hint); inner() })
                    Text(guess?.let { "I’d pick ${it.shape.label} for this. ${it.reason}" } ?: "Pick a shape", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    FlowRowCompat(Modifier) { Shape.entries.forEach { sh -> Chip(sh.label, sh == shape, Atlas.Primary, "new_${sh.name}") { shape = sh } } }
                    Big("Make it", "make_journey", enabled = name.isNotBlank()) {
                        val id = vm.repo.create(KObject(type = ObjType.PROJECT, title = name.trim(), projectStatus = ProjectStatus.ACTIVE))
                        vm.repo.setJourney(Journeys.new(id, shape, style), "made a ${shape.label}")
                        vm.goInstead(Route.Journey(id))
                    }
                    val tpls = vm.repo.templates()
                    Text("Or start from a template", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    if (tpls.isEmpty()) Text("Save any journey as a template from its pencil menu and it shows up here.", style = T.meta, color = Atlas.Hint)
                    tpls.forEach { (n, t) ->
                        Column(Modifier.padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                            .clickable(role = Role.Button) {
                                val id = vm.repo.create(KObject(type = ObjType.PROJECT, title = name.trim().ifBlank { n }, projectStatus = ProjectStatus.ACTIVE))
                                vm.repo.setJourney(t.copy(spaceId = id), "from template $n"); vm.goInstead(Route.Journey(id))
                            }.padding(horizontal = 14.dp, vertical = 10.dp).testTag("template_$n")) {
                            Text(n, style = T.body.copy(fontSize = 15.sp), color = Atlas.DeepPlum)
                            Text("${t.shape.label} · ${t.parts.size} ${t.shape.partNoun}s", style = T.meta, color = Atlas.SecondaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Big(t: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) =
    Text(t, style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp).fillMaxWidth().heightIn(min = 52.dp)
        .clip(RoundedCornerShape(26.dp)).background(if (enabled) Atlas.Primary else Atlas.Border).clickable(enabled = enabled, role = Role.Button) { onClick() }.padding(vertical = 15.dp).testTag(tag))

/** The small animated picture of a shape (Home, start flow). Drawn in a 72 × 72 box like the approved glyphs. */
@Composable
fun Glyph(s: Shape, size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "glyph").animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "g")
    Canvas(modifier.size(size)) {
        val k = this.size.width / 72f
        fun o(x: Float, y: Float) = Offset(x * k, y * k)
        val acc = Color(0xFF7443B1)
        when (s) {
            Shape.TRAIL -> { val p = Path().apply { moveTo(12 * k, 66 * k); cubicTo(30 * k, 56 * k, 52 * k, 58 * k, 44 * k, 44 * k); cubicTo(36 * k, 30 * k, 20 * k, 30 * k, 34 * k, 20 * k); cubicTo(48 * k, 10 * k, 58 * k, 10 * k, 60 * k, 4 * k) }
                drawPath(p, Color(0xFFD9CCEB), style = Stroke(8 * k, cap = StrokeCap.Round)); drawPath(p, acc, style = Stroke(1.6f * k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * k, 5 * k), -t.value * 16 * k)))
                drawCircle(Color.White, 5 * k, o(44f, 46f)); drawCircle(acc, 5 * k, o(44f, 46f), style = Stroke(2.5f * k)) }
            Shape.TREE -> { drawLine(Color(0xFF8C7A6B), o(36f, 68f), o(36f, 28f), 3 * k); drawLine(Color(0xFF8C7A6B), o(35f, 46f), o(18f, 27f), 1.8f * k); drawLine(Color(0xFF8C7A6B), o(36f, 39f), o(54f, 20f), 1.8f * k)
                drawCircle(Color(0xFF9FD9C2), 5 * k, o(18f, 25f)); drawCircle(Color(0xFF9FD9C2), 8 * k, o(37f, 14f)); drawCircle(acc, 6 * k, o(54f, 19f)) }
            Shape.REMEMBER -> { drawRoundRect(Color(0xFFF3E6DA), o(10f, 14f), Size(52 * k, 44 * k), androidx.compose.ui.geometry.CornerRadius(6 * k)); drawRect(Color(0xFFC9B6E4), o(16f, 20f), Size(16 * k, 18 * k))
                drawLine(Color(0xFF7A5A3E), o(40f, 22f), o(60f, 28f), 2 * k); drawCircle(Color(0xFFB5654F), 7 * k, o(22f, 48f)); drawRect(Color(0xFFA9774F), o(38f, 44f), Size(20 * k, 4 * k)) }
            Shape.CROSSROADS -> { listOf(12f, 36f, 60f).forEach { x -> drawPath(Path().apply { moveTo(x * k, 6 * k); cubicTo(x * k, 30 * k, 36 * k, 40 * k, 36 * k, 60 * k) }, Color(0xFFA9C8EC), style = Stroke(3 * k)) }
                drawCircle(Color.White, 7 * k, o(36f, 62f)); drawCircle(acc, 7 * k, o(36f, 62f), style = Stroke(2.5f * k)) }
            Shape.RIVER -> { val p = Path().apply { moveTo(30 * k, 2 * k); cubicTo(20 * k, 18 * k, 46 * k, 26 * k, 34 * k, 40 * k); cubicTo(22 * k, 54 * k, 24 * k, 60 * k, 40 * k, 70 * k) }
                drawPath(p, Color(0xFFCFE3F6), style = Stroke(12 * k, cap = StrokeCap.Round)); drawPath(p, Color(0xFF4A86C5), style = Stroke(1.4f * k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * k, 6 * k), -t.value * 20 * k)))
                drawCircle(Color(0xFF4A86C5), 3 * k, o(33f, 20f)); drawCircle(Color(0xFF4A86C5), 3 * k, o(36f, 44f)) }
            Shape.ORBIT -> { drawCircle(Color(0xFFE4DAF1), 28 * k, o(36f, 36f), style = Stroke(1 * k)); drawCircle(Color(0xFFC9B5E6), 19 * k, o(36f, 36f), style = Stroke(1.5f * k)); drawCircle(acc, 10 * k, o(36f, 36f), style = Stroke(2 * k))
                val a = t.value * 6.283f; drawCircle(acc, 3.5f * k, o(36f + 10f * kotlin.math.cos(a), 36f + 10f * kotlin.math.sin(a))) }
            Shape.SHELF -> { drawRoundRect(Color(0xFFEFE3D3), o(8f, 10f), Size(56 * k, 54 * k), androidx.compose.ui.geometry.CornerRadius(6 * k)); drawRect(Color(0xFFC9A889), o(8f, 30f), Size(56 * k, 3 * k)); drawRect(Color(0xFFC9A889), o(8f, 48f), Size(56 * k, 3 * k))
                drawRect(Color(0xFFE8B9CB), o(14f, 16f), Size(8 * k, 14 * k)); drawRect(Color(0xFFB9A3DA), o(26f, 20f), Size(8 * k, 10 * k)); drawRect(Color(0xFFA9D8BE), o(16f, 36f), Size(10 * k, 12 * k)) }
            Shape.VOYAGE -> { drawRoundRect(Color(0xFFD2E8F2), o(4f, 8f), Size(64 * k, 58 * k), androidx.compose.ui.geometry.CornerRadius(10 * k))
                drawPath(Path().apply { moveTo(14 * k, 20 * k); cubicTo(30 * k, 24 * k, 50 * k, 30 * k, 52 * k, 40 * k); cubicTo(54 * k, 50 * k, 24 * k, 50 * k, 26 * k, 58 * k) }, acc, style = Stroke(1.6f * k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * k, 4 * k))))
                listOf(Offset(14f, 20f), Offset(52f, 40f), Offset(26f, 58f)).forEach { drawOval(Color(0xFFE8D7B4), o(it.x - 8f, it.y - 4f), Size(16 * k, 8 * k)) } }
            Shape.CLIMB -> { drawPath(Path().apply { moveTo(4 * k, 64 * k); lineTo(22 * k, 40 * k); lineTo(30 * k, 48 * k); lineTo(46 * k, 18 * k); lineTo(56 * k, 34 * k); lineTo(68 * k, 28 * k); lineTo(68 * k, 64 * k); close() }, Color(0xFFDCE4EE))
                drawPath(Path().apply { moveTo(8 * k, 62 * k); cubicTo(18 * k, 56 * k, 24 * k, 50 * k, 30 * k, 46 * k); cubicTo(36 * k, 42 * k, 40 * k, 28 * k, 46 * k, 18 * k) }, acc, style = Stroke(2 * k))
                drawPath(Path().apply { moveTo(46 * k, 18 * k); lineTo(46 * k, 8 * k); lineTo(54 * k, 11 * k); lineTo(46 * k, 14 * k) }, Color(0xFFC25B85)) }
            Shape.COUNTDOWN -> { drawCircle(Color(0xFFEBDCEA), 26 * k, o(36f, 36f), style = Stroke(6 * k))
                drawArc(Color(0xFFC25B85), -90f, 270f - t.value * 10f, false, o(10f, 10f), Size(52 * k, 52 * k), style = Stroke(6 * k, cap = StrokeCap.Round)) }
        }
    }
}
