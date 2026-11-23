package com.jmr.atlas.data

import com.jmr.atlas.model.*
import java.time.LocalDate

/** Optional sample universe, every object marked Origin.EXAMPLE and removable in one tap. Example files carry no bytes. */
object ExampleWorkspace {
    fun seed(repo: Repository, today: LocalDate = LocalDate.now()) {
        val ex = Origin.EXAMPLE
        fun o(t: ObjType, title: String, body: String = "", f: KObject.() -> KObject = { this }) = KObject(type = t, title = title, body = body, origin = ex).f()
        val home = o(ObjType.PROJECT, "Home Renovation", "Kitchen, living room and hallway. Keep the original oak floors; open the kitchen to the living room.") {
            copy(projectStatus = ProjectStatus.ACTIVE, startDate = today.minusWeeks(6), targetDate = today.plusMonths(4), pinned = true, health = Health.ON_TRACK) }
        val garden = o(ObjType.PROJECT, "Garden Redesign", "Native planting, a small patio and better drainage along the fence.") { copy(projectStatus = ProjectStatus.PLANNING) }
        val office = o(ObjType.PROJECT, "Basement Office", "Quiet room for work calls. Needs egress window and dehumidifier.") { copy(projectStatus = ProjectStatus.ON_HOLD) }
        val jamie = o(ObjType.PERSON, "Jamie Carter", "Contractor for the kitchen and living room.") { copy(role = "General contractor", org = "Carter Build Co.", email = "jamie@carterbuild.example", phone = "(555) 014-2290") }
        val priya = o(ObjType.PERSON, "Priya Shah") { copy(role = "Architect", org = "Shah Studio") }
        val marco = o(ObjType.PERSON, "Marco Ruiz") { copy(role = "Electrician") }
        val lena = o(ObjType.PERSON, "Lena Brooks") { copy(role = "Interior designer") }
        val plan = o(ObjType.DOCUMENT, "Floor plan v3", "Revised layout from Priya: island moved 40 cm toward the window.") { copy(mime = "application/pdf") }
        val estimate = o(ObjType.DOCUMENT, "Contractor estimate", "First estimate from Carter Build Co. Excludes electrical.") {
            copy(mime = "application/pdf", kind = "Estimate", docDate = today.minusDays(4), sizeBytes = 188_416,
                extracted = "CARTER BUILD CO. ESTIMATE EST-2026-0118\nKitchen demolition, cabinet allowance revised per site visit, drywall and finish.\nExcludes electrical.\nTotal \$48,200\nValid until ${today.plusDays(39)}") }
        val permit = o(ObjType.DOCUMENT, "Building permit application") { copy(mime = "application/pdf") }
        val mood = o(ObjType.IMAGE, "Kitchen inspiration", "Warm wood, white oak shelves, matte tiles.") { copy(mime = "image/jpeg") }
        val shelves = o(ObjType.IDEA, "Open shelving instead of upper cabinets")
        val skylight = o(ObjType.IDEA, "Skylight in the hallway", "Hallway is the darkest part of the house.")
        val floor = o(ObjType.IDEA, "Heated bathroom floor")
        val quotes = o(ObjType.TASK, "Compare contractor quotes", "Make sure both quotes include demo haul-away.") { copy(due = today, deadline = today.plusDays(5), priority = Priority.HIGH) }
        val waiting = o(ObjType.TASK, "Expect updated estimate from Jamie Carter") { copy(due = today.plusDays(3)) }
        val insurance = o(ObjType.TASK, "Call insurance about roof claim") { copy(due = today) }
        val link = o(ObjType.LINK, "12 kitchen layouts that work", "For the island idea — check the 42-inch clearance against Priya's plan.") {
            copy(url = "https://houseideas.example/kitchen-layouts", site = "houseideas.example", kind = "Reference", readMinutes = 8,
                summary = "A tour of galley, L-shape, U-shape and island layouts, with clearances and work-triangle tips.") }
        val oak = o(ObjType.DECISION, "Keep the original oak floors")
        val layout = o(ObjType.TASK, "Finalize kitchen layout with Priya") { copy(due = today.plusDays(5)) }
        val tiles = o(ObjType.TASK, "Order tile samples") { copy(due = today.minusDays(1)) }
        val inspect = o(ObjType.TASK, "Book electrical inspection") { copy(due = today.plusDays(12), blocked = true) }
        val measure = o(ObjType.TASK, "Measure hallway for skylight") { copy(done = true, completedAt = System.currentTimeMillis() - 3 * 86_400_000L) }
        val design = o(ObjType.EVENT, "Design approved") { copy(eventDate = today.minusWeeks(3)) }
        val demo = o(ObjType.EVENT, "Demolition starts") { copy(eventDate = today.plusWeeks(3)) }
        val install = o(ObjType.EVENT, "Kitchen install") { copy(eventDate = today.plusMonths(2)) }
        val note = o(ObjType.NOTE, "Site visit notes", "Jamie confirmed the wall between kitchen and living room is not load-bearing. Marco wants to upgrade the panel to 200A before anything else.")
        val renos = o(ObjType.COLLECTION, "House")

        val all = listOf(home, garden, office, jamie, priya, marco, lena, plan, estimate, permit, mood, shelves, skylight, floor, quotes, waiting, insurance, link, oak, layout, tiles, inspect, measure, design, demo, install, note, renos)
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
        l(waiting, jamie); l(waiting, home, RelType.BELONGS_TO); l(quotes, jamie); l(link, home, RelType.BELONGS_TO); l(link, shelves); l(oak, home, RelType.BELONGS_TO); l(insurance, renos, RelType.BELONGS_TO)

        // details shaped like the item cards (design I01–I11)
        fun d(o: KObject, k: DetailKind, label: String, value: String, type: FieldType = FieldType.TEXT, flag: Boolean = false, extra: String = "") =
            Detail(objectId = o.id, kind = k, label = label, value = value, type = type, flag = flag, extra = extra, origin = ex)
        repo.replaceDetails(jamie.id, com.jmr.atlas.integrations.Sync.CONTACT_KINDS, listOf(
            d(jamie, DetailKind.PHONE, "Mobile", "(555) 014-2290"), d(jamie, DetailKind.PHONE, "Work", "(555) 014-8800"),
            d(jamie, DetailKind.EMAIL, "Work", "jamie@carterbuild.example"), d(jamie, DetailKind.ADDRESS, "Work", "1180 Ridge Rd, Boulder, CO"),
            d(jamie, DetailKind.WEBSITE, "Website", "carterbuild.example"), d(jamie, DetailKind.DATE, "Birthday", "--03-14"),
            d(jamie, DetailKind.SOCIAL, "LinkedIn", "linkedin.com/in/example")))
        listOf("Prefers a text before 8 AM, not calls", "Recommended Dana at the tile shop").forEach { repo.putDetail(d(jamie, DetailKind.REMEMBER, "", it)) }
        repo.replaceDetails(jamie.id, setOf(DetailKind.CUSTOM), listOf(d(jamie, DetailKind.CUSTOM, "License number", "CO-4471-2291", flag = true), d(jamie, DetailKind.CUSTOM, "Rate", "85 / hour", FieldType.MONEY)))
        repo.replaceDetails(home.id, setOf(DetailKind.CUSTOM), listOf(d(home, DetailKind.CUSTOM, "Budget", "60,000", FieldType.MONEY), d(home, DetailKind.CUSTOM, "Goal", "Open the kitchen to the living room")))
        repo.putDetail(d(home, DetailKind.UPDATE, "", "Demolition confirmed for ${demo.eventDate!!.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}. Waiting on Jamie's revised estimate before ordering cabinets.", extra = Health.ON_TRACK.name))
        repo.replaceDetails(quotes.id, setOf(DetailKind.STEP), listOf(d(quotes, DetailKind.STEP, "", "Get Carter Build estimate", flag = true, extra = today.minusDays(3).toString()),
            d(quotes, DetailKind.STEP, "Ridge Builders", "Get a second quote"), d(quotes, DetailKind.STEP, "", "Compare line by line")))
        repo.replaceDetails(estimate.id, setOf(DetailKind.FACT), listOf(d(estimate, DetailKind.FACT, "Total", "$48,200", FieldType.MONEY, extra = "Read from the document"),
            d(estimate, DetailKind.FACT, "Valid until", today.plusDays(39).toString(), FieldType.DATE, extra = "Read from the document"),
            d(estimate, DetailKind.FACT, "Excludes", "Electrical work", extra = "Read from the document"),
            d(estimate, DetailKind.FACT, "Estimate number", "EST-2026-0118", flag = true, extra = "Read from the document")))
        repo.putDetail(d(link, DetailKind.HIGHLIGHT, "", "Leave at least 42 inches between the island and the counters."))

        // a recent voice note, connected automatically (shows “Just captured” on Now)
        val cap = repo.captureText("Jamie said the cabinets ship in three weeks. He'll send the updated estimate Friday.", voice = true)
        repo.snap.captures.firstOrNull { it.id == cap }?.noteId?.let { id -> repo.snap.objects[id]?.let { repo.update(it.copy(origin = ex), "Example") } }
        repo.process(cap, today)
    }
}
