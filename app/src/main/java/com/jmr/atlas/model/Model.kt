package com.jmr.atlas.model

import java.time.LocalDate
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Every knowledge object type. One durable identity per object; relationships connect them. */
enum class ObjType(val label: String, val plural: String) {
    PERSON("Person", "People"),
    PROJECT("Project", "Projects"),
    TASK("Task", "Tasks"),
    IDEA("Idea", "Ideas"),
    NOTE("Note", "Notes"),
    DOCUMENT("Document", "Documents"),
    IMAGE("Image", "Images"),
    LINK("Link", "Links"),
    COLLECTION("Collection", "Collections"),
    EVENT("Event", "Events"),
    DECISION("Decision", "Decisions");

    val isFile get() = this == DOCUMENT || this == IMAGE || this == LINK
}

enum class ProjectStatus(val label: String) { ACTIVE("Active"), PLANNING("Planning"), ON_HOLD("On hold"), COMPLETED("Completed"), ARCHIVED("Archived") }
enum class Priority(val label: String) { NONE("None"), LOW("Low"), MEDIUM("Medium"), HIGH("High") }

/** How an object or relationship came to exist. Shown to the user; never hidden. */
enum class Origin(val label: String) {
    USER("Added by you"), EXTRACTED("Extracted on this phone"), IMPORTED("Imported"), AI("Suggested by on-device AI"), EXAMPLE("Example data"),
    AUTO("Connected automatically"), CONTACTS("From your phone contacts"), CALENDAR("From your calendar"), WEB("From the web page"),
}

/** Project health, posted with an update (Linear-style). */
enum class Health(val label: String) { ON_TRACK("On track"), AT_RISK("At risk"), OFF_TRACK("Off track") }
enum class Repeat(val label: String) { NONE("Doesn't repeat"), DAILY("Every day"), WEEKDAYS("Every weekday"), WEEKLY("Every week"), MONTHLY("Every month"), YEARLY("Every year") }

/** Repeated values that belong to one item: contact fields, custom fields, key facts, checklists, updates, highlights. */
enum class DetailKind(val label: String, val toContact: Boolean = false) {
    PHONE("Phone", true), EMAIL("Email", true), ADDRESS("Address", true), WEBSITE("Website", true), SOCIAL("Social", true), DATE("Date", true),
    CUSTOM("Field"), FACT("Key fact"), REMEMBER("Thing to remember"), STEP("Step"), UPDATE("Update"), HIGHLIGHT("Highlight"),
    /** A band's name on a project board: objectId = project, label = band key, value = name; origin AI or USER. */
    BAND("Band name"),
}

/** The ten custom field types (design I06). SECRET is a flag, not a type: any text-like value can be masked. */
enum class FieldType(val label: String) {
    TEXT("Text"), NUMBER("Number"), MONEY("Money"), DATE("Date"), CHOICE("Choice"), CHECKBOX("Checkbox"), LINK("Link"), PERSON("Person"), ITEM("Item"), RATING("Rating"),
}

data class Detail(
    val id: String = newId(),
    val objectId: String,
    val kind: DetailKind,
    val label: String = "",
    val value: String = "",
    val type: FieldType = FieldType.TEXT,
    val sort: Int = 0,
    /** STEP: done. CUSTOM/FACT: secret (masked until “Show”). */
    val flag: Boolean = false,
    /** STEP: completed date; UPDATE: Health name; FACT: “page 2”; CUSTOM: field definition id. */
    val extra: String = "",
    val created: Long = System.currentTimeMillis(),
    val origin: Origin = Origin.USER,
)

/** A custom field added to every item of a type (“Every project”). Values live in Detail(kind = CUSTOM). */
data class FieldDef(val id: String = newId(), val objType: ObjType, val label: String, val type: FieldType, val options: List<String> = emptyList(), val secret: Boolean = false, val sort: Int = 0)

/** Relationship review state. PROPOSED edges are drawn dashed and are never treated as facts. */
enum class Review { CONFIRMED, PROPOSED, REJECTED }

enum class RelType(val label: String, val inverse: String) {
    RELATED_TO("Related to", "Related to"),
    BELONGS_TO("Belongs to", "Contains"),
    REFERENCES("References", "Referenced by"),
    DISCUSSED_WITH("Discussed with", "Discussed in"),
    SUPPORTS("Supports", "Supported by"),
    DEPENDS_ON("Depends on", "Blocks"),
    ASSIGNED_TO("Assigned to", "Responsible for"),
    DERIVED_FROM("Derived from", "Source of"),
    PART_OF("Part of", "Includes"),
    FOLLOW_UP_TO("Follow-up to", "Followed up by"),
    DUPLICATE_OF("Duplicate of", "Duplicate of"),
}

data class KObject(
    val id: String = newId(),
    val type: ObjType,
    val title: String,
    val body: String = "",
    val created: Long = System.currentTimeMillis(),
    val updated: Long = created,
    val trashed: Boolean = false,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val origin: Origin = Origin.USER,
    // person
    val role: String = "",
    val org: String = "",
    val email: String = "",
    val phone: String = "",
    // project
    val projectStatus: ProjectStatus? = null,
    val startDate: LocalDate? = null,
    val targetDate: LocalDate? = null,
    // task
    val due: LocalDate? = null,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val priority: Priority = Priority.NONE,
    val blocked: Boolean = false,
    // files & links
    val mime: String = "",
    val vaultFile: String = "",
    val url: String = "",
    val sizeBytes: Long = 0,
    val extracted: String = "",
    // event / milestone
    val eventDate: LocalDate? = null,
    // 0.5: per-type details
    /** content:// photo from the phone contact, or "vault:<file>" for a photo chosen in Atlas (encrypted). */
    val photo: String = "",
    /** ContactsContract lookup key when this person is linked to a phone contact. */
    val contactKey: String = "",
    val nickname: String = "",
    /** Document kind (Estimate, Invoice…) or link kind (Reference, Article…). */
    val kind: String = "",
    /** The date printed on a document, as opposed to when it was added (Paperless-style). */
    val docDate: LocalDate? = null,
    /** Hard deadline, separate from the day you plan to do it (`due`) — Todoist-style. */
    val deadline: LocalDate? = null,
    val remindAt: Long? = null,
    val repeat: Repeat = Repeat.NONE,
    val health: Health? = null,
    val summary: String = "",
    val site: String = "",
    val author: String = "",
    val published: LocalDate? = null,
    val readMinutes: Int = 0,
    val isRead: Boolean = false,
    /** Encrypted preview image for links (vault file). */
    val coverFile: String = "",
    val previewAt: Long? = null,
    val calendarEventId: Long? = null,
) {
    fun searchText() = listOf(title, body, extracted, role, org, email, url, nickname, kind, summary, site, author).filter { it.isNotBlank() }.joinToString("\n")
    val firstName get() = title.trim().substringBefore(' ')
    val initials get() = title.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

data class Relation(
    val id: String = newId(),
    val sourceId: String,
    val targetId: String,
    val type: RelType = RelType.RELATED_TO,
    val origin: Origin = Origin.USER,
    val review: Review = Review.CONFIRMED,
    val evidence: String = "",
    val created: Long = System.currentTimeMillis(),
) {
    fun other(id: String) = if (sourceId == id) targetId else sourceId
    fun label(from: String) = if (sourceId == from) type.label else type.inverse
}

enum class CaptureStatus(val label: String) {
    SAVED("Saved"), READY_FOR_REVIEW("Ready for review"), CONNECTED("Connected"), NOTHING_FOUND("Saved · nothing to connect"), FAILED("Processing failed")
}

/** The original input, kept verbatim. Processing never replaces it. */
data class Capture(
    val id: String = newId(),
    val text: String,
    val noteId: String?,
    val attachmentId: String? = null,
    val created: Long = System.currentTimeMillis(),
    val status: CaptureStatus = CaptureStatus.SAVED,
)

enum class SuggestionKind(val label: String) { NEW_PERSON("New person"), LINK_EXISTING("Connect"), NEW_TASK("New task"), NEW_EVENT("Possible milestone") }
enum class SuggestionStatus { PENDING, ACCEPTED, REJECTED }

/** A proposed change. Unreviewed suggestions are not facts. */
data class Suggestion(
    val id: String = newId(),
    val captureId: String,
    val kind: SuggestionKind,
    val objType: ObjType? = null,
    val title: String,
    val targetId: String? = null,
    val relType: RelType = RelType.RELATED_TO,
    val evidence: String,
    val due: LocalDate? = null,
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    /** For tasks/events: other suggestions (person/project) this one should also connect to when accepted. */
    val linkTo: List<String> = emptyList(),
)

data class Activity(
    val id: String = newId(),
    val objectId: String,
    val at: Long = System.currentTimeMillis(),
    val kind: String,
    val text: String,
)

/** Immutable snapshot of the whole personal universe, fed to the UI. */
data class Snapshot(
    val objects: Map<String, KObject> = emptyMap(),
    val relations: List<Relation> = emptyList(),
    val captures: List<Capture> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val activity: List<Activity> = emptyList(),
    val details: List<Detail> = emptyList(),
    val fieldDefs: List<FieldDef> = emptyList(),
) {
    private val detailsBy: Map<String, List<Detail>> by lazy { details.groupBy { it.objectId }.mapValues { e -> e.value.sortedWith(compareBy({ it.sort }, { it.created })) } }
    fun detailsOf(id: String, kind: DetailKind? = null) = detailsBy[id].orEmpty().filter { kind == null || it.kind == kind }
    val live: List<KObject> by lazy { objects.values.filter { !it.trashed } }
    private val byObj: Map<String, List<Relation>> by lazy {
        val m = HashMap<String, MutableList<Relation>>()
        relations.filter { it.review != Review.REJECTED }.forEach { r -> m.getOrPut(r.sourceId) { mutableListOf() }.add(r); m.getOrPut(r.targetId) { mutableListOf() }.add(r) }
        m
    }
    fun relationsOf(id: String): List<Relation> = byObj[id].orEmpty().filter { objects[it.other(id)]?.trashed == false }
    fun neighbours(id: String, includeProposed: Boolean = true): List<Pair<KObject, Relation>> =
        relationsOf(id).filter { includeProposed || it.review == Review.CONFIRMED }.mapNotNull { r -> objects[r.other(id)]?.let { it to r } }
    fun ofType(t: ObjType) = live.filter { it.type == t }
    fun pending() = suggestions.filter { it.status == SuggestionStatus.PENDING }
    fun activityOf(id: String) = activity.filter { it.objectId == id }.sortedByDescending { it.at }
}
