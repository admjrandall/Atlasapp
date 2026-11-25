package com.jmr.atlas.ui.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.jmr.atlas.model.*
import com.jmr.atlas.model.Shape
import kotlin.math.*

/** Every scene is drawn in a 390 × 620 design space and scaled to the screen width (spec mockups, Journeys v2). */
const val SW = 390f
const val SH = 620f

/** Where a part sits in a scene, and how it looks. [w]/[h] in design units; [hit] = invisible tap area over drawn art. */
data class Spot(val part: Part, val x: Float, val y: Float, val w: Float, val h: Float = w, val hit: Boolean = false, val label: LabelAt = LabelAt.BELOW)
enum class LabelAt { BELOW, RIGHT, LEFT, ABOVE, NONE }

object Look {
    val ink = Color(0xFF241C33); val ink2 = Color(0xFF6B6280); val acc = Color(0xFF7443B1); val accSoft = Color(0xFFD3C2EA)
    val lilac = Color(0xFFE2D8F0); val rose = Color(0xFFC25B85); val blue = Color(0xFF4A86C5); val mint = Color(0xFF9FD3B7)
    fun bg(s: Shape, style: String) = when (s) {
        Shape.TRAIL -> Color(0xFFF4F0FA); Shape.TREE -> Color(0xFFF2F8F4); Shape.CROSSROADS -> Color(0xFFF3F6FB); Shape.RIVER -> Color(0xFFF2F7FC)
        Shape.ORBIT -> Color(0xFFF6F2FB); Shape.SHELF -> Color(0xFFF7F2EC); Shape.VOYAGE -> Color(0xFFE9F3F8); Shape.CLIMB -> Color(0xFFF1F4F9)
        Shape.COUNTDOWN -> Color(0xFFF7F1F6)
        Shape.REMEMBER -> when (style) { "GARDEN" -> Color(0xFFEEF4F1); "LETTER" -> Color(0xFFF4EEF6); else -> Color(0xFFF3E6DA) }
    }
    fun accent(s: Shape) = when (s) { Shape.RIVER -> blue; Shape.COUNTDOWN -> rose; else -> acc }
}

/** Layout of parts for each shape. Counts vary, so every layout spreads n parts along its own form. */
object Layout {
    fun spots(j: Journey): List<Spot> {
        val ps = j.parts; val n = ps.size
        if (n == 0) return emptyList()
        return when (j.shape) {
            Shape.TRAIL -> { val pts = trailPoints(n); ps.mapIndexed { i, p -> Spot(p, pts[i].x, pts[i].y, size(p), label = if (pts[i].x < SW / 2) LabelAt.RIGHT else LabelAt.LEFT) } }
            Shape.TREE -> ps.mapIndexed { i, p ->
                val y = 470f - i * (330f / max(1, n - 1).coerceAtLeast(1)) .coerceAtMost(110f); val left = i % 2 == 0
                Spot(p, if (left) 92f else 298f, y.coerceAtLeast(150f), if (p.state == PartState.HERE) 36f else 28f, label = LabelAt.BELOW) }
            Shape.REMEMBER -> when (j.style) {
                "LETTER" -> ps.mapIndexed { i, p -> Spot(p, SW / 2, 175f + i * (370f / max(n, 4)) + (370f / max(n, 4)) / 2, 330f, 370f / max(n, 4) - 6f, hit = true, label = LabelAt.NONE) }
                "GARDEN" -> ps.mapIndexed { i, p -> val s = gardenSlots[i % gardenSlots.size]; Spot(p, s.x, s.y + (i / gardenSlots.size) * 18f, 78f, 70f, hit = true, label = LabelAt.ABOVE) }
                else -> ps.mapIndexed { i, p -> val s = roomSlots[i % roomSlots.size]; Spot(p, s.x, s.y, s.w, s.h, hit = true, label = LabelAt.BELOW) }
            }
            Shape.CROSSROADS -> ps.mapIndexed { i, p -> Spot(p, if (n == 1) SW / 2 else 60f + i * (270f / (n - 1)), 120f, 58f, label = LabelAt.BELOW) }
            Shape.RIVER -> { val pts = riverPoints(n); ps.mapIndexed { i, p -> Spot(p, pts[i].x, pts[i].y, if (p.state == PartState.HERE) 40f else 26f, label = LabelAt.RIGHT) } }
            Shape.ORBIT -> ps.mapIndexed { i, p ->
                // rings fit inside the screen whatever the count; each marker sits further round so labels don't stack
                val r = 64f + i * (110f / max(1, n - 1)); val a = (-1.25f + i * (2.3f / max(1, n - 1)))
                Spot(p, SW / 2 + r * cos(a), 300f + r * sin(a), if (p.state == PartState.HERE) 30f else 22f, label = if (i == 0) LabelAt.ABOVE else LabelAt.RIGHT) }
            Shape.SHELF -> ps.mapIndexed { i, p -> Spot(p, SW / 2, 120f + i * (420f / max(n, 3)), 350f, 420f / max(n, 3) - 10f, hit = true, label = LabelAt.NONE) }
            Shape.VOYAGE -> { val pts = voyagePoints(n); ps.mapIndexed { i, p -> Spot(p, pts[i].x, pts[i].y, if (p.state == PartState.HERE) 50f else 34f, label = LabelAt.BELOW) } }
            Shape.CLIMB -> ps.mapIndexed { i, p ->
                val t = if (n == 1) 1f else i / (n - 1f)
                Spot(p, 40f + t * 190f, 540f - t * 300f, if (i == n - 1) 34f else if (p.state == PartState.HERE) 30f else 24f, label = if (40f + t * 190f < 120f) LabelAt.RIGHT else LabelAt.LEFT) }
            Shape.COUNTDOWN -> ps.mapIndexed { i, p ->
                val a = -PI.toFloat() / 2 + i * (2 * PI.toFloat() / n)
                Spot(p, SW / 2 + 168f * cos(a), 300f + 168f * sin(a), 44f, label = if (sin(a) < -0.5f) LabelAt.ABOVE else LabelAt.BELOW) }
        }
    }
    private fun size(p: Part) = when (p.state) { PartState.HERE -> 32f; PartState.NEXT -> 22f; PartState.DONE -> 22f; else -> 18f }
    fun trailPoints(n: Int): List<Offset> = List(n) { i -> val t = if (n == 1) 0f else i / (n - 1f); Offset(SW / 2 + 85f * sin(t * PI.toFloat() * (1.4f + n * 0.15f)) , 560f - t * 440f) }
    fun riverPoints(n: Int): List<Offset> = List(n) { i -> val t = if (n == 1) 0.5f else i / (n - 1f); Offset(195f + 45f * sin(t * 5.5f), 130f + t * 420f) }
    fun voyagePoints(n: Int): List<Offset> = List(n) { i -> val t = if (n == 1) 0.5f else i / (n - 1f); Offset(if (i % 2 == 0) 90f else 290f, 150f + t * 380f) }
    /** Keepsake Room slots (center x, center y, w, h): wall shelf, corner, frame, table, floor, door, window, shelf end. */
    val roomSlots = listOf(Slot(292f, 150f, 176f, 64f), Slot(70f, 360f, 104f, 220f), Slot(294f, 214f, 96f, 70f), Slot(228f, 462f, 56f, 34f),
        Slot(338f, 518f, 96f, 70f), Slot(170f, 455f, 44f, 40f), Slot(90f, 190f, 110f, 130f), Slot(346f, 248f, 44f, 30f))
    val gardenSlots = listOf(Offset(300f, 210f), Offset(90f, 300f), Offset(196f, 336f), Offset(82f, 462f), Offset(300f, 474f), Offset(196f, 520f))
}
data class Slot(val x: Float, val y: Float, val w: Float, val h: Float)

/** Which keepsake stands for a memory, from words in its name or its items (the illustrated library starts with these). */
enum class Keepsake { ROD, GUITAR, FRAME, PHONE, CARD, COOLER, BOOK, MUG, KEYS, BOX }
fun keepsakeFor(name: String): Keepsake {
    val t = name.lowercase()
    return when {
        Regex("lake|fish|boat|river|camp").containsMatchIn(t) -> Keepsake.ROD
        Regex("band|guitar|music|song|garage").containsMatchIn(t) -> Keepsake.GUITAR
        Regex("wedding|toast|photo|picture|family").containsMatchIn(t) -> Keepsake.FRAME
        Regex("text|message|call|phone|last").containsMatchIn(t) -> Keepsake.PHONE
        Regex("obituary|service|funeral|card").containsMatchIn(t) -> Keepsake.CARD
        Regex("cooler|fire|campfire|bbq").containsMatchIn(t) -> Keepsake.COOLER
        Regex("book|read|letter|school").containsMatchIn(t) -> Keepsake.BOOK
        Regex("coffee|breakfast|kitchen|mug").containsMatchIn(t) -> Keepsake.MUG
        Regex("car|truck|drive|road|trip").containsMatchIn(t) -> Keepsake.KEYS
        else -> Keepsake.BOX
    }
}

/** The scene art behind the parts. Pure drawing; taps are handled by the part spots on top. */
/** Ground bands (design y → colour) that run edge to edge, so the scene fills wide screens (unfolded) too. */
private fun ground(j: Journey): List<Pair<Float, Color>> = when (j.shape) {
    Shape.TRAIL -> listOf(245f to Color(0xFFE7F1EA))
    Shape.TREE -> listOf(582f to Color(0xFFDDEEE4))
    Shape.REMEMBER -> when (j.style) { "GARDEN" -> listOf(295f to Color(0xFFCFE6D6), 355f to Color(0xFFBFDDC8)); "LETTER" -> emptyList()
        else -> listOf(0f to Color(0xFFF3E6DA), 432f to Color(0xFFC9A889), 442f to Color(0xFFDDC4A8)) }
    Shape.VOYAGE -> listOf(140f to Color(0xFFD2E8F2))
    Shape.CLIMB -> listOf(560f to Color(0xFFDCE4EE))
    else -> emptyList()
}

@Composable
fun SceneArt(j: Journey, spots: List<Spot>, modifier: Modifier = Modifier, dx: Float = 0f) {
    val pulse = rememberInfiniteTransition(label = "scene").animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "flow")
    Canvas(modifier) {
        val k = (size.width - 2 * dx) / SW
        ground(j).forEach { (y, c) -> drawRect(c, Offset(0f, y * k), Size(size.width, size.height - y * k)) }
        translate(left = dx) { drawScene(j, spots, k, pulse.value) }
    }
}

private fun DrawScope.drawScene(j: Journey, spots: List<Spot>, k: Float, pv: Float) {
    run {
        val size = Size(SW * k, SH * k)
        fun o(x: Float, y: Float) = Offset(x * k, y * k)
        fun path(pts: List<Offset>) = Path().apply {
            if (pts.isEmpty()) return@apply
            moveTo(pts[0].x * k, pts[0].y * k)
            for (i in 1 until pts.size) { val a = pts[i - 1]; val b = pts[i]; val m = (a.y + b.y) / 2; cubicTo(a.x * k, m * k, b.x * k, m * k, b.x * k, b.y * k) }
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f * k, 8f * k), -pv * 28f * k)
        when (j.shape) {
            Shape.TRAIL -> {
                drawPath(Path().apply { moveTo(0f, 250 * k); cubicTo(120 * k, 220 * k, 250 * k, 270 * k, size.width, 240 * k); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, Color(0xFFE7F1EA))
                listOf(o(40f, 236f) to 22f, o(66f, 228f) to 16f, o(344f, 226f) to 20f, o(330f, 470f) to 18f, o(22f, 560f) to 20f).forEach { (c, r) -> drawCircle(Color(0xFFCFE6D6), r * k, c) }
                val pts = spots.map { Offset(it.x, it.y) }
                drawPath(path(pts), Look.lilac, style = Stroke(22 * k, cap = StrokeCap.Round))
                val hereI = spots.indexOfFirst { it.part.state == PartState.HERE }.let { if (it < 0) 0 else it }
                drawPath(path(pts.take(hereI + 1)), Look.accSoft, style = Stroke(40 * k, cap = StrokeCap.Round))
                drawPath(path(pts.take(hereI + 1)), Look.acc, style = Stroke(2.4f * k, pathEffect = dash))
                drawPath(path(pts.drop(hereI)), Color(0xFFB9A3DA), style = Stroke(2f * k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * k, 8f * k))))
                pts.lastOrNull()?.let { e -> drawLine(Look.acc, o(e.x, e.y - 14f), o(e.x, e.y - 48f), 2 * k); drawPath(Path().apply { moveTo(e.x * k, (e.y - 48f) * k); lineTo((e.x + 22f) * k, (e.y - 41f) * k); lineTo(e.x * k, (e.y - 34f) * k); close() }, Look.acc) }
            }
            Shape.TREE -> {
                listOf(Offset(150f, 150f) to 48f, Offset(215f, 122f) to 56f, Offset(270f, 160f) to 40f, Offset(120f, 196f) to 32f, Offset(292f, 214f) to 30f).forEach { (c, r) -> drawCircle(Color(0xFFD6EEE2), r * k, o(c.x, c.y)) }
                drawPath(path(listOf(Offset(195f, 580f), Offset(194f, 400f), Offset(195f, 150f))), Color(0xFF8C7A6B), style = Stroke(14 * k, cap = StrokeCap.Round))
                spots.forEach { s ->
                    val from = Offset(195f, s.y + 30f)
                    val col = if (s.part.state == PartState.HERE) Look.acc else Color(0xFF8C7A6B)
                    drawPath(path(listOf(from, Offset(s.x, s.y))), col, style = Stroke((if (s.part.state == PartState.HERE) 6f else 4f) * k, cap = StrokeCap.Round,
                        pathEffect = if (s.part.state == PartState.LATER) PathEffect.dashPathEffect(floatArrayOf(3 * k, 7 * k)) else null))
                }
                drawLine(Color(0xFFB9D7C8), o(130f, 582f), o(262f, 582f), 3 * k, StrokeCap.Round)
            }
            Shape.REMEMBER -> when (j.style) {
                "GARDEN" -> {
                    drawPath(Path().apply { moveTo(0f, 300 * k); cubicTo(120 * k, 280 * k, 260 * k, 300 * k, size.width, 290 * k); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, Color(0xFFCFE6D6))
                    drawPath(Path().apply { moveTo(0f, 360 * k); cubicTo(120 * k, 340 * k, 260 * k, 370 * k, size.width, 350 * k); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, Color(0xFFBFDDC8))
                    drawPath(path(listOf(Offset(150f, 620f), Offset(190f, 540f), Offset(200f, 330f))), Color(0xFFE9DCC9), style = Stroke(30 * k, cap = StrokeCap.Round))
                    spots.forEachIndexed { i, s -> plant(this, i, s, k) }
                    drawRoundRect(Color(0xFFA9774F), o(140f, 560f), Size(110 * k, 10 * k), CornerRadius(4 * k)); drawRoundRect(Color(0xFFA9774F), o(146f, 542f), Size(98 * k, 8 * k), CornerRadius(4 * k))
                }
                "LETTER" -> {
                    drawRoundRect(Color(0xFFFFFDF8), o(18f, 96f), Size(354 * k, 470 * k), CornerRadius(6 * k))
                }
                else -> {
                    drawRect(Color(0xFFF3E6DA), size = Size(size.width, 440 * k)); drawRect(Color(0xFFDDC4A8), o(0f, 440f), Size(size.width, size.height))
                    listOf(480f, 530f, 590f).forEach { drawLine(Color(0xFFCDB193), o(0f, it), o(SW, it), 2 * k) }
                    drawRect(Color(0xFFC9A889), o(0f, 432f), Size(size.width, 10 * k))
                    drawRect(Brush.radialGradient(listOf(Color(0xD9FFE3C8), Color.Transparent), o(86f, 200f), 380 * k), size = size)
                    drawOval(Color(0x73CDB0C9), o(30f, 560f), Size(380 * k, 80 * k))
                    spots.forEach { s -> keepsake(this, keepsakeFor(s.part.name), s, k) }
                }
            }
            Shape.CROSSROADS -> {
                val gate = Offset(SW / 2, 520f)
                spots.forEach { s ->
                    val out = s.part.state == PartState.OUT
                    val col = listOf(Look.acc, Look.blue, Look.rose, Color(0xFF3E9C7A))[spots.indexOf(s) % 4]
                    val p = path(listOf(Offset(s.x, s.y + 30f), gate))
                    drawPath(p, col.copy(alpha = if (out) 0.08f else 0.18f), style = Stroke(22 * k, cap = StrokeCap.Round))
                    if (!out) drawPath(p, col, style = Stroke(2.4f * k, pathEffect = dash))
                }
            }
            Shape.RIVER -> {
                listOf(Pair(Offset(0f, 220f), Offset(182f, 200f)), Pair(Offset(SW, 270f), Offset(222f, 274f)), Pair(Offset(0f, 400f), Offset(136f, 410f))).forEach { (a, b) ->
                    drawLine(Color(0xFFDCE8F4), o(a.x, a.y), o(b.x, b.y), 7 * k, StrokeCap.Round) }
                val pts = spots.map { Offset(it.x, it.y) }.ifEmpty { listOf(Offset(200f, 120f), Offset(200f, 560f)) }
                drawPath(path(listOf(Offset(200f, 100f)) + pts + Offset(200f, 600f)), Color(0xFFC3DBF2), style = Stroke(40 * k, cap = StrokeCap.Round))
                drawPath(path(listOf(Offset(200f, 100f)) + pts + Offset(200f, 600f)), Look.blue, style = Stroke(1.8f * k, pathEffect = dash))
            }
            Shape.ORBIT -> {
                spots.forEachIndexed { i, s -> drawCircle(if (s.part.state == PartState.HERE) Look.acc else Color(0xFFCDBDE6).copy(alpha = (1f - i * 0.12f).coerceAtLeast(0.3f)), (64f + i * (110f / max(1, spots.size - 1))) * k, o(SW / 2, 300f), style = Stroke((if (i == 0) 3f else 2f) * k)) }
            }
            Shape.SHELF -> {
                drawRoundRect(Color(0xFFEFE3D3), o(20f, 96f), Size(350 * k, 470 * k), CornerRadius(18 * k))
                spots.forEach { s -> drawRoundRect(Color(0xFFC9A889), o(20f, s.y + s.h / 2 - 6f), Size(350 * k, 12 * k), CornerRadius(3 * k)) }
            }
            Shape.VOYAGE -> {
                drawPath(Path().apply { moveTo(0f, 140 * k); cubicTo(120 * k, 150 * k, 260 * k, 130 * k, size.width, 140 * k); lineTo(size.width, 600 * k); lineTo(0f, 600 * k); close() }, Color(0xFFD2E8F2))
                drawPath(path(spots.map { Offset(it.x, it.y) }), Look.acc, style = Stroke(2.2f * k, pathEffect = dash))
                spots.forEach { s -> drawOval(Color(0xFFE8D7B4), o(s.x - s.w, s.y - s.w * 0.5f), Size(s.w * 2 * k, s.w * k)); drawCircle(Look.mint, 8 * k, o(s.x - 12f, s.y - 8f)) }
            }
            Shape.CLIMB -> {
                drawPath(Path().apply { moveTo(0f, 560 * k); lineTo(90 * k, 420 * k); lineTo(150 * k, 470 * k); lineTo(230 * k, 230 * k); lineTo(300 * k, 360 * k); lineTo(size.width, 300 * k); lineTo(size.width, 600 * k); lineTo(0f, 600 * k); close() }, Color(0xFFDCE4EE))
                drawPath(Path().apply { moveTo(230 * k, 230 * k); lineTo(210 * k, 270 * k); lineTo(230 * k, 264 * k); lineTo(246 * k, 276 * k); lineTo(254 * k, 260 * k); close() }, Color.White)
                val pts = spots.map { Offset(it.x, it.y) }
                drawPath(path(pts), Look.acc, style = Stroke(3 * k, cap = StrokeCap.Round))
                val hereI = spots.indexOfFirst { it.part.state == PartState.HERE }
                if (hereI > 0) drawPath(path(pts.take(hereI + 1)), Look.acc.copy(alpha = 0.18f), style = Stroke(8 * k, cap = StrokeCap.Round))
            }
            Shape.COUNTDOWN -> {
                drawCircle(Color(0xFFEBDCEA), 150 * k, o(SW / 2, 300f), style = Stroke(18 * k))
                drawCircle(Color.White, 104 * k, o(SW / 2, 300f))
            }
        }
    }
}

private fun plant(d: DrawScope, i: Int, s: Spot, k: Float) = with(d) {
    val c = Offset(s.x * k, s.y * k)
    when (i % 6) {
        0 -> { drawRect(Color(0xFF8C7A6B), Offset(c.x - 6 * k, c.y - 10 * k), Size(12 * k, 80 * k)); drawCircle(Color(0xFF9FD3B7), 48 * k, Offset(c.x, c.y - 30 * k)); drawCircle(Color(0xFFB5DFC6), 28 * k, Offset(c.x - 24 * k, c.y - 14 * k)) }
        1 -> listOf(-20f to 0f, 0f to -10f, 18f to 4f, -6f to 12f).forEach { (x, y) -> drawCircle(Color(0xFFB39AD9), 9 * k, Offset(c.x + x * k, c.y + y * k)) }
        2 -> { drawRect(Color(0xFF8C7A6B), Offset(c.x - 2 * k, c.y - 4 * k), Size(5 * k, 40 * k)); drawCircle(Color(0xFFA9DCC6), 16 * k, Offset(c.x, c.y - 8 * k)) }
        3 -> listOf(-16f to 6f, 2f to -4f, -22f to -8f, 0f to 10f).forEach { (x, y) -> drawCircle(Color(0xFFE2A9BF), 11 * k, Offset(c.x + x * k, c.y + y * k)) }
        4 -> listOf(-12f, 0f, 12f).forEach { x -> drawLine(Color(0xFF7DB592), Offset(c.x + x * k, c.y + 24 * k), Offset(c.x + x * k, c.y - 8 * k), 3 * k); drawCircle(Color(0xFFF6F2FB), 7 * k, Offset(c.x + x * k, c.y - 12 * k)) }
        else -> { drawRect(Color(0xFF8E623F), Offset(c.x - 2 * k, c.y), Size(4 * k, 26 * k)); drawRoundRect(Color(0xFFFBF8F2), Offset(c.x - 14 * k, c.y - 14 * k), Size(28 * k, 18 * k), CornerRadius(3 * k)) }
    }
}

private fun keepsake(d: DrawScope, kind: Keepsake, s: Spot, k: Float) = with(d) {
    val c = Offset(s.x * k, s.y * k); val w = s.w * k; val h = s.h * k
    when (kind) {
        Keepsake.ROD -> { drawLine(Color(0xFF7A5A3E), Offset(c.x - w / 2, c.y - h / 4), Offset(c.x + w / 2, c.y + h / 4), 3 * k, StrokeCap.Round); drawCircle(Color(0xFF4F5B66), 9 * k, Offset(c.x - w / 3, c.y - h / 6)) }
        Keepsake.GUITAR -> { drawRect(Color(0xFF6E4A33), Offset(c.x - 6 * k, c.y - h / 2), Size(12 * k, h * 0.55f)); drawCircle(Color(0xFFB5654F), w * 0.32f, Offset(c.x, c.y + h * 0.12f)); drawCircle(Color(0xFFB5654F), w * 0.42f, Offset(c.x, c.y + h * 0.32f)); drawCircle(Color(0xFF5B2F25), 10 * k, Offset(c.x, c.y + h * 0.26f)) }
        Keepsake.FRAME -> { drawRect(Color(0xFFB98B5E), Offset(c.x - w / 2, c.y - h / 2), Size(w, h)); drawRect(Color(0xFFC9B6E4), Offset(c.x - w / 2 + 6 * k, c.y - h / 2 + 6 * k), Size(w - 12 * k, h - 12 * k)) }
        Keepsake.PHONE -> drawRoundRect(Color(0xFF2E2A36), Offset(c.x - w / 2, c.y - h / 3), Size(w, h * 0.5f), CornerRadius(4 * k))
        Keepsake.CARD -> drawPath(Path().apply { moveTo(c.x - w / 2, c.y + h / 3); lineTo(c.x, c.y - h / 3); lineTo(c.x + w / 2, c.y + h / 3); close() }, Color(0xFFFBF8F2))
        Keepsake.COOLER -> { drawRoundRect(Color(0xFF5D8FC0), Offset(c.x - w / 2, c.y - h / 4), Size(w, h * 0.7f), CornerRadius(8 * k)); drawRoundRect(Color(0xFFE9EEF5), Offset(c.x - w / 2 - 4 * k, c.y - h / 3), Size(w + 8 * k, 12 * k), CornerRadius(5 * k)) }
        Keepsake.BOOK -> { drawRect(Color(0xFF7443B1), Offset(c.x - w / 2, c.y - h / 3), Size(w * 0.45f, h * 0.66f)); drawRect(Color(0xFF4A86C5), Offset(c.x, c.y - h / 3), Size(w * 0.45f, h * 0.66f)) }
        Keepsake.MUG -> { drawRoundRect(Color(0xFFF3F0F6), Offset(c.x - w / 3, c.y - h / 3), Size(w * 0.6f, h * 0.66f), CornerRadius(4 * k)); drawCircle(Color(0xFFF3F0F6), w * 0.15f, Offset(c.x + w * 0.32f, c.y), style = Stroke(4 * k)) }
        Keepsake.KEYS -> { drawCircle(Color(0xFF9C9C9C), w * 0.2f, Offset(c.x - w * 0.15f, c.y), style = Stroke(3 * k)); drawLine(Color(0xFF9C9C9C), c, Offset(c.x + w * 0.35f, c.y), 4 * k) }
        Keepsake.BOX -> { drawRoundRect(Color(0xFFC2876B), Offset(c.x - w / 3, c.y - h / 4), Size(w * 0.66f, h * 0.5f), CornerRadius(4 * k)); drawRect(Color(0xFFE8B9CB), Offset(c.x - 3 * k, c.y - h / 4), Size(6 * k, h * 0.5f)) }
    }
}
