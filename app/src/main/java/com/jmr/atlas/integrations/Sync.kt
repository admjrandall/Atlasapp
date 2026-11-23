package com.jmr.atlas.integrations

import android.content.Context
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.jmr.atlas.data.Repository
import com.jmr.atlas.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Glue between the repository and the phone: contact sync both ways, the chosen calendars, and link previews.
 * Phone contact wins for contact-owned fields when it changes on the phone; Atlas writes back when you edit in Atlas.
 */
class Sync(private val ctx: Context, private val repo: Repository, val contacts: Contacts, val calendars: Calendars, private val scope: CoroutineScope) {
    private val _events = MutableStateFlow<List<Calendars.Event>>(emptyList())
    /** Next 14 days from the calendars you chose. */
    val events = _events.asStateFlow()
    private val _calendarList = MutableStateFlow<List<Calendars.CalendarInfo>>(emptyList())
    val calendarList = _calendarList.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var contactsObs: ContentObserver? = null
    private var calObs: ContentObserver? = null

    fun start() {
        refreshCalendars(); pullContacts()
        if (contactsObs == null && contacts.canRead()) contactsObs = object : ContentObserver(main) { override fun onChange(self: Boolean) = debounce("contacts") { pullContacts() } }
            .also { runCatching { ctx.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, it) } }
        if (calObs == null && calendars.canRead()) calObs = object : ContentObserver(main) { override fun onChange(self: Boolean) = debounce("cal") { refreshCalendars() } }
            .also { runCatching { ctx.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, it) } }
    }

    private val jobs = HashMap<String, Job>()
    private fun debounce(key: String, block: () -> Unit) { jobs[key]?.cancel(); jobs[key] = scope.launch(Dispatchers.IO) { delay(1500); block() } }

    // ---------------------------------------------------------------- calendars
    fun refreshCalendars() = scope.launch(Dispatchers.IO) {
        val list = calendars.list(); _calendarList.value = list
        val s = repo.settings
        if (!s.calendarsChosen && list.isNotEmpty()) {           // first time: everything visible except holidays/birthdays feeds
            s.calendars = list.filterNot { it.accountType.contains("holiday", true) || it.name.contains("holiday", true) || it.name.contains("birthday", true) }.map { it.id }.toSet()
            list.firstOrNull { it.writable && it.accountType.contains("google") }?.let { s.newEventsCalendar = it.id } ?: list.firstOrNull { it.writable }?.let { s.newEventsCalendar = it.id }
            s.calendarsChosen = true
        }
        _events.value = calendars.instances(s.calendars, LocalDate.now(), 14)
    }

    /** Calendar events that involve this person (attendee email or name in the title) or mention this project. */
    fun eventsFor(o: KObject, snap: Snapshot): List<Calendars.Event> {
        val ev = _events.value
        return when (o.type) {
            ObjType.PERSON -> {
                val emails = snap.detailsOf(o.id, DetailKind.EMAIL).map { it.value.lowercase() }.toSet() + listOf(o.email.lowercase()).filter { it.isNotBlank() }
                val uniqueFirst = snap.ofType(ObjType.PERSON).count { it.firstName.equals(o.firstName, true) } == 1 && o.firstName.length >= 3
                ev.filter { e -> e.attendees.any { it in emails } || e.title.contains(o.title, true) || (uniqueFirst && Regex("\\b${Regex.escape(o.firstName)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(e.title)) }
            }
            ObjType.PROJECT -> ev.filter { it.title.contains(o.title, true) || (o.calendarEventId != null && it.eventId == o.calendarEventId) }
            ObjType.TASK, ObjType.EVENT -> ev.filter { o.calendarEventId != null && it.eventId == o.calendarEventId }
            else -> emptyList()
        }
    }

    /** Adds a calendar block for a task or milestone. Returns true if written directly; false means the caller should open the calendar app. */
    fun block(o: KObject, start: Long, minutes: Int, allDay: Boolean): Boolean {
        val cal = repo.settings.newEventsCalendar
        val end = if (allDay) start + 86_400_000L else start + minutes * 60_000L
        val id = calendars.insert(cal, o.title, "From Atlas", start, end, allDay) ?: return false
        repo.update(o.copy(calendarEventId = id), "Added to calendar")
        refreshCalendars()
        return true
    }

    // ---------------------------------------------------------------- contacts
    /** Adds (or connects) a phone contact as an Atlas person. Returns the person id. */
    fun addContact(lookupKey: String, existingPersonId: String? = null): String? {
        val card = contacts.read(lookupKey) ?: return null
        val existing = existingPersonId?.let { repo.snap.objects[it] } ?: repo.snap.ofType(ObjType.PERSON).firstOrNull { it.contactKey == lookupKey }
        val p = (existing ?: KObject(type = ObjType.PERSON, title = card.name, origin = Origin.CONTACTS)).copy(
            title = card.name.ifBlank { existing?.title ?: "Contact" }, nickname = card.nickname, org = card.org, role = card.title, photo = card.photo.ifBlank { existing?.photo.orEmpty() }, contactKey = lookupKey,
            email = card.details.firstOrNull { it.kind == DetailKind.EMAIL }?.value.orEmpty(), phone = card.details.firstOrNull { it.kind == DetailKind.PHONE }?.value.orEmpty(),
        )
        val id = if (existing == null) repo.create(p) else { repo.update(p, "Connected to phone contact"); p.id }
        repo.replaceDetails(id, CONTACT_KINDS, card.details)
        return id
    }

    /** Re-reads every linked contact; phone wins for contact-owned fields. Deleted contacts are unlinked, never deleted. */
    fun pullContacts() {
        if (!contacts.canRead()) return
        repo.snap.ofType(ObjType.PERSON).filter { it.contactKey.isNotBlank() }.forEach { p ->
            val card = contacts.read(p.contactKey)
            if (card == null) { repo.update(p.copy(contactKey = ""), "Phone contact was removed — kept in Atlas"); return@forEach }
            val changed = card.name != p.title || card.org != p.org || card.title != p.role || card.nickname != p.nickname || (card.photo != p.photo && !p.photo.startsWith("vault:")) ||
                repo.snap.detailsOf(p.id).filter { it.kind in CONTACT_KINDS }.map { it.kind to it.label + it.value }.toSet() != card.details.map { it.kind to it.label + it.value }.toSet()
            if (changed) {
                repo.update(p.copy(title = card.name.ifBlank { p.title }, org = card.org, role = card.title, nickname = card.nickname, photo = card.photo.ifBlank { if (p.photo.startsWith("vault:")) p.photo else "" },
                    email = card.details.firstOrNull { it.kind == DetailKind.EMAIL }?.value.orEmpty(), phone = card.details.firstOrNull { it.kind == DetailKind.PHONE }?.value.orEmpty()), "Updated from phone contact")
                repo.replaceDetails(p.id, CONTACT_KINDS, card.details)
            }
        }
    }

    /** After an Atlas edit: write contact-owned fields back to the phone, or create the contact if sync is on and it has none. */
    fun pushPerson(id: String) {
        val p = repo.snap.objects[id] ?: return
        if (!repo.settings.contactsSync || !contacts.canWrite()) return
        val details = repo.snap.detailsOf(id).filter { it.kind in CONTACT_KINDS }
        if (p.contactKey.isNotBlank()) contacts.write(p, details)
        else contacts.create(p, details, repo.settings.contactsAccount)?.let { key -> repo.update(p.copy(contactKey = key), "Saved to phone contacts") }
    }

    // ---------------------------------------------------------------- links
    fun unmetered(): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Fetches the preview (and, when allowed, a readable copy) for a link object. Safe to call repeatedly. */
    fun preview(id: String) = scope.launch(Dispatchers.IO) {
        val o = repo.snap.objects[id] ?: return@launch
        if (!repo.settings.linkPreviews || o.url.isBlank()) return@launch
        val keep = !repo.settings.copiesWifiOnly || unmetered()
        runCatching { Web.fetch(o.url, keep) }.onSuccess { pg ->
            val cover = pg.image?.let { runCatching { repo.files.store(it) }.getOrNull() }
            val cur = repo.snap.objects[id] ?: return@onSuccess
            if (cover != null && cur.coverFile.isNotBlank()) repo.files.delete(cur.coverFile)
            repo.update(cur.copy(
                title = if (cur.title == cur.url || cur.title.isBlank() || cur.title == Web.host(cur.url)) pg.title.ifBlank { cur.title } else cur.title,
                summary = cur.summary.ifBlank { pg.description }, site = pg.site, author = pg.author, published = pg.published,
                readMinutes = pg.readMinutes, extracted = pg.text.ifBlank { cur.extracted }, coverFile = cover ?: cur.coverFile, previewAt = System.currentTimeMillis(),
            ), if (pg.text.isNotBlank()) "Preview and readable copy saved" else "Preview saved")
        }.onFailure { repo.update(o.copy(previewAt = System.currentTimeMillis()), "Preview failed: ${it.message?.take(80)}") }
    }

    companion object {
        val CONTACT_KINDS = setOf(DetailKind.PHONE, DetailKind.EMAIL, DetailKind.ADDRESS, DetailKind.WEBSITE, DetailKind.SOCIAL, DetailKind.DATE)
    }
}
