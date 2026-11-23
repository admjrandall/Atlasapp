package com.jmr.atlas.integrations

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars as Cal
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * The phone's calendar provider: every account Samsung Calendar or Google Calendar syncs (Google, Samsung, Exchange)
 * appears here without a separate sign-in. Atlas reads only the calendars you choose and writes only to the one you pick.
 * Source: developer.android.com/identity/providers/calendar-provider, checked 2026-11-22.
 */
class Calendars(private val ctx: Context) {
    data class CalendarInfo(val id: Long, val name: String, val account: String, val accountType: String, val color: Int, val writable: Boolean)
    data class Event(val instanceId: Long, val eventId: Long, val calendarId: Long, val title: String, val location: String, val begin: Long, val end: Long, val allDay: Boolean,
                     val attendees: List<String> = emptyList()) {
        val day: LocalDate get() = if (allDay) Instant.ofEpochMilli(begin).atZone(ZoneId.of("UTC")).toLocalDate() else Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun canRead() = ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    fun canWrite() = ctx.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun list(): List<CalendarInfo> {
        if (!canRead()) return emptyList()
        return runCatching {
            ctx.contentResolver.query(Cal.CONTENT_URI, arrayOf(Cal._ID, Cal.CALENDAR_DISPLAY_NAME, Cal.ACCOUNT_NAME, Cal.ACCOUNT_TYPE, Cal.CALENDAR_COLOR, Cal.CALENDAR_ACCESS_LEVEL),
                "${Cal.VISIBLE} = 1", null, "${Cal.ACCOUNT_NAME}, ${Cal.CALENDAR_DISPLAY_NAME}")?.use { c ->
                buildList { while (c.moveToNext()) add(CalendarInfo(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getString(3).orEmpty(), c.getInt(4), c.getInt(5) >= Cal.CAL_ACCESS_CONTRIBUTOR)) }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Event instances (recurrences expanded) in the chosen calendars between two days. */
    fun instances(calendarIds: Set<Long>, from: LocalDate, days: Int): List<Event> {
        if (!canRead() || calendarIds.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val start = from.atStartOfDay(zone).toInstant().toEpochMilli(); val end = from.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = Instances.CONTENT_URI.buildUpon().also { ContentUris.appendId(it, start); ContentUris.appendId(it, end) }.build()
        val sel = "${Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
        val events = runCatching {
            ctx.contentResolver.query(uri, arrayOf(Instances._ID, Instances.EVENT_ID, Instances.CALENDAR_ID, Instances.TITLE, Instances.EVENT_LOCATION, Instances.BEGIN, Instances.END, Instances.ALL_DAY),
                sel, null, "${Instances.BEGIN} ASC")?.use { c ->
                buildList { while (c.moveToNext()) add(Event(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3).orEmpty().ifBlank { "(No title)" }, c.getString(4).orEmpty(), c.getLong(5), c.getLong(6), c.getInt(7) == 1)) }
            }.orEmpty()
        }.getOrDefault(emptyList())
        if (events.isEmpty()) return events
        val att = HashMap<Long, MutableList<String>>()
        runCatching {
            ctx.contentResolver.query(CalendarContract.Attendees.CONTENT_URI, arrayOf(CalendarContract.Attendees.EVENT_ID, CalendarContract.Attendees.ATTENDEE_EMAIL),
                "${CalendarContract.Attendees.EVENT_ID} IN (${events.map { it.eventId }.distinct().joinToString(",")})", null, null)?.use { c ->
                while (c.moveToNext()) c.getString(1)?.takeIf { it.isNotBlank() }?.let { att.getOrPut(c.getLong(0)) { mutableListOf() }.add(it.lowercase()) }
            }
        }
        return events.map { it.copy(attendees = att[it.eventId].orEmpty()) }
    }

    /** Inserts an event directly (needs WRITE_CALENDAR). Returns the event id, or null. */
    fun insert(calendarId: Long, title: String, description: String, begin: Long, end: Long, allDay: Boolean): Long? {
        if (!canWrite() || calendarId < 0) return null
        val v = ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId); put(Events.TITLE, title); put(Events.DESCRIPTION, description)
            put(Events.DTSTART, begin); put(Events.DTEND, end); put(Events.ALL_DAY, if (allDay) 1 else 0)
            put(Events.EVENT_TIMEZONE, if (allDay) "UTC" else TimeZone.getDefault().id)
        }
        return runCatching { ctx.contentResolver.insert(Events.CONTENT_URI, v)?.let { ContentUris.parseId(it) } }.getOrNull()
    }

    companion object {
        /** No-permission fallback: opens the calendar app's own "new event" screen, prefilled. */
        fun insertIntent(title: String, description: String, begin: Long, end: Long, allDay: Boolean) = Intent(Intent.ACTION_INSERT).setData(Events.CONTENT_URI)
            .putExtra(Events.TITLE, title).putExtra(Events.DESCRIPTION, description).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end).putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
        fun viewIntent(eventId: Long) = Intent(Intent.ACTION_VIEW).setData(ContentUris.withAppendedId(Events.CONTENT_URI, eventId))
    }
}
