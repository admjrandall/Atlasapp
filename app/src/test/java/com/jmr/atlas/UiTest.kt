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

typealias Rule_ = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

fun Rule_.shot(name: String) {
    waitForIdle(); mainClock.advanceTimeBy(1200); waitForIdle()
    val img = onAllNodes(isRoot())[0].captureToImage().asAndroidBitmap()
    File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
/** Sheets and dialogs live in their own window; capture that node directly. */
fun Rule_.shotNode(tag: String, name: String) {
    waitForIdle(); mainClock.advanceTimeBy(800); waitForIdle()
    runCatching {
        val img = onNodeWithTag(tag, useUnmergedTree = true).captureToImage().asAndroidBitmap()
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
fun Rule_.repo() = (activity.application as AtlasApp).repo
fun Rule_.tap(tag: String) { onNodeWithTag(tag).performClick(); waitForIdle(); mainClock.advanceTimeBy(500); waitForIdle() }
fun Rule_.tapText(t: String) { onAllNodesWithText(t).onFirst().performClick(); waitForIdle(); mainClock.advanceTimeBy(500); waitForIdle() }
fun Rule_.example() { repeat(4) { tap("setup_next") }; tap("start_example") }
fun Rule_.vmOpen(id: String) { activityRule.scenario.onActivity { a -> androidx.lifecycle.ViewModelProvider(a)[com.jmr.atlas.ui.AtlasViewModel::class.java].open(id) }; waitForIdle(); mainClock.advanceTimeBy(600); waitForIdle() }
fun Rule_.captureJamie() {
    onNodeWithTag("capture_field").performTextInput("Talked with Jamie Lee about replacing the kitchen cabinets. He will send me an updated estimate Friday. Remember to follow up. He said he could start next month.")
    onNodeWithTag("capture_save").performClick()
    waitUntil(8000) { onAllNodesWithTag("suggestion").fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

fun grantNotifications() = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun setupThenNow() {
        rule.onNodeWithTag("onboarding").assertIsDisplayed()
        rule.shot("p01_welcome")
        rule.tap("setup_next"); rule.shot("p02_locked")
        rule.tap("setup_next"); rule.shot("p03_briefing")
        rule.tap("setup_next"); rule.shot("p04_phone")
        rule.tap("setup_next"); rule.shot("p05_start")
        rule.tap("start_example")
        rule.onNodeWithTag("now").assertIsDisplayed()
        rule.onNodeWithTag("briefing").assert(hasAnyDescendant(hasText("overdue", substring = true)))
        assertTrue(rule.onAllNodesWithTag("needs_row", useUnmergedTree = true).fetchSemanticsNodes().size >= 2)
        rule.onNodeWithTag("just_captured", useUnmergedTree = true).assertExists()
        rule.shot("p06_now")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun captureAutoConnectAndReview() {
        rule.example()
        rule.tap("capture_fab")
        rule.onNodeWithTag("capture").assertIsDisplayed()
        rule.shotNode("capture", "p07_capture_sheet")
        rule.captureJamie()
        rule.shotNode("capture", "p08_capture_result")
        val before = rule.repo().snap.ofType(ObjType.PERSON).size
        rule.onNodeWithTag("accept_all").performClick(); rule.waitForIdle()
        assertEquals(before + 1, rule.repo().snap.ofType(ObjType.PERSON).size)
        assertTrue(rule.repo().snap.ofType(ObjType.TASK).any { it.title == "Expect updated estimate from Jamie Lee" })
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun libraryTasksSearchSettings() {
        rule.example()
        rule.tap("nav_Library"); rule.onNodeWithTag("library").assertIsDisplayed(); rule.shot("p09_library")
        rule.tap("tab_Tasks"); rule.tap("filter_Overdue")
        rule.onNodeWithText("Order tile samples").assertExists(); rule.onNodeWithText("Finalize kitchen layout with Priya").assertDoesNotExist()
        rule.tap("filter_Open"); rule.shot("p10_library_tasks")
        rule.tap("nav_Search"); rule.onNodeWithTag("search_field").performTextInput("load-bearing"); rule.waitForIdle()
        rule.onNodeWithText("Site visit notes").assertExists(); rule.shot("p11_search")
        rule.tap("nav_Now"); rule.tap("nav_Settings"); rule.onNodeWithTag("settings").assertIsDisplayed(); rule.shot("p12_settings")
        rule.tap("integrations"); rule.shot("p13_integrations"); rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.tap("ai_settings"); rule.tap("ai_ON_DEVICE"); rule.onNodeWithTag("ai_download").assertExists(); rule.shot("p14_ai")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.tap("security"); rule.shot("p15_security")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun universeMapAndInsights() {
        rule.example()
        rule.tap("nav_Universe")
        rule.onNodeWithTag("universe").assertIsDisplayed()
        rule.shot("p16_universe")
        rule.tap("mode_Insights"); rule.onNodeWithTag("insights").assertIsDisplayed(); rule.shot("p17_insights")
    }

    @Test @Config(sdk = [36], qualifiers = "w344dp-h882dp-xxhdpi")
    fun coverScreen() {
        rule.example()
        rule.onNodeWithTag("now").assertIsDisplayed()
        rule.shot("c01_cover_now")
    }
}

/** Tall viewport so each card's whole length is captured in one screenshot. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()
    private fun open(t: String) = rule.vmOpen(rule.repo().snap.live.first { it.title == t }.id)

    @Test @Config(sdk = [36], qualifiers = "w411dp-h2200dp-xhdpi")
    fun personCardAndEditor() {
        rule.example(); open("Jamie Carter")
        rule.onNodeWithTag("person_card").assertIsDisplayed()
        rule.onNodeWithText("(555) 014-2290").assertExists()
        rule.onNodeWithText("Prefers a text before 8 AM, not calls").assertExists()
        rule.onNodeWithText("CO-••••2291").assertExists()                 // secret field masked
        rule.shot("i01_person_card")
        rule.tap("edit"); rule.onNodeWithTag("edit_sheet").assertExists(); rule.shotNode("edit_sheet", "i02_person_edit")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h2200dp-xhdpi")
    fun projectCardUpdateAndField() {
        rule.example(); open("Home Renovation")
        rule.onNodeWithTag("project_card").assertIsDisplayed()
        rule.onNodeWithText("Demolition starts").assertExists()
        rule.shot("i04_project_card")
        rule.tap("post_update"); rule.onNodeWithTag("update_text").performTextInput("Cabinets ordered. Tile samples still missing."); rule.tap("sheet_ok")
        assertEquals("Cabinets ordered. Tile samples still missing.", rule.repo().snap.detailsOf(rule.repo().snap.live.first { it.title == "Home Renovation" }.id, DetailKind.UPDATE).maxBy { it.created }.value)
        rule.onNodeWithTag("section_Add field").performScrollTo().performClick(); rule.waitForIdle()
        rule.shotNode("add_field", "i06_add_field")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h2000dp-xhdpi")
    fun taskCardAndDates() {
        rule.example(); open("Compare contractor quotes")
        rule.onNodeWithTag("task_card").assertIsDisplayed()
        rule.onNodeWithText("1 of 3").assertExists()
        rule.shot("i07_task_card")
        rule.onNodeWithText("Plan: Today").performClick(); rule.waitForIdle()
        rule.shotNode("task_dates", "i08_task_dates")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h2000dp-xhdpi")
    fun documentAndLinkCards() {
        rule.example(); open("Contractor estimate")
        rule.onNodeWithTag("document_card").assertIsDisplayed()
        rule.onNodeWithText("$48,200").assertExists()
        rule.shot("i09_document_card")
        open("12 kitchen layouts that work")
        rule.onNodeWithTag("link_card").assertIsDisplayed()
        rule.shot("i11_link_card")
        open("Demolition starts"); rule.shot("i12_event_card")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WideUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()

    @Test @Config(sdk = [36], qualifiers = "w880dp-h720dp-land-xhdpi")
    fun foldInner() {
        rule.example()
        rule.onNodeWithTag("now").assertIsDisplayed()
        rule.shot("f01_fold_now")
        rule.tap("nav_Universe"); rule.onNodeWithTag("inspector").assertIsDisplayed(); rule.shot("f02_fold_universe")
        rule.tap("nav_Library"); rule.tap("tab_People"); rule.tapText("Jamie Carter")
        rule.onNodeWithTag("person_card").assertIsDisplayed(); rule.shot("f03_fold_library_person")
        rule.tap("capture_fab"); rule.captureJamie(); rule.shotNode("capture", "f04_fold_capture")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun foldInnerPortrait() {
        rule.example()
        rule.tap("nav_Universe")
        rule.onNodeWithTag("universe").assertIsDisplayed()
        rule.shot("f05_fold_portrait_universe")
    }

    @Test @Config(sdk = [36], qualifiers = "w880dp-h720dp-land-xhdpi")
    fun stateSurvivesRecreate() {
        rule.example()
        val id = rule.repo().snap.live.first { it.title == "Priya Shah" }.id
        rule.vmOpen(id)
        rule.onNodeWithTag("person_card").assertIsDisplayed()
        rule.activityRule.scenario.recreate(); rule.waitForIdle()
        rule.onNodeWithTag("person_card").assertIsDisplayed()
        rule.onAllNodesWithText("Priya Shah").onFirst().assertExists()
    }
}
