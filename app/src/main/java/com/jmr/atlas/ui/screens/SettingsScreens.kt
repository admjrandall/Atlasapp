package com.jmr.atlas.ui.screens

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.MainActivity
import com.jmr.atlas.data.AiMode
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.integrations.Contacts
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.components.plural
import com.jmr.atlas.ui.theme.Atlas
import kotlinx.coroutines.delay

@Composable
fun SettingsGroup(title: String, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Atlas.DeepTeal) }
    }
    Surface(shape = RoundedCornerShape(24.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(4.dp), content = content) }
}

@Composable
fun SRow(ic: ImageVector, t: String, d: String, danger: Boolean = false, tag: String? = null, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 64.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = if (danger) Atlas.Error else Atlas.Primary) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t, style = MaterialTheme.typography.titleSmall, color = if (danger) Atlas.Error else Atlas.DeepPlum)
            if (d.isNotBlank()) Text(d, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
        }
        when { trailing != null -> trailing(); onClick != null -> Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Atlas.SecondaryText) }
    }
}

@Composable
fun SToggle(ic: ImageVector, t: String, d: String, on: Boolean, tag: String? = null, onChange: (Boolean) -> Unit) =
    SRow(ic, t, d, tag = tag, trailing = { Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Atlas.Primary)) }) { onChange(!on) }

/** Settings (design P26): grouped, plain language, each row says what it does. */
@Composable
fun SettingsScreen(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    val v by vm.settings.version.collectAsState()
    val aiStatus by vm.ai.status.collectAsState()
    var confirmWipe by remember { mutableStateOf(false) }
    val s = vm.settings
    val trashed = snap.objects.values.count { it.trashed }
    val examples = snap.objects.values.count { it.origin == Origin.EXAMPLE }
    key(v) {
    Column(Modifier.fillMaxSize().testTag("settings")) {
        PageHeader("Settings", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 80.dp).widthIn(max = 760.dp)) {
            SettingsGroup("Morning briefing") {
                SRow(Icons.Outlined.WbSunny, "Briefing time", if (s.briefingOn) "${hm(s.briefingMinutes)} · ${if (s.briefingWeekdaysOnly) "weekdays" else "every day"}" else "Off", tag = "briefing_time") { vm.show(Overlay.BriefingTime) }
            }
            SettingsGroup("Capture") {
                SToggle(Icons.Outlined.Hub, "Connect automatically", "People and projects you already have", s.autoConnect) { s.autoConnect = it }
                SToggle(Icons.Outlined.ContentPaste, "Offer copied links", "In the capture sheet and widget", s.offerClipboard) { s.offerClipboard = it }
                SRow(Icons.Outlined.Mic, "Voice notes", "Transcribed by your phone's speech recognizer, offline when it can")
            }
            SettingsGroup("Connected to your phone") {
                SRow(Icons.Outlined.Contacts, "Contacts, calendars, internet", listOf(if (s.contactsSync) "Two-way contact sync" else if (vm.sync.contacts.canRead()) "Contacts read-only" else "Contacts off",
                    if (vm.sync.calendars.canRead()) plural(s.calendars.size, "calendar") else "Calendars off", if (s.linkPreviews) "link previews on" else "link previews off").joinToString(" · "), tag = "integrations") { vm.go(Route.Integrations) }
                SRow(Icons.Outlined.AutoAwesome, "AI", when (s.aiMode) { AiMode.ON_DEVICE -> if (aiStatus == Ai.Status.Ready) "On this phone · ready" else "On this phone · model not downloaded"; AiMode.SERVER -> "My server · coming later"; AiMode.OFF -> "Off · Atlas's built-in rules only" }, tag = "ai_settings") { vm.go(Route.AiSettings) }
            }
            SettingsGroup("Security") {
                SRow(Icons.Outlined.Lock, "Lock and encryption", listOf(if (vm.repo.encrypted) "Encrypted" else "Not encrypted (test build)", if (s.lockOn) "fingerprint" else "no lock", if (s.lockOn) "lock after ${s.lockAfterMinutes} min" else null).filterNotNull().joinToString(" · "), tag = "security") { vm.go(Route.Security) }
            }
            SettingsGroup("Your data") {
                SRow(Icons.Outlined.Download, "Export everything", "Readable copy you keep (files listed, not included)") { platform.export(vm.repo.exportJson()) }
                SRow(Icons.Outlined.Delete, "Trash", if (trashed == 0) "Empty" else plural(trashed, "item")) { vm.go(Route.Trash) }
                if (examples > 0) SRow(Icons.Outlined.CleaningServices, "Remove example workspace", plural(examples, "example item")) { vm.show(Overlay.RemoveExample) }
                else SRow(Icons.Outlined.Lightbulb, "Load example workspace", "Sample spaces, people and tasks · remove any time") { vm.loadExample() }
                SRow(Icons.Outlined.DeleteForever, "Erase everything", "All items, connections and files on this phone", danger = true) { confirmWipe = true }
            }
            Text("Atlas 0.5 · No ads, analytics or tracking. Roboto Flex © Google (OFL 1.1). ML Kit, LiteRT-LM © Google. Gemma 4 (Apache 2.0). SQLCipher © Zetetic (BSD).",
                style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 20.dp))
        }
    }
    }
    if (confirmWipe) AlertDialog({ confirmWipe = false }, title = { Text("Erase everything?") }, text = { Text("All items, connections, captures and files are permanently deleted from this phone. Phone contacts and calendar events are not touched.") },
        confirmButton = { TextButton({ vm.repo.wipeAll(); vm.home(); confirmWipe = false; vm.say("Everything erased.") }) { Text("Erase", color = Atlas.Error) } },
        dismissButton = { TextButton({ confirmWipe = false }) { Text("Cancel") } })
}

fun hm(min: Int) = java.time.LocalTime.of(min / 60, min % 60).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

/** Design I13: contacts, calendars of your choice, internet use — each with what it does. */
@Composable
fun IntegrationsScreen(vm: AtlasViewModel, snap: Snapshot) {
    val platform = LocalPlatform.current
    val v by vm.settings.version.collectAsState()
    val cals by vm.sync.calendarList.collectAsState()
    var perm by remember { mutableIntStateOf(0) }
    var accountMenu by remember { mutableStateOf(false) }
    var calMenu by remember { mutableStateOf(false) }
    val s = vm.settings
    val accounts = remember(perm) { vm.sync.contacts.accounts() }
    key(v, perm) {
    Column(Modifier.fillMaxSize().testTag("integrations_screen")) {
        PageHeader("Contacts, calendars, internet", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 80.dp).widthIn(max = 760.dp)) {
            SettingsGroup("Contacts", if (s.contactsSync && vm.sync.contacts.canWrite()) "✓ Two-way sync on" else null) {
                if (!vm.sync.contacts.canRead()) SRow(Icons.Outlined.Contacts, "Allow contacts", "Find phone contacts in search and add them as people. Read only when you pick or connect one.", tag = "allow_contacts") {
                    platform.request(listOf(Manifest.permission.READ_CONTACTS)) { perm++; vm.sync.start() }
                } else {
                    SToggle(Icons.Outlined.People, "Sync with phone contacts", "Edits go both ways for people you connect", s.contactsSync, tag = "contacts_sync") { on ->
                        if (on) platform.request(listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) { ok -> s.contactsSync = ok; perm++ } else s.contactsSync = false
                    }
                    Box {
                        SRow(Icons.Outlined.Download, "New people save to", accounts.firstOrNull { it.key == s.contactsAccount }?.label ?: "This phone only") { accountMenu = true }
                        DropdownMenu(accountMenu, { accountMenu = false }) {
                            DropdownMenuItem({ Text("This phone only") }, { s.contactsAccount = ""; accountMenu = false })
                            accounts.forEach { a -> DropdownMenuItem({ Text(a.label) }, { s.contactsAccount = a.key; accountMenu = false }) }
                        }
                    }
                    SToggle(Icons.Outlined.Hub, "Match contacts in captures", "“Jamie” finds Jamie Carter", s.matchContacts) { s.matchContacts = it }
                }
            }
            SettingsGroup("Calendars") {
                if (!vm.sync.calendars.canRead()) SRow(Icons.Outlined.CalendarMonth, "Allow calendars", "Show today's events on Home and on people's pages; add tasks and milestones to the calendar you choose.", tag = "allow_calendar") {
                    platform.request(listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) { perm++; vm.sync.refreshCalendars() }
                } else {
                    if (cals.isEmpty()) SRow(Icons.Outlined.CalendarMonth, "No calendars found", "Add an account in your phone's Calendar app")
                    cals.forEach { c ->
                        val on = c.id in s.calendars
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { s.calendars = if (on) s.calendars - c.id else s.calendars + c.id; vm.sync.refreshCalendars() }.heightIn(min = 60.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(Color(c.color or 0xFF000000.toInt())))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) { Text(c.name, style = MaterialTheme.typography.titleSmall); Text(Contacts.pretty(c.accountType) + if (c.account.isNotBlank() && c.account != c.name) " · ${c.account}" else "", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
                            Checkbox(on, { s.calendars = if (on) s.calendars - c.id else s.calendars + c.id; vm.sync.refreshCalendars() }, colors = CheckboxDefaults.colors(checkedColor = Atlas.DeepTeal))
                        }
                    }
                    Box {
                        SRow(Icons.Outlined.EditCalendar, "New events go to", cals.firstOrNull { it.id == s.newEventsCalendar }?.name ?: "Ask the calendar app") { calMenu = true }
                        DropdownMenu(calMenu, { calMenu = false }) { cals.filter { it.writable }.forEach { c -> DropdownMenuItem({ Text("${c.name} · ${Contacts.pretty(c.accountType)}") }, { s.newEventsCalendar = c.id; calMenu = false }) } }
                    }
                    SToggle(Icons.Outlined.WbSunny, "Use in briefing and cards", "Meetings, prep, attendees", s.calendarInBriefing) { s.calendarInBriefing = it }
                }
            }
            SettingsGroup("Internet") {
                SToggle(Icons.Outlined.Link, "Link previews", "Downloads the page you saved", s.linkPreviews, tag = "link_previews") { s.linkPreviews = it }
                SToggle(Icons.Outlined.Download, "Saved copies", if (s.copiesWifiOnly) "Wi-Fi only" else "Any connection", s.copiesWifiOnly) { s.copiesWifiOnly = it }
                Text("Atlas never uploads your notes, contacts or files. It downloads only pages you save and, if you ask, the on-device AI model. No ads, no tracking.",
                    style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(16.dp))
            }
            SettingsGroup("Galaxy Watch") {
                SRow(Icons.Outlined.Watch, "Reminders on your watch", "Task reminders and the briefing appear on a paired watch, with Done, Snooze and voice reply. Nothing to install.")
            }
            SettingsGroup("Gmail and other apps") {
                SRow(Icons.Outlined.Share, "Share into Atlas", "In Gmail, Chrome or any app: Share → Atlas. The subject becomes the title; links get a preview.")
            }
        }
    }
    }
}

/** AI choice: all three options are shown; only on-device is built in 0.5. */
@Composable
fun AiSettingsScreen(vm: AtlasViewModel) {
    val v by vm.settings.version.collectAsState()
    val st by vm.ai.status.collectAsState()
    val s = vm.settings
    var confirmDl by remember { mutableStateOf(false) }
    val platform = com.jmr.atlas.LocalPlatform.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(st) { while (st is Ai.Status.Downloading || st is Ai.Status.Verifying) { vm.ai.poll(); delay(1500) } }
    key(v) {
    Column(Modifier.fillMaxSize().testTag("ai_screen")) {
        PageHeader("AI", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 80.dp).widthIn(max = 760.dp)) {
            Text("AI reads your captures and suggests people, projects and tasks. Suggestions always wait for you in To review — the model never changes anything on its own.",
                style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
            SettingsGroup("Where AI runs") {
                AiMode.entries.forEach { m ->
                    val enabled = m != AiMode.SERVER
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(enabled) { s.aiMode = m }.heightIn(min = 68.dp).padding(horizontal = 8.dp, vertical = 8.dp).testTag("ai_${m.name}"),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(s.aiMode == m, { s.aiMode = m }, enabled = enabled)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m.label, style = MaterialTheme.typography.titleSmall, color = if (enabled) Atlas.DeepPlum else Atlas.SecondaryText)
                                if (!enabled) { Spacer(Modifier.width(8.dp)); com.jmr.atlas.ui.components.Pill("Coming later", container = Atlas.SurfaceSecondary, content = Atlas.SecondaryText) }
                            }
                            Text(m.detail, style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText)
                        }
                    }
                }
            }
            if (s.aiMode == AiMode.ON_DEVICE) SettingsGroup("On-device model") {
                when (val x = st) {
                    Ai.Status.NotDownloaded, is Ai.Status.Failed -> {
                        SRow(Icons.Outlined.Download, "Download Gemma 4 E2B", "2.6 GB, once. Free and open (Apache 2.0). Runs offline after that.", tag = "ai_download") { confirmDl = true }
                        SRow(Icons.Outlined.FolderOpen, "Import the model file", "Already have gemma-4-E2B-it.litertlm? Pick it — no internet needed. It's checked before use.", tag = "ai_import") {
                            platform.pickAnyFile { uri -> scope.launch { vm.ai.importModel(uri) } } }
                        if (x is Ai.Status.Failed) Text(x.why, color = Atlas.Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    is Ai.Status.Downloading -> Column(Modifier.padding(16.dp)) {
                        Text("Downloading… ${x.done / 1_048_576} of ${x.total / 1_048_576} MB", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator({ (x.done.toFloat() / x.total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = Atlas.Primary)
                        TextButton({ vm.ai.cancelDownload() }) { Text("Cancel") }
                    }
                    Ai.Status.Verifying -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Checking the file against its published checksum…") }
                    Ai.Status.Ready -> {
                        SRow(Icons.Outlined.CheckCircle, "Ready", "Gemma 4 E2B · verified · ${if (vm.ai.backendUsed.isNotBlank()) "runs on ${vm.ai.backendUsed}" else "loads when first used"}")
                        SRow(Icons.Outlined.DeleteOutline, "Remove the model", "Frees 2.6 GB. Atlas falls back to its rules.", danger = true) { vm.ai.deleteModel() }
                    }
                }
                Text("Your Galaxy Z Fold 6 isn't on Google's list for its built-in Gemini Nano, so Atlas brings its own model. Expect a few seconds per capture and some battery use.",
                    style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, modifier = Modifier.padding(16.dp))
            }
        }
    }
    }
    if (confirmDl) AlertDialog({ confirmDl = false }, title = { Text("Download 2.6 GB?") },
        text = { Text("Atlas downloads Gemma 4 E2B from the LiteRT Community on Hugging Face (Google's published build), pinned to a known version and checked before use. Nothing about you is sent. Wi-Fi recommended.") },
        confirmButton = { TextButton({ vm.ai.startDownload(wifiOnly = true); confirmDl = false }, Modifier.testTag("dl_wifi")) { Text("Download on Wi-Fi") } },
        dismissButton = { TextButton({ vm.ai.startDownload(wifiOnly = false); confirmDl = false }) { Text("Any connection") } })
}

@Composable
fun SecurityScreen(vm: AtlasViewModel) {
    val platform = LocalPlatform.current
    val v by vm.settings.version.collectAsState()
    val s = vm.settings
    var lockMenu by remember { mutableStateOf(false) }
    val activity = LocalPlatform.current as? MainActivity
    key(v) {
    Column(Modifier.fillMaxSize().testTag("security_screen")) {
        PageHeader("Lock and encryption", vm)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 80.dp).widthIn(max = 760.dp)) {
            SettingsGroup("Encryption", if (vm.repo.encrypted) "✓ On" else null) {
                SRow(Icons.Outlined.Shield, "Notes, people, tasks", if (vm.repo.encrypted) "Database encrypted with SQLCipher (AES-256). Key wrapped by the Android Keystore." else "Not encrypted in this test build")
                SRow(Icons.Outlined.Lock, "Photos, scans, documents", "Each file AES-256-GCM, key in the Android Keystore")
                SRow(Icons.Outlined.CloudOff, "Not in Android backup", "Use Export to keep your own copy")
            }
            SettingsGroup("Lock") {
                SToggle(Icons.Outlined.Fingerprint, "Unlock with fingerprint", if (platform.canAuthenticate()) "Fingerprint, face or your screen lock when Atlas opens" else "Set up a screen lock on this phone first", s.lockOn, tag = "lock_toggle") { on ->
                    if (!on) { s.lockOn = false; activity?.applyRecents() }
                    else if (platform.canAuthenticate()) platform.authenticate { s.lockOn = true; activity?.applyRecents() }
                    else vm.say("Set up a fingerprint or screen lock in your phone's settings first.")
                }
                if (s.lockOn) Box {
                    SRow(Icons.Outlined.Timer, "Lock after", if (s.lockAfterMinutes == 0) "Immediately" else "${s.lockAfterMinutes} min in the background") { lockMenu = true }
                    DropdownMenu(lockMenu, { lockMenu = false }) { listOf(0, 1, 5, 15).forEach { m -> DropdownMenuItem({ Text(if (m == 0) "Immediately" else "$m min") }, { s.lockAfterMinutes = m; lockMenu = false }) } }
                }
                if (s.lockOn) SRow(Icons.Outlined.VisibilityOff, "Hidden in recent apps", "The app switcher shows a blank card")
            }
        }
    }
    }
}
