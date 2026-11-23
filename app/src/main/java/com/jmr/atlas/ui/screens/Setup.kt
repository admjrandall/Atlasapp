package com.jmr.atlas.ui.screens

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.MainActivity
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.components.AtlasMark
import com.jmr.atlas.ui.theme.Atlas

/** First run (design P01–P05): welcome → locked to this phone → briefing → your phone → start. Every step can be skipped. */
@Composable
fun Onboarding(vm: AtlasViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val platform = LocalPlatform.current
    val s = vm.settings
    val v by s.version.collectAsState()
    var perm by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Atlas.Background).windowInsetsPadding(WindowInsets.safeDrawing).testTag("onboarding"), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxHeight().padding(horizontal = 24.dp)) {
            if (step > 0) Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ step-- }, Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                Spacer(Modifier.weight(1f))
                (1..4).forEach { i -> Box(Modifier.padding(horizontal = 3.dp).size(width = 26.dp, height = 6.dp).clip(CircleShape).background(if (i <= step) Atlas.Primary else Atlas.Border)) }
                Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
            }
            key(v, perm) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (step) {
                    0 -> Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AtlasMark(96.dp); Spacer(Modifier.height(20.dp))
                        Text("ATLAS", style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 10.sp))
                        Spacer(Modifier.height(16.dp))
                        Text("Everything you know, connected — on this phone.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        Point(Icons.Outlined.Add, "Capture in one tap from anywhere — voice, text, photos, links, files.")
                        Point(Icons.Outlined.Hub, "Atlas connects people, projects and documents for you.")
                        Point(Icons.Outlined.WbSunny, "A short briefing every morning, built from your own notes.")
                    }
                    1 -> {
                        H("Your Atlas is locked to this phone", "Everything you capture is encrypted here. There's no account and no cloud copy.")
                        Row3(Icons.Outlined.Shield, "Encrypted", "Notes, people, documents and photos — always on.") { Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal) }
                        Row3(Icons.Outlined.Fingerprint, "Unlock with fingerprint", if (platform.canAuthenticate()) "Ask for your fingerprint when Atlas opens." else "Set up a screen lock on this phone to use this.") {
                            Switch(s.lockOn, { on -> if (!on) s.lockOn = false else if (platform.canAuthenticate()) platform.authenticate { s.lockOn = true; (platform as? MainActivity)?.applyRecents() } },
                                enabled = platform.canAuthenticate(), modifier = Modifier.testTag("setup_lock"), colors = SwitchDefaults.colors(checkedTrackColor = Atlas.Primary))
                        }
                        Row3(Icons.Outlined.CloudOff, "Nothing is uploaded", "Atlas downloads only pages you save and, if you ask, its AI model.") { Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal) }
                    }
                    2 -> {
                        H("When should Atlas brief you?", "A short summary of what needs you today, from your own notes, tasks and calendar.")
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(390, 420, 450, 480).forEach { m -> val on = s.briefingMinutes == m && s.briefingOn
                                Surface(onClick = { s.briefingMinutes = m; s.briefingOn = true }, shape = CircleShape, color = if (on) Atlas.PrimaryContainer else Atlas.SurfaceSecondary, border = if (on) BorderStroke(2.dp, Atlas.Primary) else null) {
                                    Text(hm(m).removeSuffix(" AM"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) } }
                            Surface(onClick = { s.briefingOn = false }, shape = CircleShape, color = if (!s.briefingOn) Atlas.PrimaryContainer else Atlas.SurfaceSecondary) { Text("No briefing", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) }
                        }
                        Text("Preview", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                        Surface(shape = RoundedCornerShape(24.dp), color = Atlas.Surface, border = BorderStroke(1.dp, Atlas.Border), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { AtlasMark(18.dp); Spacer(Modifier.width(8.dp)); Text("Atlas · ${hm(s.briefingMinutes)}", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText) }
                                Text("Good morning", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                                Text("1 overdue, 2 due today. Waiting on Jamie's estimate (Fri).", style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText)
                            }
                        }
                        Row3(Icons.Outlined.CalendarMonth, "Weekends too", if (s.briefingWeekdaysOnly) "Off: briefings Monday to Friday." else "On: every day.") {
                            Switch(!s.briefingWeekdaysOnly, { s.briefingWeekdaysOnly = !it }, colors = SwitchDefaults.colors(checkedTrackColor = Atlas.Primary))
                        }
                    }
                    3 -> {
                        H("Connect your phone", "Optional. Atlas works with the contacts and calendars already on your phone — no extra sign-ins.")
                        Row3(Icons.Outlined.Contacts, "Contacts", if (vm.sync.contacts.canRead()) "Allowed. Phone contacts show up in search." else "Find people you already know; add them with their photo and details.") {
                            if (vm.sync.contacts.canRead()) Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal)
                            else FilledTonalButton({ platform.request(listOf(Manifest.permission.READ_CONTACTS)) { perm++ } }, Modifier.testTag("setup_contacts")) { Text("Allow") }
                        }
                        Row3(Icons.Outlined.CalendarMonth, "Calendars", if (vm.sync.calendars.canRead()) "Allowed. Choose which calendars later in Settings." else "Today's meetings in your briefing and on people's cards.") {
                            if (vm.sync.calendars.canRead()) Icon(Icons.Outlined.Check, null, tint = Atlas.DeepTeal)
                            else FilledTonalButton({ platform.request(listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) { perm++ } }, Modifier.testTag("setup_calendar")) { Text("Allow") }
                        }
                        Row3(Icons.Outlined.Widgets, "Quick capture widget", "Long-press your home screen → Widgets → Atlas. One tap for text, voice, scan or a copied link.") {}
                    }
                    else -> {
                        H("Start with an example?", "See how Atlas connects a home renovation — people, quotes, tasks and a timeline. Remove it any time.")
                        Spacer(Modifier.height(12.dp))
                        Button({ vm.finishOnboarding(true) }, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("start_example"), colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary)) { Text("Explore the example") }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton({ vm.finishOnboarding(false) }, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("start_empty")) { Text("Start empty") }
                    }
                }
            }
            }
            if (step < 4) {
                Button({
                    if (step == 2 && s.briefingOn) platform.request(listOf(Manifest.permission.POST_NOTIFICATIONS)) { step++ } else step++
                }, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("setup_next"), colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary)) {
                    Text(when (step) { 0 -> "Get started"; 2 -> if (s.briefingOn) "Allow notifications" else "Continue"; else -> "Continue" }, style = MaterialTheme.typography.titleSmall)
                }
                if (step == 0) TextButton({ vm.finishOnboarding(false) }, Modifier.align(Alignment.CenterHorizontally).testTag("skip_setup")) { Text("Skip setup") }
                Text(if (step == 0) "No account. Your notes never leave this phone." else " ", style = MaterialTheme.typography.bodySmall, color = Atlas.SecondaryText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun H(t: String, d: String) {
    Text(t, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 24.dp))
    Text(d, style = MaterialTheme.typography.bodyLarge, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
}

@Composable
private fun Row3(ic: ImageVector, t: String, d: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.Primary) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { Text(t, style = MaterialTheme.typography.titleSmall); Text(d, style = MaterialTheme.typography.bodyMedium, color = Atlas.SecondaryText) }
        Spacer(Modifier.width(8.dp)); trailing()
    }
}

@Composable
private fun Point(ic: ImageVector, t: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Atlas.SurfaceSecondary), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Atlas.Primary) }
        Spacer(Modifier.width(14.dp)); Text(t, style = MaterialTheme.typography.bodyLarge)
    }
}
