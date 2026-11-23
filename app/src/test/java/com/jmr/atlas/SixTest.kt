package com.jmr.atlas

import androidx.test.core.app.ApplicationProvider
import com.jmr.atlas.capture.Sentence
import com.jmr.atlas.data.Board
import com.jmr.atlas.data.Briefing
import com.jmr.atlas.data.ExampleWorkspace
import com.jmr.atlas.data.Repository
import com.jmr.atlas.integrations.Ai
import com.jmr.atlas.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** 0.6: bands, the timeline, the briefing sentence, the sentence parser and the AI output validators. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SixTest {
    private lateinit var repo: Repository
    private val today = LocalDate.of(2026, 11, 23)   // a Monday
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun id(t: String) = repo.snap.live.first { it.title == t }.id
    private val home get() = id("Home Renovation")

    @Before fun setUp() { repo = Repository(ctx, null); ExampleWorkspace.seed(repo, today) }

    @Test fun bandsGroupByPersonAndPutLateFirst() {
        val bands = Board.bands(repo.snap, home, today)
        fun bandOf(t: String) = bands.first { b -> b.items.any { it.title == t } || b.anchor?.title == t }
        assertEquals("Jamie Carter", bandOf("Contractor estimate").anchor?.title)
        assertEquals("Jamie Carter", bandOf("Expect updated estimate from Jamie Carter").anchor?.title)
        assertEquals("Priya Shah", bandOf("Floor plan v3").anchor?.title)
        assertEquals("Priya Shah", bandOf("Finalize kitchen layout with Priya").anchor?.title)
        // one step away: the permit is linked to the inspection, which is Marco's
        assertEquals("Marco Ruiz", bandOf("Building permit application").anchor?.title)
        // linked to nobody → one shared band; it holds the late task, so it comes first
        val rest = bandOf("Order tile samples")
        assertEquals(Board.REST, rest.key)
        assertSame(rest, bands.first())
        assertTrue(rest.late)
        assertEquals("Order tile samples · 1 day late", rest.status)
        // people with nothing of their own share the last band
        assertEquals(Board.PEOPLE_BAND, bands.last().key)
        assertEquals(listOf("Lena Brooks"), bands.last().items.map { it.title })
        // done tasks and milestones are not tiles
        assertTrue(bands.none { b -> b.items.any { it.title == "Measure hallway for skylight" || it.type == ObjType.EVENT } })
        // stable: same data, same layout
        assertEquals(bands.map { it.key to it.items.map { o -> o.id } }, Board.bands(repo.snap, home, today).map { it.key to it.items.map { o -> o.id } })
    }

    @Test fun bandNamesYouSetWinAndStick() {
        val jamie = id("Jamie Carter")
        assertEquals("With Jamie", Board.bands(repo.snap, home, today).first { it.key == jamie }.name)
        repo.putDetail(Detail(objectId = home, kind = DetailKind.BAND, label = jamie, value = "Contractor & demolition", origin = Origin.AI))
        assertEquals(Origin.AI, Board.bands(repo.snap, home, today).first { it.key == jamie }.nameOrigin)
        val d = repo.snap.detailsOf(home, DetailKind.BAND).first()
        repo.putDetail(d.copy(value = "Carter Build", origin = Origin.USER))
        val b = Board.bands(repo.snap, home, today).first { it.key == jamie }
        assertEquals("Carter Build" to Origin.USER, b.name to b.nameOrigin)
    }

    @Test fun timelineAndCover() {
        val p = repo.snap.objects[home]!!
        val marks = Board.timeline(repo.snap, p, today)
        assertEquals(listOf("Started", "Design approved", "Demolition starts", "Kitchen install", "Done"), marks.map { it.title })
        assertTrue(marks[1].past && !marks[2].past)
        val c = Board.cover(repo.snap, p, today)
        assertTrue(c.line, c.line.startsWith("Demolition starts 14 Dec"))
        assertTrue(c.late)
        assertEquals(3, c.people.size)
    }

    @Test fun specialSpaces() {
        val tasks = Board.kindBands(repo.snap, Board.Kind.TASKS, today)
        assertTrue(tasks.first { it.name == "Late" }.items.any { it.title == "Order tile samples" })
        assertTrue(tasks.first { it.name == "Waiting on others" }.items.any { it.title.startsWith("Expect updated estimate") })
        val people = Board.kindBands(repo.snap, Board.Kind.PEOPLE, today)
        assertTrue(people.first { it.name == "Home Renovation" }.items.any { it.title == "Priya Shah" })
        assertTrue(Board.kindBands(repo.snap, Board.Kind.EVERYTHING, today).map { it.name }.containsAll(listOf("People", "Tasks", "Files", "Ideas & notes")))
    }

    @Test fun briefingSentenceIsSpecificAndSourced() {
        val segs = Briefing.sentence(repo.snap, today)
        val text = segs.joinToString("") { it.text }
        assertTrue(text, text.contains("Order tile samples is 1 day late."))
        assertTrue(text, text.contains("is due today."))
        assertTrue(text, text.contains("Jamie owes you an updated estimate Thursday."))
        assertTrue(text, text.contains("Demolition starts in 3 weeks."))
        // every entity is tappable and points at something real
        assertTrue(segs.filter { it.id != null }.all { s -> s.id!!.startsWith("space:") || repo.snap.objects.containsKey(s.id) })
        assertEquals("space:$home", segs.first { it.text == "Demolition starts" }.id)
        val week = Briefing.week(repo.snap, today)
        assertTrue(week.first().late)
        assertTrue(week.any { it.waiting && it.whenText.contains("waiting on Jamie") })
    }

    @Test fun sentenceParser() {
        val s = repo.snap
        val a = Sentence.parse("Order grout by Friday for the kitchen", s, today)
        assertEquals(Sentence.Kind.TASK, a.kind); assertEquals("Order grout", a.title)
        assertEquals(LocalDate.of(2026, 11, 27), a.due); assertEquals("Home Renovation", a.space?.title); assertFalse(a.question)
        val b = Sentence.parse("Call Jamie tomorrow", s, today)
        assertEquals(LocalDate.of(2026, 11, 24), b.due); assertEquals(listOf("Jamie Carter"), b.people.map { it.title }); assertEquals("Home Renovation", b.space?.title)
        assertTrue(Sentence.parse("What could delay demolition?", s, today).question)
        assertTrue(Sentence.parse("what's late", s, today).question)
        assertFalse(Sentence.parse("Do laundry", s, today).question)
        val c = Sentence.parse("Idea: heated towel rail", s, today)
        assertEquals(Sentence.Kind.IDEA to "Heated towel rail", c.kind to c.title)
        val d = Sentence.parse("Priya liked the walnut shelves", s, today)
        assertEquals(Sentence.Kind.NOTE, d.kind); assertEquals(listOf("Priya Shah"), d.people.map { it.title })
        assertEquals(LocalDate.of(2026, 12, 9), Sentence.parse("Dentist on Dec 9", s, today).due)
        assertEquals(LocalDate.of(2026, 12, 4), Sentence.parse("Pay the plumber next Friday", s, today).due)
        // the space you're in wins
        assertEquals("Garden Redesign", Sentence.parse("Buy mulch", s, today, id("Garden Redesign")).space?.title)
        assertEquals("delay demolition", Sentence.searchTerms("What could delay demolition?"))
    }

    @Test fun aiOutputIsValidated() {
        val items = listOf(KObject(type = ObjType.TASK, title = "A"), KObject(type = ObjType.TASK, title = "B"), KObject(type = ObjType.NOTE, title = "C"))
        val a = Ai.parseAnswer("""noise {"answer":"Two things.\u0007","items":[2,9,1,2]} trailing""", items)
        assertEquals("Two things.", a.text)
        assertEquals(listOf(items[1].id, items[0].id), a.ids)
        assertEquals(listOf("Kitchen design", null, "Permits"), Ai.parseNames("""{"names":["kitchen design","<script>alert(1)</script>","Permits"]}""", 3))
        assertEquals(listOf(null, null), Ai.parseNames("not json", 2))
    }
}
