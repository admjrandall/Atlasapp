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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import com.jmr.atlas.ui.Turn
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
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T

private const val PLACEHOLDER = "Tell or ask Atlas anything"

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
        Text(if (LocalWide.current) PLACEHOLDER else "Tell or ask Atlas", style = T.input, color = Atlas.Hint, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
    var enterHit by remember { mutableStateOf(0) }
    LaunchedEffect(enterHit) { if (enterHit > 0) vm.send() }
    val kindOv by vm.kindOverride.collectAsState()
    val dropped by vm.dropped.collectAsState()
    val dueOff by vm.dueOff.collectAsState()
    val ctxId by vm.context.collectAsState()
    val extra by vm.extraLinks.collectAsState()
    val attach by vm.attachOpen.collectAsState()
    val thread by vm.thread.collectAsState()
    val hidden by vm.threadHidden.collectAsState()
    val files by vm.attachments.collectAsState()
    val newLinks by vm.newLinks.collectAsState()
    val parsed = remember(text, snap, ctxId) { if (text.isBlank()) null else vm.parsed(text) }
    // what Atlas reads the line as, decided by its own code as you type
    val intent = remember(text, snap, thread.size) { if (text.isBlank()) null else vm.intentOf(text) }
    val hits = remember(text, snap) {
        if (text.isBlank()) emptyList() else vm.find(if (com.jmr.atlas.capture.Sentence.isQuestion(text)) com.jmr.atlas.capture.Sentence.searchTerms(text).ifBlank { text } else (parsed?.title ?: text))
    }
    var picking by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var typing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().testTag("catch_sheet")) {
        // The backdrop. Taps in the first moment (keyboard still sliding the sheet up) are ignored; with a draft, a tap
        // only puts the keyboard away — a second tap closes, and the draft is kept either way.
        Box(Modifier.fillMaxSize().background(Atlas.DeepPlum.copy(alpha = 0.5f)).testTag("catch_scrim").clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close") {
            when {
                android.os.SystemClock.uptimeMillis() - vm.catchOpenedAt < 700 -> {}
                typing && (text.isNotBlank() || files.isNotEmpty()) -> focusManager.clearFocus()
                else -> vm.closeCatch()
            }
        })
        // The input row is laid out last and never shrinks; everything above it gets only the room that's left over the
        // keyboard (0.7.1: on the Fold the panel used to push the input under the keyboard).
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().statusBarsPadding().imePadding().padding(horizontal = pad).padding(top = 8.dp, bottom = 14.dp).widthIn(max = 760.dp)
            // a tap between the cards belongs to the sheet, not to the backdrop behind it
            .pointerInput(Unit) { detectTapGestures { } }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val ctx = ctxId?.let { snap.objects[it] }
            Box(Modifier.weight(1f, fill = false)) {
            when {
                picking -> LinkPicker(vm, snap, exclude = setOfNotNull(ctxId) + extra) { picking = false }
                parsed == null && files.isNotEmpty() ->
                    if (newLinks.isNotEmpty() || extra.isNotEmpty()) Preview(vm, snap, vm.parsed("x").copy(raw = "", title = "", space = null, people = emptyList(), mentions = emptyList(), target = null, newSpace = null), Sentence.Kind.NOTE, ctx, extra, newLinks, dropped, dueOff, files.size, onSend = { vm.send() }) { picking = true }
                    else Column(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val it_ = if (files.size == 1) "it" else "them"
                        Text(if (ctx != null) "Send to add $it_ to ${ctx.title}, or type where $it_ should go." else "Type where $it_ should go — “Add to Ankle Surgery” makes the space if it’s new — or just send.", style = T.meta.copy(fontSize = 15.sp), color = Atlas.DeepPlum)
                        Row(verticalAlignment = Alignment.CenterVertically) { LinkButton { picking = true }; Spacer(Modifier.weight(1f)); SendPill(if (files.size == 1) "Save it" else "Save ${files.size}") { vm.send() } }
                    }
                parsed == null && attach -> {}
                parsed == null && thread.isNotEmpty() && !hidden -> AtlasPanel(vm, showClear = true, onSwipeDown = { vm.threadHidden.value = true }) { ThreadView(vm, snap, thread) }
                parsed == null && thread.isNotEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Row(Modifier.heightIn(min = 44.dp).shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Atlas.Background)
                        .clickable(role = Role.Button, onClickLabel = "Show the conversation") { vm.threadHidden.value = false }.padding(horizontal = 16.dp).testTag("show_thread"), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Sparkle, null, Modifier.size(16.dp), tint = Atlas.Primary); Spacer(Modifier.width(8.dp))
                        Text("Conversation · ${thread.count { it is Turn.You }}", style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum)
                        Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.ExpandLess, null, Modifier.size(18.dp), tint = Atlas.SecondaryText)
                    }
                }
                parsed == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ctx != null) Bubble("Adding to ${ctx.title}")
                    Examples(vm, ctx)
                }
                files.isEmpty() && intent == com.jmr.atlas.capture.Sentence.Intent.FIND -> AtlasPanel(vm, showClear = false) { FindResults(vm, snap, text, hits) }
                files.isEmpty() && intent == com.jmr.atlas.capture.Sentence.Intent.ASK_MINE -> QuestionHint(vm, mine = true, inChat = false)
                files.isEmpty() && (intent == com.jmr.atlas.capture.Sentence.Intent.ASK_WORLD || intent == com.jmr.atlas.capture.Sentence.Intent.CHAT) ->
                    QuestionHint(vm, mine = false, inChat = intent == com.jmr.atlas.capture.Sentence.Intent.CHAT)
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (files.isEmpty()) AlsoInAtlas(vm, snap, hits.filter { h -> h.inTitle && h.o.id != ctxId && parsed.people.none { it.id == h.o.id } && parsed.space?.id != h.o.id })
                    Preview(vm, snap, parsed, vm.kindOf(parsed), ctx, extra, newLinks, dropped, dueOff, files.size, onSend = { vm.send() }) { picking = true }
                }
            }
            }
            AnimatedVisibility(attach, enter = fadeIn(), exit = fadeOut()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Photo", Icons.Outlined.Image) { vm.attachOpen.value = false; platform.pickImage() }
                    Chip("Scan", Icons.Outlined.DocumentScanner) { vm.attachOpen.value = false; platform.scan() }
                    Chip("File", Icons.Outlined.AttachFile) { vm.attachOpen.value = false; platform.pickFile() }
                    platform.clipboardUrl()?.let { url -> Chip("Paste link", Icons.Outlined.Link) { vm.attachOpen.value = false; vm.closeCatch(); vm.captureLink(url) } }
                }
            }
            if (files.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("attachments"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                files.forEach { f -> Attachment(f) { vm.removeAttachment(f.id) } }
            }
            Row(Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Atlas.Surface).border(1.5.dp, Atlas.Orchid, RoundedCornerShape(30.dp)).padding(8.dp),
                verticalAlignment = Alignment.Bottom) {
                CircleButton(Icons.Outlined.Add, if (attach) "Hide attachments" else "Add a photo, scan or file", outlined = true, rotate = if (attach) 45f else 0f) { vm.attachOpen.value = !attach }
                Box(Modifier.weight(1f).heightIn(min = 44.dp).padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text(if (LocalWide.current) PLACEHOLDER else "Tell or ask Atlas", style = T.input, color = Atlas.Hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(vm.catchField, Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { typing = it.isFocused }.testTag("catch_input"), textStyle = T.input.copy(color = Atlas.DeepPlum),
                        // multi-line fields get a return key, not an action key — so a typed return means "send"
                        inputTransformation = remember { EnterSends { enterHit++ } },
                        cursorBrush = SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        onKeyboardAction = { vm.send() }, lineLimits = TextFieldLineLimits.MultiLine(1, 5))
                }
                CircleButton(Icons.Outlined.Mic, "Talk") { platform.dictate { t -> vm.setCatch((vm.catchText.trimEnd() + " " + t).trim()) } }
                CircleButton(Icons.Outlined.ArrowUpward, when (intent) { com.jmr.atlas.capture.Sentence.Intent.FIND -> "Open"; com.jmr.atlas.capture.Sentence.Intent.CAPTURE, null -> "Save"; else -> "Ask" }, filled = text.isNotBlank() || files.isNotEmpty(), enabled = text.isNotBlank() || files.isNotEmpty()) { vm.send() }
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
        else -> if (vm.currentProject != null) listOf("Order samples by Friday", "Idea: open shelving", "What’s late here?") else listOf("Call the dentist tomorrow", "Idea: a reading nook", "What’s due this week?", "Why is the sky blue?")
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
private fun Preview(vm: AtlasViewModel, s: Snapshot, p: Sentence.Parsed, kind: Sentence.Kind, ctx: KObject?, extra: List<String>, newLinks: List<String>, dropped: Set<String>, dueOff: Boolean, files: Int, onSend: () -> Unit = {}, onLink: () -> Unit) {
    val asSpace = kind == Sentence.Kind.SPACE && files == 0
    Column(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Atlas.Background).padding(horizontal = 20.dp, vertical = 18.dp).testTag("catch_preview"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtlasDid(when {
            files > 0 -> "Atlas will save ${if (files == 1) "it" else "them"} — tap a part to change it"
            asSpace && p.target != null -> "${p.target.title} already exists — send to open it"
            asSpace -> "Atlas will make a new space — tap a part to change it"
            kind.addsToContext -> "Atlas will add this — tap a part to change it"
            else -> "Atlas will save this as — tap a part to change it" })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            val (kc, ki) = when (kind) {
                Sentence.Kind.TASK, Sentence.Kind.STEP -> Atlas.CoolMint to Atlas.DeepTeal
                Sentence.Kind.IDEA -> Atlas.Peach to Atlas.CoralText
                Sentence.Kind.REMEMBER, Sentence.Kind.UPDATE -> Atlas.PrimaryContainer to Atlas.Primary
                Sentence.Kind.NOTE -> Atlas.PowderBlue to Atlas.BlueText
                Sentence.Kind.SPACE -> Atlas.PrimaryContainer to Atlas.Primary
            }
            if (files > 0) Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(Atlas.PowderBlue).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text(if (files == 1) "1 attachment" else "$files attachments", style = T.tileLabel.copy(fontSize = 14.sp), color = Atlas.BlueText) }
            else Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(kc).clickable(role = Role.Button, onClickLabel = "Change type") { vm.cycleKind(kind) }.padding(horizontal = 12.dp).testTag("kind_chip"),
                contentAlignment = Alignment.Center) { Text(kind.label, style = T.tileLabel.copy(fontSize = 14.sp), color = ki) }
            val shown = when {
                files > 0 -> if (kind == Sentence.Kind.SPACE) "" else p.title
                kind == Sentence.Kind.NOTE && (p.target != null || p.newSpace != null) -> p.title
                kind == Sentence.Kind.NOTE || kind == Sentence.Kind.UPDATE || kind == Sentence.Kind.REMEMBER -> p.raw.replace(Regex("@(?=\\S)"), "")
                else -> p.title
            }
            if (shown.isNotBlank()) Text(shown, style = T.preview.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Atlas.DeepPlum)
            if (kind == Sentence.Kind.TASK && p.due != null) {
                Word("due")
                PartChip(com.jmr.atlas.data.Board.whenShort(p.due, java.time.LocalDate.now()).replaceFirstChar { it.uppercase() } + " " + com.jmr.atlas.data.Board.shortDate(p.due), Atlas.PowderBlue, Atlas.BlueText, dueOff,
                    if (dueOff) "Keep the date" else "Drop the date") { vm.dueOff.value = !dueOff }
            }
            if (ctx != null) {
                Word(when (kind) { Sentence.Kind.STEP -> "on"; Sentence.Kind.REMEMBER -> "about"; Sentence.Kind.UPDATE -> "for"; else -> "linked to" })
                PartChip(ctx.title, Atlas.PrimaryContainer, Atlas.Primary, ctx.id in dropped, if (ctx.id in dropped) "Link to ${ctx.title}" else "Don't link to ${ctx.title}") { vm.toggleDrop(ctx.id) }
            }
            if (asSpace) {
                if (p.people.isNotEmpty()) { Word("with"); p.people.forEach { person -> PartChip(person.firstName, Atlas.CoolMint, Atlas.DeepTeal, person.id in dropped, if (person.id in dropped) "Link ${person.title}" else "Don't link ${person.title}") { vm.toggleDrop(person.id) } } }
            } else if (!kind.addsToContext) {
                // a space that isn't there yet: shown as a dashed bubble, made when you send
                val fresh = (listOfNotNull(p.newSpace, p.title.takeIf { files > 0 && kind == Sentence.Kind.SPACE && p.target == null }) + newLinks).distinctBy { it.lowercase() }
                if (fresh.isNotEmpty()) { Word(if (files > 0) "to" else "in"); fresh.forEach { n -> NewSpaceChip(n, "new:${n.lowercase()}" in dropped, n in newLinks) { if (n in newLinks) vm.removeNewLink(n) else vm.toggleDrop("new:${n.lowercase()}") } } }
                p.target?.takeIf { it.type == ObjType.PROJECT && it.id != ctx?.id && files > 0 && p.space?.id != it.id }?.let { t -> Word("to"); PartChip(t.title, Atlas.PrimaryContainer, Atlas.Primary, t.id in dropped, if (t.id in dropped) "Put it in ${t.title}" else "Don't put it in ${t.title}") { vm.toggleDrop(t.id) } }
                p.space?.takeIf { it.id != ctx?.id }?.let { sp -> Word("in"); PartChip(sp.title, Atlas.PrimaryContainer, Atlas.Primary, sp.id in dropped, if (sp.id in dropped) "Put it in ${sp.title}" else "Don't put it in ${sp.title}") { vm.toggleDrop(sp.id) } }
                val ppl = p.people.filter { it.id != ctx?.id }
                if (ppl.isNotEmpty()) { Word("with"); ppl.forEach { person -> PartChip(person.firstName, Atlas.CoolMint, Atlas.DeepTeal, person.id in dropped, if (person.id in dropped) "Link ${person.title}" else "Don't link ${person.title}") { vm.toggleDrop(person.id) } } }
                p.mentions.filter { it.id != ctx?.id }.forEach { m -> PartChip(m.title, Atlas.PowderBlue, Atlas.BlueText, m.id in dropped, if (m.id in dropped) "Link ${m.title}" else "Don't link ${m.title}") { vm.toggleDrop(m.id) } }
                extra.mapNotNull { s.objects[it] }.forEach { o -> PartChip(o.title + "  ×", Atlas.PowderBlue, Atlas.BlueText, false, "Remove link to ${o.title}") { vm.removeLink(o.id) } }
                LinkButton(onLink)
            }
        }
        if (!kind.addsToContext && !asSpace && files == 0 && p.space == null && p.newSpace == null && p.target == null && newLinks.isEmpty() && ctx == null && p.people.isEmpty() && extra.isEmpty())
            Text("Goes to Today. Add “add to Ankle Surgery”, type @ and a name, or tap Link to file it — new spaces are made for you.", style = T.meta, color = Atlas.SecondaryText)
        // the send button sits on the sentence you're reading, named for what it does
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SendPill(sendLabel(kind, files, p, ctx), onSend) }
    }
}

private fun sendLabel(kind: Sentence.Kind, files: Int, p: Sentence.Parsed, ctx: KObject?): String = when {
    files > 0 -> if (files == 1) "Save it" else "Save $files"
    kind == Sentence.Kind.SPACE -> if (p.target != null) "Open it" else "Make space"
    kind == Sentence.Kind.STEP -> "Add step"
    kind == Sentence.Kind.REMEMBER -> "Remember"
    kind == Sentence.Kind.UPDATE -> "Post update"
    else -> "Save ${kind.label.lowercase()}"
}

@Composable
private fun SendPill(label: String, onClick: () -> Unit) =
    Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Primary).clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(start = 18.dp, end = 14.dp).testTag("preview_send"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.metaStrong.copy(fontSize = 15.sp), color = Color.White)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.ArrowUpward, null, Modifier.size(16.dp), tint = Color.White)
    }

/** A typed return (soft keyboard's ⏎ in a multi-line field, or a hardware Enter) is taken out of the text and means "send". */
private class EnterSends(val onEnter: () -> Unit) : androidx.compose.foundation.text.input.InputTransformation {
    override fun androidx.compose.foundation.text.input.TextFieldBuffer.transformInput() {
        val t = asCharSequence()
        val i = t.indexOf('\n'); if (i < 0) return
        replace(0, length, t.toString().replace("\n", " ").trimEnd())
        onEnter()
    }
}

@Composable
private fun LinkButton(onLink: () -> Unit) =
    Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).border(1.5.dp, Atlas.Border, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onLink).padding(horizontal = 12.dp).testTag("link_to"),
        contentAlignment = Alignment.Center) { Text("+ Link to…", style = T.metaStrong.copy(fontSize = 15.sp), color = Atlas.Primary) }

/** A space that will be made when you send: dashed, with "New space" written on it so it's never a surprise. */
@Composable
private fun NewSpaceChip(name: String, off: Boolean, removable: Boolean, onClick: () -> Unit) {
    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
    Row(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(if (off) Color.Transparent else Atlas.PrimaryContainer.copy(alpha = 0.55f))
        .drawBehind { drawRoundRect(Atlas.Primary.copy(alpha = if (off) 0.35f else 0.9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = dash)) }
        .clickable(role = Role.Button, onClickLabel = if (removable) "Don't make $name" else if (off) "Make the space $name" else "Don't make $name", onClick = onClick)
        .padding(horizontal = 12.dp).testTag("new_space_chip").semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text("+ New space", style = T.tileLabel.copy(fontSize = 12.sp), color = Atlas.Primary.copy(alpha = if (off) 0.5f else 1f))
        Spacer(Modifier.width(8.dp))
        Text(name + if (removable) "  ×" else "", style = T.metaStrong.copy(fontSize = 15.sp, textDecoration = if (off) androidx.compose.ui.text.style.TextDecoration.LineThrough else null), color = Atlas.Primary.copy(alpha = if (off) 0.5f else 1f))
    }
}

/** A photo or file waiting in the input box. */
@Composable
private fun Attachment(f: AtlasViewModel.Pending, onRemove: () -> Unit) =
    Box(Modifier.size(72.dp).shadow(6.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Atlas.Surface)) {
        if (f.thumb != null) androidx.compose.foundation.Image(f.thumb, f.read.name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        else Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(if (f.read.mime == "application/pdf") Icons.Outlined.DocumentScanner else Icons.Outlined.AttachFile, null, Modifier.size(20.dp), tint = Atlas.Primary)
            Text(f.read.name, style = T.meta.copy(fontSize = 10.sp), color = Atlas.DeepPlum, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Atlas.DeepPlum.copy(alpha = 0.75f)).clickable(role = Role.Button, onClickLabel = "Remove ${f.read.name}", onClick = onRemove),
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, "Remove", Modifier.size(14.dp), tint = Color.White) }
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
                if (q.isNotBlank() && s.live.none { it.title.equals(q.trim(), true) }) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) { vm.addNewLink(q); onDone() }.padding(horizontal = 8.dp, vertical = 8.dp).testTag("create_space"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Atlas.PrimaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Atlas.Primary) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Create “${Sentence.nameCase(q)}”", style = T.tileTitle.copy(fontSize = 15.sp), color = Atlas.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("New space · made when you send", style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText)
                        }
                    }
                }
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
