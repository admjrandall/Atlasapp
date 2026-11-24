package com.jmr.atlas

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.Route
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Journeys, start to finish, as on the approved boards (Home → journey → part → Lens → item; edit; finish; start flow). */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class JourneyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun perms() = grantNotifications()

    private fun journeyOf(id: String): Journey? { var j: Journey? = null; rule.vm { j = Journeys.of(it.repo.snap, id) }; return j }

    @Test fun homeShowsJourneysAndOpensOne() {
        rule.example()
        rule.onAllNodesWithTag("home_journey").onFirst().performScrollTo().performClick(); rule.settle()
        rule.onNodeWithTag("journey").assertIsDisplayed()
        rule.onNodeWithText("Trail · stop 2 of 5", substring = true).assertExists()
        rule.onNodeWithTag("part_sheet").assertExists()
        rule.onNodeWithText("YOU ARE HERE · STOP 2", substring = true).assertExists()
        rule.shot("j01_trail")
    }

    @Test fun partBundleOpensLensAndViewer() {
        rule.example()
        val pid = rule.idOf("Home Renovation")
        rule.vm { it.go(Route.Journey(pid)) }
        rule.onNodeWithTag("stack_DOCS").performClick(); rule.settle()
        rule.onNodeWithTag("lens").assertIsDisplayed()
        rule.onAllNodesWithTag("lens_tile").onFirst().performClick(); rule.settle()
        rule.onNodeWithTag("lens_viewer").assertIsDisplayed()
        rule.onNodeWithText("PULLED OUT FOR YOU").assertExists()
        rule.onNodeWithText("$48,200").assertExists()
        rule.shot("j02_lens_pdf")
        rule.tap("lens_back"); rule.onNodeWithTag("lens_viewer").assertDoesNotExist()
        rule.tap("lens_close"); rule.onNodeWithTag("lens").assertDoesNotExist()
    }

    @Test fun movingOnAndUndo() {
        rule.example()
        val pid = rule.idOf("Home Renovation")
        rule.vm { it.go(Route.Journey(pid)) }
        rule.tap("part_action")
        assertEquals("Demolition", journeyOf(pid)!!.here!!.name)
        rule.onNodeWithText("Undo").performClick(); rule.settle()
        assertEquals("Quotes", journeyOf(pid)!!.here!!.name)
    }

    @Test fun editorRenamesAddsReshapesAndKeepsItems() {
        rule.example()
        val pid = rule.idOf("Home Renovation")
        var items = 0; rule.vm { items = JourneyItems.inSpace(it.repo.snap, pid).size }
        rule.vm { it.go(Route.Journey(pid)) }
        rule.tap("journey_edit"); rule.onNodeWithTag("journey_editor").assertIsDisplayed()
        rule.onNodeWithTag("edit_add").performScrollTo().performClick(); rule.settle()
        assertEquals(6, journeyOf(pid)!!.parts.size)
        rule.onNodeWithTag("shape_TREE").performScrollTo().performClick(); rule.settle()
        assertEquals(Shape.TREE, journeyOf(pid)!!.shape)
        var after = 0; rule.vm { after = JourneyItems.inSpace(it.repo.snap, pid).size }
        assertEquals(items, after)
        rule.onNodeWithTag("save_template").performScrollTo().performClick(); rule.settle()
        var t = 0; rule.vm { t = it.repo.templates().size }; assertEquals(1, t)
        rule.shot("j03_editor")
        rule.tap("editor_done"); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onNodeWithTag("journey_editor").assertDoesNotExist()
    }

    @Test fun startFlowFromTheSpaceSwitch() {
        rule.example()
        val gid = rule.idOf("Garden Redesign")
        rule.vm { it.go(Route.Space("p:$gid")) }
        rule.onNodeWithTag("view_Journey").performScrollTo().performClick(); rule.settle()
        rule.onNodeWithTag("journey_start").assertIsDisplayed()
        rule.onNodeWithTag("shape_reason").assertExists()
        rule.shot("j04_start_shape")
        rule.onNodeWithTag("pick_CLIMB").performScrollTo().performClick(); rule.settle()
        rule.onNodeWithTag("use_shape").performScrollTo().performClick(); rule.settle()
        rule.onNodeWithTag("open_journey").performScrollTo().performClick(); rule.settle()
        assertEquals(Shape.CLIMB, journeyOf(gid)!!.shape)
        rule.onNodeWithTag("journey").assertIsDisplayed()
        rule.vm { it.back() }
        rule.onNodeWithTag("journey_start").assertDoesNotExist()
    }

    @Test fun brandNewJourneyAndRememberStyles() {
        rule.example()
        rule.vm { it.go(Route.JourneyStart(null)) }
        rule.onNodeWithTag("new_name").performTextInput("Remembering Dave, obituary")
        rule.settle()
        rule.onNodeWithTag("make_journey").performScrollTo().performClick(); rule.settle()
        var id = ""; rule.vm { id = it.repo.snap.objects.values.first { o -> o.title.startsWith("Remembering Dave") }.id }
        assertEquals(Shape.REMEMBER, journeyOf(id)!!.shape)
        rule.onNodeWithTag("style_GARDEN").performClick(); rule.settle()
        assertEquals("GARDEN", journeyOf(id)!!.style)
        rule.onNodeWithTag("style_LETTER").performClick(); rule.settle()
        rule.shot("j05_remember_letter")
    }

    @Test fun finishingATrailKeepsItAsAMemory() {
        rule.example()
        val pid = rule.idOf("Home Renovation")
        rule.vm { it.repo.setJourney(Journeys.of(it.repo.snap, pid)!!.let { j -> j.copy(parts = j.parts.mapIndexed { i, p -> p.copy(state = if (i == j.parts.lastIndex) PartState.HERE else PartState.DONE) }) }) }
        rule.vm { it.go(Route.Journey(pid)) }
        rule.tap("part_action")
        rule.onNodeWithTag("journey_finish").assertIsDisplayed()
        rule.shot("j06_finish")
        rule.tap("finish_confirm")
        var st: ProjectStatus? = null; rule.vm { st = it.repo.snap.objects[pid]!!.projectStatus }
        assertEquals(ProjectStatus.COMPLETED, st)
    }
}
