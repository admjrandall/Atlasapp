package com.jmr.atlas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.*
import com.jmr.atlas.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.icon
import java.time.LocalDate

@Composable
fun PageHeader(title: String, vm: AtlasViewModel, showBack: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton({ vm.back() }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } else Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

// ================================================================ capture (design C09: a sheet over whatever you were doing)
@Composable
fun SuggestionCard(vm: AtlasViewModel, snap: Snapshot, s: Suggestion) {
    val existing = s.targetId?.let { snap.objects[it] }
    val (badge, title, action) = when (s.kind) {
        SuggestionKind.LINK_EXISTING -> Triple("Connect to ${(existing?.type ?: s.objType)?.label?.lowercase() ?: "item"}", s.title, "Connect")
        SuggestionKind.NEW_PERSON -> Triple("New person", s.title, "Add")
        SuggestionKind.NEW_TASK -> Triple("New task", s.title, "Add")
        SuggestionKind.NEW_EVENT -> Triple("Possible milestone", s.title, "Add")
    }
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("suggestion")) {
        Pill(badge, s.objType?.let { icon(it) }, Atlas.PrimaryContainer, Atlas.DeepPlum)
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        s.due?.let { Text("${if (s.kind == SuggestionKind.NEW_EVENT) "Around" else "Due"} ${dayDate(it)}", style = MaterialTheme.typography.bodySmall, color = Atlas.DeepTeal) }
        Text(s.evidence, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton({ vm.repo.rejectSuggestion(s.id) }, Modifier.testTag("reject")) { Text("Dismiss") }
            FilledTonalButton({ vm.repo.accept(s.id) }, Modifier.testTag("accept")) { Text(action) }
        }
    }
}

// ================================================================ review inbox
@Composable
fun ReviewScreen(vm: AtlasViewModel, snap: Snapshot) {
    val byCap = snap.pending().groupBy { it.captureId }
    Column(Modifier.fillMaxSize().testTag("review")) {
        PageHeader("Review", vm)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), modifier = Modifier.widthIn(max = 800.dp)) {
            if (byCap.isEmpty()) item { Empty("All caught up", "What Atlas finds in your captures waits here until you add or dismiss it.") }
            byCap.forEach { (capId, sugs) ->
                val cap = snap.captures.firstOrNull { it.id == capId }
                item(key = capId) {
                    val src = cap?.let { (it.noteId ?: it.attachmentId)?.let { id -> snap.objects[id] } }
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
                        Text("“" + (cap?.text?.ifBlank { src?.title } ?: "").take(200) + "”", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cap?.let { ago(it.created) } ?: "", style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.weight(1f))
                            TextButton({ vm.repo.rejectAll(capId) }) { Text("Dismiss all") }; TextButton({ vm.repo.acceptAll(capId) }) { Text("Add all") }
                        }
                    }
                }
                items(sugs, key = { it.id }) { SuggestionCard(vm, snap, it) }
            }
        }
    }
}

@Composable
fun TrashScreen(vm: AtlasViewModel, snap: Snapshot) {
    val l = snap.objects.values.filter { it.trashed }.sortedByDescending { it.updated }
    Column(Modifier.fillMaxSize().testTag("trash")) {
        PageHeader("Trash", vm) { if (l.isNotEmpty()) TextButton({ vm.repo.emptyTrash(); vm.say("Trash emptied.") }) { Text("Empty trash", color = Atlas.Error) } }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            if (l.isEmpty()) item { Empty("Trash is empty", "Deleted items stay here until you empty it.") }
            items(l, key = { it.id }) { o ->
                ObjRow(o, snap, trailing = {
                    TextButton({ vm.repo.restore(o.id) }) { Text("Restore") }
                    IconButton({ vm.show(Overlay.ConfirmPurge(o.id)) }, Modifier.semantics { contentDescription = "Delete ${o.title} forever" }) { Icon(Icons.Outlined.DeleteForever, null, tint = Atlas.Error) }
                }) { vm.open(o.id) }
            }
        }
    }
}

