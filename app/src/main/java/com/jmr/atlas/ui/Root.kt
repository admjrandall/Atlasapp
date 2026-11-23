package com.jmr.atlas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.objects.*
import com.jmr.atlas.ui.screens.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.universe.MapActions
import com.jmr.atlas.ui.universe.MapMenu
import com.jmr.atlas.ui.universe.MapView
import com.jmr.atlas.ui.universe.UniverseMap
import kotlinx.coroutines.flow.MutableStateFlow

private data class Dest(val route: Route, val label: String, val icon: ImageVector)

/** Navigation (design 0.5): Now · Universe · Capture · Search · Library — same on the phone bar and the Fold rail. */
private val DESTS = listOf(
    Dest(Route.Now, "Now", Icons.Outlined.WbSunny),
    Dest(Route.Universe, "Universe", Icons.Outlined.Hub),
    Dest(Route.Search, "Search", Icons.Outlined.Search),
    Dest(Route.Library(), "Library", Icons.Outlined.LibraryBooks),
)
private fun Route.dest() = when (this) { is Route.Library -> Route.Library(); else -> this }

@Composable
fun AtlasRoot(vm: AtlasViewModel, locked: MutableStateFlow<Boolean>) {
    val snap by vm.snap.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val stack by vm.stack.collectAsState()
    val focus by vm.focus.collectAsState()
    val msg by vm.messages.collectAsState()
    val isLocked by locked.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val mapView = remember { MapView() }
    LaunchedEffect(msg) {
        msg?.let { m ->
            val r = snackbar.showSnackbar(m.text, actionLabel = if (m.undo != null) "Undo" else null, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) m.undo?.invoke()
            vm.messages.value = null
        }
    }

    CompositionLocalProvider(LocalFiles provides vm.repo.files) {
        Box(Modifier.fillMaxSize().background(Atlas.Background)) {
            when {
                isLocked -> LockScreen { locked.value = false }
                !onboarded -> Onboarding(vm)
                else -> {
                    BackHandler(enabled = stack.size > 1) { vm.back() }
                    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        if (maxWidth >= 600.dp) Wide(vm, snap, stack.last(), focus, mapView, side = maxWidth >= 760.dp)
                        else Compact(vm, snap, stack.last(), focus, mapView, narrow = maxWidth < 380.dp)
                    }
                    Overlays(vm, snap)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 96.dp))
        }
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

private fun mapActions(vm: AtlasViewModel, focus: String?) = MapActions(
    focus = { vm.focusOn(it) },
    openCluster = { vm.show(Overlay.Cluster(focus, it)) },
    addTo = { vm.show(Overlay.Cluster(focus, it)) },
    toggleTask = { vm.repo.toggleDone(it) },
    openFocus = { focus?.let { vm.open(it) } },
    pickFocus = { vm.show(Overlay.FocusPicker) },
)

@Composable
private fun ModeSwitch(mode: MapMode, onMode: (MapMode) -> Unit) {
    Row(Modifier.clip(CircleShape).background(Atlas.SurfaceSecondary).padding(3.dp)) {
        listOf(MapMode.MAP to ("Map" to Icons.Outlined.Hub), MapMode.INSIGHTS to ("Insights" to Icons.Outlined.Insights)).forEach { (m, li) ->
            val on = m == mode
            Row(Modifier.clip(CircleShape).background(if (on) Atlas.PrimaryContainer else Color.Transparent)
                .selectable(on, role = Role.Tab) { onMode(m) }.heightIn(min = 40.dp).padding(horizontal = 14.dp).testTag("mode_${li.first}"), verticalAlignment = Alignment.CenterVertically) {
                Icon(li.second, null, Modifier.size(18.dp), tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText); Spacer(Modifier.width(6.dp))
                Text(li.first, style = MaterialTheme.typography.labelLarge, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
            }
        }
    }
}

@Composable
private fun MapBar(vm: AtlasViewModel, snap: Snapshot, focus: String?, mapView: MapView, showExample: Boolean) {
    val mode by vm.mode.collectAsState()
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vm.hasFocusHistory) IconButton({ vm.focusBack() }, Modifier.semantics { contentDescription = "Previous focus" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
        ModeSwitch(mode) { vm.setMode(it) }
        Spacer(Modifier.weight(1f))
        if (showExample && snap.objects.values.any { it.origin == Origin.EXAMPLE }) Pill("Example data", Icons.Outlined.Science, Atlas.Peach, Atlas.DeepPlum, Modifier.testTag("example_chip")) { vm.show(Overlay.RemoveExample) }
        if (mode == MapMode.MAP) MapMenu(mapView, focus != null, { vm.show(Overlay.FocusPicker) }, { vm.focusOn(null) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetMap(vm: AtlasViewModel, snap: Snapshot, focus: String?, mapView: MapView, phone: Boolean, peek: Dp) {
    val mode by vm.mode.collectAsState()
    BottomSheetScaffold(
        sheetContent = { InspectorSheet(vm, snap, focus) }, sheetPeekHeight = peek, sheetContainerColor = Atlas.Surface, sheetShadowElevation = 6.dp,
        containerColor = Atlas.Background, sheetDragHandle = { BottomSheetDefaults.DragHandle(color = Atlas.Border, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) },
    ) {
        Box(Modifier.fillMaxSize().padding(bottom = peek)) {
            if (mode == MapMode.MAP) UniverseMap(snap, focus, phone, mapView, mapActions(vm, focus)) else InsightsView(vm, snap, focus)
        }
    }
}

@Composable
private fun RouteContent(vm: AtlasViewModel, snap: Snapshot, route: Route, libSel: String? = null, onLibSel: ((String) -> Unit)? = null) {
    val q by vm.query.collectAsState()
    AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route") { r ->
        when (r) {
            Route.Now -> NowScreen(vm, snap)
            is Route.Detail -> DetailScreen(vm, snap, r.id)
            Route.Review -> ReviewScreen(vm, snap)
            Route.Search -> SearchScreen(vm, snap, q)
            Route.Settings -> SettingsScreen(vm, snap)
            Route.Integrations -> IntegrationsScreen(vm, snap)
            Route.AiSettings -> AiSettingsScreen(vm)
            Route.Security -> SecurityScreen(vm)
            Route.Trash -> TrashScreen(vm, snap)
            Route.Timeline -> TimelineScreen(vm, snap)
            is Route.Library -> LibraryScreen(vm, snap, r.type, libSel, onLibSel)
            is Route.Connections -> ConnectionsScreen(vm, snap, r.id)
            is Route.History -> HistoryScreen(vm, snap, r.id)
            Route.Universe -> {}
        }
    }
}

// ============================================================ wide: rail + content (+ card pane) — Fold inner screen, tablets
@Composable
private fun Wide(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?, mapView: MapView, side: Boolean) {
    val mode by vm.mode.collectAsState()
    var libSel by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        Rail(vm, snap, route)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                route == Route.Universe -> Column(Modifier.fillMaxSize()) {
                    MapBar(vm, snap, focus, mapView, showExample = true)
                    if (side) Box(Modifier.weight(1f)) { if (mode == MapMode.MAP) UniverseMap(snap, focus, false, mapView, mapActions(vm, focus)) else InsightsView(vm, snap, focus) }
                    else Box(Modifier.weight(1f)) { SheetMap(vm, snap, focus, mapView, phone = false, peek = 236.dp) }
                }
                route == Route.Now -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { NowScreen(vm, snap, Modifier.widthIn(max = 760.dp)) }
                route is Route.Library -> RouteContent(vm, snap, route, libSel.takeIf { side }, if (side) ({ libSel = it }) else null)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { Box(Modifier.widthIn(max = 820.dp)) { RouteContent(vm, snap, route) } }
            }
        }
        val paneId = when { route == Route.Universe -> focus; route is Route.Library -> libSel?.takeIf { snap.objects[it]?.trashed == false }; else -> null }
        if (side && route == Route.Universe) InspectorPane(vm, snap, paneId, Modifier.width(340.dp).padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp))
        if (side && route is Route.Library && paneId != null) Surface(Modifier.width(400.dp).fillMaxHeight().padding(end = 12.dp, top = 8.dp, bottom = 12.dp), shape = RoundedCornerShape(28.dp), color = Atlas.Background,
            border = androidx.compose.foundation.BorderStroke(1.dp, Atlas.Border)) { CompositionLocalProvider(com.jmr.atlas.ui.cards.LocalInPane provides true) { DetailScreen(vm, snap, paneId) } }
    }
}

@Composable
private fun Rail(vm: AtlasViewModel, snap: Snapshot, route: Route) {
    Column(Modifier.width(92.dp).fillMaxHeight().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AtlasMark(32.dp); Spacer(Modifier.height(18.dp))
        FloatingActionButton({ vm.show(Overlay.Capture) }, shape = CircleShape, containerColor = Atlas.Primary, contentColor = Color.White,
            modifier = Modifier.size(60.dp).testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
        Spacer(Modifier.height(18.dp))
        DESTS.forEach { d -> RailItem(d, route.dest() == d.route || (d.route == Route.Library() && route is Route.Library)) { vm.top(d.route) } }
        Spacer(Modifier.weight(1f))
        BadgeIcon(snap.pending().size, Icons.Outlined.Inbox, "To review", { vm.go(Route.Review) }, Modifier.testTag("bell"))
        IconButton({ vm.go(Route.Settings) }, Modifier.semantics { contentDescription = "Settings" }) { Icon(Icons.Outlined.Settings, null) }
    }
}

@Composable
private fun RailItem(d: Dest, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.padding(vertical = 4.dp).width(80.dp).selectable(on, role = Role.Tab, onClick = onClick).padding(vertical = 4.dp).testTag("nav_${d.label}"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 60.dp, height = 34.dp).clip(CircleShape).background(if (on) Atlas.PrimaryContainer else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(d.icon, null, tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
        }
        Text(d.label, style = MaterialTheme.typography.labelMedium, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText, maxLines = 1)
    }
}

// ============================================================ compact: phone and Fold cover screen
@Composable
private fun Compact(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?, mapView: MapView, narrow: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (route) {
                Route.Universe -> Column(Modifier.fillMaxSize()) { MapBar(vm, snap, focus, mapView, showExample = !narrow); Box(Modifier.weight(1f)) { SheetMap(vm, snap, focus, mapView, phone = true, peek = 220.dp) } }
                else -> RouteContent(vm, snap, route)
            }
        }
        if (route.top) BottomBar(vm, route)
    }
}

@Composable
private fun BottomBar(vm: AtlasViewModel, route: Route) {
    Surface(color = Atlas.Background, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = Atlas.Border)
            Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                DESTS.take(2).forEach { d -> BarItem(d, route.dest() == d.route) { vm.top(d.route) } }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton({ vm.show(Overlay.Capture) }, shape = CircleShape, containerColor = Atlas.Primary, contentColor = Color.White,
                        modifier = Modifier.size(64.dp).testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null, Modifier.size(30.dp)) }
                }
                DESTS.drop(2).forEach { d -> BarItem(d, route.dest() == d.route || (d.route == Route.Library() && route is Route.Library)) { vm.top(d.route) } }
            }
        }
    }
}

@Composable
private fun RowScope.BarItem(d: Dest, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxHeight().selectable(on, role = Role.Tab, onClick = onClick).testTag("nav_${d.label}"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(width = 56.dp, height = 32.dp).clip(CircleShape).background(if (on) Atlas.PrimaryContainer else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(d.icon, null, tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
        }
        Spacer(Modifier.height(4.dp))
        Text(d.label, style = MaterialTheme.typography.labelMedium, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText, maxLines = 1)
    }
}
