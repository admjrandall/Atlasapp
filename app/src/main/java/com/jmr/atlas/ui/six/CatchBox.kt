package com.jmr.atlas.ui.six

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.capture.Sentence
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AskState
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T

private const val PLACEHOLDER = "Catch a thought, or ask anything"

/**
 * The one input on every screen (design 0.6, shaped like Claude's): + · text · mic · send in a single row.
 * Collapsed, it waits at the bottom. Tapped, it grows in place: what Atlas understood shows just above it, and Send
 * saves a note or task — or, for a question, answers from your own things.
 */
@Composable
fun CatchBar(vm: AtlasViewModel, modifier: Modifier = Modifier) {
    val platform = LocalPlatform.current
    Row(modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(30.dp), ambientColor = Atlas.DeepPlum, spotColor = Atlas.DeepPlum).clip(RoundedCornerShape(30.dp))
        .background(Atlas.Surface).border(1.dp, Atlas.Border, RoundedCornerShape(30.dp)).padding(8.dp).testTag("catch_bar"), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(Icons.Outlined.Add, "Add a photo, scan or file", outlined = true) { vm.openCatch(); vm.attachOpen.value = true }
        Text(if (LocalWide.current) PLACEHOLDER else "Catch or ask", style = T.input, color = Atlas.Hint, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = "Type") { vm.openCatch() }
                .padding(horizontal = 10.dp).wrapContentHeight(Alignment.CenterVertically).testTag("catch_open"))
        CircleButton(Icons.Outlined.Mic, "Talk") { platform.dictate { t -> vm.openCatch(t) } }
        CircleButton(Icons.Outlined.ArrowUpward, "Send", filled = false, enabled = false) {}
    }
}

@Composable
private fun CircleButton(icon: ImageVector, desc: String, outlined: Boolean = false, filled: Boolean = false, enabled: Boolean = true, rotate: Float = 0f, onClick: () -> Unit) {
    val bg = when { filled -> Atlas.Primary; !enabled -> Atlas.Hairline; else -> Color.Transparent }
    Box(Modifier.size(44.dp).clip(CircleShape).background(bg).then(if (outlined) Modifier.border(1.dp, Atlas.Border, CircleShape) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = desc, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp).rotate(rotate), tint = when { filled -> Color.White; !enabled -> Color.White; else -> Atlas.SecondaryText })
    }
}

/** The input box opened: scrim, what Atlas understood (or its answer), and the input row with the keyboard. */
@Composable
fun CatchSheet(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    val text = vm.catchField.text.toString()          // snapshot state: recomposes as you type, no lag
    LaunchedEffect(text) { vm.textChanged(text) }
    val kindOv by vm.kindOverride.collectAsState()
    val dropped by vm.dropped.collectAsState()
    val dueOff by vm.dueOff.collectAsState()
    val ctxId by vm.context.collectAsState()
    val extra by vm.extraLinks.collectAsState()
    val attach by vm.attachOpen.collectAsState()
    val ask by vm.ask.collectAsState()
    val parsed = remember(text, snap, ctxId) { if (text.isBlank()) null else vm.parsed(text) }
    var picking by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().testTag("catch_sheet")) {
        Box(Modifier.fillMaxSize().background(Atlas.DeepPlum.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close") { vm.closeCatch() })
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().padding(horizontal = pad).padding(bottom = 14.dp).widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val ctx = ctxId?.let { snap.objects[it] }
            when {
                picking -> LinkPicker(vm, snap, exclude = setOfNotNull(ctxId) + extra) { picking = false }
                ask is AskState.Thinking -> Bubble("Looking through your things…")
                ask is AskState.Answer -> AnswerCard(vm, snap, ask as AskState.Answer)
                parsed == null && attach -> {}
                parsed == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ctx != null) Bubble("Adding to ${ctx.title}")
                    Examples(vm, ctx)
                }
                parsed.question -> Bubble("Sounds like a question. Send to ask.")
                else -> Preview(vm, snap, parsed, vm.kindOf(parsed), ctx, extra, dropped, dueOff) { picking = true }
            }
            AnimatedVisibility(attach, enter = fadeIn(), exit = fadeOut()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Photo", Icons.Outlined.Image) { vm.attachOpen.value = false; platform.pickImage() }
                    Chip("Scan", Icons.Outlined.DocumentScanner) { vm.attachOpen.value = false; platform.scan() }
                    Chip("File", Icons.Outlined.AttachFile) { vm.attachOpen.value = false; platform.pickFile() }
                    platform.clipboardUrl()?.let { url -> Chip("Paste link", Icons.Outlined.Link) { vm.attachOpen.value = false; vm.closeCatch(); vm.captureLink(url) } }
                }
            }
            Row(Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Atlas.Surface).border(1.5.dp, Atlas.Orchid, RoundedCornerShape(30.dp)).padding(8.dp),
                verticalAlignment = Alignment.Bottom) {
                CircleButton(Icons.Outlined.Add, if (attach) "Hide attachments" else "Add a photo, scan or file", outlined = true, rotate = if (attach) 45f else 0f) { vm.attachOpen.value = !attach }
                Box(Modifier.weight(1f).heightIn(min = 44.dp).padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text(if (LocalWide.current) PLACEHOLDER else "Catch or ask", style = T.input, color = Atlas.Hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(vm.catchField, Modifier.fillMaxWidth().focusRequester(focus).testTag("catch_input"), textStyle = T.input.copy(color = Atlas.DeepPlum),
                        cursorBrush = SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        onKeyboardAction = { vm.send() }, lineLimits = TextFieldLineLimits.MultiLine(1, 5))
                }
                CircleButton(Icons.Outlined.Mic, "Talk") { platform.dictate { t -> vm.setCatch((vm.catchText.trimEnd() + " " + t).trim()) } }
                CircleButton(Icons.Outlined.ArrowUpward, if (parsed?.question == true) "Ask" else "Save", filled = text.isNotBlank(), enabled = text.isNotBlank()) { vm.send() }
            }
        }
    }
}

@Composable
private fun Chip(text: String, icon: ImageVector, onClick: () -> Unit) =
    Row(Modifier.heightIn(min = 44.dp).shadow(4.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Atlas.Surface).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Atlas.Primary); Spacer(Modifier.width(6.dp)); Text(text, style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.DeepPlum)
    }

@Composable
private fun Bubble(text: String) =
    Text(text, style = T.meta.copy(fontSize = 14.sp), color = Atlas.SecondaryText, modifier = Modifier.shadow(6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Atlas.Background).padding(horizontal = 16.dp, vertical = 10.dp))

@Composable
private fun Examples(vm: AtlasViewModel, ctx: KObject?) {
    val ex = when (ctx?.type) {
        ObjType.PERSON -> listOf("Send ${ctx.firstName} the deposit by Friday", "Remember: prefers texts", "What does ${ctx.firstName} owe me?")
        ObjType.TASK -> listOf("Check the haul-away fee", "Call about it tomorrow", "What’s left on this?")
        ObjType.PROJECT -> listOf("Order samples by Friday", "Idea: open shelving", "What’s late here?")
        else -> if (vm.currentProject != null) listOf("Order samples by Friday", "Idea: open shelving", "What’s late here?") else listOf("Call the dentist tomorrow", "Idea: a reading nook", "What’s due this week?")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ex.forEach { e ->
            Text(e, style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.DeepPlum, modifier = Modifier.heightIn(min = 44.dp).shadow(4.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                .background(Atlas.Surface).clickable(role = Role.Button) { vm.setCatch(e) }.padding(horizontal = 16.dp).wrapContentHeight(Alignment.CenterVertically))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Preview(vm: AtlasViewModel, s: Snapshot, p: Sentence.Parsed, kind: Sentence.Kind, ctx: KObject?, extra: List<String>, dropped: Set<String>, dueOff: Boolean, onLink: () -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).padding(horizontal = 20.dp, vertical = 18.dp).testTag("catch_preview"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtlasDid(if (kind.addsToContext) "Atlas will add this — tap a part to change it" else "Atlas will save this as — tap a part to change it")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            val (kc, ki) = when (kind) {
                Sentence.Kind.TASK, Sentence.Kind.STEP -> Atlas.CoolMint to Atlas.DeepTeal
                Sentence.Kind.IDEA -> Atlas.Peach to Atlas.CoralText
                Sentence.Kind.REMEMBER, Sentence.Kind.UPDATE -> Atlas.PrimaryContainer to Atlas.Primary
                Sentence.Kind.NOTE -> Atlas.PowderBlue to Atlas.BlueText
            }
            Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(kc).clickable(role = Role.Button, onClickLabel = "Change type") { vm.cycleKind(kind) }.padding(horizontal = 12.dp).testTag("kind_chip"),
                contentAlignment = Alignment.Center) { Text(kind.label, style = T.tileLabel.copy(fontSize = 14.sp), color = ki) }
            Text(if (kind == Sentence.Kind.NOTE || kind == Sentence.Kind.UPDATE || kind == Sentence.Kind.REMEMBER) p.raw.replace(Regex("@(?=\\S)"), "") else p.title, style = T.preview.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Atlas.DeepPlum)
            if (kind == Sentence.Kind.TASK && p.due != null) {
                Word("due")
                PartChip(com.jmr.atlas.data.Board.whenShort(p.due, java.time.LocalDate.now()).replaceFirstChar { it.uppercase() } + " " + com.jmr.atlas.data.Board.shortDate(p.due), Atlas.PowderBlue, Atlas.BlueText, dueOff,
                    if (dueOff) "Keep the date" else "Drop the date") { vm.dueOff.value = !dueOff }
            }
            if (ctx != null) {
                Word(when (kind) { Sentence.Kind.STEP -> "on"; Sentence.Kind.REMEMBER -> "about"; Sentence.Kind.UPDATE -> "for"; else -> "linked to" })
                PartChip(ctx.title, Atlas.PrimaryContainer, Atlas.Primary, ctx.id in dropped, if (ctx.id in dropped) "Link to ${ctx.title}" else "Don't link to ${ctx.title}") { vm.toggleDrop(ctx.id) }
            }
            if (!kind.addsToContext) {
                p.space?.takeIf { it.id != ctx?.id }?.let { sp -> Word("in"); PartChip(sp.title, Atlas.PrimaryContainer, Atlas.Primary, sp.id in dropped, if (sp.id in dropped) "Put it in ${sp.title}" else "Don't put it in ${sp.title}") { vm.toggleDrop(sp.id) } }
                val ppl = p.people.filter { it.id != ctx?.id }
                if (ppl.isNotEmpty()) { Word("with"); ppl.forEach { person -> PartChip(person.firstName, Atlas.CoolMint, Atlas.DeepTeal, person.id in dropped, if (person.id in dropped) "Link ${person.title}" else "Don't link ${person.title}") { vm.toggleDrop(person.id) } } }
                p.mentions.filter { it.id != ctx?.id }.forEach { m -> PartChip(m.title, Atlas.PowderBlue, Atlas.BlueText, m.id in dropped, if (m.id in dropped) "Link ${m.title}" else "Don't link ${m.title}") { vm.toggleDrop(m.id) } }
                extra.mapNotNull { s.objects[it] }.forEach { o -> PartChip(o.title + "  ×", Atlas.PowderBlue, Atlas.BlueText, false, "Remove link to ${o.title}") { vm.removeLink(o.id) } }
                Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).border(1.5.dp, Atlas.Border, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onLink).padding(horizontal = 12.dp).testTag("link_to"),
                    contentAlignment = Alignment.Center) { Text("+ Link to…", style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.Primary) }
            }
        }
        if (!kind.addsToContext && p.space == null && ctx == null && p.people.isEmpty() && extra.isEmpty()) Text("Goes to Today. Say “for the kitchen”, type @ and a name, or tap Link to file it.", style = T.meta, color = Atlas.SecondaryText)
    }
}

@Composable
private fun Word(t: String) = Text(t, style = T.preview, color = Atlas.SecondaryText)

/** Pick any item to link the line to: search by title, people and projects first. */
@Composable
private fun LinkPicker(vm: AtlasViewModel, s: Snapshot, exclude: Set<String>, onDone: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val list = remember(q, s) {
        s.live.filter { it.id !in exclude && it.type != ObjType.COLLECTION && (q.isBlank() || it.title.contains(q, true)) }
            .sortedWith(compareBy({ when (it.type) { ObjType.PERSON -> 0; ObjType.PROJECT -> 1; else -> 2 } }, { -it.updated })).take(30)
    }
    run {
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).padding(18.dp).testTag("link_picker"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Link to…", style = T.band, color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
                Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("Done", style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.Primary) }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Atlas.Surface).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (q.isEmpty()) Text("Search people, projects, tasks, files…", style = T.input, color = Atlas.Hint)
                androidx.compose.foundation.text.BasicTextField(q, { q = it }, Modifier.fillMaxWidth().testTag("link_search"), textStyle = T.input.copy(color = Atlas.DeepPlum), singleLine = true, cursorBrush = SolidColor(Atlas.Primary))
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                list.forEach { o ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) { vm.addLink(o.id); onDone() }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (o.type == ObjType.PERSON) Face(o, 34.dp) else Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(com.jmr.atlas.ui.theme.accent(o.type).container), contentAlignment = Alignment.Center) {
                            Icon(com.jmr.atlas.ui.theme.icon(o.type), null, Modifier.size(17.dp), tint = com.jmr.atlas.ui.theme.accent(o.type).content) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(o.title, style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(o.type.label, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText)
                        }
                    }
                }
                if (list.isEmpty()) Text("Nothing matches.", style = T.meta, color = Atlas.SecondaryText, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun AnswerCard(vm: AtlasViewModel, s: Snapshot, a: AskState.Answer) {
    Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).verticalScroll(rememberScrollState()).padding(20.dp).testTag("answer"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(a.text, style = T.heroTitle.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Atlas.DeepPlum)
        val items = a.ids.mapNotNull { s.objects[it] }.take(6)
        items.forEachIndexed { i, o ->
            val (label, lc) = humanLabel(o, s, java.time.LocalDate.now())
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) { vm.closeCatch(); vm.reveal(o.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Text("${i + 1}", style = T.title.copy(fontSize = 22.sp, lineHeight = 26.sp), color = if (lc == Atlas.CoralText) Atlas.CoralText else Atlas.Primary, modifier = Modifier.width(26.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.title.removePrefix("Expect ").replaceFirstChar { it.uppercase() }, style = T.tileTitle, color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(label, style = T.meta, color = lc)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.Hint)
            }
        }
        if (a.refining) Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Atlas.Primary)
            Spacer(Modifier.width(8.dp)); Text("Asking the on-device model too…", style = T.meta, color = Atlas.SecondaryText)
        }
        a.note?.let { Text(it, style = T.meta, color = Atlas.SecondaryText) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtlasDid(if (a.byAi) "Answered on this phone by the on-device model" else "Put together on this phone from your things", Modifier.weight(1f))
            if (a.spaceId != null && a.ids.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.DeepPlum).clickable(role = Role.Button) { vm.showOnBoard(a) }.padding(horizontal = 16.dp).testTag("show_on_board"),
                    contentAlignment = Alignment.Center) { Text("Show on the board", style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White) }
            }
        }
    }
}

/** While a tile is selected the input box steps aside for this: what's selected, how much it's linked to, Open. */
@Composable
fun SelectionBar(vm: AtlasViewModel, o: KObject, linked: Int, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().height(68.dp).shadow(18.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Atlas.DeepPlum).padding(start = 20.dp, end = 10.dp).testTag("selection_bar"),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(o.title, style = T.tileTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (linked == 0) "Not linked to anything here" else "Connected to $linked ${if (linked == 1) "thing" else "things"} here", style = T.meta, color = Atlas.Orchid)
        }
        Box(Modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(role = Role.Button) { vm.go(com.jmr.atlas.ui.Route.Item(o.id)) }.padding(horizontal = 22.dp).testTag("open_selected"),
            contentAlignment = Alignment.Center) { Text("Open", style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.DeepPlum) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.12f)).clickable(role = Role.Button, onClickLabel = "Close") { vm.select(null) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, "Close", tint = Color.White)
        }
    }
