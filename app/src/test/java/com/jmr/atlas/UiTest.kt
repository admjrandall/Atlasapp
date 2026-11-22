package com.jmr.atlas

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.jmr.atlas.model.ObjType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        val img = onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
fun Rule_.focalIs(t: String) = onNodeWithTag("focal").assert(hasText(t, substring = true))
fun Rule_.repo() = (activity.application as AtlasApp).repo
fun Rule_.example() { onNodeWithTag("start_example").performClick(); waitForIdle() }
fun Rule_.captureJamie() {
    onNodeWithTag("capture_field").performTextInput("Talked with Jamie Lee about replacing the kitchen cabinets. He will send me an updated estimate Friday. Remember to follow up. He said he could start next month.")
    onNodeWithTag("capture_save").performClick()
    waitUntil(8000) { onAllNodesWithTag("suggestion").fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun onboardingExampleMapRefocusAndBack() {
        rule.onNodeWithTag("onboarding").assertIsDisplayed()
        rule.shot("p01_onboarding")
        rule.example()
        rule.onNodeWithTag("universe").assertIsDisplayed()
        rule.onNodeWithTag("focal").assertIsDisplayed()
        rule.onAllNodesWithText("Home Renovation").onFirst().assertExists()
        rule.shot("p02_universe_phone")
        // open a neighbourhood and refocus on a person
        rule.onNodeWithContentDescription("People, 4", substring = true).performClick(); rule.waitForIdle()
        rule.onNodeWithTag("cluster_sheet").assertIsDisplayed()
        rule.shotNode("cluster_sheet", "p03_cluster_people")
        rule.onNodeWithText("Jamie Carter").performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(800)
        rule.focalIs("Jamie Carter")
        rule.shot("p04_focus_jamie")
        rule.onNodeWithContentDescription("Previous focus").performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(800)
        rule.focalIs("Home Renovation")
        // accessible list alternative, from the labeled map menu
        rule.onNodeWithTag("map_menu").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("list_toggle").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("universe_list").assertIsDisplayed()
        rule.shot("p05_universe_list")
        rule.onNodeWithTag("map_menu").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("list_toggle").performClick(); rule.waitForIdle()
        // insights
        rule.onNodeWithTag("mode_Insights").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("insights").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("insight").fetchSemanticsNodes().isNotEmpty())
        rule.shot("p06_insights")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun captureReviewAccept() {
        rule.example()
        rule.onNodeWithTag("capture_fab").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("capture").assertIsDisplayed()
        rule.captureJamie()
        rule.shotNode("capture", "p07_capture_suggestions")
        val before = rule.repo().snap.ofType(ObjType.PERSON).size
        rule.onNodeWithTag("accept_all").performClick(); rule.waitForIdle()
        assertEquals(before + 1, rule.repo().snap.ofType(ObjType.PERSON).size)
        assertTrue(rule.repo().snap.ofType(ObjType.TASK).any { it.title == "Expect updated estimate from Jamie Lee" })
        assertTrue(rule.repo().snap.pending().isEmpty())
        rule.onNodeWithText("Saved and connected.").assertExists()
        rule.shotNode("capture", "p08_capture_done")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun detailPageTabsAndDrawerNavigation() {
        rule.example()
        rule.onNodeWithTag("focal").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail").assertIsDisplayed()
        rule.shot("p09_project_page")
        rule.onNodeWithTag("tab_Tasks").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Compare contractor quotes").assertExists()
        rule.onNodeWithTag("tab_Timeline").performClick(); rule.waitForIdle()
        rule.shot("p10_project_timeline")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.onNodeWithTag("menu").performClick(); rule.waitForIdle()
        rule.shot("p11_drawer")
        rule.onNodeWithTag("nav_Tasks").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("list_TASK").assertIsDisplayed()
        rule.onNodeWithText("Order tile samples").assertExists()
        rule.onNodeWithTag("filter_Overdue").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Compare contractor quotes").assertDoesNotExist()
        rule.onNodeWithTag("filter_Open").performClick(); rule.waitForIdle()
        rule.shot("p12_tasks")
        rule.onNodeWithTag("menu").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("nav_Settings").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("settings").assertIsDisplayed()
        rule.shot("p13_settings")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun searchExactAndConnected() {
        rule.example()
        rule.onNodeWithContentDescription("Search").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("search_field").performTextInput("load-bearing"); rule.waitForIdle()
        rule.onNodeWithText("Site visit notes").assertExists()
        rule.onNodeWithText("Connected to those", substring = true).assertExists()
        rule.shot("p14_search")
    }

    @Test @Config(sdk = [36], qualifiers = "w344dp-h882dp-xxhdpi")
    fun coverScreen() {
        rule.example()
        rule.onNodeWithTag("universe").assertIsDisplayed()
        rule.shot("c01_cover_universe")
    }

    @Test @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun insightsAndReviewEmptyStates() {
        rule.example()
        rule.onNodeWithTag("bell").performClick(); rule.waitForIdle()
        rule.onNodeWithText("All caught up").assertExists()
        rule.shot("p15_review_empty")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WideUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test @Config(sdk = [36], qualifiers = "w880dp-h720dp-land-xhdpi")
    fun foldInnerLandscape() {
        rule.example()
        rule.onNodeWithTag("inspector").assertIsDisplayed()
        rule.onNodeWithTag("search_bar").assertIsDisplayed()
        rule.onNodeWithTag("nav_Universe").assertIsDisplayed()
        rule.shot("f01_fold_universe")
        rule.onNodeWithTag("tab_Sources").performClick(); rule.waitForIdle()
        rule.shot("f02_fold_sources")
        rule.onNodeWithTag("mode_Insights").performClick(); rule.waitForIdle()
        rule.shot("f03_fold_insights")
        rule.onNodeWithTag("mode_Map").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("nav_People").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Priya Shah").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("inspector").assertIsDisplayed()
        rule.shot("f04_fold_people_inspector")
        rule.onNodeWithTag("capture_fab").performClick(); rule.waitForIdle()
        rule.captureJamie()
        rule.shotNode("capture", "f05_fold_capture")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun foldInnerPortrait() {
        rule.example()
        rule.onNodeWithTag("universe").assertIsDisplayed()
        rule.shot("f06_fold_portrait")
    }

    @Test @Config(sdk = [36], qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet() {
        rule.example()
        rule.shot("t01_tablet_universe")
    }

    @Test @Config(sdk = [36], qualifiers = "w880dp-h720dp-land-xhdpi")
    fun stateSurvivesRecreate() {
        rule.example()
        rule.onNodeWithTag("cluster_PEOPLE").assertExists()
        rule.onAllNodesWithText("Priya").onFirst().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(800)
        rule.focalIs("Priya Shah")
        rule.activityRule.scenario.recreate(); rule.waitForIdle()
        rule.focalIs("Priya Shah")
    }
}
