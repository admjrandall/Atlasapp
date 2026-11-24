package com.jmr.atlas

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

typealias Rule_ = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

fun Rule_.shot(name: String) {
    waitForIdle(); mainClock.advanceTimeBy(1200); waitForIdle()
    val img = onAllNodes(isRoot())[0].captureToImage().asAndroidBitmap()
    File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
/** Dialogs live in their own window; capture that node directly. */
fun Rule_.shotNode(tag: String, name: String) {
    waitForIdle(); mainClock.advanceTimeBy(800); waitForIdle()
    runCatching {
        val img = onNodeWithTag(tag, useUnmergedTree = true).captureToImage().asAndroidBitmap()
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
fun Rule_.repo() = (activity.application as AtlasApp).repo
fun Rule_.settle() { waitForIdle(); mainClock.advanceTimeBy(600); waitForIdle() }
fun Rule_.tap(tag: String) { onNodeWithTag(tag).performClick(); settle() }
fun Rule_.tapDesc(d: String) { onNodeWithContentDescription(d).performClick(); settle() }
fun Rule_.example() { repeat(4) { tap("setup_next") }; tap("start_example") }
fun Rule_.vm(block: (AtlasViewModel) -> Unit) { activityRule.scenario.onActivity { a -> block(androidx.lifecycle.ViewModelProvider(a)[AtlasViewModel::class.java]) }; settle() }
fun Rule_.vmOpen(id: String) = vm { it.open(id) }
fun Rule_.idOf(t: String) = repo().snap.live.first { it.title == t }.id
fun Rule_.type(text: String) { tap("catch_open"); onNodeWithTag("catch_input").performTextInput(text); settle() }
fun Rule_.ask(q: String) {
    type(q); tapDesc("Ask")
    waitUntil(10_000) { onAllNodesWithTag("answer", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    settle()
}

fun grantNotifications() = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

/** Galaxy Z Fold 6 cover screen (968 × 2376 px ≈ 369 × 905 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun setupThenHome() {
        rule.onNodeWithTag("onboarding").assertIsDisplayed()
        rule.shot("c00_welcome")
        rule.example()
        rule.onNodeWithTag("home").assertIsDisplayed()
        rule.onNodeWithTag("brief").assert(hasText("late", substring = true))
        assertTrue(rule.onAllNodesWithTag("agenda_row", useUnmergedTree = true).fetchSemanticsNodes().size >= 3)
        rule.onNodeWithTag("catch_bar").assertIsDisplayed()
        rule.onNodeWithTag("nav_Settings").assertIsDisplayed()
        rule.shot("c01_home")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun catchATaskFromASentence() {
        rule.example()
        rule.type("Order grout by Friday for the kitchen")
        rule.onNodeWithTag("catch_preview").assertIsDisplayed()
        rule.onNodeWithTag("kind_chip").assert(hasText("Task"))
        rule.onNodeWithTag("catch_preview").assert(hasAnyDescendant(hasText("Home Renovation")))
        rule.shot("c02_catch_task")
        rule.tapDesc("Save")
        val t = rule.repo().snap.live.first { it.title == "Order grout" }
        assertEquals(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)), t.due)
        assertTrue(rule.repo().snap.neighbours(t.id).any { it.first.title == "Home Renovation" })
        rule.onNodeWithTag("did", useUnmergedTree = true).assertExists()   // 0.7: the box stays open and the thread says what Atlas did
        rule.shot("c03_saved")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun boardTapOpen() {
        rule.example()
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.onNodeWithTag("space").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("band", useUnmergedTree = true).fetchSemanticsNodes().size >= 4)
        rule.shot("c04_board")
        // 0.7.1: long-press shows links; tap opens
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performTouchInput { longClick() }; rule.settle()
        rule.onNodeWithTag("selection_bar").assertIsDisplayed()
        rule.shot("c05_board_tap")
        rule.tap("open_selected")
        rule.onNodeWithTag("item_page").assertIsDisplayed()
        rule.vmOpen(rule.idOf("Priya Shah"))
        rule.shot("c06_item_person")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun settingsScreens() {
        rule.example()
        rule.tap("nav_Settings"); rule.onNodeWithTag("settings").assertIsDisplayed(); rule.shot("c07_settings")
        rule.tap("ai_settings"); rule.tap("ai_ON_DEVICE"); rule.onNodeWithTag("ai_download").assertExists(); rule.shot("c08_ai")
    }
}

/** Galaxy Z Fold 6 inner screen (1856 × 2160 px ≈ 707 × 823 dp) — where most use happens. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FoldUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun homeBoardTapPage() {
        rule.example()
        rule.onNodeWithTag("home").assertIsDisplayed()
        rule.shot("f01_home")
        rule.onAllNodesWithTag("cover").onFirst().performClick(); rule.settle()
        rule.onNodeWithTag("ribbon").assertExists()
        rule.shot("f02_board")
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performTouchInput { longClick() }; rule.settle()
        rule.onNodeWithTag("selection_bar").assertIsDisplayed()
        rule.shot("f03_board_tap")
        rule.vm { it.select(rule.idOf("Priya Shah")) }
        rule.shot("f03b_board_tap_person")
        rule.tap("open_selected")
        rule.onNodeWithTag("item_page").assertIsDisplayed()
        rule.onNodeWithTag("hero").assertIsDisplayed()
        rule.shot("f04_item_person")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun catchAndAsk() {
        rule.example()
        rule.type("Order tile samples by Friday for the kitchen")
        rule.shot("f05_catch")
        rule.onNodeWithTag("catch_input").performTextClearance(); rule.settle()
        rule.onNodeWithTag("catch_input").performTextInput("What could delay demolition?"); rule.settle()
        rule.tapDesc("Ask")
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("answer", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.settle()
        rule.onNodeWithTag("answer").assert(hasAnyDescendant(hasText("before demolition", substring = true)))
        rule.shot("f06_ask")
        rule.tap("show_on_board")
        rule.onNodeWithTag("space").assertIsDisplayed()
        rule.shot("f07_answer_on_board")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun specialSpacesAndPages() {
        rule.example()
        rule.onNodeWithTag("core_People", useUnmergedTree = true).assertExists()
        rule.vm { it.go(com.jmr.atlas.ui.Route.Space("people")) }; rule.shot("f08_people")
        rule.vm { it.go(com.jmr.atlas.ui.Route.Space("tasks")) }; rule.shot("f09_tasks")
        rule.vmOpen(rule.idOf("Compare contractor quotes")); rule.shot("f10_item_task")
        rule.vmOpen(rule.idOf("Contractor estimate")); rule.onNodeWithText("$48,200").assertExists(); rule.shot("f11_item_document")
    }

    @Test @Config(sdk = [36], qualifiers = "w823dp-h707dp-land-xhdpi")
    fun landscape() {
        rule.example()
        rule.shot("f12_home_land")
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.shot("f13_board_land")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun stateSurvivesRecreate() {
        rule.example()
        rule.vmOpen(rule.idOf("Priya Shah"))
        rule.onNodeWithTag("item_page").assertIsDisplayed()
        rule.activityRule.scenario.recreate(); rule.waitForIdle()
        rule.onNodeWithTag("item_page").assertIsDisplayed()
        rule.onAllNodesWithText("Priya Shah").onFirst().assertExists()
    }
}

/** Tall viewport so a whole item page is captured in one screenshot, with the 0.5 details below the new header. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ItemPageUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()

    @Test @Config(sdk = [36], qualifiers = "w369dp-h2200dp-xhdpi")
    fun personPageKeepsEveryDetail() {
        rule.example(); rule.vmOpen(rule.idOf("Jamie Carter"))
        rule.onNodeWithTag("person_card").assertExists()
        rule.onNodeWithText("(555) 014-2290").assertExists()
        rule.onNodeWithText("Prefers a text before 8 AM, not calls").assertExists()
        rule.onNodeWithText("CO-••••2291").assertExists()
        rule.shot("i01_person_page")
        rule.tap("edit"); rule.onNodeWithTag("edit_sheet").assertExists()
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h2200dp-xhdpi")
    fun projectDetailsAndTask() {
        rule.example(); rule.vmOpen(rule.idOf("Home Renovation")); rule.tap("project_details")
        rule.onNodeWithTag("project_card").assertExists()
        rule.onNodeWithTag("post_update").performScrollTo().performClick(); rule.settle()
        rule.onNodeWithTag("update_text").performTextInput("Cabinets ordered."); rule.tap("sheet_ok")
        assertEquals("Cabinets ordered.", rule.repo().snap.detailsOf(rule.idOf("Home Renovation"), DetailKind.UPDATE).maxBy { it.created }.value)
        rule.shot("i02_project_details")
        rule.vmOpen(rule.idOf("Compare contractor quotes"))
        rule.tap("task_done")
        assertTrue(rule.repo().snap.objects[rule.idOf("Compare contractor quotes")]!!.done)
        rule.shot("i03_task_done")
    }
}

/** The input box end to end on the inner screen: toasts leave, lines land on the item you're looking at, @ and Link to work. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InputUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()
    private fun gone(tag: String) = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun toastGoesAway() {
        rule.example()
        rule.mainClock.autoAdvance = false
        rule.activityRule.scenario.onActivity { a -> androidx.lifecycle.ViewModelProvider(a)[AtlasViewModel::class.java].say("Marked done") }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag("toast", useUnmergedTree = true).assertExists()
        rule.shot("n01_toast")
        rule.mainClock.advanceTimeBy(6000)
        assertTrue("toast should leave on its own", gone("toast"))
        rule.mainClock.autoAdvance = true
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun lineOnATaskBecomesAStep() {
        rule.example()
        val task = rule.idOf("Order tile samples"); rule.vmOpen(task)
        rule.type("Check the haul-away fee")
        rule.onNodeWithTag("kind_chip").assert(hasText("Step", substring = true, ignoreCase = true))
        rule.onNodeWithTag("catch_preview").assert(hasAnyDescendant(hasText("Order tile samples", substring = true)))
        rule.shot("n02_step_on_task")
        rule.tapDesc("Save")
        assertTrue(rule.repo().snap.detailsOf(task, DetailKind.STEP).any { it.value == "Check the haul-away fee" })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun ideaOnAPersonIsLinkedToThem() {
        rule.example()
        val jamie = rule.idOf("Jamie Carter"); rule.vmOpen(jamie)
        rule.type("Idea: built-in bench by the window")
        rule.tapDesc("Save")
        val idea = rule.repo().snap.live.first { it.type == ObjType.IDEA && it.title.contains("bench", true) }
        assertTrue(rule.repo().snap.neighbours(idea.id).any { it.first.id == jamie })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun mentionAndLinkPicker() {
        rule.example()
        rule.type("Price out pendant lights @Home Renovation")
        rule.tap("link_to")
        rule.onNodeWithTag("link_search").performTextInput("Priya"); rule.settle()
        rule.shot("n03_link_picker")
        rule.onNodeWithText("Priya Shah").performClick(); rule.settle()
        rule.shot("n04_mention_and_link")
        rule.tapDesc("Save")
        val t = rule.repo().snap.live.first { it.title.startsWith("Price out pendant lights") }
        assertFalse(t.title.contains("@"))
        val near = rule.repo().snap.neighbours(t.id).map { it.first.id }
        assertTrue(rule.idOf("Home Renovation") in near)
        assertTrue(rule.idOf("Priya Shah") in near)
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun askShowsAnAnswer() {
        rule.example()
        rule.ask("What's late?")
        rule.onNodeWithTag("answer").assert(hasAnyDescendant(hasText("late", substring = true)))
        rule.shot("n05_answer")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun boardLines() {
        rule.example()
        rule.vm { it.go(com.jmr.atlas.ui.Route.Space("p:" + rule.idOf("Home Renovation"))) }
        rule.shot("n06_board_badges")
        rule.vm { it.select(rule.idOf("Jamie Carter")) }
        rule.shot("n07_board_lines_jamie")
        rule.vm { it.select(rule.idOf("Jamie Carter")) }
        rule.vm { it.select(rule.idOf("Order tile samples")) }
        rule.shot("n08_board_lines_task")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun boardLinesCover() {
        rule.example()
        rule.vm { it.go(com.jmr.atlas.ui.Route.Space("p:" + rule.idOf("Home Renovation"))) }
        rule.vm { it.select(rule.idOf("Jamie Carter")) }
        rule.shot("n09_cover_lines")
    }

    /** 0.7.2: starting empty still leaves a way to load the example (Settings) and a visible way to make a space (Home). */
    @Test
    fun emptyStartCanLoadExampleAndMakeSpace() {
        rule.tap("skip_setup")
        assertEquals(0, rule.repo().snap.objects.values.count { it.origin == Origin.EXAMPLE })
        rule.onAllNodesWithTag("home_new_space").onFirst().performClick(); rule.settle()
        rule.onNodeWithTag("catch_input").assertTextContains("New space", substring = true)
        rule.vm { it.closeCatch() }
        rule.tap("nav_Settings"); rule.onNodeWithTag("settings").assertIsDisplayed()
        rule.onNodeWithTag("load_example").performScrollTo().performClick(); rule.settle()
        assertTrue(rule.repo().snap.objects.values.any { it.origin == Origin.EXAMPLE })
        val n = rule.repo().snap.objects.size
        rule.vm { it.loadExample() }
        assertEquals("loading twice adds nothing", n, rule.repo().snap.objects.size)
        rule.tap("nav_Settings")
        rule.onNodeWithTag("remove_example").performScrollTo().assertIsDisplayed()
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun readsWhereThingsGo() {
        rule.example()
        rule.vm { vm ->
            fun p(t: String) = vm.parsed(t)
            p("Add to Ankle Surgery").let { assertEquals(com.jmr.atlas.capture.Sentence.Kind.SPACE, it.kind); assertEquals("Ankle Surgery", it.title) }
            p("Post-op instructions, add to ankle surgery").let { assertEquals("Ankle Surgery", it.newSpace); assertEquals("Post-op instructions", it.title) }
            p("Call the surgeon tomorrow @Ankle Surgery").let { assertEquals("Ankle Surgery", it.newSpace); assertEquals(com.jmr.atlas.capture.Sentence.Kind.TASK, it.kind); assertEquals("Call the surgeon", it.title) }
            p("Order grout add to Home Renovation").let { assertNull(it.newSpace); assertEquals("Home Renovation", it.space?.title); assertEquals("Order grout", it.title) }
            p("new group called ankle surgery").let { assertEquals(com.jmr.atlas.capture.Sentence.Kind.SPACE, it.kind); assertEquals("Ankle Surgery", it.title) }
            p("Move to Denver").let { assertNull(it.newSpace); assertNotEquals(com.jmr.atlas.capture.Sentence.Kind.SPACE, it.kind) }
            p("Add milk to the shopping list").let { assertNull(it.newSpace) }
        }
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun lineMakesTheSpaceItNames() {
        rule.example()
        rule.type("Ask about crutches add to Ankle Surgery")
        rule.onNodeWithTag("new_space_chip").assertExists()
        rule.shot("n10_new_space_bubble")
        rule.tapDesc("Save")
        val sp = rule.repo().snap.live.first { it.type == ObjType.PROJECT && it.title == "Ankle Surgery" }
        val t = rule.repo().snap.live.first { it.title == "Ask about crutches" }
        assertTrue(rule.repo().snap.neighbours(t.id).any { it.first.id == sp.id })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun photoGoesToANewSpace() {
        rule.example()
        rule.tap("catch_open")
        val png = java.io.ByteArrayOutputStream().also { android.graphics.Bitmap.createBitmap(120, 90, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFB39DDB.toInt()) }.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        rule.vm { it.attachments.value = listOf(AtlasViewModel.Pending("p1", com.jmr.atlas.capture.Readers.Read(png, "image/png", "IMG_2041.png"),
            android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size).asImageBitmap())) }
        rule.onNodeWithTag("attachments").assertExists()
        rule.shot("n11_photo_waiting")
        rule.onNodeWithTag("catch_input").performTextInput("Add to Ankle Surgery"); rule.settle()
        rule.onNodeWithTag("new_space_chip").assertExists()
        rule.shot("n12_photo_to_new_space")
        rule.tapDesc("Save")
        val snap = rule.repo().snap
        val sp = snap.live.first { it.type == ObjType.PROJECT && it.title == "Ankle Surgery" }
        val img = snap.live.first { it.type == ObjType.IMAGE && it.title == "IMG_2041" }
        assertTrue(snap.neighbours(img.id).any { it.first.id == sp.id })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun linkPickerCreatesASpace() {
        rule.example()
        rule.type("Book physical therapy")
        rule.tap("link_to")
        rule.onNodeWithTag("link_search").performTextInput("ankle surgery"); rule.settle()
        rule.shot("n13_picker_create")
        rule.tap("create_space")
        rule.tapDesc("Save")
        val sp = rule.repo().snap.live.first { it.type == ObjType.PROJECT && it.title == "Ankle Surgery" }
        val t = rule.repo().snap.live.first { it.title == "Book physical therapy" }
        assertTrue(rule.repo().snap.neighbours(t.id).any { it.first.id == sp.id })
    }

    private fun later() { org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(1)); rule.settle() }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun returnKeySends() {
        rule.example()
        rule.type("Call the pharmacy tomorrow\n")
        assertTrue(rule.repo().snap.live.any { it.type == ObjType.TASK && it.title == "Call the pharmacy" })
        rule.onNodeWithTag("did", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("catch_input").assert(hasText(""))
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun sendButtonOnTheSentence() {
        rule.example()
        rule.type("Ask about crutches add to Ankle Surgery")
        rule.onNodeWithTag("preview_send").assert(hasText("Save task", substring = true))
        rule.shot("n14_send_on_card")
        rule.tap("preview_send")
        assertTrue(rule.repo().snap.live.any { it.title == "Ask about crutches" })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun backdropDoesntThrowAwayADraft() {
        rule.example()
        rule.vm { it.openCatch() }; rule.settle()
        rule.tap("catch_scrim")                                   // right after opening: ignored
        rule.onNodeWithTag("catch_sheet").assertExists()
        rule.onNodeWithTag("catch_input").performTextInput("Pick up crutches Friday"); later()
        rule.tap("catch_scrim")                                   // with a draft: only the keyboard goes away
        rule.onNodeWithTag("catch_sheet").assertExists()
        later(); rule.tap("catch_scrim")                          // second tap closes…
        assertTrue(gone("catch_sheet"))
        rule.tap("catch_open")                                    // …and the draft is still there
        rule.onNodeWithTag("catch_input").assert(hasText("Pick up crutches Friday"))
    }
}
