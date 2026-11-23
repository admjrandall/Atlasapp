package com.jmr.atlas.ui.cards

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.AiMode
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.components.LocalFiles
import com.jmr.atlas.ui.components.Pill
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.components.shortDate
import com.jmr.atlas.ui.components.size
import com.jmr.atlas.ui.theme.Atlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** First page of a PDF (or the image itself), decrypted in memory only. Returns bitmap + page count. */
@Composable
fun rememberPreview(o: KObject): Pair<ImageBitmap?, Int> {
    val files = LocalFiles.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return produceState<Pair<ImageBitmap?, Int>>(null to 0, o.vaultFile, o.coverFile) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val f = files ?: return@runCatching null to 0
                when {
                    o.type == ObjType.LINK && o.coverFile.isNotBlank() -> f.read(o.coverFile)?.let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() } to 0
                    o.mime == "application/pdf" && o.vaultFile.isNotBlank() -> {
                        val bytes = f.read(o.vaultFile) ?: return@runCatching null to 0
                        val tmp = File.createTempFile("prev", ".pdf", ctx.cacheDir)
                        try {
                            tmp.writeBytes(bytes)
                            PdfRenderer(ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)).use { r ->
                                r.openPage(0).use { p ->
                                    val bmp = Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
                                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); bmp.asImageBitmap() to r.pageCount
                                }
                            }
                        } finally { tmp.delete() }
                    }
                    o.mime.startsWith("image/") && o.vaultFile.isNotBlank() -> f.read(o.vaultFile)?.let { b ->
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(b, 0, b.size, opts)
                        var s = 1; while (maxOf(opts.outWidth, opts.outHeight) / (s * 2) >= 1400) s *= 2
                        BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
                    } to 1
                    else -> null to 0
                }
            }.getOrDefault(null to 0)
        }
    }.value
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    val (img, pages) = rememberPreview(o)
    val aiStatus by vm.ai.status.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    var showAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp).testTag("document_card")) {
        CardTopBar(vm, o)
        Surface(onClick = { if (o.vaultFile.isNotBlank()) platform.openFile(o) }, shape = RoundedCornerShape(22.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 380.dp)) {
            if (img != null) Image(img, "Preview of ${o.title}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
            else Box(Modifier.height(180.dp), contentAlignment = Alignment.Center) { Icon(com.jmr.atlas.ui.theme.icon(o.type), null, tint = Atlas.BlueText, modifier = Modifier.size(48.dp)) }
        }
        if (pages > 1) CenteredNote("Page 1 of $pages")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            if (o.kind.isNotBlank()) Pill(o.kind, Icons.Outlined.Description, Atlas.PowderBlue, Atlas.BlueText) { vm.show(Overlay.Edit(o.id)) }
            val fmt = when { o.mime == "application/pdf" -> "PDF"; o.mime.startsWith("image/") -> o.mime.substringAfter('/').uppercase(); o.mime.startsWith("text/") -> "Text"; else -> "File" }
            Pill(listOfNotNull(fmt, if (pages > 1) "$pages pages" else null, if (o.sizeBytes > 0) size(o.sizeBytes) else null).joinToString(" · "), container = Atlas.SurfaceSecondary, content = Atlas.DeepPlum)
        }
        Spacer(Modifier.height(8.dp))
        Text(o.title, style = MaterialTheme.typography.headlineSmall)
        ActionRow(listOf(
            CardAction(Icons.Outlined.OpenInNew, "Open", o.vaultFile.isNotBlank()) { platform.openFile(o) },
            CardAction(Icons.Outlined.Share, "Share", o.extracted.isNotBlank()) { platform.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, o.title).putExtra(Intent.EXTRA_TEXT, o.extracted.take(20_000)), o.title)) },
            CardAction(Icons.Outlined.DocumentScanner, "Scan more") { platform.scan() },
            CardAction(Icons.Outlined.ContentCopy, "Copy text", o.extracted.isNotBlank()) { platform.copy(o.title, o.extracted) },
        ))
        val from = snap.neighbours(o.id, false).map { it.first }.filter { it.type == ObjType.PERSON }
        Box16 {
            from.firstOrNull()?.let { p -> FieldRow(Icons.Outlined.Person, listOf(p.org, p.title).filter { it.isNotBlank() }.joinToString(" · "), "From", onClick = { vm.open(p.id) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.SecondaryText) }; Divider16() }
            FieldRow(Icons.Outlined.CalendarMonth, o.docDate?.let { dayDate(it) } ?: "Not found", "Document date · the date on the document", valueColor = if (o.docDate == null) Atlas.SecondaryText else Atlas.DeepPlum, onClick = { vm.show(Overlay.Edit(o.id)) })
            Divider16()
            val added = java.time.Instant.ofEpochMilli(o.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            FieldRow(Icons.Outlined.Download, dayDate(added), "Added" + (snap.activityOf(o.id).lastOrNull()?.text?.substringAfter("·", "")?.takeIf { it.isNotBlank() }?.let { " ·$it" } ?: ""))
        }

        val facts = snap.detailsOf(o.id, DetailKind.FACT)
        if (facts.isNotEmpty()) {
            Section("Key facts", trailing = "Read from the document · tap to fix")
            Box16 {
                facts.forEachIndexed { i, f ->
                    if (i > 0) Divider16()
                    var shown by remember(f.id) { mutableStateOf(false) }
                    val v = when { f.flag && !shown -> mask(f.value); f.type == FieldType.DATE -> runCatching { dayDate(LocalDate.parse(f.value)) }.getOrDefault(f.value); else -> f.value }
                    FieldRow(fieldIcon(f.type, f.flag), v, f.label, onClick = { vm.show(Overlay.Edit(o.id)) }) { if (f.flag) TextButton({ shown = !shown }) { Text(if (shown) "Hide" else "Show") } }
                }
            }
        }
        YourFields(vm, o)

        if (o.summary.isNotBlank() || (vm.settings.aiMode == AiMode.ON_DEVICE && aiStatus == Ai.Status.Ready && o.extracted.isNotBlank())) {
            Section("Summary", trailing = if (o.summary.isNotBlank()) "Written on this phone" else null)
            if (o.summary.isNotBlank()) Quote(o.summary, "")
            else OutlinedButton({ vm.summarize(o.id) }, enabled = aiBusy != o.id, modifier = Modifier.fillMaxWidth()) {
                if (aiBusy == o.id) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Reading…") }
                else { Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Summarize on this phone") }
            }
        }
        if (o.extracted.isNotBlank()) {
            Section("Text", action = if (showAll) null else "Show all", plus = false) { showAll = true }
            Surface(shape = RoundedCornerShape(20.dp), color = Atlas.SurfaceSecondary, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(if (showAll) o.extracted else "“${o.extracted.replace('\n', ' ').take(220)}…”", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                }
            }
        } else if (o.vaultFile.isNotBlank()) { Section("Text"); Text("No readable text was found on this phone.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText) }
        if (o.body.isNotBlank()) { Section("Your note"); Quote(o.body, "") }
        ConnectedChips(vm, snap, o)
        CollapsedFooter(vm, snap, o)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    val (img, _) = rememberPreview(o)
    val aiStatus by vm.ai.status.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp).testTag("link_card")) {
        Box {
            if (img != null) Image(img, null, Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
            else CoverBand(o.id.hashCode(), Atlas.PowderBlue, Atlas.Surface, Atlas.Peach)
            CardTopBar(vm, o, overImage = true)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(Atlas.BlueText)); Spacer(Modifier.width(8.dp))
                Text(listOfNotNull(o.site.ifBlank { com.jmr.atlas.integrations.Web.host(o.url) }, o.author.takeIf { it.isNotBlank() }, o.published?.let { it.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")) }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
            }
            Spacer(Modifier.height(6.dp))
            Text(o.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                if (o.kind.isNotBlank()) Pill(o.kind, Icons.Outlined.Lightbulb) { vm.show(Overlay.Edit(o.id)) }
                Pill(if (o.isRead) "Read" else "Unread", container = if (o.isRead) Atlas.SurfaceSecondary else Atlas.Peach, content = if (o.isRead) Atlas.DeepPlum else Atlas.CoralText)
                if (o.readMinutes > 0) Pill("${o.readMinutes} min read", Icons.Outlined.Schedule, Atlas.SurfaceSecondary, Atlas.DeepPlum)
                if (o.extracted.isNotBlank()) Pill("Copy saved", Icons.Outlined.Check, Atlas.CoolMint, Atlas.DeepTeal)
            }
            var reading by remember { mutableStateOf(false) }
            ActionRow(listOf(
                CardAction(Icons.Outlined.OpenInNew, "Open") { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) },
                CardAction(Icons.Outlined.Article, "Read copy", o.extracted.isNotBlank()) { reading = !reading },
                CardAction(Icons.Outlined.Share, "Share") { platform.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, o.url), o.title)) },
                CardAction(Icons.Outlined.Check, if (o.isRead) "Mark unread" else "Mark read") { vm.repo.update(o.copy(isRead = !o.isRead), if (o.isRead) "Marked unread" else "Marked read") },
            ))
            if (o.summary.isNotBlank()) { Section("Summary", trailing = if (vm.repo.snap.activityOf(o.id).any { it.text == "Summarized on this phone" }) "Written on this phone" else "From the page"); Quote(o.summary, "") }
            else if (vm.settings.aiMode == AiMode.ON_DEVICE && aiStatus == Ai.Status.Ready && o.extracted.isNotBlank())
                OutlinedButton({ vm.summarize(o.id) }, enabled = aiBusy != o.id, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Summarize on this phone") }
            if (reading && o.extracted.isNotBlank()) {
                Section("Saved copy")
                Surface(shape = RoundedCornerShape(20.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(o.extracted.take(60_000), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp)) }
                }
            }
            Section("Your note")
            Quote(o.body.ifBlank { "Why you saved this — tap to add" }, "") { vm.show(Overlay.Edit(o.id)) }
            val hl = snap.detailsOf(o.id, DetailKind.HIGHLIGHT)
            Section("Highlights", action = "Add") { vm.show(Overlay.AddLine(o.id, DetailKind.HIGHLIGHT)) }
            if (hl.isEmpty()) Text("Paste a passage worth keeping.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
            else Box16 { hl.forEach { h -> FieldRow(Icons.Outlined.FormatQuote, "“${h.value}”", "Highlighted ${shortDate(java.time.Instant.ofEpochMilli(h.created).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}") } }
            ConnectedChips(vm, snap, o)
            Spacer(Modifier.height(12.dp))
            val fetched = o.previewAt?.let { "Preview fetched ${shortDate(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}. Atlas downloaded this page only." }
                ?: if (!vm.settings.linkPreviews) "Link previews are off in Settings." else if (o.origin == Origin.EXAMPLE) "Example link — no page to fetch." else "Fetching the preview…"
            Text(fetched, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            if (vm.settings.linkPreviews) TextButton({ vm.sync.preview(o.id); vm.say("Refreshing the preview…") }) { Text("Refresh") }
            CollapsedFooter(vm, snap, o)
        }
    }
}

/** Notes, ideas, events, decisions, collections: content first, then what it touches. */
@Composable
fun GenericCard(vm: AtlasViewModel, snap: Snapshot, o: KObject) {
    val platform = LocalPlatform.current
    val a = com.jmr.atlas.ui.theme.accent(o.type)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp).testTag("generic_card")) {
        CardTopBar(vm, o)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(a.container), contentAlignment = Alignment.Center) { Icon(if (o.kind == "Voice note") Icons.Outlined.Mic else com.jmr.atlas.ui.theme.icon(o.type), null, tint = a.content) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(o.kind.ifBlank { o.type.label }, style = MaterialTheme.typography.labelLarge, color = Atlas.SecondaryText)
                val created = java.time.Instant.ofEpochMilli(o.created).atZone(java.time.ZoneId.systemDefault())
                Text("${dayDate(created.toLocalDate())} · ${com.jmr.atlas.data.Briefing.time(o.created)}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(o.title, style = MaterialTheme.typography.headlineSmall)
        if (o.type == ObjType.EVENT) {
            Spacer(Modifier.height(10.dp))
            Box16 {
                FieldRow(Icons.Outlined.Flag, o.eventDate?.let { dayDate(it) } ?: "No date", "Date", onClick = { vm.show(Overlay.Edit(o.id)) })
                Divider16()
                if (o.calendarEventId != null) FieldRow(Icons.Outlined.Event, "Open in calendar", "On your calendar", onClick = { platform.launch(com.jmr.atlas.integrations.Calendars.viewIntent(o.calendarEventId)) })
                else FieldRow(Icons.Outlined.EventAvailable, "Add to calendar", "Choose the calendar in Settings", valueColor = Atlas.Primary, onClick = { vm.show(Overlay.Block(o.id)) })
            }
        }
        if (o.body.isNotBlank() && o.body != o.title) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.text.selection.SelectionContainer { Text(o.body, style = MaterialTheme.typography.bodyLarge) }
        } else if (o.type != ObjType.EVENT) TextButton({ vm.show(Overlay.Edit(o.id)) }) { Text("Add details") }
        // auto-connections from capture, with the evidence
        val rels = snap.neighbours(o.id)
        if (rels.isNotEmpty()) {
            Section("Connected", action = "Connect") { vm.show(Overlay.Connect(o.id)) }
            Box16 { rels.take(8).forEachIndexed { i, (n, r) -> if (i > 0) Divider16()
                FieldRow(com.jmr.atlas.ui.theme.icon(n.type), n.title, r.label(o.id) + if (r.origin == Origin.AUTO) " · connected automatically" else if (r.review == Review.PROPOSED) " · proposed" else "", onClick = { vm.open(n.id) }) } }
        } else ConnectedChips(vm, snap, o)
        YourFields(vm, o)
        CollapsedFooter(vm, snap, o)
    }
}
