package com.jmr.atlas.ui.objects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.icon
import com.jmr.atlas.ui.universe.slotsFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun Overlays(vm: AtlasViewModel, snap: Snapshot) {
    val ov by vm.overlay.collectAsState()
    when (val o = ov) {
        null -> {}
        is Overlay.Edit -> snap.objects[o.id]?.let { EditSheet(vm, snap, it, null, null) } ?: vm.show(null)
        is Overlay.Create -> EditSheet(vm, snap, null, o.type, o.linkTo, o.title)
        is Overlay.Connect -> snap.objects[o.id]?.let { ConnectDialog(vm, snap, it, o.types) } ?: vm.show(null)
        is Overlay.Cluster -> ClusterSheet(vm, snap, o)
        Overlay.FocusPicker -> FocusPicker(vm, snap)
        is Overlay.ConfirmPurge -> snap.objects[o.id]?.let { obj ->
            AlertDialog({ vm.show(null) }, title = { Text("Delete “${obj.title}” forever?") },
                text = { Text("Its connections and activity are removed${if (obj.vaultFile.isNotBlank()) ", and the encrypted original is erased" else ""}. This can't be undone.") },
                confirmButton = { TextButton({ vm.repo.purge(obj.id); vm.show(null); vm.say("Deleted.") }) { Text("Delete forever", color = Atlas.Error) } },
                dismissButton = { TextButton({ vm.show(null) }) { Text("Cancel") } })
        } ?: vm.show(null)
    }
}

private val CREATABLE = listOf(ObjType.NOTE, ObjType.TASK, ObjType.PROJECT, ObjType.PERSON, ObjType.IDEA, ObjType.LINK, ObjType.EVENT, ObjType.COLLECTION, ObjType.DECISION)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditSheet(vm: AtlasViewModel, snap: Snapshot, existing: KObject?, newType: ObjType?, linkTo: String?, initialTitle: String = "") {
    var type by remember { mutableStateOf(existing?.type ?: newType ?: ObjType.NOTE) }
    var title by remember { mutableStateOf(existing?.title ?: initialTitle) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var role by remember { mutableStateOf(existing?.role.orEmpty()) }
    var org by remember { mutableStateOf(existing?.org.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var url by remember { mutableStateOf(existing?.url.orEmpty()) }
    var status by remember { mutableStateOf(existing?.projectStatus ?: ProjectStatus.ACTIVE) }
    var due by remember { mutableStateOf(existing?.due) }
    var target by remember { mutableStateOf(existing?.targetDate) }
    var eventDate by remember { mutableStateOf(existing?.eventDate) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.NONE) }
    var blocked by remember { mutableStateOf(existing?.blocked ?: false) }
    var picking by remember { mutableStateOf<String?>(null) }
    val linkObj = linkTo?.let { snap.objects[it] }

    ModalBottomSheet({ vm.show(null) }, containerColor = Atlas.Surface, modifier = Modifier.testTag("edit_sheet")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(if (existing == null) "New ${type.label.lowercase()}" else "Edit ${type.label.lowercase()}", style = MaterialTheme.typography.titleLarge)
            if (linkObj != null) Text("Connected to ${linkObj.title}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            if (existing == null && newType == null) FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CREATABLE.forEach { t -> FilterChip(type == t, { type = t }, { Text(t.label) }, leadingIcon = { Icon(icon(t), null, Modifier.size(18.dp)) }) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().testTag("field_title"), label = { Text(if (type == ObjType.PERSON) "Name" else "Title") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            when (type) {
                ObjType.PERSON -> {
                    OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Role") }, singleLine = true)
                    OutlinedTextField(org, { org = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Organization") }, singleLine = true)
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Phone") }, singleLine = true)
                }
                ObjType.PROJECT -> {
                    Text("Status", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ProjectStatus.entries.forEach { s -> FilterChip(status == s, { status = s }, { Text(s.label) }) } }
                    DateRow("Target date", target, { picking = "target" }) { target = null }
                }
                ObjType.TASK -> {
                    DateRow("Due", due, { picking = "due" }) { due = null }
                    Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Priority.entries.forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.label) }) } }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { Switch(blocked, { blocked = it }); Spacer(Modifier.width(12.dp)); Text("Blocked") }
                }
                ObjType.EVENT -> DateRow("Date", eventDate, { picking = "event" }) { eventDate = null }
                ObjType.LINK -> OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Address (https://…)") }, singleLine = true)
                else -> {}
            }
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 120.dp), label = { Text(if (type == ObjType.NOTE) "Note" else "Description") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton({ vm.show(null) }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button({
                    val t = title.trim().ifEmpty { if (type == ObjType.NOTE && body.isNotBlank()) com.jmr.atlas.data.Repository.titleFrom(body) else "" }
                    if (t.isEmpty()) { vm.say("Give it a title."); return@Button }
                    val cleanUrl = url.trim().takeIf { it.isEmpty() || it.startsWith("https://") || it.startsWith("http://") } ?: "https://${url.trim()}"
                    val base = existing ?: KObject(type = type, title = t)
                    val obj = base.copy(type = type, title = t, body = body.trim(), role = role.trim(), org = org.trim(), email = email.trim(), phone = phone.trim(), url = cleanUrl,
                        projectStatus = if (type == ObjType.PROJECT) status else base.projectStatus, due = if (type == ObjType.TASK) due else base.due,
                        targetDate = if (type == ObjType.PROJECT) target else base.targetDate, eventDate = if (type == ObjType.EVENT) eventDate else base.eventDate,
                        priority = priority, blocked = blocked)
                    if (existing == null) {
                        val rel = when { linkObj == null -> null; type == ObjType.TASK && linkObj.type == ObjType.PERSON -> RelType.RELATED_TO
                            type in setOf(ObjType.TASK, ObjType.EVENT, ObjType.NOTE, ObjType.DOCUMENT, ObjType.IDEA) && linkObj.type in setOf(ObjType.PROJECT, ObjType.COLLECTION) -> RelType.BELONGS_TO
                            type == ObjType.PROJECT && linkObj.type == ObjType.COLLECTION -> RelType.PART_OF; else -> RelType.RELATED_TO }
                        vm.repo.create(obj, listOfNotNull(rel?.let { linkObj!!.id to it }))
                        vm.say("${type.label} created.")
                    } else vm.repo.update(obj)
                    vm.show(null)
                }, Modifier.testTag("save")) { Text("Save") }
            }
        }
    }
    picking?.let { which ->
        val initial = when (which) { "due" -> due; "target" -> target; else -> eventDate } ?: LocalDate.now()
        val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog({ picking = null }, confirmButton = {
            TextButton({
                st.selectedDateMillis?.let { ms -> val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate(); when (which) { "due" -> due = d; "target" -> target = d; else -> eventDate = d } }
                picking = null
            }) { Text("Set") }
        }, dismissButton = { TextButton({ picking = null }) { Text("Cancel") } }) { DatePicker(st) }
    }
}

@Composable
private fun DateRow(label: String, d: LocalDate?, pick: () -> Unit, clear: () -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(100.dp))
        AssistChip(pick, { Text(d?.let { relDate(it) + " · " + it } ?: "Set date") }, leadingIcon = { Icon(Icons.Outlined.Event, null) })
        if (d != null) TextButton(clear) { Text("Clear") }
    }
}

@Composable
fun ConnectDialog(vm: AtlasViewModel, snap: Snapshot, o: KObject, types: Set<ObjType>?) {
    var q by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf(RelType.RELATED_TO) }
    var relMenu by remember { mutableStateOf(false) }
    val already = snap.relationsOf(o.id).map { it.other(o.id) }.toSet()
    val candidates = snap.live.filter { it.id != o.id && it.id !in already && (types == null || it.type in types) && (q.isBlank() || it.title.contains(q, true)) }
        .sortedByDescending { it.updated }.take(60)
    AlertDialog({ vm.show(null) }, title = { Text("Connect ${o.title}") }, modifier = Modifier.testTag("connect_dialog"), text = {
        Column {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().testTag("connect_search"), placeholder = { Text("Search…") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            Box(Modifier.padding(vertical = 8.dp)) {
                AssistChip({ relMenu = true }, { Text("As: ${rel.label}") }, trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) })
                DropdownMenu(relMenu, { relMenu = false }) { RelType.entries.filter { it != RelType.DUPLICATE_OF }.forEach { r -> DropdownMenuItem({ Text(r.label) }, { rel = r; relMenu = false }) } }
            }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (candidates.isEmpty()) item { Text("Nothing to connect.", color = Atlas.SecondaryText, modifier = Modifier.padding(8.dp)) }
                items(candidates, key = { it.id }) { c ->
                    ObjRow(c, snap) {
                        if (vm.repo.link(o.id, c.id, rel)) vm.say("Connected ${o.title} and ${c.title}.") else vm.say("Already connected.")
                        vm.show(null)
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton({ vm.show(null) }) { Text("Close") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClusterSheet(vm: AtlasViewModel, snap: Snapshot, c: Overlay.Cluster) {
    val focus = c.focusId?.let { snap.objects[it] }
    val slot = remember(snap, c) { slotsFor(snap, c.focusId).first { it.kind == c.kind } }
    ModalBottomSheet({ vm.show(null) }, containerColor = Atlas.Surface, modifier = Modifier.testTag("cluster_sheet")) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(slot.label(focus), style = MaterialTheme.typography.titleLarge)
            Text(if (focus == null) "${slot.total} in your Atlas" else "${slot.total} connected to ${focus.title}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ vm.show(Overlay.Create(c.kind.createType, c.focusId)) }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("New ${c.kind.createType.label.lowercase()}") }
                if (focus != null) OutlinedButton({ vm.show(Overlay.Connect(focus.id, c.kind.types)) }) { Icon(Icons.Outlined.AddLink, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Connect existing") }
            }
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                items(slot.items, key = { it.obj.id }) { it ->
                    ObjRow(it.obj, snap, proposed = it.proposed, onToggle = { vm.repo.toggleDone(it.obj.id) }) { vm.show(null); vm.focusOn(it.obj.id) }
                }
            }
        }
    }
}

@Composable
fun FocusPicker(vm: AtlasViewModel, snap: Snapshot) {
    var q by remember { mutableStateOf("") }
    val order = listOf(ObjType.PROJECT, ObjType.PERSON, ObjType.COLLECTION, ObjType.IDEA, ObjType.DOCUMENT, ObjType.NOTE, ObjType.TASK, ObjType.EVENT, ObjType.IMAGE, ObjType.LINK, ObjType.DECISION)
    val list = snap.live.filter { q.isBlank() || it.title.contains(q, true) }.sortedWith(compareBy<KObject> { order.indexOf(it.type) }.thenByDescending { snap.relationsOf(it.id).size }).take(80)
    AlertDialog({ vm.show(null) }, title = { Text("Focus the map on…") }, modifier = Modifier.testTag("focus_dialog"), text = {
        Column {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search…") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                item {
                    ListItem({ Text("My Universe") }, supportingContent = { Text("Everything, summarized") }, leadingContent = { AtlasMark(36.dp) },
                        modifier = Modifier.clickableRow { vm.show(null); vm.focusOn(null) })
                }
                items(list, key = { it.id }) { o -> ObjRow(o, snap, sub = "${typeLine(o)} · ${snap.relationsOf(o.id).size} connections") { vm.show(null); vm.focusOn(o.id) } }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton({ vm.show(null) }) { Text("Close") } })
}

fun Modifier.clickableRow(onClick: () -> Unit) = this.then(Modifier.clickable(onClick = onClick))
