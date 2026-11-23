package com.jmr.atlas.ui.cards

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.integrations.Calendars
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.components.Pill
import com.jmr.atlas.ui.components.dayDate
import com.jmr.atlas.ui.theme.Atlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sheet(vm: AtlasViewModel, tag: String, content: @Composable ColumnScope.() -> Unit) =
    ModalBottomSheet({ vm.show(null) }, containerColor = Atlas.Surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp).imePadding().testTag(tag), content = content)
    }

@Composable
private fun Buttons(vm: AtlasViewModel, ok: String, enabled: Boolean = true, onOk: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton({ vm.show(null) }) { Text("Cancel") }; Spacer(Modifier.width(8.dp))
        Button(onOk, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary), modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_ok")) { Text(ok) }
    }
}

/** Design I06: name, one of ten types, and whether every item of this type gets it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFieldSheet(vm: AtlasViewModel, o: KObject) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(FieldType.TEXT) }
    var every by remember { mutableStateOf(false) }
    var secret by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf("") }
    Sheet(vm, "add_field") {
        Text("Add a field", style = MaterialTheme.typography.headlineSmall)
        Text("To ${o.type.plural}", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Field(name, { name = it }, "Field name", Modifier.testTag("field_name"))
        Text("Type", style = MaterialTheme.typography.labelLarge, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldType.entries.forEach { t ->
                val on = t == type
                Surface(onClick = { type = t }, shape = RoundedCornerShape(16.dp), color = if (on) Atlas.PrimaryContainer else Atlas.SurfaceSecondary,
                    border = if (on) BorderStroke(2.dp, Atlas.Primary) else null, modifier = Modifier.size(width = 96.dp, height = 72.dp).testTag("ftype_${t.name}")) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(fieldIcon(t), null, tint = Atlas.Primary); Spacer(Modifier.height(4.dp)); Text(t.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (type == FieldType.CHOICE) Field(options, { options = it }, "Choices, separated by commas")
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Every ${o.type.label.lowercase()}", style = MaterialTheme.typography.titleSmall); Text(if (every) "All ${o.type.plural.lowercase()} get this field" else "Off: only ${o.title}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
            Switch(every, { every = it })
        }
        if (type in setOf(FieldType.TEXT, FieldType.NUMBER)) Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Hide the value", style = MaterialTheme.typography.titleSmall); Text("Masked until you tap Show (license or account numbers)", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
            Switch(secret, { secret = it })
        }
        Buttons(vm, "Add field", name.isNotBlank()) {
            val opts = options.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (every) {
                if (vm.repo.snap.fieldDefs.any { it.objType == o.type && it.label.equals(name.trim(), true) }) { vm.say("${o.type.plural} already have “${name.trim()}”."); return@Buttons }
                vm.repo.addFieldDef(FieldDef(objType = o.type, label = name.trim(), type = type, options = opts, secret = secret))
            } else vm.repo.putDetail(Detail(objectId = o.id, kind = DetailKind.CUSTOM, label = name.trim(), type = type, flag = secret, value = if (type == FieldType.CHOICE) opts.firstOrNull().orEmpty() else ""), "Added field “${name.trim()}”")
            vm.show(com.jmr.atlas.ui.Overlay.Edit(o.id))
        }
    }
}

@Composable
fun PostUpdateSheet(vm: AtlasViewModel, o: KObject) {
    var text by remember { mutableStateOf("") }
    var health by remember { mutableStateOf(o.health ?: Health.ON_TRACK) }
    Sheet(vm, "post_update_sheet") {
        Text("Post an update", style = MaterialTheme.typography.headlineSmall)
        Text(o.title, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Health.entries.forEach { h -> val (bg, fg) = healthColors(h); FilterChip(health == h, { health = h }, { Text(h.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = bg, selectedLabelColor = fg)) }
        }
        Field(text, { text = it }, "What changed, what's next, what's blocking", Modifier.testTag("update_text"), minLines = 4)
        Buttons(vm, "Post", text.isNotBlank()) { vm.repo.postUpdate(o.id, text, health); vm.show(null); vm.say("Update posted.") }
    }
}

@Composable
fun AddLineSheet(vm: AtlasViewModel, o: KObject, kind: DetailKind) {
    var text by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val (title, hint) = when (kind) {
        DetailKind.REMEMBER -> "Something to remember" to "e.g. Prefers a text before 8 AM"
        DetailKind.STEP -> "Add a step" to "Step"
        DetailKind.HIGHLIGHT -> "Add a highlight" to "Paste the passage"
        else -> "Add" to ""
    }
    Sheet(vm, "add_line") {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(o.title, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Field(text, { text = it }, hint, Modifier.testTag("line_text"), minLines = if (kind == DetailKind.HIGHLIGHT) 3 else 1)
        if (kind == DetailKind.STEP) Field(note, { note = it }, "Detail (optional)")
        Buttons(vm, "Add", text.isNotBlank()) {
            vm.repo.putDetail(Detail(objectId = o.id, kind = kind, value = text.trim(), label = note.trim(), sort = vm.repo.snap.detailsOf(o.id, kind).size), "${kind.label} added")
            vm.show(null)
        }
    }
}

/** Plan vs deadline (Todoist-style), reminder, priority, repeat, blocked — design I08. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDatesSheet(vm: AtlasViewModel, o: KObject) {
    val platform = LocalPlatform.current
    val today = LocalDate.now()
    var due by remember { mutableStateOf(o.due) }
    var deadline by remember { mutableStateOf(o.deadline) }
    var remind by remember { mutableStateOf(o.remindAt) }
    var priority by remember { mutableStateOf(o.priority) }
    var repeat by remember { mutableStateOf(o.repeat) }
    var blocked by remember { mutableStateOf(o.blocked) }
    var pick by remember { mutableStateOf<String?>(null) }
    var timeFor by remember { mutableStateOf<LocalDate?>(null) }
    Sheet(vm, "task_dates") {
        Text("When", style = MaterialTheme.typography.headlineSmall)
        Text("Plan", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp)); Text("The day you mean to do it", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Today" to today, "Tomorrow" to today.plusDays(1), "Next week" to today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY)).forEach { (l, d) -> FilterChip(due == d, { due = d }, { Text(l) }) }
            FilterChip(due != null && due!! !in listOf(today, today.plusDays(1)), { pick = "due" }, { Text(due?.takeIf { it !in listOf(today, today.plusDays(1)) }?.let { dayDate(it) } ?: "Pick a day") })
            FilterChip(due == null, { due = null }, { Text("Someday") })
        }
        Text("Deadline", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp)); Text("When it truly must be done", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip({ pick = "deadline" }, { Text(deadline?.let { dayDate(it) } ?: "Set deadline") }, leadingIcon = { Icon(Icons.Outlined.Flag, null) })
            if (deadline != null) TextButton({ deadline = null }) { Text("Clear") }
        }
        Text("Remind me", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip({ platform.request(listOf(Manifest.permission.POST_NOTIFICATIONS)) { timeFor = due ?: today } }, { Text(remind?.let { remindLabel(it, today) } ?: "Set a time") }, leadingIcon = { Icon(Icons.Outlined.NotificationsNone, null) })
            if (remind != null) TextButton({ remind = null }) { Text("Clear") }
        }
        Text("Shows on your watch too, with Done and Snooze.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        Text("Priority", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Priority.entries.forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.label) }) } }
        Text("Repeat", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Repeat.entries.forEach { r -> FilterChip(repeat == r, { repeat = r }, { Text(r.label) }) } }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("Blocked", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Switch(blocked, { blocked = it }) }
        Buttons(vm, "Done") {
            vm.repo.update(o.copy(due = due, deadline = deadline, remindAt = remind, priority = priority, repeat = repeat, blocked = blocked), "Dates updated")
            vm.show(null)
        }
    }
    pick?.let { which -> DatePick((if (which == "due") due else deadline) ?: today, { pick = null }) { d -> if (which == "due") due = d else deadline = d; pick = null } }
    timeFor?.let { day ->
        val st = rememberTimePickerState(9, 0, false)
        AlertDialog({ timeFor = null }, confirmButton = { TextButton({
            remind = day.atTime(st.hour, st.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().let { if (it < System.currentTimeMillis()) it + 86_400_000L else it }
            timeFor = null }) { Text("Set") } }, dismissButton = { TextButton({ timeFor = null }) { Text("Cancel") } },
            title = { Text("Remind ${if (day == today) "today" else dayDate(day)} at") }, text = { TimePicker(st) })
    }
}

/** Puts a task or milestone on the calendar chosen in Settings, or opens the calendar app prefilled when Atlas can't write. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSheet(vm: AtlasViewModel, o: KObject) {
    val platform = LocalPlatform.current
    var day by remember { mutableStateOf(o.due ?: o.eventDate ?: LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var minutes by remember { mutableIntStateOf(if (o.type == ObjType.EVENT) 0 else 60) }
    var pickDay by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val cals by vm.sync.calendarList.collectAsState()
    val target = cals.firstOrNull { it.id == vm.settings.newEventsCalendar }
    val scope = rememberCoroutineScope()
    Sheet(vm, "block_sheet") {
        Text(if (o.type == ObjType.EVENT) "Add to calendar" else "Block time", style = MaterialTheme.typography.headlineSmall)
        Text(o.title, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip({ pickDay = true }, { Text(dayDate(day)) }, leadingIcon = { Icon(Icons.Outlined.Event, null) })
            if (minutes > 0) AssistChip({ pickTime = true }, { Text(com.jmr.atlas.data.Briefing.time(day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())) }, leadingIcon = { Icon(Icons.Outlined.Schedule, null) })
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30 to "30 min", 60 to "1 h", 120 to "2 h", 0 to "All day").forEach { (m, l) -> FilterChip(minutes == m, { minutes = m }, { Text(l) }) }
        }
        Text(target?.let { "Goes to ${it.name} (${com.jmr.atlas.integrations.Contacts.pretty(it.accountType)})" } ?: "Opens your calendar app to save it", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 10.dp))
        Buttons(vm, "Add") {
            val allDay = minutes == 0
            val start = if (allDay) day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() else day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            platform.request(listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { vm.sync.block(o, start, minutes, allDay) }
                    if (ok) vm.say("Added to ${target?.name ?: "your calendar"}.")
                    else platform.launch(Calendars.insertIntent(o.title, "From Atlas", start, if (allDay) start + 86_400_000L else start + minutes * 60_000L, allDay))
                    vm.show(null)
                }
            }
        }
    }
    if (pickDay) DatePick(day, { pickDay = false }) { day = it; pickDay = false }
    if (pickTime) {
        val st = rememberTimePickerState(time.hour, time.minute, false)
        AlertDialog({ pickTime = false }, confirmButton = { TextButton({ time = LocalTime.of(st.hour, st.minute); pickTime = false }) { Text("Set") } }, text = { TimePicker(st) })
    }
}

/** Search phone contacts and add one to Atlas (or link an existing Atlas person to it). */
@Composable
fun AddContactSheet(vm: AtlasViewModel, linkTo: String?) {
    val platform = LocalPlatform.current
    var q by remember { mutableStateOf("") }
    var granted by remember { mutableStateOf(vm.sync.contacts.canRead()) }
    val scope = rememberCoroutineScope()
    val hits by produceState(emptyList<com.jmr.atlas.integrations.Contacts.Hit>(), q, granted) { value = if (granted) withContext(Dispatchers.IO) { vm.sync.contacts.search(q.ifBlank { "a" }, 20) } else emptyList() }
    val target = linkTo?.let { vm.repo.snap.objects[it] }
    Sheet(vm, "add_contact") {
        Text(if (target != null) "Connect ${target.title} to a phone contact" else "Add from your contacts", style = MaterialTheme.typography.headlineSmall)
        if (!granted) {
            Text("Atlas reads a contact only when you pick it or connect it. Nothing is uploaded.", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(vertical = 12.dp))
            Button({ platform.request(listOf(Manifest.permission.READ_CONTACTS)) { granted = it } }, colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary)) { Text("Allow contacts") }
            return@Sheet
        }
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("contact_search"), placeholder = { Text("Name, phone or email") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
        hits.forEach { h ->
            val already = vm.repo.snap.ofType(ObjType.PERSON).firstOrNull { it.contactKey == h.lookupKey }
            ListItem({ Text(h.name) }, supportingContent = { Text(if (already != null) "Already in Atlas" else h.sub.ifBlank { "Phone contact" }) },
                leadingContent = { Box(Modifier.size(40.dp).clip(CircleShape).background(Atlas.PrimaryContainer), contentAlignment = Alignment.Center) { Text(h.name.split(" ").take(2).joinToString("") { it.take(1) }, color = Atlas.Primary) } },
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                    scope.launch {
                        val id = withContext(Dispatchers.IO) { vm.sync.addContact(h.lookupKey, linkTo ?: already?.id) }
                        vm.show(null)
                        if (id != null) { vm.open(id); vm.say(if (linkTo != null) "Connected to ${h.name}." else "${h.name} added.") } else vm.say("Couldn't read that contact.")
                    }
                })
        }
        if (target == null) TextButton({ vm.show(com.jmr.atlas.ui.Overlay.Create(ObjType.PERSON, title = q)) }, Modifier.padding(top = 8.dp)) { Icon(Icons.Outlined.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("New person in Atlas") }
    }
}

