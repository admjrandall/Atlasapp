package com.jmr.atlas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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
import kotlinx.coroutines.launch

private data class Dest(val route: Route, val label: String, val icon: ImageVector)

private val DESTS = listOf(
    Dest(Route.Universe, "Universe", Icons.Outlined.Hub),
    Dest(Route.Objects(ObjType.PERSON), "People", Icons.Outlined.Groups),
    Dest(Route.Objects(ObjType.PROJECT), "Projects", Icons.Outlined.FolderOpen),
    Dest(Route.Objects(ObjType.DOCUMENT), "Documents", Icons.Outlined.Description),
    Dest(Route.Objects(ObjType.IDEA), "Ideas", Icons.Outlined.Lightbulb),
    Dest(Route.Objects(ObjType.TASK), "Tasks", Icons.Outlined.TaskAlt),
    Dest(Route.Objects(ObjType.COLLECTION), "Collections", Icons.Outlined.Layers),
    Dest(Route.Timeline, "Timeline", Icons.Outlined.CalendarMonth),
)

private fun Route.isTop() = this == Route.Universe || this is Route.Objects || this == Route.Timeline

@Composable
fun AtlasRoot(vm: AtlasViewModel) {
    val snap by vm.snap.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val stack by vm.stack.collectAsState()
    val focus by vm.focus.collectAsState()
    val msg by vm.messages.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val mapView = remember { MapView() }
    LaunchedEffect(msg) { msg?.let { snackbar.showSnackbar(it); vm.messages.value = null } }

    CompositionLocalProvider(LocalFiles provides vm.repo.files) {
        Box(Modifier.fillMaxSize().background(Atlas.Background)) {
            if (!onboarded) Onboarding(vm)
            else {
                BackHandler(enabled = stack.size > 1 || vm.hasFocusHistory) { vm.back() }
                BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (maxWidth >= 600.dp) Wide(vm, snap, stack.last(), focus, mapView, side = maxWidth >= 760.dp)
                    else Compact(vm, snap, stack.last(), focus, mapView, narrow = maxWidth < 380.dp)
                }
                Overlays(vm, snap)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 24.dp))
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

// ---------------------------------------------------------------- shared controls
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
private fun RowScope.MapControls(vm: AtlasViewModel, snap: Snapshot, focus: String?, mapView: MapView, showExample: Boolean, trailing: Boolean = true) {
    val mode by vm.mode.collectAsState()
    if (vm.hasFocusHistory) IconButton({ vm.focusBack() }, Modifier.semantics { contentDescription = "Previous focus" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
    ModeSwitch(mode) { vm.setMode(it) }
    if (trailing) {
        Spacer(Modifier.weight(1f))
        if (showExample) ExampleChip(vm, snap)
        if (mode == MapMode.MAP) MapMenu(mapView, focus != null, { vm.show(Overlay.FocusPicker) }, { vm.focusOn(null) })
    }
}

@Composable
private fun ExampleChip(vm: AtlasViewModel, snap: Snapshot) {
    if (snap.objects.values.none { it.origin == Origin.EXAMPLE }) return
    Pill("Example data", Icons.Outlined.Science, Atlas.Peach, Atlas.DeepPlum, Modifier.testTag("example_chip")) { vm.show(Overlay.RemoveExample) }
}

@Composable
private fun NewFab(vm: AtlasViewModel, route: Route, modifier: Modifier) {
    val (label, action) = when (route) {
        is Route.Objects -> if (route.type == ObjType.DOCUMENT) "Add" to { vm.show(Overlay.Capture) } else "New ${route.type.label.lowercase()}" to { vm.show(Overlay.Create(route.type)) }
        Route.Timeline -> "New milestone" to { vm.show(Overlay.Create(ObjType.EVENT)) }
        else -> return
    }
    ExtendedFloatingActionButton({ action() }, modifier.testTag("new_fab"), containerColor = Atlas.Primary, contentColor = Color.White, shape = CircleShape) {
        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

/** Searches everything. Voice uses the system recognizer (on-device preferred). Not an assistant — it looks things up. */
@Composable
private fun SearchBar(vm: AtlasViewModel) {
    val platform = LocalPlatform.current
    var q by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).widthIn(max = 720.dp).testTag("search_bar"), shape = CircleShape, color = Atlas.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Atlas.Border), shadowElevation = 2.dp) {
        Row(Modifier.heightIn(min = 56.dp).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = Atlas.SecondaryText)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (q.isEmpty()) Text("Search people, projects, documents…", style = MaterialTheme.typography.bodyLarge, color = Atlas.SecondaryText)
                BasicTextField(q, { q = it }, Modifier.fillMaxWidth().testTag("search_bar_field"), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Atlas.DeepPlum),
                    cursorBrush = SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search(q); q = "" }))
            }
            IconButton({ platform.dictate { q = it; vm.search(it); q = "" } }, Modifier.semantics { contentDescription = "Search by voice" }) { Icon(Icons.Outlined.Mic, null, tint = Atlas.SecondaryText) }
        }
    }
}

@Composable
private fun RouteContent(vm: AtlasViewModel, snap: Snapshot, route: Route) {
    val q by vm.query.collectAsState()
    AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route") { r ->
        when (r) {
            is Route.Detail -> DetailScreen(vm, snap, r.id)
            Route.Review -> ReviewScreen(vm, snap)
            Route.Search -> SearchScreen(vm, snap, q)
            Route.Settings -> SettingsScreen(vm, snap)
            Route.Trash -> TrashScreen(vm, snap)
            Route.Timeline -> TimelineScreen(vm, snap)
            is Route.Objects -> ObjectsScreen(vm, snap, r.type, null, null)
            Route.Universe -> {}
        }
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

// ============================================================ wide: rail + toolbar + map + inspector (Fold inner, tablets)
@Composable
private fun Wide(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?, mapView: MapView, side: Boolean) {
    val mode by vm.mode.collectAsState()
    var listSel by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasWordmark(Modifier.width(150.dp))
            Spacer(Modifier.weight(1f))
            if (route == Route.Universe) Row(verticalAlignment = Alignment.CenterVertically) { MapControls(vm, snap, focus, mapView, showExample = false, trailing = false); if (mode == MapMode.MAP) MapMenu(mapView, focus != null, { vm.show(Overlay.FocusPicker) }, { vm.focusOn(null) }) }
            Spacer(Modifier.weight(1f))
            ExampleChip(vm, snap)
            IconButton({ vm.search("") }, Modifier.semantics { contentDescription = "Search" }) { Icon(Icons.Outlined.Search, null) }
            BadgeIcon(snap.pending().size, Icons.Outlined.Notifications, "Review", { vm.go(Route.Review) }, Modifier.testTag("bell"))
            IconButton({ vm.go(Route.Settings) }, Modifier.semantics { contentDescription = "Settings" }) { Icon(Icons.Outlined.Settings, null) }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Rail(vm, route)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    route == Route.Universe && side -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) { if (mode == MapMode.MAP) UniverseMap(snap, focus, false, mapView, mapActions(vm, focus)) else InsightsView(vm, snap, focus) }
                        SearchBar(vm)
                    }
                    route == Route.Universe -> SheetMap(vm, snap, focus, mapView, phone = false, peek = 236.dp)
                    route is Route.Objects -> ObjectsScreen(vm, snap, route.type, listSel.takeIf { side }, if (side) ({ listSel = it }) else null)
                    else -> RouteContent(vm, snap, route)
                }
                if (route is Route.Objects || route == Route.Timeline) NewFab(vm, route, Modifier.align(Alignment.BottomEnd).padding(20.dp))
            }
            val inspectorId = when { route == Route.Universe -> focus; route is Route.Objects -> listSel?.takeIf { snap.objects[it]?.trashed == false }; else -> null }
            if (side && (route == Route.Universe || (route is Route.Objects && inspectorId != null))) {
                InspectorPane(vm, snap, inspectorId, Modifier.width(320.dp).padding(start = 4.dp, end = 12.dp, bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun Rail(vm: AtlasViewModel, route: Route) {
    Column(Modifier.width(88.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DESTS.forEach { d ->
            val on = route == d.route
            Column(Modifier.padding(vertical = 2.dp).width(76.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Atlas.PrimaryContainer else Color.Transparent)
                .selectable(on, role = Role.Tab) { vm.top(d.route) }.padding(vertical = 7.dp).testTag("nav_${d.label}"), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(d.icon, null, tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
                Text(d.label, style = MaterialTheme.typography.labelSmall, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        FloatingActionButton({ vm.show(Overlay.Capture) }, shape = CircleShape, containerColor = Atlas.Primary, contentColor = Color.White,
            modifier = Modifier.size(56.dp).testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
        Text("Capture", style = MaterialTheme.typography.labelSmall, color = Atlas.SecondaryText, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================ compact: phone and Fold cover screen
@Composable
private fun Compact(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?, mapView: MapView, narrow: Boolean) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = drawer.isOpen, drawerContent = {
        ModalDrawerSheet(drawerContainerColor = Atlas.Surface, modifier = Modifier.testTag("drawer")) {
            Box(Modifier.padding(16.dp)) { AtlasWordmark() }
            DESTS.forEach { d ->
                NavigationDrawerItem({ Text(d.label) }, route == d.route, { vm.top(d.route); scope.launch { drawer.close() } }, icon = { Icon(d.icon, null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("nav_${d.label}"), colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Atlas.PrimaryContainer))
            }
            HorizontalDivider(Modifier.padding(16.dp), color = Atlas.Border)
            NavigationDrawerItem({ Text("Review") }, false, { vm.go(Route.Review); scope.launch { drawer.close() } }, icon = { Icon(Icons.Outlined.Inbox, null) },
                badge = { if (snap.pending().isNotEmpty()) Text("${snap.pending().size}") }, modifier = Modifier.padding(horizontal = 12.dp))
            NavigationDrawerItem({ Text("Settings") }, false, { vm.go(Route.Settings); scope.launch { drawer.close() } }, icon = { Icon(Icons.Outlined.Settings, null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("nav_Settings"))
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            if (route.isTop()) Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ scope.launch { drawer.open() } }, Modifier.testTag("menu").semantics { contentDescription = "Menu" }) { Icon(Icons.Outlined.Menu, null, tint = Atlas.DeepPlum) }
                AtlasWordmark()
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton({ vm.show(Overlay.Capture) }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Atlas.Primary, contentColor = Color.White),
                    modifier = Modifier.testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
                IconButton({ vm.search("") }, Modifier.semantics { contentDescription = "Search" }) { Icon(Icons.Outlined.Search, null) }
                BadgeIcon(snap.pending().size, Icons.Outlined.Notifications, "Review", { vm.go(Route.Review) }, Modifier.testTag("bell"))
            }
            if (route == Route.Universe) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                MapControls(vm, snap, focus, mapView, showExample = !narrow)
            }
            Box(Modifier.weight(1f)) {
                when (route) {
                    Route.Universe -> SheetMap(vm, snap, focus, mapView, phone = true, peek = 236.dp)
                    else -> {
                        RouteContent(vm, snap, route)
                        NewFab(vm, route, Modifier.align(Alignment.BottomEnd).padding(20.dp))
                    }
                }
            }
        }
    }
}
