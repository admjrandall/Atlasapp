package com.jmr.atlas

import androidx.test.core.app.ApplicationProvider
import com.jmr.atlas.data.Repository
import com.jmr.atlas.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Phase 2 of Journeys: shape + parts stored on a space; the space and its items are never touched. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JourneyTest {
    private lateinit var repo: Repository
    private lateinit var space: String
    private lateinit var items: List<String>
    @Before fun setUp() {
        repo = Repository(ApplicationProvider.getApplicationContext(), null)
        space = repo.create(KObject(type = ObjType.PROJECT, title = "Siding renovation", projectStatus = ProjectStatus.ACTIVE))
        items = listOf("Pine Ridge quote", "HOA rules").map { repo.create(KObject(type = ObjType.DOCUMENT, title = it)) }
        items.forEach { repo.link(it, space, RelType.BELONGS_TO) }
    }

    @Test fun aSpaceHasNoJourneyUntilGivenOne() {
        assertNull(Journeys.of(repo.snap, space))
    }

    @Test fun journeySavesAndReloadsInOrder() {
        val j = Journeys.new(space, Shape.TRAIL)
        repo.setJourney(j)
        val back = Journeys.of(repo.snap, space)!!
        assertEquals(Shape.TRAIL, back.shape)
        assertEquals(listOf("Research", "Plan", "Do it", "Finish"), back.parts.map { it.name })
        assertEquals(PartState.HERE, back.parts.first().state)
        assertEquals(0f, back.progress!!, 0.001f)
    }

    @Test fun reshapingKeepsItemsAndPartNames() {
        repo.setJourney(Journeys.new(space, Shape.TRAIL))
        val before = repo.snap.relationsOf(space).size
        repo.setJourney(Journeys.reshape(Journeys.of(repo.snap, space)!!, Shape.TREE))
        val j = Journeys.of(repo.snap, space)!!
        assertEquals(Shape.TREE, j.shape)
        assertEquals(listOf("Research", "Plan", "Do it", "Finish"), j.parts.map { it.name })
        assertTrue("states reset for shapes that never finish", j.parts.all { it.state == PartState.LATER })
        assertNull(j.progress)
        assertEquals(before, repo.snap.relationsOf(space).size)
        assertEquals(1, repo.snap.detailsOf(space, DetailKind.SHAPE).size)
    }

    @Test fun rememberKeepsItsStyle() {
        repo.setJourney(Journeys.new(space, Shape.REMEMBER, "GARDEN"))
        assertEquals("Memory Garden", Journeys.of(repo.snap, space)!!.styleLabel)
        repo.setJourney(Journeys.new(space, Shape.REMEMBER, "NOPE"))
        assertEquals("ROOM", Journeys.of(repo.snap, space)!!.style)
    }

    @Test fun removingAJourneyKeepsTheSpace() {
        repo.setJourney(Journeys.new(space, Shape.CLIMB))
        repo.clearJourney(space)
        assertNull(Journeys.of(repo.snap, space))
        assertNotNull(repo.snap.objects[space])
        assertEquals(2, repo.snap.relationsOf(space).size)
    }

    @Test fun partNamesAreCleaned() {
        val j = Journeys.new(space, Shape.TRAIL).let { it.copy(parts = it.parts.map { p -> p.copy(name = "Line\u0007break " + "x".repeat(60)) }) }
        repo.setJourney(j)
        assertTrue(Journeys.of(repo.snap, space)!!.parts.all { it.name.length <= 40 && '\u0007' !in it.name })
    }

    @Test fun exportCarriesTheJourney() {
        repo.setJourney(Journeys.new(space, Shape.VOYAGE))
        val json = org.json.JSONObject(repo.exportJson())
        val kinds = (0 until json.getJSONArray("details").length()).map { json.getJSONArray("details").getJSONObject(it).getString("kind") }
        assertTrue("SHAPE" in kinds)
        assertEquals(3, kinds.count { it == "PART" })
    }

    @Test fun itemsGoToHerePartUntilPlaced() {
        repo.setJourney(Journeys.new(space, Shape.TRAIL))
        val j = Journeys.of(repo.snap, space)!!
        assertEquals(2, JourneyItems.inPart(repo.snap, j, j.here!!.id).size)
        repo.placeInPart(space, items[1], j.parts[2].id)
        assertEquals(listOf("Pine Ridge quote"), JourneyItems.inPart(repo.snap, j, j.here!!.id).map { it.title })
        assertEquals(listOf("HOA rules"), JourneyItems.inPart(repo.snap, j, j.parts[2].id).map { it.title })
        repo.placeInPart(space, items[1], j.parts[1].id)
        assertEquals(1, repo.snap.detailsOf(space, DetailKind.PLACE).size)
    }

    @Test fun bundlesGroupByKindAndFoldDuplicates() {
        repo.setJourney(Journeys.new(space, Shape.TRAIL))
        repeat(6) { repo.link(repo.create(KObject(type = ObjType.IMAGE, title = "House front $it")), space, RelType.BELONGS_TO) }
        val j = Journeys.of(repo.snap, space)!!
        val b = JourneyItems.bundles(repo.snap, j, j.here!!.id)
        assertEquals(listOf("PHOTOS", "DOCS"), b.map { it.key })
        val photos = b.first()
        assertEquals(6, photos.items.size)
        assertEquals("near-duplicates fold to one", 1, photos.best.size)
        assertEquals(5, photos.folded)
    }

    @Test fun hiddenSourcesStayInTheSpace() {
        repo.setJourney(Journeys.new(space, Shape.TRAIL).copy(hidden = setOf("DOCS")))
        val j = Journeys.of(repo.snap, space)!!
        assertEquals(setOf("DOCS"), j.hidden)
        assertTrue(JourneyItems.bundles(repo.snap, j, j.here!!.id).isEmpty())
        assertEquals(2, JourneyItems.inSpace(repo.snap, space).size)
    }

    @Test fun shapePickFollowsTheSignals() {
        assertEquals(Shape.TRAIL, ShapePick.pick(repo.snap, space).shape)
        val dave = repo.create(KObject(type = ObjType.PROJECT, title = "Remembering Dave", projectStatus = ProjectStatus.ACTIVE))
        repo.link(repo.create(KObject(type = ObjType.LINK, title = "Obituary", body = "David Morales, 1961 – 2026")), dave, RelType.BELONGS_TO)
        assertEquals(Shape.REMEMBER, ShapePick.pick(repo.snap, dave).shape)
        val trip = repo.create(KObject(type = ObjType.PROJECT, title = "Oregon coast trip", projectStatus = ProjectStatus.ACTIVE))
        assertEquals(Shape.VOYAGE, ShapePick.pick(repo.snap, trip).shape)
        val k = repo.create(KObject(type = ObjType.PROJECT, title = "Which play kitchen", projectStatus = ProjectStatus.ACTIVE))
        assertEquals(Shape.CROSSROADS, ShapePick.pick(repo.snap, k).shape)
    }

    @Test fun suggestPartMatchesWords() {
        val j = Journeys.new(space, Shape.COUNTDOWN)
        assertEquals("Guests", JourneyItems.suggestPart(j, "Reply from the guests list")?.name)
        assertEquals(j.here ?: j.parts.first(), JourneyItems.suggestPart(j, "random thing"))
    }

    @Test fun templatesKeepPartsNotItems() {
        repo.setJourney(Journeys.new(space, Shape.TRAIL))
        repo.saveTemplate("Home renovation", Journeys.of(repo.snap, space)!!)
        val (name, t) = repo.templates().single()
        assertEquals("Home renovation", name)
        assertEquals(Shape.TRAIL, t.shape)
        assertEquals(listOf("Research", "Plan", "Do it", "Finish"), t.parts.map { it.name })
        repo.saveTemplate("Home renovation", Journeys.new(space, Shape.CLIMB))
        assertEquals(Shape.CLIMB, repo.templates().single().second.shape)
    }

    @Test fun sourcesCountByKind() {
        assertEquals(listOf("2 documents"), Sources.of(repo.snap, space).map { it.label })
    }
}
