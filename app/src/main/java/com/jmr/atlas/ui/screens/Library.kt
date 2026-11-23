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

/** An item's history: every change, newest first. */
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
