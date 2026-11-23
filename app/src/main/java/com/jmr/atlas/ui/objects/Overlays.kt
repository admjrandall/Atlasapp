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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun Overlays(vm: AtlasViewModel, snap: Snapshot) {
    val ov by vm.overlay.collectAsState()
    fun obj(id: String, f: @Composable (KObject) -> Unit): @Composable () -> Unit = { snap.objects[id]?.let { f(it) } ?: vm.show(null) }
    when (val o = ov) {
        null -> {}
        is Overlay.Edit -> obj(o.id) { com.jmr.atlas.ui.cards.EditorScreen(vm, snap, it, null, null) }()
        is Overlay.Create -> com.jmr.atlas.ui.cards.EditorScreen(vm, snap, null, o.type, o.linkTo, o.title)
        is Overlay.Connect -> obj(o.id) { ConnectDialog(vm, snap, it, o.types) }()
        is Overlay.RenameBand -> RenameBandDialog(vm, o)
        is Overlay.AddField -> obj(o.id) { com.jmr.atlas.ui.cards.AddFieldSheet(vm, it) }()
        is Overlay.PostUpdate -> obj(o.id) { com.jmr.atlas.ui.cards.PostUpdateSheet(vm, it) }()
        is Overlay.AddLine -> obj(o.id) { com.jmr.atlas.ui.cards.AddLineSheet(vm, it, o.kind) }()
        is Overlay.TaskDates -> obj(o.id) { com.jmr.atlas.ui.cards.TaskDatesSheet(vm, it) }()
        is Overlay.Block -> obj(o.id) { com.jmr.atlas.ui.cards.BlockSheet(vm, it) }()
        is Overlay.AddContact -> com.jmr.atlas.ui.cards.AddContactSheet(vm, o.linkTo)
        Overlay.BriefingTime -> BriefingTimeDialog(vm)
        Overlay.RemoveExample -> AlertDialog({ vm.show(null) }, title = { Text("Remove the example workspace?") },
            text = { Text("Deletes the ${snap.objects.values.count { it.origin == Origin.EXAMPLE }} example items. Anything you added stays.") },
            confirmButton = { TextButton({ vm.repo.clearExample(); vm.home(); vm.show(null); vm.say("Example removed.") }) { Text("Remove") } },
            dismissButton = { TextButton({ vm.show(null) }) { Text("Keep") } })
        is Overlay.ConfirmPurge -> obj(o.id) { item ->
            AlertDialog({ vm.show(null) }, title = { Text("Delete “${item.title}” forever?") },
                text = { Text("Its connections and activity are removed${if (item.vaultFile.isNotBlank()) ", and the encrypted original is erased" else ""}. This can't be undone.") },
                confirmButton = { TextButton({ vm.repo.purge(item.id); vm.show(null); vm.say("Deleted.") }) { Text("Delete forever", color = Atlas.Error) } },
                dismissButton = { TextButton({ vm.show(null) }) { Text("Cancel") } })
        }()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingTimeDialog(vm: AtlasViewModel) {
    val s = vm.settings
    val st = rememberTimePickerState(s.briefingMinutes / 60, s.briefingMinutes % 60, false)
    var on by remember { mutableStateOf(s.briefingOn) }
    var weekdays by remember { mutableStateOf(s.briefingWeekdaysOnly) }
    AlertDialog({ vm.show(null) }, title = { Text("Morning briefing") }, text = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Briefing", Modifier.weight(1f)); Switch(on, { on = it }) }
            if (on) { TimePicker(st); Row(verticalAlignment = Alignment.CenterVertically) { Text("Weekends too", Modifier.weight(1f)); Switch(!weekdays, { weekdays = !it }) } }
        }
    }, confirmButton = { TextButton({ s.briefingOn = on; s.briefingMinutes = st.hour * 60 + st.minute; s.briefingWeekdaysOnly = weekdays; vm.show(null) }) { Text("Save") } },
        dismissButton = { TextButton({ vm.show(null) }) { Text("Cancel") } })
}

/** Rename a band on a project board. Clearing the name goes back to Atlas's name. */
@Composable
private fun RenameBandDialog(vm: AtlasViewModel, o: Overlay.RenameBand) {
    var name by remember { mutableStateOf(o.current) }
    AlertDialog({ vm.show(null) }, title = { Text("Rename band") }, text = {
        Column {
            OutlinedTextField(name, { name = it.take(40) }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().testTag("band_rename"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Spacer(Modifier.height(6.dp))
            Text("Leave it empty to let Atlas name it again.", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
    }, confirmButton = { TextButton({ vm.renameBand(o.projectId, o.key, name); vm.show(null) }) { Text("Save") } },
        dismissButton = { TextButton({ vm.show(null) }) { Text("Cancel") } })
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
fun Modifier.clickableRow(onClick: () -> Unit) = this.then(Modifier.clickable(onClick = onClick))
