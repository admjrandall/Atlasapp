package com.jmr.atlas

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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
        rule.onNodeWithTag("toast").assertExists()
        rule.shot("c03_saved")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun boardTapOpen() {
        rule.example()
        rule.onAllNodesWithTag("cover").onFirst().performScrollTo(); rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.onNodeWithTag("space").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("band", useUnmergedTree = true).fetchSemanticsNodes().size >= 4)
        rule.shot("c04_board")
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performClick(); rule.settle()
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
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performClick(); rule.settle()
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
        rule.onNodeWithTag("space_People").assertExists()
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
