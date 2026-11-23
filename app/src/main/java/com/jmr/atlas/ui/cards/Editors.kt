package com.jmr.atlas.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.integrations.Contacts
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.icon
import com.jmr.atlas.ui.theme.T
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val CREATABLE = listOf(ObjType.NOTE, ObjType.TASK, ObjType.PROJECT, ObjType.PERSON, ObjType.IDEA, ObjType.LINK, ObjType.EVENT, ObjType.COLLECTION, ObjType.DECISION)
val DOC_KINDS = listOf("Estimate", "Invoice", "Receipt", "Contract", "Policy", "Statement", "Permit", "Warranty", "Manual", "Letter", "Report", "Other")
val LINK_KINDS = listOf("Reference", "Article", "Recipe", "Product", "Video", "Tool", "Inspiration")

/** Full-screen editor shaped by the item's type (design I02, I10). Contact fields save to the phone contact when sync is on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: AtlasViewModel, snap: Snapshot, existing: KObject?, newType: ObjType?, linkTo: String?, initialTitle: String = "") {
    val platform = LocalPlatform.current
    var type by remember { mutableStateOf(existing?.type ?: newType ?: ObjType.NOTE) }
    val base = existing ?: KObject(type = type, title = "")
    var title by remember { mutableStateOf(existing?.title ?: initialTitle) }
    var first by remember { mutableStateOf(existing?.title?.substringBefore(' ').orEmpty().ifEmpty { initialTitle.substringBefore(' ') }) }
    var last by remember { mutableStateOf(existing?.title?.substringAfter(' ', "").orEmpty().ifEmpty { initialTitle.substringAfter(' ', "") }) }
    var nickname by remember { mutableStateOf(base.nickname) }
    var body by remember { mutableStateOf(base.body) }
    var role by remember { mutableStateOf(base.role) }
    var org by remember { mutableStateOf(base.org) }
    var url by remember { mutableStateOf(base.url) }
    var kind by remember { mutableStateOf(base.kind) }
    var status by remember { mutableStateOf(base.projectStatus ?: ProjectStatus.ACTIVE) }
    var due by remember { mutableStateOf(base.due) }
    var deadline by remember { mutableStateOf(base.deadline) }
    var start by remember { mutableStateOf(base.startDate) }
    var target by remember { mutableStateOf(base.targetDate) }
    var eventDate by remember { mutableStateOf(base.eventDate) }
    var docDate by remember { mutableStateOf(base.docDate) }
    var priority by remember { mutableStateOf(base.priority) }
    var blocked by remember { mutableStateOf(base.blocked) }
    var picking by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    val contact = remember { mutableStateListOf<Detail>().apply { addAll(snap.detailsOf(base.id).filter { it.kind in com.jmr.atlas.integrations.Sync.CONTACT_KINDS }) } }
    val facts = remember { mutableStateListOf<Detail>().apply { addAll(snap.detailsOf(base.id, DetailKind.FACT)) } }
    val custom = remember { mutableStateListOf<Detail>().apply { addAll(if (existing != null) vm.repo.fieldsFor(existing) else vm.repo.fieldsFor(base.copy(type = type))) } }
    val linkObj = linkTo?.let { snap.objects[it] }
    val sync = vm.settings.contactsSync

    fun save() {
        val t = (if (type == ObjType.PERSON) "${first.trim()} ${last.trim()}".trim() else title.trim()).ifEmpty { if (body.isNotBlank()) com.jmr.atlas.data.Repository.titleFrom(body) else "" }
        if (t.isEmpty()) { vm.say(if (type == ObjType.PERSON) "Add a name." else "Give it a title."); return }
        val cleanUrl = url.trim().let { if (it.isEmpty() || it.startsWith("https://") || it.startsWith("http://")) it else "https://$it" }
        val obj = base.copy(type = type, title = t, body = body.trim(), role = role.trim(), org = org.trim(), nickname = nickname.trim(), url = cleanUrl, kind = kind,
            projectStatus = if (type == ObjType.PROJECT) status else base.projectStatus, due = due, deadline = deadline, startDate = start, targetDate = target,
            eventDate = eventDate, docDate = docDate, priority = priority, blocked = blocked,
            email = contact.firstOrNull { it.kind == DetailKind.EMAIL && it.value.isNotBlank() }?.value.orEmpty(), phone = contact.firstOrNull { it.kind == DetailKind.PHONE && it.value.isNotBlank() }?.value.orEmpty())
        val rel = when { linkObj == null -> null; type == ObjType.TASK && linkObj.type == ObjType.PERSON -> RelType.RELATED_TO
            type in setOf(ObjType.TASK, ObjType.EVENT, ObjType.NOTE, ObjType.DOCUMENT, ObjType.IDEA, ObjType.DECISION) && linkObj.type in setOf(ObjType.PROJECT, ObjType.COLLECTION) -> RelType.BELONGS_TO
            type == ObjType.PROJECT && linkObj.type == ObjType.COLLECTION -> RelType.PART_OF; else -> RelType.RELATED_TO }
        val id = if (type == ObjType.PERSON) vm.savePerson(obj, contact.filter { it.value.isNotBlank() }, custom.toList(), existing == null, linkObj?.id)
        else {
            val id = if (existing == null) vm.repo.create(obj, listOfNotNull(rel?.let { linkObj!!.id to it })) else { vm.repo.update(obj); obj.id }
            vm.repo.replaceDetails(id, setOf(DetailKind.CUSTOM), custom.filter { it.value.isNotBlank() })
            if (type == ObjType.DOCUMENT || type == ObjType.IMAGE) vm.repo.replaceDetails(id, setOf(DetailKind.FACT), facts.filter { it.value.isNotBlank() })
            if (existing == null && type == ObjType.LINK && cleanUrl.isNotBlank()) vm.sync.preview(id)
            id
        }
        vm.show(null)
        if (existing == null) { vm.say("${type.label} created."); vm.open(id) }
    }

    val (tint, ink) = heroColors(type)
    Dialog({ vm.show(null) }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("edit_sheet"), color = Atlas.Background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill6("Cancel", Atlas.Chip, Atlas.DeepPlum) { vm.show(null) }
                    Text((if (existing == null) "New " else "Edit ") + type.label.lowercase(), style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Pill6("Save", Atlas.DeepPlum, androidx.compose.ui.graphics.Color.White, Modifier.testTag("save")) { save() }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 48.dp).imePadding().widthIn(max = 720.dp)) {
                    if (existing == null && newType == null) ChoiceRow(CREATABLE.map { it.label }, type.label) { l -> type = CREATABLE.first { it.label == l } }
                    // ---- hero: what it is, its name, its words — edited in place, on the same tint as its page
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(28.dp)).background(tint).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(type.label.uppercase(), style = T.eyebrow, color = ink)
                        if (linkObj != null) Text("Linked to ${linkObj.title}", style = T.meta, color = Atlas.DeepPlum)
                        when (type) {
                            ObjType.PERSON -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Choose photo") {
                                        if (existing != null) platform.pickPhoto { vm.setPhoto(existing.id, it) } else vm.say("Save first, then choose a photo.") }) { PersonPhoto(base.copy(title = "$first $last"), 72.dp) }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        TitleField(first, { first = it }, "First name", Modifier.testTag("field_title"))
                                        TitleField(last, { last = it }, "Last name")
                                    }
                                }
                                HeroLine(nickname, { nickname = it }, "Nickname")
                            }
                            else -> TitleField(title, { title = it }, when (type) { ObjType.TASK -> "What needs doing"; ObjType.EVENT -> "Milestone"; ObjType.PROJECT -> "Project name"; else -> "Title" }, Modifier.testTag("field_title"))
                        }
                        val bodyHint = when (type) { ObjType.PERSON -> "How you know them (stays in Atlas)"; ObjType.PROJECT -> "Goal and description"; ObjType.NOTE -> "Note"; ObjType.TASK -> "Notes"; else -> "Your note" }
                        HeroLine(body, { body = it }, bodyHint, minLines = if (type == ObjType.NOTE || type == ObjType.IDEA || type == ObjType.DECISION) 4 else 2)
                    }
                    when (type) {
                        ObjType.PERSON -> {
                            if (base.contactKey.isNotBlank() || sync) Text(if (sync) "Phone, email, address, work and dates save to your phone contact. Notes stay in Atlas." else "Linked to a phone contact. Turn on two-way sync in Settings to save edits back to it.",
                                style = T.meta, color = Atlas.DeepTeal, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                            Repeatable("Phone", DetailKind.PHONE, contact, Contacts.PHONE_LABELS, "Number", KeyboardType.Phone)
                            Repeatable("Email", DetailKind.EMAIL, contact, Contacts.EMAIL_LABELS, "Email", KeyboardType.Email)
                            Group("Work")
                            Card6 { RowField(org, { org = it }, "Company"); Line6(); RowField(role, { role = it }, "Title") }
                            Repeatable("Address", DetailKind.ADDRESS, contact, Contacts.ADDRESS_LABELS, "Street, city, state", KeyboardType.Text)
                            Repeatable("Dates", DetailKind.DATE, contact, listOf("Birthday", "Anniversary", "Other"), "YYYY-MM-DD or --MM-DD", KeyboardType.Text, addLabel = "Add date")
                            RepeatableSocial(contact)
                        }
                        ObjType.PROJECT -> {
                            Group("Status")
                            ChoiceRow(ProjectStatus.entries.map { it.label }, status.label) { l -> status = ProjectStatus.entries.first { it.label == l } }
                            Group("Dates")
                            Card6 { DateField("Started", start, { picking = { d -> start = d } }) { start = null }; Line6(); DateField("Finish by", target, { picking = { d -> target = d } }) { target = null } }
                        }
                        ObjType.TASK -> {
                            Group("When")
                            Card6 { DateField("Plan", due, { picking = { d -> due = d } }) { due = null }; Line6(); DateField("Deadline", deadline, { picking = { d -> deadline = d } }) { deadline = null } }
                            Group("Priority")
                            ChoiceRow(Priority.entries.map { it.label }, priority.label) { l -> priority = Priority.entries.first { it.label == l } }
                            Spacer(Modifier.height(10.dp))
                            Card6 { Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("Blocked", style = T.row.copy(fontSize = 16.sp), color = Atlas.DeepPlum); Text("Waiting on something before you can start", style = T.meta, color = Atlas.SecondaryText) }
                                Switch(blocked, { blocked = it }) } }
                        }
                        ObjType.DOCUMENT, ObjType.IMAGE -> {
                            Group("Kind")
                            ChoiceRow(DOC_KINDS, kind.takeIf { it.isNotBlank() }) { k -> kind = if (kind == k) "" else k }
                            Group("Dates")
                            Card6 { DateField("On the document", docDate, { picking = { d -> docDate = d } }) { docDate = null } }
                            Group("Key facts", "Read from the document — fix anything wrong")
                            Card6 {
                                facts.forEachIndexed { i, f ->
                                    if (i > 0) Line6()
                                    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Plain(f.label, { facts[i] = f.copy(label = it) }, "Label", Modifier.weight(0.42f))
                                        Plain(f.value, { facts[i] = f.copy(value = it) }, "Value", Modifier.weight(0.58f))
                                        IconButton({ facts.removeAt(i) }, Modifier.semantics { contentDescription = "Remove fact" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.Hint) }
                                    }
                                }
                                if (facts.isNotEmpty()) Line6()
                                AddRow("Add fact") { facts.add(Detail(objectId = base.id, kind = DetailKind.FACT, label = "", value = "")) }
                            }
                        }
                        ObjType.LINK -> {
                            Group("Address")
                            Card6 { RowField(url, { url = it }, "https://…", keyboard = KeyboardType.Uri) }
                            Group("Kind")
                            ChoiceRow(LINK_KINDS, kind.takeIf { it.isNotBlank() }) { k -> kind = if (kind == k) "" else k }
                        }
                        ObjType.EVENT -> {
                            Group("Date")
                            Card6 { DateField("Date", eventDate, { picking = { d -> eventDate = d } }) { eventDate = null } }
                        }
                        else -> {}
                    }
                    if (custom.isNotEmpty()) {
                        Group("Your fields")
                        Card6(Modifier.padding(horizontal = 0.dp)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { custom.forEachIndexed { i, f -> CustomFieldEditor(vm, snap, f) { custom[i] = it } } } }
                    }
                }
            }
        }
    }
    picking?.let { cb -> DatePick(LocalDate.now(), { picking = null }) { cb(it); picking = null } }
}

private fun heroColors(t: ObjType): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (t) {
    ObjType.PERSON, ObjType.PROJECT, ObjType.COLLECTION -> Atlas.PrimaryContainer to Atlas.Primary
    ObjType.IMAGE -> Atlas.Peach to Atlas.CoralText
    else -> com.jmr.atlas.ui.theme.accent(t).let { it.container to it.content }
}

@Composable
private fun Group(title: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(title.uppercase(), style = T.eyebrow, color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
        sub?.let { Text(it, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText) }
    }
}

/** White grouped card that holds rows, like a page section. */
@Composable
fun Card6(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Atlas.Surface), content = content)

@Composable
fun Line6() = HorizontalDivider(Modifier.padding(start = 16.dp), color = Atlas.Hairline)

@Composable
private fun Pill6(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Box(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(bg).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center) { Text(text, style = T.row.copy(fontSize = 15.sp), color = fg) }

/** The big title, typed straight into the hero. */
@Composable
private fun TitleField(v: String, on: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) =
    androidx.compose.foundation.text.BasicTextField(v, on, modifier.fillMaxWidth(), textStyle = T.heroTitle.copy(fontSize = 28.sp, lineHeight = 33.sp, color = Atlas.DeepPlum),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner -> Box { if (v.isEmpty()) Text(placeholder, style = T.heroTitle.copy(fontSize = 28.sp, lineHeight = 33.sp), color = Atlas.DeepPlum.copy(alpha = 0.35f)); inner() } })

/** A borderless line of text on the hero tint (notes, nickname). */
@Composable
private fun HeroLine(v: String, on: (String) -> Unit, placeholder: String, minLines: Int = 1) =
    androidx.compose.foundation.text.BasicTextField(v, on, Modifier.fillMaxWidth().padding(top = 4.dp), textStyle = T.body.copy(fontSize = 17.sp, lineHeight = 25.sp, color = Atlas.DeepPlum),
        minLines = minLines, cursorBrush = androidx.compose.ui.graphics.SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner -> Box { if (v.isEmpty()) Text(placeholder, style = T.body.copy(fontSize = 17.sp, lineHeight = 25.sp), color = Atlas.DeepPlum.copy(alpha = 0.45f)); inner() } })

@Composable
private fun Plain(v: String, on: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, minLines: Int = 1) =
    androidx.compose.foundation.text.BasicTextField(v, on, modifier.padding(vertical = 16.dp), textStyle = T.body.copy(fontSize = 17.sp, color = Atlas.DeepPlum), singleLine = singleLine, minLines = minLines,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Atlas.Primary),
        keyboardOptions = KeyboardOptions(capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None, keyboardType = keyboard),
        decorationBox = { inner -> Box { if (v.isEmpty()) Text(placeholder, style = T.body.copy(fontSize = 17.sp), color = Atlas.Hint); inner() } })

/** One editable row inside a Card6: placeholder shows what goes there. */
@Composable
private fun RowField(v: String, on: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) =
    Plain(v, on, placeholder, Modifier.fillMaxWidth().padding(horizontal = 16.dp), keyboard)

/** A labelled field on its own white card (also used by the small sheets). [modifier] lands on the text field. */
@Composable
fun Field(v: String, on: (String) -> Unit, label: String, modifier: Modifier = Modifier, minLines: Int = 1, keyboard: KeyboardType = KeyboardType.Text) =
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Atlas.Surface).border(1.dp, Atlas.Hairline, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(label, style = T.meta.copy(fontSize = 12.sp), color = Atlas.SecondaryText)
        androidx.compose.foundation.text.BasicTextField(v, on, modifier.fillMaxWidth().padding(top = 4.dp), textStyle = T.body.copy(fontSize = 17.sp, color = Atlas.DeepPlum),
            singleLine = minLines == 1, minLines = minLines, cursorBrush = androidx.compose.ui.graphics.SolidColor(Atlas.Primary),
            keyboardOptions = KeyboardOptions(capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None, keyboardType = keyboard))
    }

/** A date as a tappable row: label, the date (or "Not set"), and × to clear. */
@Composable
fun DateField(label: String, d: LocalDate?, pick: () -> Unit, clear: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Set $label", onClick = pick).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.row.copy(fontSize = 16.sp), color = Atlas.DeepPlum, modifier = Modifier.weight(1f))
        Text(d?.let { dayDate(it) } ?: "Not set", style = T.row.copy(fontSize = 16.sp), color = if (d != null) Atlas.Primary else Atlas.Hint)
        if (d != null) IconButton(clear, Modifier.semantics { contentDescription = "Clear $label" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.Hint, modifier = Modifier.size(20.dp)) }
        else Spacer(Modifier.width(14.dp))
    }
}

/** Pills for picking one of a few values (status, priority, kind). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(if (on) Atlas.DeepPlum else Atlas.Surface)
                .then(if (on) Modifier else Modifier.border(1.dp, Atlas.Border, RoundedCornerShape(22.dp)))
                .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { onSelect(o) }).semantics { this.selected = on }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) { Text(o, style = T.metaStrong.copy(fontSize = 15.sp), color = if (on) androidx.compose.ui.graphics.Color.White else Atlas.DeepPlum) }
        }
    }

@Composable
private fun AddRow(text: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Add, null, tint = Atlas.Primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
        Text(text, style = T.row.copy(fontSize = 16.sp), color = Atlas.Primary)
    }

@Composable
private fun LabelPicker(label: String, options: List<String>, on: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.padding(end = 10.dp).widthIn(min = 84.dp).heightIn(min = 36.dp).clip(RoundedCornerShape(10.dp)).background(Atlas.Chip).clickable(role = androidx.compose.ui.semantics.Role.Button) { open = true }
            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label.ifBlank { "Label" }, style = T.metaStrong, color = Atlas.DeepPlum, modifier = Modifier.weight(1f, false)); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = Atlas.SecondaryText)
        }
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem({ Text(o) }, { on(o); open = false }) } }
    }
}

@Composable
private fun Repeatable(title: String, kind: DetailKind, list: SnapshotStateList<Detail>, labels: List<String>, hint: String, kb: KeyboardType, addLabel: String = "Add ${title.lowercase()}") {
    Group(title)
    Card6 {
        val rows = list.withIndex().filter { it.value.kind == kind }
        rows.forEach { (i, d) ->
            Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LabelPicker(d.label, labels) { list[i] = d.copy(label = it) }
                Plain(d.value, { list[i] = d.copy(value = it) }, hint, Modifier.weight(1f), kb)
                IconButton({ list.removeAt(i) }, Modifier.semantics { contentDescription = "Remove" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.Hint) }
            }
            Line6()
        }
        AddRow(addLabel) { list.add(Detail(objectId = "", kind = kind, label = labels.first())) }
    }
}

@Composable
private fun RepeatableSocial(list: SnapshotStateList<Detail>) {
    Group("Websites and social")
    Card6 {
        list.withIndex().filter { it.value.kind == DetailKind.WEBSITE || it.value.kind == DetailKind.SOCIAL }.forEach { (i, d) ->
            Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LabelPicker(d.label, Contacts.SOCIAL_LABELS) { list[i] = d.copy(label = it, kind = if (it == "Website") DetailKind.WEBSITE else DetailKind.SOCIAL) }
                Plain(d.value, { list[i] = d.copy(value = it) }, "Link", Modifier.weight(1f), KeyboardType.Uri)
                IconButton({ list.removeAt(i) }, Modifier.semantics { contentDescription = "Remove" }) { Icon(Icons.Outlined.Close, null, tint = Atlas.Hint) }
            }
            Line6()
        }
        AddRow("Add link") { list.add(Detail(objectId = "", kind = DetailKind.SOCIAL, label = "LinkedIn")) }
    }
}

/** Value editor for one custom field, shaped by its type. */
@Composable
fun CustomFieldEditor(vm: AtlasViewModel, snap: Snapshot, f: Detail, on: (Detail) -> Unit) {
    var pick by remember { mutableStateOf(false) }
    val defOptions = snap.fieldDefs.firstOrNull { it.id == f.extra }?.options.orEmpty()
    when (f.type) {
        FieldType.CHECKBOX -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); Switch(f.value == "true", { on(f.copy(value = it.toString())) }) }
        FieldType.RATING -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            (1..5).forEach { n -> IconButton({ on(f.copy(value = n.toString())) }, Modifier.semantics { contentDescription = "$n stars" }) {
                Icon(if ((f.value.toIntOrNull() ?: 0) >= n) Icons.Outlined.Star else Icons.Outlined.StarOutline, null, tint = Atlas.Primary) } }
        }
        FieldType.DATE -> { DateField(f.label, runCatching { LocalDate.parse(f.value) }.getOrNull(), { pick = true }) { on(f.copy(value = "")) }
            if (pick) DatePick(LocalDate.now(), { pick = false }) { on(f.copy(value = it.toString())); pick = false } }
        FieldType.CHOICE -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(130.dp)); LabelPicker(f.value, defOptions.ifEmpty { listOf(f.value).filter { it.isNotBlank() } }) { on(f.copy(value = it)) } }
        FieldType.PERSON, FieldType.ITEM -> {
            var q by remember { mutableStateOf(snap.objects[f.value]?.title.orEmpty()) }
            var open by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(q, { q = it; open = true }, Modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text(f.label) }, singleLine = true, trailingIcon = { Icon(icon(if (f.type == FieldType.PERSON) ObjType.PERSON else ObjType.COLLECTION), null) })
                DropdownMenu(open && q.isNotBlank(), { open = false }) {
                    snap.live.filter { (f.type == FieldType.ITEM || it.type == ObjType.PERSON) && it.title.contains(q, true) }.take(6).forEach { o ->
                        DropdownMenuItem({ Text(o.title) }, { q = o.title; on(f.copy(value = o.id)); open = false })
                    }
                }
            }
        }
        else -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.label, style = T.row.copy(fontSize = 16.sp), color = Atlas.DeepPlum, modifier = Modifier.width(130.dp).padding(start = 4.dp))
            if (f.type == FieldType.MONEY) Text("$", style = T.body.copy(fontSize = 17.sp), color = Atlas.SecondaryText)
            androidx.compose.foundation.text.BasicTextField(f.value, { on(f.copy(value = it)) }, Modifier.weight(1f).padding(vertical = 16.dp, horizontal = 4.dp), singleLine = true,
                textStyle = T.body.copy(fontSize = 17.sp, color = Atlas.DeepPlum), cursorBrush = androidx.compose.ui.graphics.SolidColor(Atlas.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = when (f.type) { FieldType.NUMBER, FieldType.MONEY -> KeyboardType.Decimal; FieldType.LINK -> KeyboardType.Uri; else -> KeyboardType.Text }),
                visualTransformation = if (f.flag) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                decorationBox = { inner -> Box { if (f.value.isEmpty()) Text("Empty", style = T.body.copy(fontSize = 17.sp), color = Atlas.Hint); inner() } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(initial: LocalDate, dismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(dismiss, confirmButton = { TextButton({ st.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: dismiss() }) { Text("Set") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } }) { DatePicker(st) }
}
