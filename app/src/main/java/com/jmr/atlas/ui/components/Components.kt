package com.jmr.atlas.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.data.SecureFiles
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.accent
import com.jmr.atlas.ui.theme.icon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val LocalFiles = staticCompositionLocalOf<SecureFiles?> { null }

/** Decrypted image thumbnail for IMAGE objects with an original in the vault (kept in memory only). */
@Composable
fun rememberThumb(o: KObject?, px: Int = 480): ImageBitmap? {
    val files = LocalFiles.current
    val key = o?.vaultFile
    return produceState<ImageBitmap?>(null, key) {
        value = if (o == null || files == null || o.type != ObjType.IMAGE || o.vaultFile.isBlank()) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val b = files.read(o.vaultFile) ?: return@runCatching null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(b, 0, b.size, opts)
                var s = 1; while (maxOf(opts.outWidth, opts.outHeight) / (s * 2) >= px) s *= 2
                BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

@Composable
fun ObjAvatar(o: KObject, size: Dp = 44.dp, proposed: Boolean = false, modifier: Modifier = Modifier) {
    val a = accent(o.type)
    val thumb = rememberThumb(o, 160)
    val shape = if (o.type == ObjType.PERSON || o.type == ObjType.PROJECT) CircleShape else RoundedCornerShape(size * 0.3f)
    Box(
        modifier.size(size).clip(shape).background(a.container)
            .then(if (proposed) Modifier.dashedBorder(Atlas.Primary, shape == CircleShape, size * 0.3f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            thumb != null -> Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            o.type == ObjType.PERSON -> Text(o.initials, color = a.content, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.36f).sp)
            else -> Icon(icon(o.type), null, tint = a.content, modifier = Modifier.size(size * 0.48f))
        }
    }
}

fun Modifier.dashedBorder(color: Color, circle: Boolean, radius: Dp = 12.dp) = drawBehind {
    val st = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    if (circle) drawCircle(color, radius = size.minDimension / 2 - st.width, style = st)
    else drawRoundRect(color, cornerRadius = CornerRadius(radius.toPx()), style = st)
}

@Composable
fun Pill(text: String, leading: androidx.compose.ui.graphics.vector.ImageVector? = null, container: Color = Atlas.PrimaryContainer, content: Color = Atlas.DeepPlum,
         modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val inner: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { Icon(leading, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (onClick != null) Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = content, modifier = modifier.heightIn(min = 40.dp), content = inner)
    else Surface(shape = CircleShape, color = container, contentColor = content, modifier = modifier, content = inner)
}

fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"

/** One line under a title: type, then the single most useful fact. Example-ness is shown once per screen, not per row. */
fun typeLine(o: KObject, snap: Snapshot? = null): String = buildString {
    append(o.type.label)
    when (o.type) {
        ObjType.PROJECT -> o.projectStatus?.let { append(" · ${it.label}") }
        ObjType.PERSON -> listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { append(" · $it") }
        ObjType.TASK -> { if (o.done) append(" · Done") else { o.due?.let { append(" · ${relDate(it)}") }; if (o.blocked) append(" · Blocked") } }
        ObjType.EVENT -> o.eventDate?.let { append(" · ${relDate(it)}") }
        ObjType.DOCUMENT, ObjType.IMAGE -> {
            val kind = when { o.mime == "application/pdf" -> "PDF"; o.mime.startsWith("image/") -> "Image"; o.mime.startsWith("text/") -> "Text"; else -> "" }
            if (kind.isNotEmpty() && kind != o.type.label) append(" · $kind")
            if (o.vaultFile.isBlank()) append(" · no file") else if (o.sizeBytes > 0) append(" · ${size(o.sizeBytes)}")
        }
        ObjType.LINK -> o.url.takeIf { it.isNotBlank() }?.let { append(" · ${it.removePrefix("https://").removePrefix("http://").substringBefore('/')}") }
        else -> {}
    }
}

fun shortDate(d: LocalDate, today: LocalDate = LocalDate.now()): String =
    d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "MMM d" else "MMM d, yyyy"))

/** “Fri, Nov 27” — used wherever a weekday alone would be ambiguous. */
fun dayDate(d: LocalDate, today: LocalDate = LocalDate.now()): String =
    d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"))

fun size(b: Long) = when { b < 1024 -> "$b B"; b < 1024 * 1024 -> "${b / 1024} KB"; else -> "%.1f MB".format(b / 1048576.0) }

fun relDate(d: LocalDate, today: LocalDate = LocalDate.now()): String {
    val n = ChronoUnit.DAYS.between(today, d)
    return when {
        n == 0L -> "Today"; n == 1L -> "Tomorrow"; n == -1L -> "Yesterday"
        n in 2..6 -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
        n < 0 && n > -7 -> "${-n} days ago"
        d.year == today.year -> d.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

fun ago(ms: Long, now: Long = System.currentTimeMillis()): String {
    val m = (now - ms) / 60_000
    return when { m < 1 -> "just now"; m < 60 -> "${m}m ago"; m < 1440 -> "${m / 60}h ago"; m < 10080 -> "${m / 1440}d ago"
        else -> java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")) }
}

@Composable
fun ObjRow(o: KObject, snap: Snapshot, modifier: Modifier = Modifier, sub: String? = null, proposed: Boolean = false, onToggle: (() -> Unit)? = null,
           trailing: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (o.type == ObjType.TASK && onToggle != null) {
            Checkbox(o.done, { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal), modifier = Modifier.semantics { contentDescription = if (o.done) "Mark ${o.title} not done" else "Complete ${o.title}" })
        } else ObjAvatar(o, 40.dp, proposed)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(o.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (o.done) Atlas.SecondaryText else Atlas.DeepPlum,
                textDecoration = if (o.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
            Text(sub ?: typeLine(o, snap), style = MaterialTheme.typography.bodySmall, color = if (o.type == ObjType.TASK && !o.done && o.due != null && o.due < LocalDate.now()) Atlas.Error else Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (proposed) Pill("Proposed", container = Atlas.SurfaceSecondary, content = Atlas.Primary)
        trailing?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Empty(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Hub, null, tint = Atlas.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(12.dp)); action() }
    }
}

@Composable
fun AtlasMark(size: Dp = 28.dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val c = center; val r = this.size.minDimension
        val pts = listOf(0.18f to 0.22f, 0.82f to 0.24f, 0.86f to 0.78f, 0.2f to 0.8f, 0.5f to 0.06f, 0.5f to 0.95f)
        pts.forEach { (x, y) -> drawLine(Atlas.Orchid, c, androidx.compose.ui.geometry.Offset(x * r, y * r), strokeWidth = r * 0.05f) }
        pts.forEachIndexed { i, (x, y) -> drawCircle(if (i % 2 == 0) Atlas.DeepTeal else Atlas.Primary, r * 0.075f, androidx.compose.ui.geometry.Offset(x * r, y * r)) }
        drawCircle(Atlas.Primary, r * 0.17f, c)
    }
}

@Composable
fun AtlasWordmark(modifier: Modifier = Modifier) {
    Row(modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp).semantics(mergeDescendants = true) { contentDescription = "Atlas" }, verticalAlignment = Alignment.CenterVertically) {
        AtlasMark()
        Spacer(Modifier.width(10.dp))
        Text("ATLAS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 5.sp, fontWeight = FontWeight.SemiBold), color = Atlas.DeepPlum)
    }
}

@Composable
fun BadgeIcon(count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick, modifier.semantics { contentDescription = if (count > 0) "$desc, $count waiting" else desc }) {
        BadgedBox(badge = { if (count > 0) Badge(containerColor = Atlas.Coral, contentColor = Color.White) { Text(if (count > 99) "99+" else "$count") } }) {
            Icon(icon, null, tint = Atlas.DeepPlum)
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, color: Color = Atlas.Surface, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), color = color, border = BorderStroke(1.dp, Atlas.Border)) {
        Column(Modifier.padding(16.dp), content = content)
    } else Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = color, border = BorderStroke(1.dp, Atlas.Border)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun Avatar(o: KObject?, size: Dp, modifier: Modifier = Modifier) {
    if (o != null) ObjAvatar(o, size, modifier = modifier)
    else Box(modifier.size(size).clip(CircleShape).background(Atlas.Primary).border(2.dp, Atlas.Surface, CircleShape))
}
