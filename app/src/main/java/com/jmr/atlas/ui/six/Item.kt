package com.jmr.atlas.ui.six

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Board
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.cards.LocalEmbedded
import com.jmr.atlas.ui.components.rememberThumb
import com.jmr.atlas.ui.objects.DetailScreen
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T
import com.jmr.atlas.ui.theme.accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * An item's full page (design 0.6). Every item answers two questions — how did this get here (you, or Atlas, with
 * Unlink), and what is it connected to — under a hero that says what it is, with the one or two actions it needs.
 * The 0.5 card's details follow underneath. Two columns when unfolded.
 */
@Composable
fun ItemScreen(vm: AtlasViewModel, snap: Snapshot, id: String) {
    val o = snap.objects[id]?.takeIf { !it.trashed }
    if (o == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val wide = LocalWide.current
    val today = LocalDate.now()
    val backTo = remember(snap, id) { Briefing.projectOf(snap, o)?.title ?: if (vm.stack.value.size > 1) "Back" else "Home" }
    if (o.type == ObjType.DOCUMENT || o.type == ObjType.IMAGE) { FilePage(vm, snap, o, backTo); return }

    val left: @Composable ColumnScope.() -> Unit = {
        Hero(o, today, snap)
        Spacer(Modifier.height(14.dp))
        Actions(vm, o)
    }
    val right: @Composable ColumnScope.() -> Unit = {
        HowItGotHere(vm, snap, o)
        Spacer(Modifier.height(26.dp))
        Connected(vm, snap, o)
        Spacer(Modifier.height(18.dp))
        CompositionLocalProvider(LocalEmbedded provides true) { DetailScreen(vm, snap, o.id) }
    }
    Column(Modifier.fillMaxSize().testTag("item_page")) {
        Row(Modifier.padding(horizontal = pad).padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackPill(backTo, { vm.back() })
            HomeButton(vm)
            Spacer(Modifier.weight(1f))
            RoundIcon(Icons.Outlined.Edit, "Edit", { vm.show(Overlay.Edit(o.id)) }, Modifier.testTag("edit"), container = Color.Transparent, tint = Atlas.SecondaryText)
            More(vm, o)
        }
        if (wide) Row(Modifier.weight(1f).padding(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 140.dp), content = left)
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 140.dp), content = right)
        } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = pad).padding(bottom = 140.dp)) {
            left(); Spacer(Modifier.height(24.dp)); right()
        }
    }
}

@Composable
private fun More(vm: AtlasViewModel, o: KObject) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIcon(Icons.Outlined.MoreVert, "More", { open = true }, container = Color.Transparent, tint = Atlas.SecondaryText)
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Connect to…") }, { open = false; vm.show(Overlay.Connect(o.id)) }, leadingIcon = { Icon(Icons.Outlined.AddLink, null) })
            DropdownMenuItem({ Text("Add a field") }, { open = false; vm.show(Overlay.AddField(o.id)) }, leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null) })
            DropdownMenuItem({ Text(if (o.pinned) "Unpin" else "Pin") }, { open = false; vm.repo.update(o.copy(pinned = !o.pinned), if (o.pinned) "Unpinned" else "Pinned") }, leadingIcon = { Icon(Icons.Outlined.PushPin, null) })
            DropdownMenuItem({ Text("History") }, { open = false; vm.go(Route.History(o.id)) }, leadingIcon = { Icon(Icons.Outlined.History, null) })
            DropdownMenuItem({ Text("Move to trash") }, { open = false; vm.repo.trash(o.id); vm.back(); vm.say("Moved “${o.title}” to trash.") { vm.repo.restore(o.id) } }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
        }
    }
}

private fun kindLabel(o: KObject): String = when (o.type) {
    ObjType.NOTE -> if (o.kind == "Voice note") "VOICE NOTE" else "NOTE"
    ObjType.DOCUMENT -> o.kind.ifBlank { "Document" }.uppercase()
    ObjType.LINK -> o.kind.ifBlank { "Link" }.uppercase()
    else -> o.type.label.uppercase()
}

private fun heroSub(o: KObject, s: Snapshot, today: LocalDate): String = when (o.type) {
    ObjType.PERSON -> listOf(o.role, o.org).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Person" }
    ObjType.TASK -> humanLabel(o, s, today).first
    ObjType.PROJECT -> listOfNotNull(o.projectStatus?.label, o.targetDate?.let { "finish by ${Board.shortDate(it)}" }).joinToString(" · ")
    ObjType.EVENT -> o.eventDate?.let { Board.shortDate(it) } ?: "No date"
    ObjType.LINK -> o.site.ifBlank { o.url.substringAfter("://").substringBefore('/') }
    ObjType.DOCUMENT -> listOfNotNull(o.docDate?.let { "Dated ${Board.shortDate(it)}" }, "added ${Board.shortDate(Instant.ofEpochMilli(o.created).atZone(ZoneId.systemDefault()).toLocalDate())}").joinToString(" · ").replaceFirstChar { it.uppercase() }
    else -> Board.shortDate(Instant.ofEpochMilli(o.created).atZone(ZoneId.systemDefault()).toLocalDate())
}

@Composable
private fun Hero(o: KObject, today: LocalDate, s: Snapshot) {
    val wide = LocalWide.current
    val (bg, ink) = when (o.type) {
        ObjType.PERSON -> faceColors(o)
        ObjType.IMAGE -> Atlas.Chip to Atlas.DeepPlum
        ObjType.PROJECT, ObjType.COLLECTION -> com.jmr.atlas.ui.theme.SpaceLook.of(s, o).let { it.bg to it.ink }
        else -> stateTint(o, today) ?: (Atlas.Chip to Atlas.DeepPlum)
    }
    val thumb = if (o.type == ObjType.IMAGE) rememberThumb(o, 720) else null
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(bg).testTag("hero")) {
        if (thumb != null) Image(thumb, o.title, Modifier.fillMaxWidth().heightIn(max = 260.dp), contentScale = ContentScale.Crop)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (o.type == ObjType.PERSON) { Face(o, 96.dp, ring = Color.White); Spacer(Modifier.height(4.dp)) }
            Text(kindLabel(o), style = T.eyebrow, color = ink)
            Text(o.title, style = if (wide) T.heroTitle else T.titleCompact, color = if (o.type == ObjType.PERSON || o.type == ObjType.PROJECT) Atlas.DeepPlum else ink, maxLines = 5, overflow = TextOverflow.Ellipsis)
            val sub = heroSub(o, s, today)
            if (sub.isNotBlank()) Text(sub, style = T.body.copy(fontSize = 15.sp), color = Atlas.DeepPlum)
        }
    }
}

@Composable
private fun Actions(vm: AtlasViewModel, o: KObject) {
    val platform = LocalPlatform.current
    val s = vm.repo.snap
    when (o.type) {
        ObjType.PERSON -> {
            val phone = s.detailsOf(o.id, DetailKind.PHONE).firstOrNull()?.value ?: o.phone.takeIf { it.isNotBlank() }
            val email = s.detailsOf(o.id, DetailKind.EMAIL).firstOrNull()?.value ?: o.email.takeIf { it.isNotBlank() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Outlined.Call, "Call", phone != null, Modifier.weight(1f)) { platform.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                ActionTile(Icons.AutoMirrored.Outlined.Message, "Text", phone != null, Modifier.weight(1f)) { platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))) }
                ActionTile(Icons.Outlined.Email, "Email", email != null, Modifier.weight(1f)) { platform.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }
            }
        }
        ObjType.TASK -> BigButton(if (o.done) "Done ✓ · tap to undo" else "Mark done", if (o.done) Atlas.DeepTeal else Atlas.DeepPlum, Modifier.testTag("task_done")) { vm.repo.toggleDone(o.id) }
        ObjType.DOCUMENT, ObjType.IMAGE -> if (o.vaultFile.isNotBlank()) BigButton("Open file", Atlas.DeepPlum) { platform.openFile(o) }
        ObjType.LINK -> if (o.url.isNotBlank()) BigButton("Open link", Atlas.DeepPlum) { platform.launch(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) }
        ObjType.PROJECT -> BigButton("Open the board", Atlas.DeepPlum) { vm.projectSpace(o.id) }
        else -> {}
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) =
    Column(modifier.height(64.dp).clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (enabled) Atlas.Primary else Atlas.Hint)
        Spacer(Modifier.height(4.dp))
        Text(label, style = T.metaStrong, color = if (enabled) Atlas.DeepPlum else Atlas.Hint)
    }

@Composable
fun BigButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Box(modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(color).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = T.row.copy(fontSize = 16.sp), color = Color.White)
    }

private fun originLine(o: KObject): String {
    val d = Board.shortDate(Instant.ofEpochMilli(o.created).atZone(ZoneId.systemDefault()).toLocalDate())
    return when (o.origin) {
        Origin.USER -> if (o.kind == "Voice note") "You recorded it on $d." else "You added it on $d."
        Origin.EXTRACTED -> "Atlas read it from something you caught on $d."
        Origin.AI -> "Suggested by the on-device model on $d, and you kept it."
        Origin.IMPORTED -> "Imported on $d."
        Origin.EXAMPLE -> "Example data. Remove all of it any time in Settings."
        Origin.AUTO -> "Connected automatically on $d."
        Origin.CONTACTS -> "From your phone contacts."
        Origin.CALENDAR -> "From your calendar."
        Origin.WEB -> "Saved from the web on $d."
    }
}

@Composable
private fun HowItGotHere(vm: AtlasViewModel, s: Snapshot, o: KObject) {
    Eyebrow("How it got here")
    Spacer(Modifier.height(8.dp))
    Text(originLine(o), style = T.body.copy(fontSize = 17.sp, lineHeight = 25.sp), color = Atlas.DeepPlum)
    val byAtlas = s.relationsOf(o.id).filter { (it.origin == Origin.AUTO || it.origin == Origin.AI || it.origin == Origin.EXTRACTED) && it.review == Review.CONFIRMED }
    val proposed = s.relationsOf(o.id).filter { it.review == Review.PROPOSED }
    byAtlas.forEach { r ->
        val other = s.objects[r.other(o.id)] ?: return@forEach
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtlasDid("Linked to ${other.title} by Atlas" + (r.evidence.takeIf { it.isNotBlank() }?.let { " — “${it.take(50)}”" } ?: ""), Modifier.weight(1f), Atlas.DeepPlum)
            Spacer(Modifier.width(10.dp))
            SmallButton("Unlink") { vm.repo.unlink(r.id); vm.say("Unlinked from ${other.title}.") { vm.repo.link(r.sourceId, r.targetId, r.type, r.origin, Review.CONFIRMED, r.evidence) } }
        }
    }
    proposed.forEach { r ->
        val other = s.objects[r.other(o.id)] ?: return@forEach
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtlasDid("Atlas thinks this goes with ${other.title}", Modifier.weight(1f), Atlas.DeepPlum)
            Spacer(Modifier.width(8.dp))
            SmallButton("Yes", Atlas.PrimaryContainer) { vm.repo.approve(r.id) }
            Spacer(Modifier.width(6.dp))
            SmallButton("No") { vm.repo.reject(r.id) }
        }
    }
}

@Composable
private fun SmallButton(text: String, container: Color = Atlas.Surface, onClick: () -> Unit) =
    Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(12.dp)).background(container).border(1.dp, Atlas.Border, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center) { Text(text, style = T.metaStrong, color = Atlas.DeepPlum) }

@Composable
private fun Connected(vm: AtlasViewModel, s: Snapshot, o: KObject) {
    val nb = s.neighbours(o.id, includeProposed = false).filter { !it.first.trashed }.sortedWith(compareBy({ if (it.first.type == ObjType.PROJECT) 0 else if (it.first.type == ObjType.PERSON) 1 else 2 }, { it.first.title }))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Eyebrow("Connected", Modifier.weight(1f))
        Text("Connect", style = T.metaStrong, color = Atlas.Primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { vm.show(Overlay.Connect(o.id)) }.padding(8.dp))
    }
    Spacer(Modifier.height(4.dp))
    if (nb.isEmpty()) { Text("Not connected to anything yet.", style = T.body, color = Atlas.SecondaryText); return }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        nb.take(14).forEach { (n, r) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).clickable(role = Role.Button) { vm.open(n.id) }.padding(horizontal = 14.dp, vertical = 12.dp).testTag("connected_row"),
                verticalAlignment = Alignment.CenterVertically) {
                TypeMark(s, n, 36.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(n.title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(n.type.label, r.label(o.id).lowercase()).joinToString(" · "), style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText, maxLines = 1)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.Hint)
            }
        }
        if (nb.size > 14) Text("and ${nb.size - 14} more", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}


/** Home from anywhere (0.7.1). Hidden when Back already goes home. */
@Composable
fun HomeButton(vm: AtlasViewModel) {
    val stack by vm.stack.collectAsState()
    if (stack.size > 2) { Spacer(Modifier.width(8.dp)); RoundIcon(Icons.Outlined.Home, "Home", { vm.home() }, Modifier.testTag("go_home"), container = Atlas.Chip) }
}

/**
 * A file's page (0.7.1). A file is the thing itself: its page leads with what it looks like (the photo, the PDF's first
 * page, or its format badge), its name and format, and Open — then where it's filed and what Atlas read from it.
 * Nothing empty is shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilePage(vm: AtlasViewModel, s: Snapshot, o: KObject, backTo: String) {
    val platform = LocalPlatform.current
    val wide = LocalWide.current
    val (img, pages) = com.jmr.atlas.ui.cards.rememberPreview(o)
    val k = FileKind.of(o)
    var showAll by remember { mutableStateOf(false) }
    val aiStatus by vm.ai.status.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val added = Board.shortDate(Instant.ofEpochMilli(o.created).atZone(ZoneId.systemDefault()).toLocalDate())
    val preview: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.fillMaxWidth().heightIn(min = 200.dp, max = if (wide) 560.dp else 420.dp).shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
            .background(if (img != null && o.type == ObjType.IMAGE) Color(0xFF1E1724) else Atlas.Surface)
            .clickable(enabled = o.vaultFile.isNotBlank(), role = Role.Button, onClickLabel = "Open ${o.title}") { platform.openFile(o) }.testTag("file_preview"), contentAlignment = Alignment.Center) {
            if (img != null) Image(img, "Preview of ${o.title}", Modifier.fillMaxWidth(), contentScale = if (o.type == ObjType.IMAGE) ContentScale.Fit else ContentScale.FillWidth, alignment = Alignment.TopCenter)
            else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 40.dp)) {
                FileBadge(k, 96.dp); Spacer(Modifier.height(12.dp)); Text("Tap to open", style = T.meta, color = Atlas.SecondaryText)
            }
        }
        if (pages > 1) Text("Page 1 of $pages · tap to read it all", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
    }
    val info: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.Top) {
            FileBadge(k, 44.dp); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(FileKind.name(o), style = T.band.copy(fontSize = 22.sp, lineHeight = 27.sp), color = Atlas.DeepPlum, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("file_name"))
                Text(listOfNotNull(k.label, if (pages > 1) "$pages pages" else null, if (o.sizeBytes > 0) FileKind.size(o.sizeBytes) else null, o.kind.takeIf { it.isNotBlank() && it != "Other" }, "added $added").joinToString(" · "),
                    style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).background(Atlas.DeepPlum).clickable(enabled = o.vaultFile.isNotBlank(), role = Role.Button) { platform.openFile(o) }.testTag("file_open"),
                contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Open", style = T.row.copy(fontSize = 16.sp), color = Color.White) }
            }
            FileAction(Icons.Outlined.Share, "Share", o.vaultFile.isNotBlank()) { platform.shareFile(o) }
            FileAction(Icons.Outlined.DriveFileRenameOutline, "Rename") { vm.show(Overlay.Edit(o.id)) }
            FileAction(Icons.Outlined.CreateNewFolder, "File in…") { vm.show(Overlay.Connect(o.id)) }
        }
        Spacer(Modifier.height(24.dp))
        Connected(vm, s, o)
        val facts = s.detailsOf(o.id, DetailKind.FACT)
        if (o.docDate != null || facts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp)); Eyebrow("Atlas read"); Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).padding(horizontal = 16.dp, vertical = 6.dp)) {
                o.docDate?.let { FactRow("Dated", Board.shortDate(it)) { vm.show(Overlay.Edit(o.id)) } }
                facts.forEach { f ->
                    val v = when { f.flag -> "••••" + f.value.takeLast(4); f.type == FieldType.DATE -> runCatching { Board.shortDate(LocalDate.parse(f.value)) }.getOrDefault(f.value); else -> f.value }
                    FactRow(f.label, v) { vm.show(Overlay.Edit(o.id)) }
                }
            }
        }
        if (o.summary.isNotBlank()) { Spacer(Modifier.height(24.dp)); Eyebrow("Summary · written on this phone"); Spacer(Modifier.height(6.dp)); Text(o.summary, style = T.body, color = Atlas.DeepPlum) }
        else if (vm.brainReady() && o.extracted.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.clip(RoundedCornerShape(14.dp)).border(1.5.dp, Atlas.Border, RoundedCornerShape(14.dp)).clickable(enabled = aiBusy != o.id, role = Role.Button) { vm.summarize(o.id) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Sparkle, null, Modifier.size(16.dp), tint = Atlas.Primary); Spacer(Modifier.width(8.dp)); Text(if (aiBusy == o.id) "Reading…" else "Summarize on this phone", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum) }
            }
        }
        if (o.extracted.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Text in it", Modifier.weight(1f))
                Text("Copy", style = T.metaStrong, color = Atlas.Primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { platform.copy(o.title, o.extracted) }.padding(8.dp))
            }
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(if (showAll) o.extracted else o.extracted.replace('\n', ' ').take(260) + if (o.extracted.length > 260) "…" else "", style = T.body.copy(fontSize = 15.sp), color = Atlas.DeepPlum,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Atlas.Surface).clickable(enabled = !showAll && o.extracted.length > 260) { showAll = true }.padding(16.dp))
            }
        }
        if (o.body.isNotBlank()) { Spacer(Modifier.height(24.dp)); Eyebrow("Your note"); Spacer(Modifier.height(6.dp)); Text(o.body, style = T.body, color = Atlas.DeepPlum) }
        Spacer(Modifier.height(24.dp))
        AtlasDid(originLine(o).removeSuffix("."), Modifier, Atlas.SecondaryText)
        HowItGotHereLinks(vm, s, o)
    }
    Column(Modifier.fillMaxSize().testTag("item_page")) {
        Row(Modifier.padding(horizontal = pad).padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackPill(backTo, { vm.back() })
            HomeButton(vm)
            Spacer(Modifier.weight(1f))
            More(vm, o)
        }
        if (wide) Row(Modifier.weight(1f).padding(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 140.dp), content = preview)
            Column(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 140.dp), content = info)
        } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = pad).padding(bottom = 140.dp)) {
            preview(); Spacer(Modifier.height(20.dp)); info()
        }
    }
}

@Composable
private fun FileAction(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) =
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Atlas.Chip).clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(21.dp), tint = if (enabled) Atlas.DeepPlum else Atlas.Hint) }

@Composable
private fun FactRow(label: String, value: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = "Fix $label", onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText, modifier = Modifier.width(120.dp))
        Text(value, style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
    }

/** The Atlas-made and suggested links from "How it got here", without the heading (used on the file page). */
@Composable
private fun HowItGotHereLinks(vm: AtlasViewModel, s: Snapshot, o: KObject) {
    s.relationsOf(o.id).filter { it.review == Review.PROPOSED }.forEach { r ->
        val other = s.objects[r.other(o.id)] ?: return@forEach
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtlasDid("Atlas thinks this goes with ${other.title}", Modifier.weight(1f), Atlas.DeepPlum)
            Spacer(Modifier.width(8.dp)); SmallButton("Yes", Atlas.PrimaryContainer) { vm.repo.approve(r.id) }
            Spacer(Modifier.width(6.dp)); SmallButton("No") { vm.repo.reject(r.id) }
        }
    }
}
