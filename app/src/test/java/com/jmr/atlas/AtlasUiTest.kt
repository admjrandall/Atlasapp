package com.jmr.atlas

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jmr.atlas.capture.Sentence.Intent
import com.jmr.atlas.data.Board
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.Turn
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 0.7: the Atlas box (find, ask, chat, save), Now, All spaces, kind lists and the state colors. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AtlasUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun perms() = grantNotifications()
    private fun has(tag: String) = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun routesWhatYouType() {
        rule.example()
        rule.vm { vm ->
            assertEquals(Intent.FIND, vm.intentOf("Priya"))
            assertEquals(Intent.FIND, vm.intentOf("garden"))
            assertEquals(Intent.ASK_WORLD, vm.intentOf("Why is the sky blue?"))
            assertEquals(Intent.ASK_MINE, vm.intentOf("What's late?"))
            assertEquals(Intent.ASK_MINE, vm.intentOf("What could delay demolition?"))
            assertEquals(Intent.CAPTURE, vm.intentOf("Call Priya Friday"))
            assertEquals(Intent.CAPTURE, vm.intentOf("Idea: a reading nook"))
            assertEquals(Intent.CAPTURE, vm.intentOf("New space Boat"))
        }
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun aNameFindsThings() {
        rule.example()
        rule.type("Priya")
        rule.onNodeWithTag("find_results").assertIsDisplayed()
        rule.onNodeWithTag("best_match", useUnmergedTree = true).assert(hasAnyDescendant(hasText("Priya Shah")))
        assertFalse("typing a name must not offer to save first", has("catch_preview"))
        rule.shot("a01_find_priya")
        val before = rule.repo().snap.live.size
        rule.tapDesc("Open")
        assertEquals(before, rule.repo().snap.live.size)
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun aNameFindsThingsCover() {
        rule.example()
        rule.type("tile")
        rule.onNodeWithTag("find_results").assertIsDisplayed()
        rule.shot("a02_find_cover")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun generalQuestionWithoutAModel() {
        rule.example()
        rule.type("Why is the sky blue?")
        rule.onNodeWithTag("question_hint").assertIsDisplayed()
        rule.tapDesc("Ask")
        rule.onNodeWithTag("world", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("setup_ai", useUnmergedTree = true).assertExists()
        rule.shot("a03_world_no_model")
        rule.onNodeWithText("Save the question as a note").performClick(); rule.settle()
        assertTrue(rule.repo().snap.live.any { it.type == ObjType.NOTE && it.title == "Why is the sky blue?" })
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun answerFilesIntoASpace() {
        rule.example()
        // what a model answer looks like in the thread (the model itself can't run under Robolectric)
        rule.vm { it.thread.value = listOf(Turn.You(text = "How deep should garden beds be?"),
            Turn.World(question = "How deep should garden beds be?", text = "Most vegetables do well in beds 30 cm deep.", thinking = false,
                offers = listOf(rule.idOf("Garden Redesign")))) }
        rule.tap("catch_open")
        rule.onNodeWithTag("save_offers", useUnmergedTree = true).assertExists()
        rule.shot("a04_world_offers")
        rule.vm { assertTrue(it.inChat); assertEquals(Intent.CAPTURE, it.intentOf("add to Garden Redesign")) }
        rule.onNodeWithText("Save to Garden Redesign", substring = true).performClick(); rule.settle()
        val note = rule.repo().snap.live.first { it.type == ObjType.NOTE && it.title == "How deep should garden beds be?" }
        assertTrue(rule.repo().snap.neighbours(note.id).any { it.first.title == "Garden Redesign" })
        assertTrue(has("did"))
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun threadSaysWhatItDidAndUndoes() {
        rule.example()
        rule.type("Call the plumber tomorrow\n")
        assertTrue(rule.repo().snap.live.any { it.title == "Call the plumber" })
        rule.onNodeWithTag("did", useUnmergedTree = true).assertExists()
        rule.shot("a05_thread_did")
        rule.tap("undo_turn")
        assertFalse(rule.repo().snap.live.any { it.title == "Call the plumber" })
        rule.tap("clear_thread")
        rule.vm { assertTrue(it.thread.value.isEmpty()) }
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun nowHasTheCoreStripAndPinnedSpaces() {
        rule.example()
        rule.onNodeWithTag("core_strip").assertExists()
        rule.onNodeWithTag("core_People", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("cover").onFirst().assertExists()
        rule.shot("a06_now")
        rule.onNodeWithTag("all_spaces").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.settle()
        rule.onNodeWithTag("all_spaces_screen").assertIsDisplayed()
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun nowOnTheCover() {
        rule.example()
        rule.onNodeWithTag("core_strip").assertExists()
        rule.shot("a07_now_cover")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun allSpacesPinsFiltersAndRecolors() {
        rule.example()
        rule.vm { it.go(Route.Space(Board.SPACES_KEY)) }
        rule.onNodeWithTag("all_spaces_screen").assertIsDisplayed()
        rule.shot("a08_all_spaces")
        val basement = rule.idOf("Basement Office")
        val was = rule.repo().snap.objects[basement]!!.pinned
        rule.onAllNodesWithTag("pin_Basement Office").onFirst().performClick(); rule.settle()
        assertNotEquals(was, rule.repo().snap.objects[basement]!!.pinned)
        rule.onNodeWithTag("filter").performTextInput("gard"); rule.settle()
        rule.onNodeWithTag("space_row_Garden Redesign").assertExists()
        rule.onNodeWithContentDescription("Change color and icon of Garden Redesign", useUnmergedTree = true).performClick(); rule.settle()
        rule.onNodeWithContentDescription("rose", substring = true).performClick()
        rule.onNodeWithContentDescription("garden", substring = true).performClick()
        rule.onAllNodes(isDialog()).onFirst().captureToImage()
        rule.tap("look_save")
        val look = rule.repo().snap.detailsOf(rule.idOf("Garden Redesign"), DetailKind.LOOK).single().value
        assertEquals("rose:garden", look)
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun allSpacesCover() {
        rule.example()
        rule.vm { it.go(Route.Space(Board.SPACES_KEY)) }
        rule.shot("a09_all_spaces_cover")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun peopleReadAsAList() {
        rule.example()
        rule.vm { it.go(Route.Space("people")) }
        rule.onNodeWithTag("view_toggle").assertExists()
        rule.tap("view_List")
        rule.onNodeWithTag("kind_list").assertIsDisplayed()
        rule.onNodeWithText("Priya Shah").assertExists()
        rule.shot("a10_people_list")
        rule.onNodeWithTag("filter").performTextInput("mar"); rule.settle()
        rule.onNodeWithText("Marco Ruiz").assertExists()
        rule.onAllNodesWithText("Priya Shah").assertCountEquals(0)
        rule.tap("view_By space")
        rule.onNodeWithTag("space").assertExists()
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun colorIsState() {
        rule.example()
        val today = java.time.LocalDate.now()
        val s = rule.repo().snap
        val late = s.ofType(ObjType.TASK).firstOrNull { !it.done && Board.effective(it)?.isBefore(today) == true }
        if (late != null) assertEquals(com.jmr.atlas.ui.theme.Atlas.LateBg, com.jmr.atlas.ui.six.stateTint(late, today)?.first)
        val idea = s.ofType(ObjType.IDEA).first()
        assertNull("ideas aren't colored by type any more", com.jmr.atlas.ui.six.stateTint(idea, today))
        rule.vm { it.go(Route.Space("tasks")) }
        rule.shot("a11_tasks_state_colors")
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.shot("a12_board_state_colors")
    }

    // ---------------------------------------------------------------- 0.7.1

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun cardsOpenOnTapAndShowLinksOnLongPress() {
        rule.example()
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.shot("b01_board_cards")
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performClick(); rule.settle()
        rule.onNodeWithTag("item_page").assertIsDisplayed()
        rule.tap("go_home")
        rule.onNodeWithTag("home").assertIsDisplayed()
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.onNodeWithContentDescription("Idea: Open shelving instead of upper cabinets").performTouchInput { longClick() }; rule.settle()
        rule.onNodeWithTag("selection_bar").assertIsDisplayed()
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun cardsOnTheCover() {
        rule.example()
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.shot("b02_board_cards_cover")
        rule.vm { it.go(Route.Space("files")) }
        rule.shot("b03_files_cover")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun tickingATaskOnItsCard() {
        rule.example()
        rule.vm { it.go(Route.Space("tasks")) }
        val id = rule.idOf("Order tile samples")
        rule.onAllNodesWithTag("tile_check", useUnmergedTree = true).onFirst().performClick(); rule.settle()
        assertTrue(rule.repo().snap.objects.values.any { it.type == ObjType.TASK && it.done && it.title == "Order tile samples" } || rule.repo().snap.objects[id]!!.done)
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun tasksHaveAFilter() {
        rule.example()
        rule.vm { it.go(Route.Space("tasks")) }
        rule.onNodeWithTag("filter").performTextInput("tile"); rule.settle()
        rule.onNodeWithTag("kind_list").assertIsDisplayed()
        rule.onNodeWithText("Order tile samples").assertExists()
        rule.shot("b04_tasks_filter")
    }

    @Test fun fileKinds() {
        assertEquals("PDF", com.jmr.atlas.ui.six.FileKind.of("application/pdf").label)
        assertEquals("DOC", com.jmr.atlas.ui.six.FileKind.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document").label)
        assertEquals("XLS", com.jmr.atlas.ui.six.FileKind.of("", "budget.xlsx").label)
        assertEquals("PPT", com.jmr.atlas.ui.six.FileKind.of("", "deck.pptx").label)
        assertEquals("ZIP", com.jmr.atlas.ui.six.FileKind.of("application/zip").label)
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun aFilePageLeadsWithTheFile() {
        rule.example()
        rule.vm { it.projectSpace(rule.idOf("Home Renovation")) }
        rule.vmOpen(rule.idOf("Floor plan v3"))
        rule.onNodeWithTag("file_preview").assertExists()
        rule.onNodeWithTag("file_name").assert(hasText("Floor plan v3"))
        rule.onNodeWithTag("file_open").assertExists()
        rule.onAllNodesWithText("Not found", substring = true).assertCountEquals(0)
        rule.onNodeWithTag("go_home").assertExists()
        rule.shot("b05_file_page")
    }

    @Test @Config(sdk = [36], qualifiers = "w369dp-h905dp-xxhdpi")
    fun aFilePageCover() {
        rule.example()
        rule.vmOpen(rule.idOf("Contractor estimate"))
        rule.shot("b06_file_page_cover")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun swipeDownFoldsTheConversation() {
        rule.example()
        rule.type("Call the plumber tomorrow\n")
        rule.onNodeWithTag("panel_header").performTouchInput { swipeDown(startY = centerY, endY = centerY + 600f, durationMillis = 300) }; rule.settle()
        rule.onNodeWithTag("show_thread").assertIsDisplayed()
        rule.shot("b07_thread_folded")
        rule.tap("show_thread")
        rule.onNodeWithTag("thread").assertExists()
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun longConversationKeepsTheInputOnScreen() {
        rule.example()
        rule.vm { vm -> vm.thread.value = (1..12).flatMap { i -> listOf(Turn.You(text = "Question number $i about something long enough to wrap"),
            Turn.World(question = "q$i", text = "An answer that runs a couple of lines so the conversation is taller than the screen. ".repeat(2), thinking = false)) } +
            Turn.You(text = "Here's the estimate", files = listOf("Carter estimate.pdf", "kitchen.jpg")) }
        rule.tap("catch_open")
        rule.onNodeWithTag("catch_input").assertIsDisplayed()
        rule.onNodeWithText("Carter estimate.pdf").assertExists()
        rule.onAllNodesWithText("1 attachment").assertCountEquals(0)
        rule.shot("b08_long_thread")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun searchOffersPhoneContacts() {
        rule.example()
        rule.type("Morgan")
        rule.onNodeWithTag("find_results").assertExists()
        // without the permission, a name search offers to include the phone's contacts
        rule.onNodeWithTag("allow_contacts", useUnmergedTree = true).assertExists()
        rule.shot("b09_find_contacts")
    }

    @Test @Config(sdk = [36], qualifiers = "w707dp-h823dp-port-xhdpi")
    fun peopleCanComeFromContacts() {
        rule.example()
        rule.vm { it.go(Route.Space("people")) }
        rule.onNodeWithTag("from_contacts").assertExists()
    }
}
