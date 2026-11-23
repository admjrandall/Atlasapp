package com.jmr.atlas

import android.app.Application
import com.jmr.atlas.data.Repository
import com.jmr.atlas.integrations.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AtlasApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** Encrypted with SQLCipher on devices. Robolectric (unit tests) has no Keystore or native SQLCipher, so tests use plain SQLite. */
    val repo: Repository by lazy { Repository(this, "atlas.db", encrypted = android.os.Build.FINGERPRINT != "robolectric") }
    val contacts by lazy { Contacts(this) }
    val calendars by lazy { Calendars(this) }
    val sync by lazy { Sync(this, repo, contacts, calendars, scope) }
    val ai by lazy { Ai(this) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        repo.files.cleanOpenCopies()   // decrypted copies opened in other apps never outlive a session
        Notify.channels(this)
        Notify.scheduleBriefing(this)
        // Reminders follow the data: whenever a task's reminder time changes, alarms are rescheduled.
        scope.launch {
            repo.state.map { s -> s.objects.values.filter { it.remindAt != null && !it.done && !it.trashed }.map { it.id to it.remindAt }.toSet() }
                .distinctUntilChanged().debounce(800).collect { Notify.scheduleReminders(this@AtlasApp) }
        }
        scope.launch { repo.settings.version.debounce(500).collect { Notify.scheduleBriefing(this@AtlasApp) } }
        scope.launch { ai.poll() }
    }
}
