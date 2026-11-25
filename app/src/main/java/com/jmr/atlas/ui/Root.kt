package com.jmr.atlas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.data.Board
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.objects.Overlays
import com.jmr.atlas.ui.screens.*
import com.jmr.atlas.ui.six.*
import com.jmr.atlas.ui.theme.Atlas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * App frame (design 0.6). Three places — Home, a Space, an item's full page — and one input box at the bottom of
 * all three. No tab bar or rail; Settings sits quietly at the top right of Home. Wide layouts (Fold inner screen,
 * tablets) get two-column screens from the same code.
 */
@Composable
fun AtlasRoot(vm: AtlasViewModel, locked: MutableStateFlow<Boolean>) {
    val snap by vm.snap.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val stack by vm.stack.collectAsState()
    val msg by vm.messages.collectAsState()
    val isLocked by locked.collectAsState()
    val catchOpen by vm.catchOpen.collectAsState()
    val sel by vm.boardSel.collectAsState()
    var toast by remember { mutableStateOf<AtlasViewModel.Msg?>(null) }
    // Copy the message out, then clear it — clearing changes msg, so the wait can't live in this effect (it would be cancelled).
    LaunchedEffect(msg) { msg?.let { m -> toast = m; vm.messages.value = null } }
    // The wait lives here, keyed on the toast itself: a newer toast restarts it, nothing else cancels it.
    LaunchedEffect(toast) { val t = toast ?: return@LaunchedEffect; delay(if (t.undo != null) 5000 else 3200); if (toast === t) toast = null }

    CompositionLocalProvider(LocalFiles provides vm.repo.files) {
        Box(Modifier.fillMaxSize().background(Atlas.Background)) {
            when {
                isLocked -> LockScreen { locked.value = false }
                !onboarded -> Onboarding(vm)
                else -> {
                    val route = stack.last()
                    BackHandler(enabled = stack.size > 1 || catchOpen || sel != null) { vm.back() }
                    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                        val wide = maxWidth >= 600.dp
                        CompositionLocalProvider(LocalWide provides wide) {
                            Box(Modifier.fillMaxSize()) {
                                AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route", modifier = Modifier.fillMaxSize()) { r -> RouteContent(vm, snap, r) }
                                val journeyCover by vm.journeyCover.collectAsState()
                                val inputHere = route == Route.Home || route is Route.Space || route is Route.Item || (route is Route.Journey && !journeyCover)
                                val selObj = if (route is Route.Space) sel?.let { snap.objects[it] } else null
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = pad).padding(bottom = 14.dp).widthIn(max = 760.dp)) {
                                    when {
                                        selObj != null -> SelectionBar(vm, selObj, remember(snap, selObj.id, route) {
                                            val onBoard = (route as Route.Space).space.takeIf { it.startsWith("p:") }?.let { k -> Board.members(snap, k.removePrefix("p:")).map { it.id }.toSet() }
                                            snap.neighbours(selObj.id, false).count { (onBoard == null || it.first.id in onBoard) && it.first.type != ObjType.PROJECT }
                                        })
                                        inputHere && !catchOpen -> CatchBar(vm)
                                    }
                                }
                                if (catchOpen && inputHere) CatchSheet(vm, snap)
                            }
                        }
                    }
                    Overlays(vm, snap)
                }
            }
            AnimatedVisibility(toast != null, Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 10.dp, start = 16.dp, end = 16.dp),
                enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
                toast?.let { t -> Toast(t.text, t.undo?.let { u -> { u(); toast = null } }, onDismiss = { toast = null }) }
            }
        }
    }
}

@Composable
private fun RouteContent(vm: AtlasViewModel, snap: Snapshot, r: Route) {
    val narrowPage: @Composable (@Composable () -> Unit) -> Unit = { content -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { Box(Modifier.widthIn(max = 820.dp)) { content() } } }
    when (r) {
        Route.Home -> HomeScreen(vm, snap)
        is Route.Space -> if (r.space == Board.SPACES_KEY) com.jmr.atlas.ui.six.AllSpacesScreen(vm, snap) else SpaceScreen(vm, snap, r.space)
        is Route.Item -> ItemScreen(vm, snap, r.id)
        is Route.Journey -> com.jmr.atlas.ui.journey.JourneyScreen(vm, snap, r.space)
        is Route.JourneyStart -> com.jmr.atlas.ui.journey.StartScreen(vm, snap, r.space)
        Route.Review -> narrowPage { ReviewScreen(vm, snap) }
        Route.Settings -> narrowPage { SettingsScreen(vm, snap) }
        Route.Integrations -> narrowPage { IntegrationsScreen(vm, snap) }
        Route.AiSettings -> narrowPage { AiSettingsScreen(vm) }
        Route.Security -> narrowPage { SecurityScreen(vm) }
        Route.Trash -> narrowPage { TrashScreen(vm, snap) }
        is Route.History -> narrowPage { HistoryScreen(vm, snap, r.id) }
    }
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val platform = LocalPlatform.current
    LaunchedEffect(Unit) { platform.authenticate(onUnlocked) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("lock_screen"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AtlasMark(72.dp); Spacer(Modifier.height(18.dp))
        Text("Atlas is locked", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Button({ platform.authenticate(onUnlocked) }, colors = ButtonDefaults.buttonColors(containerColor = Atlas.Primary), modifier = Modifier.heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Unlock")
        }
    }
}
