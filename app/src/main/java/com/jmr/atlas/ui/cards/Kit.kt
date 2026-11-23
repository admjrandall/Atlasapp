package com.jmr.atlas.ui.cards

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.components.LocalFiles
import com.jmr.atlas.ui.components.ago
import com.jmr.atlas.ui.components.plural
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.accent
import com.jmr.atlas.ui.theme.icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** True when a card is shown in the Fold's side pane: there is no back arrow there. */
val LocalInPane = staticCompositionLocalOf { false }

/** Shared building blocks for the per-type item cards (design I01–I15): card → actions → why now → type details → collapsed. */

@Composable
fun CardTopBar(vm: AtlasViewModel, o: KObject, overImage: Boolean = false, extra: @Composable RowScope.() -> Unit = {}) {
    var menu by remember { mutableStateOf(false) }
    val bg = if (overImage) Atlas.Surface.copy(alpha = 0.92f) else Color.Transparent
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vm.canGoBack() && !LocalInPane.current) IconButton({ vm.back() }, Modifier.clip(CircleShape).background(bg).semantics { contentDescription = "Back" }.testTag("back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Atlas.DeepPlum) }
        Spacer(Modifier.weight(1f))
        extra()
        IconButton({ vm.show(Overlay.Edit(o.id)) }, Modifier.clip(CircleShape).background(bg).semantics { contentDescription = "Edit" }.testTag("edit")) { Icon(Icons.Outlined.Edit, null, tint = Atlas.DeepPlum) }
        Box {
            IconButton({ menu = true }, Modifier.clip(CircleShape).background(bg).semantics { contentDescription = "More" }) { Icon(Icons.Outlined.MoreVert, null, tint = Atlas.DeepPlum) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Show on map") }, { menu = false; vm.focusOn(o.id) }, leadingIcon = { Icon(Icons.Outlined.Hub, null) })
                DropdownMenuItem({ Text("Connect to…") }, { menu = false; vm.show(Overlay.Connect(o.id)) }, leadingIcon = { Icon(Icons.Outlined.AddLink, null) })
                DropdownMenuItem({ Text("Add a field") }, { menu = false; vm.show(Overlay.AddField(o.id)) }, leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null) })
                DropdownMenuItem({ Text(if (o.pinned) "Unpin" else "Pin") }, { menu = false; vm.repo.update(o.copy(pinned = !o.pinned), if (o.pinned) "Unpinned" else "Pinned") }, leadingIcon = { Icon(Icons.Outlined.PushPin, null) })
                DropdownMenuItem({ Text("Move to trash") }, { menu = false; vm.repo.trash(o.id); vm.back(); vm.say("Moved “${o.title}” to trash.") { vm.repo.restore(o.id) } }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
            }
        }
    }
}

data class CardAction(val icon: ImageVector, val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

/** Up to five round actions with labels, evenly spaced (Call · Text · Email · Directions · Note). */
@Composable
fun ActionRow(actions: List<CardAction>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        actions.take(5).forEach { a ->
            Column(Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = a.enabled, onClick = a.onClick).padding(4.dp).testTag("action_${a.label}"),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(if (a.enabled) Atlas.PrimaryContainer else Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) {
                    Icon(a.icon, null, tint = if (a.enabled) Atlas.Primary else Atlas.SecondaryText.copy(alpha = 0.5f))
                }
                Spacer(Modifier.height(6.dp))
                Text(a.label, style = MaterialTheme.typography.labelMedium, color = if (a.enabled) Atlas.DeepPlum else Atlas.SecondaryText, maxLines = 1)
            }
        }
    }
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier, trailing: String? = null, trailingColor: Color = Atlas.SecondaryText, action: String? = null, plus: Boolean = true, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = trailingColor) }
        if (action != null && onAction != null) TextButton(onAction, Modifier.testTag("section_$action")) { if (plus) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }; Text(action) }
    }
}

@Composable
fun Box16(modifier: Modifier = Modifier, color: Color = Atlas.Surface, content: @Composable ColumnScope.() -> Unit) =
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = color, border = if (color == Atlas.Surface) BorderStroke(1.dp, Atlas.Border) else null) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), content = content)
    }

/** A value with its label underneath: the value is what you read, the label says what it is (design rule from I13). */
@Composable
fun FieldRow(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Atlas.DeepPlum, onClick: (() -> Unit)? = null,
             trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Atlas.Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 2)
        }
        trailing?.invoke()
    }
}

@Composable
fun Divider16() = HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = Atlas.Border.copy(alpha = 0.6f))

@Composable
fun NavRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Atlas.Primary); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(sub, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.SecondaryText)
        }
    }
}

/** The two collapsed rows every card ends with: connections and history. */
@Composable
fun CollapsedFooter(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val nb = snap.neighbours(o.id)
    if (nb.isNotEmpty()) {
        val counts = nb.groupBy { it.first.type }.entries.sortedByDescending { it.value.size }.joinToString(" · ") { (t, l) -> plural(l.size, t.label.lowercase(), t.plural.lowercase()) }
        NavRow(Icons.Outlined.Hub, "All connections", counts) { vm.go(Route.Connections(o.id)) }
    }
    val hist = snap.activityOf(o.id)
    val created = java.time.Instant.ofEpochMilli(o.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    NavRow(Icons.Outlined.History, "History", "Created ${com.jmr.atlas.ui.components.shortDate(created)}" + (hist.firstOrNull()?.takeIf { it.at - o.created > 60_000 }?.let { " · ${it.text.lowercase().take(40)} ${ago(it.at)}" } ?: "")) { vm.go(Route.History(o.id)) }
}

/** Connected items as type-coloured chips (documents, links). */
@Composable
fun ConnectedChips(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val nb = snap.neighbours(o.id).map { it.first }
    Section("Connected", action = "Connect") { vm.show(Overlay.Connect(o.id)) }
    if (nb.isEmpty()) { Text("Not connected to anything yet.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText); return }
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        nb.take(12).forEach { n -> val a = accent(n.type); com.jmr.atlas.ui.components.Pill(n.title, icon(n.type), if (n.type == ObjType.PROJECT) Atlas.PrimaryContainer else a.container, if (n.type == ObjType.PROJECT) Atlas.DeepPlum else a.content) { vm.open(n.id) } }
    }
}

/** Photo for a person: their phone-contact photo, a photo chosen in Atlas (decrypted in memory), or initials. */
@Composable
fun rememberPhoto(o: KObject, px: Int = 400): ImageBitmap? {
    val ctx = LocalContext.current
    val files = LocalFiles.current
    return produceState<ImageBitmap?>(null, o.photo) {
        value = if (o.photo.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val bytes = if (o.photo.startsWith("vault:")) files?.read(o.photo.removePrefix("vault:")) else ctx.contentResolver.openInputStream(android.net.Uri.parse(o.photo))?.use { it.readBytes() }
                bytes ?: return@runCatching null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                var s = 1; while (maxOf(opts.outWidth, opts.outHeight) / (s * 2) >= px) s *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

@Composable
fun PersonPhoto(o: KObject, size: Dp, modifier: Modifier = Modifier) {
    val img = rememberPhoto(o)
    Box(modifier.size(size).clip(CircleShape).background(Atlas.PrimaryContainer), contentAlignment = Alignment.Center) {
        if (img != null) Image(img, "Photo of ${o.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(o.initials, color = Atlas.Primary, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.36f).sp)
    }
}

/** “Why this matters now” — deterministic lines, each tied to the item it came from; open tasks can be ticked in place. */
@Composable
fun WhyNowBox(vm: AtlasViewModel, snap: Snapshot, o: KObject, extra: List<Pair<ImageVector, String>> = emptyList(), skip: (String) -> Boolean = { false }) {
    val lines = vm.repo.context(o.id).filterNot { skip(it.text) }
    if (lines.isEmpty() && extra.isEmpty()) return
    Surface(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("why_now"), shape = RoundedCornerShape(22.dp), color = Atlas.SurfaceSecondary) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Atlas.Primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Why this matters now", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(6.dp))
            extra.forEach { (ic, t) -> WhyLine(ic, t, null) }
            lines.forEach { l ->
                val src = l.sources.firstOrNull()?.let { snap.objects[it] }
                val task = src?.takeIf { it.type == ObjType.TASK && !it.done && it.id != o.id }
                val ic = when { l.text.startsWith("Waiting") -> Icons.Outlined.Schedule; l.text.startsWith("Next milestone") || l.text.startsWith("Target") -> Icons.Outlined.Flag; l.text.startsWith("New this week") -> Icons.Outlined.FiberNew; else -> Icons.Outlined.Event }
                WhyLine(ic, l.text, task?.let { { vm.repo.toggleDone(it.id) } }, late = l.text.contains("was due")) { src?.let { s -> if (s.id != o.id) vm.open(s.id) } }
            }
        }
    }
}

@Composable
private fun WhyLine(ic: ImageVector, text: String, onCheck: (() -> Unit)?, late: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 44.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (onCheck != null) Checkbox(false, { onCheck() }, modifier = Modifier.size(40.dp).semantics { contentDescription = "Complete" })
        else Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.DeepTeal, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(6.dp))
        val (a, b) = text.split(" — ", limit = 2).let { it[0] to it.getOrNull(1) }
        Text(androidx.compose.ui.text.buildAnnotatedString {
            append(a)
            if (b != null) { append(" — "); pushStyle(androidx.compose.ui.text.SpanStyle(color = if (late) Atlas.Error else Atlas.SecondaryText)); append(b); pop() }
        }, style = MaterialTheme.typography.bodyMedium, color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
    }
}

/** Custom fields block (“Your fields”). Secret values stay masked until Show. */
@Composable
fun YourFields(vm: AtlasViewModel, o: KObject) {
    val fields = vm.repo.fieldsFor(o).filter { it.value.isNotBlank() || it.extra.isNotBlank() }
    Section("Your fields", action = "Add field") { vm.show(Overlay.AddField(o.id)) }
    if (fields.isEmpty()) { Text("Add your own fields — a budget, a license number, a rating.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText); return }
    Box16 {
        fields.forEachIndexed { i, f ->
            if (i > 0) Divider16()
            var shown by remember(f.id) { mutableStateOf(false) }
            val value = when {
                f.value.isBlank() -> "Not set"
                f.flag && !shown -> mask(f.value)
                f.type == FieldType.CHECKBOX -> if (f.value == "true") "Yes" else "No"
                f.type == FieldType.RATING -> "★".repeat(f.value.toIntOrNull()?.coerceIn(0, 5) ?: 0) + "☆".repeat(5 - (f.value.toIntOrNull()?.coerceIn(0, 5) ?: 0))
                f.type == FieldType.DATE -> runCatching { com.jmr.atlas.ui.components.dayDate(java.time.LocalDate.parse(f.value)) }.getOrDefault(f.value)
                f.type == FieldType.PERSON || f.type == FieldType.ITEM -> vm.repo.snap.objects[f.value]?.title ?: f.value
                f.type == FieldType.MONEY -> money(f.value)
                else -> f.value
            }
            FieldRow(fieldIcon(f.type, f.flag), value, f.label, valueColor = if (f.value.isBlank()) Atlas.SecondaryText else Atlas.DeepPlum,
                onClick = when { f.type == FieldType.PERSON || f.type == FieldType.ITEM -> vm.repo.snap.objects[f.value]?.let { { vm.open(it.id) } }; else -> { { vm.show(Overlay.Edit(o.id)) } } }) {
                if (f.flag && f.value.isNotBlank()) TextButton({ shown = !shown }) { Text(if (shown) "Hide" else "Show") }
            }
        }
    }
}

fun money(v: String) = v.trim().let { if (it.isEmpty() || it.startsWith("$") || it.first().isLetter()) it else "$$it" }

fun mask(v: String) = if (v.length <= 4) "••••" else v.take(3) + "••••" + v.takeLast(4)

fun fieldIcon(t: FieldType, secret: Boolean = false): ImageVector = if (secret) Icons.Outlined.Shield else when (t) {
    FieldType.TEXT -> Icons.Outlined.Notes; FieldType.NUMBER -> Icons.Outlined.FormatListNumbered; FieldType.MONEY -> Icons.Outlined.Tune; FieldType.DATE -> Icons.Outlined.CalendarMonth
    FieldType.CHOICE -> Icons.Outlined.FilterList; FieldType.CHECKBOX -> Icons.Outlined.CheckBox; FieldType.LINK -> Icons.Outlined.Link; FieldType.PERSON -> Icons.Outlined.Person
    FieldType.ITEM -> Icons.Outlined.Hub; FieldType.RATING -> Icons.Outlined.StarOutline
}

@Composable
fun Quote(text: String, sub: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Atlas.DeepPlum)
            if (sub.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(sub, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary, content = inner)
    else Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary, content = inner)
}

@Composable
fun CenteredNote(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
