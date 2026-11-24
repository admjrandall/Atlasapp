package com.jmr.atlas.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the on-device language model runs. SERVER is shown but not built yet (0.5 builds on-device only). */
enum class AiMode(val label: String, val detail: String) {
    ON_DEVICE("On this phone", "Gemma 4 E2B runs offline. Nothing leaves the phone."),
    SERVER("My server", "Your own Ollama server over Tailscale. Coming in a later version."),
    OFF("Off", "Atlas uses only its built-in rules."),
}

/** User preferences. Plain SharedPreferences (no secrets live here). Observable so screens update when toggled. */
class Settings(private val p: SharedPreferences) {
    private val _version = MutableStateFlow(0)
    val version = _version.asStateFlow()
    private fun bump() { _version.value++ }

    private fun b(k: String, def: Boolean) = p.getBoolean(k, def)
    private fun setB(k: String, v: Boolean) { p.edit().putBoolean(k, v).apply(); bump() }

    /** Spaces shown in the Focus view instead of the 0.7 board (0.7.3). Board stays the default; this is per space. */
    var focusSpaces: Set<String> get() = p.getStringSet("focus_spaces", emptySet())!!.toSet(); set(v) { p.edit().putStringSet("focus_spaces", v).apply(); bump() }

    var autoConnect: Boolean get() = b("auto_connect", true); set(v) = setB("auto_connect", v)
    var offerClipboard: Boolean get() = b("offer_clip", true); set(v) = setB("offer_clip", v)
    var briefingOn: Boolean get() = b("brief_on", true); set(v) = setB("brief_on", v)
    var briefingMinutes: Int get() = p.getInt("brief_min", 7 * 60 + 30); set(v) { p.edit().putInt("brief_min", v).apply(); bump() }
    var briefingWeekdaysOnly: Boolean get() = b("brief_wd", true); set(v) = setB("brief_wd", v)
    var linkPreviews: Boolean get() = b("link_prev", true); set(v) = setB("link_prev", v)
    var copiesWifiOnly: Boolean get() = b("copy_wifi", true); set(v) = setB("copy_wifi", v)
    var contactsSync: Boolean get() = b("contacts_sync", false); set(v) = setB("contacts_sync", v)
    var matchContacts: Boolean get() = b("match_contacts", true); set(v) = setB("match_contacts", v)
    /** "type|name" of the account new people are saved to; empty = phone only (no account). */
    var contactsAccount: String get() = p.getString("contacts_account", "").orEmpty(); set(v) { p.edit().putString("contacts_account", v).apply(); bump() }
    var calendars: Set<Long> get() = p.getStringSet("calendars", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet(); set(v) { p.edit().putStringSet("calendars", v.map { it.toString() }.toSet()).apply(); bump() }
    var calendarsChosen: Boolean get() = b("calendars_chosen", false); set(v) = setB("calendars_chosen", v)
    var newEventsCalendar: Long get() = p.getLong("new_event_cal", -1); set(v) { p.edit().putLong("new_event_cal", v).apply(); bump() }
    var calendarInBriefing: Boolean get() = b("cal_brief", true); set(v) = setB("cal_brief", v)
    var lockOn: Boolean get() = b("lock_on", false); set(v) = setB("lock_on", v)
    var lockAfterMinutes: Int get() = p.getInt("lock_after", 1); set(v) { p.edit().putInt("lock_after", v).apply(); bump() }
    var aiMode: AiMode get() = AiMode.entries.firstOrNull { it.name == p.getString("ai_mode", null) } ?: AiMode.OFF; set(v) { p.edit().putString("ai_mode", v.name).apply(); bump() }
    var serverUrl: String get() = p.getString("server_url", "").orEmpty(); set(v) { p.edit().putString("server_url", v).apply(); bump() }
}
