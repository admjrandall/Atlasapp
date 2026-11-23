package com.jmr.atlas.ui.cards

import androidx.compose.foundation.background
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

    Dialog({ vm.show(null) }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("edit_sheet"), color = Atlas.Background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ vm.show(null) }) { Text("Cancel") }
                    Text(if (existing == null) "New ${type.label.lowercase()}" else "Edit ${type.label.lowercase()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(::save, Modifier.testTag("save"), colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary)) { Text("Save") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 48.dp).imePadding()) {
                    if (linkObj != null) Text("Connected to ${linkObj.title}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                    if (existing == null && newType == null) FlowRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CREATABLE.forEach { t -> FilterChip(type == t, { type = t }, { Text(t.label) }, leadingIcon = { Icon(icon(t), null, Modifier.size(18.dp)) }) }
                    }
                    when (type) {
                        ObjType.PERSON -> {
                            if (base.contactKey.isNotBlank() || sync) Surface(color = Atlas.CoolMint, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(Modifier.padding(14.dp)) { Icon(Icons.Outlined.Call, null, tint = Atlas.DeepTeal); Spacer(Modifier.width(10.dp))
                                    Text(if (sync) "Contact fields save to your phone contact. Atlas-only fields stay in Atlas." else "Linked to a phone contact. Turn on two-way sync in Settings to save edits back to it.", color = Atlas.DeepTeal, style = MaterialTheme.typography.bodyMedium) }
                            }
                            Group("Photo")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PersonPhoto(base.copy(title = "$first $last"), 72.dp); Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    FilledTonalButton({ if (existing != null) platform.pickPhoto { vm.setPhoto(existing.id, it) } else vm.say("Save first, then choose a photo.") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose photo") }
                                    Text("From your photos · kept encrypted in Atlas", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                                }
                            }
                            Group("Name")
                            Field(first, { first = it }, "First name", Modifier.testTag("field_title"))
                            Field(last, { last = it }, "Last name")
                            Field(nickname, { nickname = it }, "Nickname")
                            Repeatable("Phone", DetailKind.PHONE, contact, Contacts.PHONE_LABELS, "Number", KeyboardType.Phone)
                            Repeatable("Email", DetailKind.EMAIL, contact, Contacts.EMAIL_LABELS, "Email", KeyboardType.Email)
                            Group("Work")
                            Field(org, { org = it }, "Company")
                            Field(role, { role = it }, "Title")
                            Repeatable("Address", DetailKind.ADDRESS, contact, Contacts.ADDRESS_LABELS, "Street, city, state", KeyboardType.Text)
                            Repeatable("Dates", DetailKind.DATE, contact, listOf("Birthday", "Anniversary", "Other"), "Date (YYYY-MM-DD or --MM-DD)", KeyboardType.Text, addLabel = "Add date")
                            RepeatableSocial(contact)
                            Group("In Atlas only", "Not saved to the phone contact")
                            Field(body, { body = it }, "How you know them", minLines = 2)
                        }
                        ObjType.PROJECT -> {
                            Field(title, { title = it }, "Title", Modifier.testTag("field_title"))
                            Group("Status")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ProjectStatus.entries.forEach { s -> FilterChip(status == s, { status = s }, { Text(s.label) }) } }
                            Group("Dates")
                            DateField("Started", start, { picking = { d -> start = d } }) { start = null }
                            DateField("Target", target, { picking = { d -> target = d } }) { target = null }
                            Field(body, { body = it }, "Goal and description", minLines = 3)
                        }
                        ObjType.TASK -> {
                            Field(title, { title = it }, "Task", Modifier.testTag("field_title"))
                            Group("When")
                            DateField("Plan", due, { picking = { d -> due = d } }) { due = null }
                            DateField("Deadline", deadline, { picking = { d -> deadline = d } }) { deadline = null }
                            Group("Priority")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Priority.entries.forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.label) }) } }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { Switch(blocked, { blocked = it }); Spacer(Modifier.width(12.dp)); Text("Blocked") }
                            Field(body, { body = it }, "Notes", minLines = 3)
                        }
                        ObjType.DOCUMENT, ObjType.IMAGE -> {
                            Field(title, { title = it }, "Title", Modifier.testTag("field_title"))
                            Group("Kind")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DOC_KINDS.forEach { k -> FilterChip(kind == k, { kind = if (kind == k) "" else k }, { Text(k) }) } }
                            Group("Dates")
                            DateField("On the document", docDate, { picking = { d -> docDate = d } }) { docDate = null }
                            Group("Key facts", "Read from the document — fix anything wrong")
                            facts.forEachIndexed { i, f ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(f.label, { facts[i] = f.copy(label = it) }, Modifier.weight(0.42f).padding(end = 6.dp), label = { Text("Label") }, singleLine = true)
                                    OutlinedTextField(f.value, { facts[i] = f.copy(value = it) }, Modifier.weight(0.58f), label = { Text("Value") }, singleLine = true)
                                    IconButton({ facts.removeAt(i) }, Modifier.semantics { contentDescription = "Remove fact" }) { Icon(Icons.Outlined.Close, null) }
                                }
                            }
                            TextButton({ facts.add(Detail(objectId = base.id, kind = DetailKind.FACT, label = "", value = "")) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add fact") }
                            Field(body, { body = it }, "Your note", minLines = 2)
                        }
                        ObjType.LINK -> {
                            Field(title, { title = it }, "Title", Modifier.testTag("field_title"))
                            Field(url, { url = it }, "Address (https://…)", keyboard = KeyboardType.Uri)
                            Group("Kind")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LINK_KINDS.forEach { k -> FilterChip(kind == k, { kind = if (kind == k) "" else k }, { Text(k) }) } }
                            Field(body, { body = it }, "Your note", minLines = 2)
                        }
                        ObjType.EVENT -> {
                            Field(title, { title = it }, "Milestone", Modifier.testTag("field_title"))
                            DateField("Date", eventDate, { picking = { d -> eventDate = d } }) { eventDate = null }
                            Field(body, { body = it }, "Details", minLines = 3)
                        }
                        else -> {
                            Field(title, { title = it }, "Title", Modifier.testTag("field_title"))
                            Field(body, { body = it }, if (type == ObjType.NOTE) "Note" else "Details", minLines = 5)
                        }
                    }
                    if (custom.isNotEmpty()) {
                        Group("Your fields")
                        custom.forEachIndexed { i, f -> CustomFieldEditor(vm, snap, f) { custom[i] = it } }
                    }
                }
            }
        }
    }
    picking?.let { cb -> DatePick(LocalDate.now(), { picking = null }) { cb(it); picking = null } }
}

@Composable
private fun Group(title: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
    }
}

@Composable
fun Field(v: String, on: (String) -> Unit, label: String, modifier: Modifier = Modifier, minLines: Int = 1, keyboard: KeyboardType = KeyboardType.Text) =
    OutlinedTextField(v, on, modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text(label) }, singleLine = minLines == 1, minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None, keyboardType = keyboard))

@Composable
fun DateField(label: String, d: LocalDate?, pick: () -> Unit, clear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(130.dp))
        AssistChip(pick, { Text(d?.let { dayDate(it) } ?: "Set date") }, leadingIcon = { Icon(Icons.Outlined.Event, null) })
        if (d != null) TextButton(clear) { Text("Clear") }
    }
}

@Composable
private fun LabelPicker(label: String, options: List<String>, on: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { open = true }, shape = RoundedCornerShape(12.dp), color = Atlas.SurfaceSecondary, modifier = Modifier.padding(end = 8.dp).widthIn(min = 92.dp).heightIn(min = 52.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label.ifBlank { "Label" }, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, false)); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp)) }
        }
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem({ Text(o) }, { on(o); open = false }) } }
    }
}

@Composable
private fun Repeatable(title: String, kind: DetailKind, list: SnapshotStateList<Detail>, labels: List<String>, hint: String, kb: KeyboardType, addLabel: String = "Add ${title.lowercase()}") {
    Group(title)
    list.withIndex().filter { it.value.kind == kind }.forEach { (i, d) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPicker(d.label, labels) { list[i] = d.copy(label = it) }
            OutlinedTextField(d.value, { list[i] = d.copy(value = it) }, Modifier.weight(1f), label = { Text(hint) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = kb))
            IconButton({ list.removeAt(i) }, Modifier.semantics { contentDescription = "Remove" }) { Icon(Icons.Outlined.Close, null) }
        }
    }
    TextButton({ list.add(Detail(objectId = "", kind = kind, label = labels.first())) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text(addLabel) }
}

@Composable
private fun RepeatableSocial(list: SnapshotStateList<Detail>) {
    Group("Websites and social")
    list.withIndex().filter { it.value.kind == DetailKind.WEBSITE || it.value.kind == DetailKind.SOCIAL }.forEach { (i, d) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPicker(d.label, Contacts.SOCIAL_LABELS) { list[i] = d.copy(label = it, kind = if (it == "Website") DetailKind.WEBSITE else DetailKind.SOCIAL) }
            OutlinedTextField(d.value, { list[i] = d.copy(value = it) }, Modifier.weight(1f), label = { Text("Link") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            IconButton({ list.removeAt(i) }, Modifier.semantics { contentDescription = "Remove" }) { Icon(Icons.Outlined.Close, null) }
        }
    }
    TextButton({ list.add(Detail(objectId = "", kind = DetailKind.SOCIAL, label = "LinkedIn")) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add link") }
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
        else -> OutlinedTextField(f.value, { on(f.copy(value = it)) }, Modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text(f.label) }, singleLine = true,
            leadingIcon = if (f.type == FieldType.MONEY) ({ Text("$") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = when (f.type) { FieldType.NUMBER, FieldType.MONEY -> KeyboardType.Decimal; FieldType.LINK -> KeyboardType.Uri; else -> KeyboardType.Text }),
            visualTransformation = if (f.flag) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(initial: LocalDate, dismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(dismiss, confirmButton = { TextButton({ st.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: dismiss() }) { Text("Set") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } }) { DatePicker(st) }
}
