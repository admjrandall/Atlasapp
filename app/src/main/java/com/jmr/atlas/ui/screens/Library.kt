package com.jmr.atlas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.theme.Atlas
import java.time.LocalDate

private val TABS: List<Pair<String, Set<ObjType>?>> = listOf(
    "All" to null, "People" to setOf(ObjType.PERSON), "Projects" to setOf(ObjType.PROJECT), "Tasks" to setOf(ObjType.TASK),
    "Documents" to setOf(ObjType.DOCUMENT, ObjType.IMAGE), "Notes" to setOf(ObjType.NOTE), "Ideas" to setOf(ObjType.IDEA, ObjType.DECISION),
    "Links" to setOf(ObjType.LINK), "Milestones" to setOf(ObjType.EVENT), "Collections" to setOf(ObjType.COLLECTION),
)

/** Library (design P21–P23): every item, filtered by kind; tasks get Todoist-style filters. Only non-empty tabs are shown. */
@Composable
fun LibraryScreen(vm: AtlasViewModel, snap: Snapshot, initial: ObjType?, selected: String? = null, onSelect: ((String) -> Unit)? = null) {
    val today = LocalDate.now()
    var tab by rememberSaveable(initial) { mutableIntStateOf(TABS.indexOfFirst { initial != null && it.second?.contains(initial) == true }.coerceAtLeast(0)) }
    var taskFilter by rememberSaveable { mutableIntStateOf(0) }
    val tabs = TABS.filter { (_, t) -> t == null || snap.live.any { it.type in t } }
    val (label, types) = tabs.getOrElse(tab) { tabs.first() }
    val taskFilters: List<Pair<String, (KObject) -> Boolean>> = listOf(
        "Open" to { o -> !o.done }, "Overdue" to { o -> !o.done && listOfNotNull(o.due, o.deadline).any { it < today } },
        "Today" to { o -> !o.done && (o.due == today || o.deadline == today) }, "Upcoming" to { o -> !o.done && (o.due ?: o.deadline)?.isAfter(today) == true },
        "Waiting" to { o -> !o.done && o.title.startsWith("Expect ", true) }, "Blocked" to { o -> !o.done && o.blocked }, "Done" to { o -> o.done })
    val base = snap.live.filter { types == null || it.type in types }
    val list = base.filter { types != setOf(ObjType.TASK) || taskFilters[taskFilter].second(it) }.let { l ->
        when { types == setOf(ObjType.TASK) -> l.sortedWith(compareBy<KObject> { it.done }.thenBy { it.due ?: it.deadline ?: LocalDate.MAX })
            types == setOf(ObjType.PERSON) -> l.sortedBy { it.title.lowercase() }
            else -> l.sortedWith(compareByDescending<KObject> { it.pinned }.thenByDescending { it.updated }) }
    }
    val click: (KObject) -> Unit = { o -> if (onSelect != null) onSelect(o.id) else vm.open(o.id) }
    Box(Modifier.fillMaxSize().testTag("library")) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Library", vm, showBack = false) {
                IconButton({ vm.go(Route.Timeline) }, Modifier.semantics { contentDescription = "Timeline" }) { Icon(Icons.Outlined.CalendarMonth, null) }
                IconButton({ vm.go(Route.Trash) }, Modifier.semantics { contentDescription = "Trash" }) { Icon(Icons.Outlined.Delete, null) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, (l, _) -> FilterChip(tab == i, { tab = i }, { Text(l) }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp).testTag("tab_$l"),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Atlas.PrimaryContainer)) }
            }
            if (types == setOf(ObjType.TASK)) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                taskFilters.forEachIndexed { i, (l, f) -> FilterChip(taskFilter == i, { taskFilter = i }, { Text("$l ${base.count(f)}") }, modifier = Modifier.testTag("filter_$l")) }
            }
            Text(if (label == "All") "Recently changed" else "${list.size} ${label.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            if (list.isEmpty()) Empty("Nothing here", "Add one, or capture a note that mentions it.")
            else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(list.take(400), key = { it.id }) { o ->
                    val n = snap.relationsOf(o.id).size
                    val sub = typeLine(o, snap) + if (n > 0) " · " + plural(n, "connection") else ""
                    ObjRow(o, snap, Modifier.background(if (o.id == selected) Atlas.PrimaryContainer.copy(alpha = .6f) else Color.Transparent, RoundedCornerShape(16.dp)),
                        sub = sub, onToggle = { vm.repo.toggleDone(o.id) }) { click(o) }
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
        ExtendedFloatingActionButton({ val t = types?.first(); vm.show(if (t == ObjType.PERSON) Overlay.AddContact() else Overlay.Create(t)) },
            Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(20.dp).testTag("new_fab"), containerColor = Atlas.Primary, contentColor = Color.White, shape = CircleShape) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("New")
        }
    }
}

/** “All connections” and “History” pages behind the collapsed rows on every card. */
@Composable
fun ConnectionsScreen(vm: AtlasViewModel, snap: Snapshot, id: String) {
    val o = snap.objects[id] ?: return
    val nb = snap.neighbours(id)
    Column(Modifier.fillMaxSize().testTag("connections")) {
        PageHeader("Connections", vm) { IconButton({ vm.show(Overlay.Connect(id)) }, Modifier.semantics { contentDescription = "Connect" }) { Icon(Icons.Outlined.AddLink, null) } }
        Text(o.title, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            nb.groupBy { it.first.type }.forEach { (t, l) ->
                item(key = "h$t") { SectionLabel("${t.plural} · ${l.size}", Modifier.padding(horizontal = 8.dp)) }
                items(l, key = { it.second.id }) { (n, r) ->
                    ObjRow(n, snap, sub = r.label(id) + (if (r.origin == Origin.AUTO) " · automatic" else "") + (if (r.evidence.isNotBlank()) " · “${r.evidence.take(50)}”" else ""),
                        proposed = r.review == Review.PROPOSED, onToggle = { vm.repo.toggleDone(n.id) }, trailing = {
                            IconButton({ vm.repo.unlink(r.id); vm.say("Disconnected ${n.title}.") }, Modifier.semantics { contentDescription = "Disconnect ${n.title}" }) { Icon(Icons.Outlined.LinkOff, null, tint = Atlas.SecondaryText) }
                        }) { vm.open(n.id) }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: AtlasViewModel, snap: Snapshot, id: String) {
    val o = snap.objects[id] ?: return
    val updates = snap.detailsOf(id, DetailKind.UPDATE).sortedByDescending { it.created }
    Column(Modifier.fillMaxSize().testTag("history")) {
        PageHeader("History", vm)
        Text(o.title, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            if (updates.isNotEmpty()) {
                item { SectionLabel("Updates") }
                items(updates, key = { it.id }) { u ->
                    com.jmr.atlas.ui.cards.Quote(u.value, "${Health.entries.firstOrNull { it.name == u.extra }?.label ?: ""} · ${ago(u.created)}", Modifier.padding(bottom = 8.dp))
                }
                item { SectionLabel("Activity") }
            }
            items(snap.activityOf(id), key = { it.id }) { a ->
                ListItem({ Text(a.text) }, supportingContent = { Text(ago(a.at)) }, leadingContent = { Icon(when (a.kind) { "created" -> Icons.Outlined.AddCircleOutline; "linked" -> Icons.Outlined.Link; "unlinked" -> Icons.Outlined.LinkOff; "trashed" -> Icons.Outlined.Delete; else -> Icons.Outlined.Edit }, null, tint = Atlas.Primary) })
            }
        }
    }
}
