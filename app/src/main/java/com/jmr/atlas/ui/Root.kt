package com.jmr.atlas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jmr.atlas.LocalPlatform
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.components.*
import com.jmr.atlas.ui.objects.*
import com.jmr.atlas.ui.screens.*
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.universe.MapActions
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
    LaunchedEffect(msg) { msg?.let { snackbar.showSnackbar(it); vm.messages.value = null } }

    CompositionLocalProvider(LocalFiles provides vm.repo.files) {
        Box(Modifier.fillMaxSize().background(Atlas.Background)) {
            if (!onboarded) Onboarding(vm)
            else {
                BackHandler(enabled = stack.size > 1 || vm.hasFocusHistory) { vm.back() }
                BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (maxWidth >= 600.dp) Wide(vm, snap, stack.last(), focus, maxWidth >= 760.dp) else Compact(vm, snap, stack.last(), focus)
                }
                Overlays(vm, snap)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 80.dp))
        }
    }
}

private fun mapActions(vm: AtlasViewModel, focus: String?) = MapActions(
    focus = { vm.focusOn(it) },
    openCluster = { vm.show(Overlay.Cluster(focus, it)) },
    toggleTask = { vm.repo.toggleDone(it) },
    openFocus = { focus?.let { vm.open(it) } },
    pickFocus = { vm.show(Overlay.FocusPicker) },
    back = if (vm.hasFocusHistory) ({ vm.focusBack() }) else null,
)

// ============================================================ wide: rail + toolbar + map + inspector (Fold inner, tablets)
@Composable
private fun Wide(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?, side: Boolean) {
    val mode by vm.mode.collectAsState()
    var listSel by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasWordmark()
            Spacer(Modifier.weight(1f))
            if (route == Route.Universe) ModeSwitch(mode) { vm.setMode(it) }
            Spacer(Modifier.weight(1f))
            IconButton({ vm.search("") }, Modifier.semantics { contentDescription = "Search" }) { Icon(Icons.Outlined.Search, null) }
            BadgeIcon(snap.pending().size, Icons.Outlined.Notifications, "Review inbox", { vm.go(Route.Review) }, Modifier.testTag("bell"))
            IconButton({ vm.go(Route.Settings) }, Modifier.semantics { contentDescription = "Settings" }) {
                androidx.compose.foundation.Image(androidx.compose.ui.graphics.painter.ColorPainter(Atlas.Primary), null, Modifier.size(34.dp).clip(CircleShape))
                Icon(Icons.Outlined.Person, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Rail(vm, route)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    route == Route.Universe && side -> MapArea(vm, snap, focus, mode, compactMap = false)
                    route == Route.Universe -> SheetMap(vm, snap, focus, mode, compactMap = false, fab = false)
                    route is Route.Objects -> ObjectsScreen(vm, snap, route.type, listSel.takeIf { side }, if (side) ({ listSel = it }) else null)
                    else -> RouteContent(vm, snap, route, showBack = true)
                }
            }
            val inspectorId = when { route == Route.Universe -> focus; route is Route.Objects -> listSel?.takeIf { snap.objects[it]?.trashed == false }; else -> null }
            if (side && (route == Route.Universe || (route is Route.Objects && inspectorId != null))) {
                InspectorPane(vm, snap, inspectorId, Modifier.width(320.dp).padding(start = 4.dp, end = 12.dp, bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun ModeSwitch(mode: MapMode, onMode: (MapMode) -> Unit) {
    Row(Modifier.clip(CircleShape).background(Atlas.SurfaceSecondary).padding(4.dp)) {
        listOf(MapMode.MAP to ("Map" to Icons.Outlined.Hub), MapMode.INSIGHTS to ("Insights" to Icons.Outlined.Insights)).forEach { (m, li) ->
            val on = m == mode
            Row(Modifier.clip(CircleShape).background(if (on) Atlas.PrimaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .selectable(on, role = Role.Tab) { onMode(m) }.heightIn(min = 40.dp).padding(horizontal = 16.dp).testTag("mode_${li.first}"), verticalAlignment = Alignment.CenterVertically) {
                Icon(li.second, null, Modifier.size(18.dp), tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText); Spacer(Modifier.width(6.dp))
                Text(li.first, style = MaterialTheme.typography.labelLarge, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
            }
        }
    }
}

@Composable
private fun Rail(vm: AtlasViewModel, route: Route) {
    Column(Modifier.width(88.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DESTS.forEach { d ->
            val on = route == d.route
            Column(Modifier.padding(vertical = 3.dp).width(76.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Atlas.PrimaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .selectable(on, role = Role.Tab) { vm.top(d.route) }.padding(vertical = 8.dp).testTag("nav_${d.label}"), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(d.icon, null, tint = if (on) Atlas.DeepPlum else Atlas.SecondaryText)
                Text(d.label, style = MaterialTheme.typography.labelSmall, color = if (on) Atlas.DeepPlum else Atlas.SecondaryText, maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        FloatingActionButton({ vm.go(Route.Capture) }, shape = CircleShape, containerColor = Atlas.Primary, contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(56.dp).testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
    }
}

@Composable
private fun MapArea(vm: AtlasViewModel, snap: Snapshot, focus: String?, mode: MapMode, compactMap: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (mode == MapMode.MAP) UniverseMap(snap, focus, compactMap, mapActions(vm, focus)) else InsightsView(vm, snap, focus)
        }
        AskBar(vm)
    }
}

/** “Search your Atlas…” — searches everything; attach captures a file; mic uses the system recognizer (on-device preferred). */
@Composable
private fun AskBar(vm: AtlasViewModel) {
    val platform = LocalPlatform.current
    var q by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).widthIn(max = 720.dp).testTag("ask_bar"), shape = CircleShape, color = Atlas.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Atlas.Border), shadowElevation = 2.dp) {
        Row(Modifier.heightIn(min = 56.dp).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Atlas.Primary)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (q.isEmpty()) Text("Search your Atlas…", style = MaterialTheme.typography.bodyLarge, color = Atlas.SecondaryText)
                BasicTextField(q, { q = it }, Modifier.fillMaxWidth().testTag("ask_field"), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Atlas.DeepPlum),
                    cursorBrush = SolidColor(Atlas.Primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (q.isNotBlank()) { vm.search(q); q = "" } }))
            }
            IconButton({ platform.pickFile() }, Modifier.semantics { contentDescription = "Capture a file" }) { Icon(Icons.Outlined.AttachFile, null, tint = Atlas.SecondaryText) }
            IconButton({ platform.dictate { q = it } }, Modifier.semantics { contentDescription = "Dictate" }) { Icon(Icons.Outlined.Mic, null, tint = Atlas.SecondaryText) }
            FilledIconButton({ if (q.isNotBlank()) { vm.search(q); q = "" } else vm.search("") }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Atlas.PrimaryContainer, contentColor = Atlas.DeepPlum),
                modifier = Modifier.semantics { contentDescription = "Search" }) { Icon(Icons.AutoMirrored.Outlined.Send, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetMap(vm: AtlasViewModel, snap: Snapshot, focus: String?, mode: MapMode, compactMap: Boolean, fab: Boolean) {
    val peek = 168.dp
    BottomSheetScaffold(
        sheetContent = { InspectorSheet(vm, snap, focus) }, sheetPeekHeight = peek, sheetContainerColor = Atlas.Surface, sheetShadowElevation = 6.dp,
        containerColor = Atlas.Background, sheetDragHandle = { BottomSheetDefaults.DragHandle(color = Atlas.Border) },
    ) {
        Box(Modifier.fillMaxSize().padding(bottom = peek - 16.dp)) {
            if (mode == MapMode.MAP) UniverseMap(snap, focus, compactMap, mapActions(vm, focus)) else InsightsView(vm, snap, focus)
            if (fab) FloatingActionButton({ vm.go(Route.Capture) }, shape = CircleShape, containerColor = Atlas.Primary, contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("capture_fab").semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
        }
    }
}

@Composable
private fun RouteContent(vm: AtlasViewModel, snap: Snapshot, route: Route, showBack: Boolean) {
    val q by vm.query.collectAsState()
    AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route") { r ->
        when (r) {
            is Route.Detail -> DetailScreen(vm, snap, r.id)
            Route.Capture -> CaptureScreen(vm, snap, showBack)
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

// ============================================================ compact: phone and Fold cover screen
@Composable
private fun Compact(vm: AtlasViewModel, snap: Snapshot, route: Route, focus: String?) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val mode by vm.mode.collectAsState()
    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = drawer.isOpen, drawerContent = {
        ModalDrawerSheet(drawerContainerColor = Atlas.Surface, modifier = Modifier.testTag("drawer")) {
            Box(Modifier.padding(16.dp)) { AtlasWordmark() }
            DESTS.forEach { d ->
                NavigationDrawerItem({ Text(d.label) }, route == d.route, { vm.top(d.route); scope.launch { drawer.close() } }, icon = { Icon(d.icon, null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("nav_${d.label}"), colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Atlas.PrimaryContainer))
            }
            HorizontalDivider(Modifier.padding(16.dp), color = Atlas.Border)
            NavigationDrawerItem({ Text("Review inbox") }, false, { vm.go(Route.Review); scope.launch { drawer.close() } }, icon = { Icon(Icons.Outlined.Inbox, null) },
                badge = { if (snap.pending().isNotEmpty()) Text("${snap.pending().size}") }, modifier = Modifier.padding(horizontal = 12.dp))
            NavigationDrawerItem({ Text("Settings") }, false, { vm.go(Route.Settings); scope.launch { drawer.close() } }, icon = { Icon(Icons.Outlined.Settings, null) }, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            if (route.isTop()) Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AtlasWordmark(Modifier.testTag("menu"), onClick = { scope.launch { drawer.open() } })
                Spacer(Modifier.weight(1f))
                if (route == Route.Universe) IconButton({ vm.setMode(if (mode == MapMode.MAP) MapMode.INSIGHTS else MapMode.MAP) },
                    Modifier.testTag("mode_toggle").semantics { contentDescription = if (mode == MapMode.MAP) "Show insights" else "Show map" }) {
                    Icon(if (mode == MapMode.MAP) Icons.Outlined.Insights else Icons.Outlined.Hub, null, tint = Atlas.DeepPlum)
                }
                IconButton({ vm.search("") }, Modifier.semantics { contentDescription = "Search" }) { Icon(Icons.Outlined.Search, null) }
                BadgeIcon(snap.pending().size, Icons.Outlined.Notifications, "Review inbox", { vm.go(Route.Review) }, Modifier.testTag("bell"))
                IconButton({ vm.go(Route.Settings) }, Modifier.semantics { contentDescription = "Settings" }) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Atlas.Primary), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            Box(Modifier.weight(1f)) {
                when (route) {
                    Route.Universe -> SheetMap(vm, snap, focus, mode, compactMap = true, fab = true)
                    else -> {
                        RouteContent(vm, snap, route, showBack = true)
                        if (route is Route.Objects || route == Route.Timeline) FloatingActionButton({ vm.go(Route.Capture) }, shape = CircleShape, containerColor = Atlas.Primary,
                            contentColor = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).semantics { contentDescription = "Capture" }) { Icon(Icons.Outlined.Add, null) }
                    }
                }
            }
        }
    }
}
