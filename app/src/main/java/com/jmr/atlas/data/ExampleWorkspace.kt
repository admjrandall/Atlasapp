package com.jmr.atlas.data

import com.jmr.atlas.model.*
import java.time.LocalDate

/** Optional sample universe, every object marked Origin.EXAMPLE and removable in one tap. Example files carry no bytes. */
object ExampleWorkspace {
    fun seed(repo: Repository, today: LocalDate = LocalDate.now()) {
        val ex = Origin.EXAMPLE
        fun o(t: ObjType, title: String, body: String = "", f: KObject.() -> KObject = { this }) = KObject(type = t, title = title, body = body, origin = ex).f()
        val home = o(ObjType.PROJECT, "Home Renovation", "Kitchen, living room and hallway. Keep the original oak floors; open the kitchen to the living room.") {
            copy(projectStatus = ProjectStatus.ACTIVE, startDate = today.minusWeeks(6), targetDate = today.plusMonths(4), pinned = true) }
        val garden = o(ObjType.PROJECT, "Garden Redesign", "Native planting, a small patio and better drainage along the fence.") { copy(projectStatus = ProjectStatus.PLANNING) }
        val office = o(ObjType.PROJECT, "Basement Office", "Quiet room for work calls. Needs egress window and dehumidifier.") { copy(projectStatus = ProjectStatus.ON_HOLD) }
        val jamie = o(ObjType.PERSON, "Jamie Carter") { copy(role = "General contractor", org = "Carter Build Co.") }
        val priya = o(ObjType.PERSON, "Priya Shah") { copy(role = "Architect", org = "Shah Studio") }
        val marco = o(ObjType.PERSON, "Marco Ruiz") { copy(role = "Electrician") }
        val lena = o(ObjType.PERSON, "Lena Brooks") { copy(role = "Interior designer") }
        val plan = o(ObjType.DOCUMENT, "Floor plan v3", "Revised layout from Priya: island moved 40 cm toward the window.") { copy(mime = "application/pdf") }
        val estimate = o(ObjType.DOCUMENT, "Contractor estimate", "First estimate from Carter Build Co. Excludes electrical.") { copy(mime = "application/pdf") }
        val permit = o(ObjType.DOCUMENT, "Building permit application") { copy(mime = "application/pdf") }
        val mood = o(ObjType.IMAGE, "Kitchen inspiration", "Warm wood, white oak shelves, matte tiles.") { copy(mime = "image/jpeg") }
        val shelves = o(ObjType.IDEA, "Open shelving instead of upper cabinets")
        val skylight = o(ObjType.IDEA, "Skylight in the hallway", "Hallway is the darkest part of the house.")
        val floor = o(ObjType.IDEA, "Heated bathroom floor")
        val quotes = o(ObjType.TASK, "Compare contractor quotes") { copy(due = today.plusDays(2), priority = Priority.HIGH) }
        val layout = o(ObjType.TASK, "Finalize kitchen layout with Priya") { copy(due = today.plusDays(5)) }
        val tiles = o(ObjType.TASK, "Order tile samples") { copy(due = today.minusDays(1)) }
        val inspect = o(ObjType.TASK, "Book electrical inspection") { copy(due = today.plusDays(12), blocked = true) }
        val measure = o(ObjType.TASK, "Measure hallway for skylight") { copy(done = true, completedAt = System.currentTimeMillis() - 3 * 86_400_000L) }
        val design = o(ObjType.EVENT, "Design approved") { copy(eventDate = today.minusWeeks(3)) }
        val demo = o(ObjType.EVENT, "Demolition starts") { copy(eventDate = today.plusWeeks(3)) }
        val install = o(ObjType.EVENT, "Kitchen install") { copy(eventDate = today.plusMonths(2)) }
        val note = o(ObjType.NOTE, "Site visit notes", "Jamie confirmed the wall between kitchen and living room is not load-bearing. Marco wants to upgrade the panel to 200A before anything else.")
        val renos = o(ObjType.COLLECTION, "House")

        val all = listOf(home, garden, office, jamie, priya, marco, lena, plan, estimate, permit, mood, shelves, skylight, floor, quotes, layout, tiles, inspect, measure, design, demo, install, note, renos)
        all.forEach { repo.create(it) }
        fun l(a: KObject, b: KObject, t: RelType = RelType.RELATED_TO, review: Review = Review.CONFIRMED) = repo.link(a.id, b.id, t, ex, review)
        listOf(jamie, priya, marco, lena).forEach { l(it, home) }
        listOf(plan, estimate, permit, mood, shelves, skylight, floor, quotes, layout, tiles, inspect, measure, design, demo, install, note).forEach { l(it, home, RelType.BELONGS_TO) }
        l(home, garden); l(home, office)
        l(plan, priya, RelType.DERIVED_FROM); l(estimate, jamie, RelType.DERIVED_FROM); l(quotes, estimate, RelType.REFERENCES)
        l(layout, priya, RelType.ASSIGNED_TO); l(inspect, marco, RelType.ASSIGNED_TO); l(inspect, permit, RelType.DEPENDS_ON)
        l(note, jamie, RelType.DISCUSSED_WITH); l(note, marco, RelType.DISCUSSED_WITH); l(mood, shelves, RelType.SUPPORTS); l(skylight, measure)
        l(home, renos, RelType.PART_OF); l(garden, renos, RelType.PART_OF); l(office, renos, RelType.PART_OF)
        l(lena, mood, review = Review.PROPOSED)
    }
}
