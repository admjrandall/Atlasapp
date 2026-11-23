package com.jmr.atlas.integrations

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import com.jmr.atlas.model.*
import java.time.LocalDate

/**
 * Two-way link between Atlas people and phone contacts (ContactsContract). Only contacts you add or connect join
 * Atlas; the rest stay searchable and are read on demand. Contact fields (name, work, phones, emails, addresses,
 * websites, dates, nickname) live on the phone contact; Atlas-only fields never leave Atlas.
 * Sources: developer.android.com/reference/android/provider/ContactsContract (CommonDataKinds), checked 2026-11-22.
 */
class Contacts(private val ctx: Context) {
    data class Hit(val lookupKey: String, val name: String, val photo: String, val sub: String)
    data class Account(val type: String, val name: String) { val key get() = "$type|$name"; val label get() = "${pretty(type)} · $name" }
    data class Card(
        val lookupKey: String, val name: String, val nickname: String, val org: String, val title: String, val photo: String,
        val details: List<Detail>, val account: String,
    )

    fun canRead() = granted(Manifest.permission.READ_CONTACTS)
    fun canWrite() = granted(Manifest.permission.WRITE_CONTACTS)
    private fun granted(p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    /** Phone contacts whose name, phone or email matches — for search and "Add person". */
    fun search(q: String, limit: Int = 8): List<Hit> {
        if (!canRead() || q.isBlank()) return emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(q.trim()))
        return runCatching {
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI),
                null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY)?.use { c ->
                buildList { while (c.moveToNext() && size < limit) add(Hit(c.getString(0) ?: continue, c.getString(1).orEmpty(), c.getString(2).orEmpty(), "")) }
            }.orEmpty().map { it.copy(sub = orgLine(it.lookupKey)) }
        }.getOrDefault(emptyList())
    }

    private fun orgLine(key: String): String = runCatching {
        ctx.contentResolver.query(Data.CONTENT_URI, arrayOf(Organization.COMPANY, Organization.TITLE), "${Data.LOOKUP_KEY} = ? AND ${Data.MIMETYPE} = ?",
            arrayOf(key, Organization.CONTENT_ITEM_TYPE), null)?.use { c -> if (c.moveToFirst()) listOfNotNull(c.getString(1), c.getString(0)).filter { it.isNotBlank() }.joinToString(" · ") else "" }.orEmpty()
    }.getOrDefault("")

    /** Accounts that hold contacts (Google, Samsung, …) for "New people save to". */
    fun accounts(): List<Account> {
        if (!canRead()) return emptyList()
        return runCatching {
            ctx.contentResolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME), "${RawContacts.DELETED} = 0", null, null)?.use { c ->
                buildSet { while (c.moveToNext()) { val t = c.getString(0); val n = c.getString(1); if (!t.isNullOrBlank() && !n.isNullOrBlank()) add(Account(t, n)) } }.toList()
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Reads one contact completely. Returns null if it was deleted or permission is missing. */
    fun read(lookupKey: String): Card? {
        if (!canRead()) return null
        val contactUri = ContactsContract.Contacts.lookupContact(ctx.contentResolver, ContactsContract.Contacts.getLookupUri(0, lookupKey)) ?: return null
        val id = ContentUris.parseId(contactUri)
        var name = ""; var photo = ""
        ctx.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.PHOTO_URI, ContactsContract.Contacts.LOOKUP_KEY), null, null, null)?.use { c ->
            if (c.moveToFirst()) { name = c.getString(0).orEmpty(); photo = c.getString(1).orEmpty() }
        }
        var nickname = ""; var org = ""; var title = ""
        val out = mutableListOf<Detail>()
        val res = ctx.resources
        ctx.contentResolver.query(Data.CONTENT_URI, arrayOf(Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA7, Data.DATA8, Data.DATA9),
            "${Data.CONTACT_ID} = ?", arrayOf(id.toString()), null)?.use { c ->
            while (c.moveToNext()) {
                val mime = c.getString(0); val d1 = c.getString(1).orEmpty(); val type = c.getInt(2); val label = c.getString(3)
                fun det(kind: DetailKind, l: String, v: String = d1) { if (v.isNotBlank() && out.none { it.kind == kind && it.value == v }) out += Detail(objectId = "", kind = kind, label = l, value = v, origin = Origin.CONTACTS) }
                when (mime) {
                    Phone.CONTENT_ITEM_TYPE -> det(DetailKind.PHONE, Phone.getTypeLabel(res, type, label).toString())
                    Email.CONTENT_ITEM_TYPE -> det(DetailKind.EMAIL, Email.getTypeLabel(res, type, label).toString())
                    StructuredPostal.CONTENT_ITEM_TYPE -> det(DetailKind.ADDRESS, StructuredPostal.getTypeLabel(res, type, label).toString(), d1.replace('\n', ','))
                    Website.CONTENT_ITEM_TYPE -> { val l = label?.takeIf { it.isNotBlank() } ?: socialLabel(d1) ?: "Website"; det(if (isSocial(l, d1)) DetailKind.SOCIAL else DetailKind.WEBSITE, l) }
                    Event.CONTENT_ITEM_TYPE -> det(DetailKind.DATE, if (type == Event.TYPE_BIRTHDAY) "Birthday" else Event.getTypeLabel(res, type, label).toString())
                    Nickname.CONTENT_ITEM_TYPE -> nickname = d1
                    Organization.CONTENT_ITEM_TYPE -> { org = d1; title = c.getString(4).orEmpty() }
                }
            }
        }
        val account = ctx.contentResolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME), "${RawContacts.CONTACT_ID} = ?", arrayOf(id.toString()), null)?.use { c ->
            if (c.moveToFirst()) listOfNotNull(c.getString(0), c.getString(1)).joinToString("|") else ""
        }.orEmpty()
        return Card(lookupKey, name, nickname, org, title, photo, out, account)
    }

    /** Writes Atlas edits back to the phone contact. Replaces the contact-owned rows of the first writable raw contact. */
    fun write(person: KObject, details: List<Detail>): Boolean {
        if (!canWrite() || person.contactKey.isBlank()) return false
        val contactUri = ContactsContract.Contacts.lookupContact(ctx.contentResolver, ContactsContract.Contacts.getLookupUri(0, person.contactKey)) ?: return false
        val cid = ContentUris.parseId(contactUri)
        val raw = ctx.contentResolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts._ID), "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0", arrayOf(cid.toString()), null)
            ?.use { c -> if (c.moveToFirst()) c.getLong(0) else null } ?: return false
        val ops = ArrayList<ContentProviderOperation>()
        val mimes = listOf(StructuredName.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, StructuredPostal.CONTENT_ITEM_TYPE, Website.CONTENT_ITEM_TYPE,
            Event.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE, Nickname.CONTENT_ITEM_TYPE)
        ops += ContentProviderOperation.newDelete(Data.CONTENT_URI)
            .withSelection("${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} IN (${mimes.joinToString(",") { "?" }})", arrayOf(raw.toString()) + mimes).build()
        rows(person, details).forEach { cv -> ops += ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.RAW_CONTACT_ID, raw).withValues(cv).build() }
        return runCatching { ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops); true }.getOrDefault(false)
    }

    /** Creates a phone contact for an Atlas person in the chosen account (or the phone itself) and returns its lookup key. */
    fun create(person: KObject, details: List<Detail>, accountKey: String): String? {
        if (!canWrite()) return null
        val (type, name) = accountKey.split("|").let { if (it.size == 2) it[0] to it[1] else null to null }
        val ops = ArrayList<ContentProviderOperation>()
        ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, type).withValue(RawContacts.ACCOUNT_NAME, name).build()
        rows(person, details).forEach { cv -> ops += ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(cv).build() }
        return runCatching {
            val r = ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawUri = r.first().uri ?: return null
            val rawId = ContentUris.parseId(rawUri)
            ctx.contentResolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts.CONTACT_ID), "${RawContacts._ID} = ?", arrayOf(rawId.toString()), null)?.use { c ->
                if (c.moveToFirst()) ctx.contentResolver.query(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.getLong(0)), arrayOf(ContactsContract.Contacts.LOOKUP_KEY), null, null, null)
                    ?.use { k -> if (k.moveToFirst()) k.getString(0) else null } else null
            }
        }.getOrNull()
    }

    private fun rows(p: KObject, details: List<Detail>): List<android.content.ContentValues> = buildList {
        fun cv(mime: String, vararg kv: Pair<String, Any?>) = android.content.ContentValues().apply {
            put(Data.MIMETYPE, mime); kv.forEach { (k, v) -> when (v) { null -> putNull(k); is Int -> put(k, v); else -> put(k, v.toString()) } }
        }
        add(cv(StructuredName.CONTENT_ITEM_TYPE, StructuredName.DISPLAY_NAME to p.title))
        if (p.nickname.isNotBlank()) add(cv(Nickname.CONTENT_ITEM_TYPE, Nickname.NAME to p.nickname))
        if (p.org.isNotBlank() || p.role.isNotBlank()) add(cv(Organization.CONTENT_ITEM_TYPE, Organization.COMPANY to p.org, Organization.TITLE to p.role))
        details.forEach { d ->
            val custom = 0 // BaseTypes.TYPE_CUSTOM
            when (d.kind) {
                DetailKind.PHONE -> add(cv(Phone.CONTENT_ITEM_TYPE, Phone.NUMBER to d.value, Phone.TYPE to phoneType(d.label), Phone.LABEL to d.label.takeIf { phoneType(it) == custom }))
                DetailKind.EMAIL -> add(cv(Email.CONTENT_ITEM_TYPE, Email.ADDRESS to d.value, Email.TYPE to emailType(d.label), Email.LABEL to d.label.takeIf { emailType(it) == custom }))
                DetailKind.ADDRESS -> add(cv(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.FORMATTED_ADDRESS to d.value, StructuredPostal.TYPE to postalType(d.label), StructuredPostal.LABEL to d.label.takeIf { postalType(it) == custom }))
                DetailKind.WEBSITE, DetailKind.SOCIAL -> add(cv(Website.CONTENT_ITEM_TYPE, Website.URL to d.value, Website.TYPE to custom, Website.LABEL to d.label))
                DetailKind.DATE -> add(cv(Event.CONTENT_ITEM_TYPE, Event.START_DATE to d.value, Event.TYPE to if (d.label.equals("Birthday", true)) Event.TYPE_BIRTHDAY else custom, Event.LABEL to d.label.takeIf { !it.equals("Birthday", true) }))
                else -> {}
            }
        }
    }

    companion object {
        val PHONE_LABELS = listOf("Mobile", "Work", "Home", "Main", "Other")
        val EMAIL_LABELS = listOf("Work", "Home", "Other")
        val ADDRESS_LABELS = listOf("Work", "Home", "Other")
        val SOCIAL_LABELS = listOf("LinkedIn", "Instagram", "Facebook", "X", "Threads", "Bluesky", "Mastodon", "GitHub", "Website")
        fun phoneType(l: String) = when (l.lowercase()) { "mobile" -> Phone.TYPE_MOBILE; "work" -> Phone.TYPE_WORK; "home" -> Phone.TYPE_HOME; "main" -> Phone.TYPE_MAIN; "other" -> Phone.TYPE_OTHER; else -> 0 }
        fun emailType(l: String) = when (l.lowercase()) { "work" -> Email.TYPE_WORK; "home" -> Email.TYPE_HOME; "other" -> Email.TYPE_OTHER; else -> 0 }
        fun postalType(l: String) = when (l.lowercase()) { "work" -> StructuredPostal.TYPE_WORK; "home" -> StructuredPostal.TYPE_HOME; "other" -> StructuredPostal.TYPE_OTHER; else -> 0 }
        fun socialLabel(url: String): String? { val h = url.lowercase(); return SOCIAL_LABELS.firstOrNull { it != "Website" && h.contains(if (it == "X") "x.com" else it.lowercase()) } }
        fun isSocial(label: String, url: String) = label in SOCIAL_LABELS && label != "Website" || socialLabel(url) != null
        fun pretty(type: String) = when { type.contains("google") -> "Google"; type.contains("samsung") || type.contains("osp") -> "Samsung"; type.contains("exchange") || type.contains("outlook") -> "Exchange"; type.contains("whatsapp") -> "WhatsApp"; else -> type.substringAfterLast('.').replaceFirstChar { it.uppercase() } }

        /** "--03-14" or "1980-03-14" → next occurrence, for "in 16 weeks". */
        fun nextOccurrence(v: String, today: LocalDate = LocalDate.now()): LocalDate? {
            val m = Regex("(?:(\\d{4})|-)-(\\d{2})-(\\d{2})").find(v) ?: return null
            val month = m.groupValues[2].toInt(); val day = m.groupValues[3].toInt()
            return runCatching { LocalDate.of(today.year, month, day).let { if (it.isBefore(today)) it.plusYears(1) else it } }.getOrNull()
        }
    }
}
