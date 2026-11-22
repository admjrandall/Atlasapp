package com.jmr.atlas.ui.universe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.theme.Accent
import com.jmr.atlas.ui.theme.accent
import java.time.LocalDate

/** The six neighbourhoods around a focal object. Order matters: it is the reading order for the list alternative. */
enum class SlotKind(val label: String, val icon: ImageVector, val types: Set<ObjType>) {
    PEOPLE("People", Icons.Outlined.Groups, setOf(ObjType.PERSON)),
    DOCUMENTS("Documents", Icons.Outlined.FolderOpen, setOf(ObjType.DOCUMENT, ObjType.IMAGE, ObjType.LINK, ObjType.NOTE)),
    TASKS("Tasks", Icons.Outlined.TaskAlt, setOf(ObjType.TASK)),
    IDEAS("Ideas", Icons.Outlined.Lightbulb, setOf(ObjType.IDEA, ObjType.DECISION)),
    PROJECTS("Projects", Icons.Outlined.Layers, setOf(ObjType.PROJECT, ObjType.COLLECTION)),
    TIMELINE("Timeline", Icons.Outlined.CalendarMonth, setOf(ObjType.EVENT));

    val accent: Accent get() = when (this) {
        PEOPLE -> accent(ObjType.PERSON); DOCUMENTS -> accent(ObjType.DOCUMENT); TASKS -> accent(ObjType.TASK)
        IDEAS -> accent(ObjType.IDEA); PROJECTS -> accent(ObjType.COLLECTION); TIMELINE -> accent(ObjType.EVENT)
    }
    /** The type a new object gets when created from this slot. */
    val createType get() = when (this) { PEOPLE -> ObjType.PERSON; DOCUMENTS -> ObjType.NOTE; TASKS -> ObjType.TASK; IDEAS -> ObjType.IDEA; PROJECTS -> ObjType.PROJECT; TIMELINE -> ObjType.EVENT }
}

data class SlotItem(val obj: KObject, val proposed: Boolean, val relId: String?)
data class Slot(val kind: SlotKind, val items: List<SlotItem>) {
    val total get() = items.size
    fun label(focus: KObject?) = if (kind == SlotKind.PROJECTS && focus?.type == ObjType.PROJECT) "Related Projects" else kind.label
}

/** Builds the neighbourhoods. With no focus, the whole universe is summarized (most relevant first). */
fun slotsFor(snap: Snapshot, focusId: String?, today: LocalDate = LocalDate.now()): List<Slot> {
    val items: List<SlotItem> = if (focusId == null || snap.objects[focusId]?.trashed != false) {
        snap.live.map { SlotItem(it, false, null) }
    } else {
        snap.neighbours(focusId, includeProposed = true).map { (o, r) -> SlotItem(o, r.review == Review.PROPOSED, r.id) }.distinctBy { it.obj.id }
    }
    return SlotKind.entries.map { k ->
        val l = items.filter { it.obj.type in k.types }
        val sorted = when (k) {
            SlotKind.TASKS -> l.sortedWith(compareBy<SlotItem> { it.obj.done }.thenBy { it.obj.due ?: LocalDate.MAX }.thenByDescending { it.obj.updated })
            SlotKind.TIMELINE -> l.sortedBy { it.obj.eventDate ?: LocalDate.MAX }
            SlotKind.PROJECTS -> l.sortedWith(compareBy<SlotItem> { it.obj.projectStatus == ProjectStatus.ARCHIVED || it.obj.projectStatus == ProjectStatus.COMPLETED }.thenByDescending { it.obj.pinned }.thenByDescending { it.obj.updated })
            else -> l.sortedWith(compareByDescending<SlotItem> { it.obj.pinned }.thenByDescending { it.obj.updated })
        }
        Slot(k, sorted)
    }
}

/** Milestones to show on a short timeline: the latest past ones and the next upcoming ones. */
fun timelineWindow(items: List<SlotItem>, today: LocalDate = LocalDate.now(), n: Int = 4): List<SlotItem> {
    val dated = items.filter { it.obj.eventDate != null }
    val past = dated.filter { it.obj.eventDate!! < today }.takeLast(2)
    val next = dated.filter { it.obj.eventDate!! >= today }.take(n - past.size)
    return (past + next).take(n).ifEmpty { items.take(n) }
}
