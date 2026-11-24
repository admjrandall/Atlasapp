# Atlas Journeys — design spec (draft for approval)

Status: **mockups approved (Nov 23 2026).** Next: phased build plan, then code.
Mockup canvas: https://claude.ai/artifact/L9srPA2YwQw9SnpYRwHbJR — page **Journeys v2** is the approved reference (page 1 holds the v1 concepts).

## 1. The idea

A **journey** is the life of one thing in your head, drawn in the shape of its time.
Data stays one graph (objects + relationships). A journey is a *view* over a space: the AI picks a
shape, lays the space's items along it, and keeps it organized as things arrive.

- You never organize. You ask, capture, live. Atlas knows which life-thing an item belongs to.
- Sources: your phone (photos, messages, email shares, contacts, calendar, files) plus anything you
  asked Atlas to find on the web (an obituary, a product page). Nothing leaves the phone.
- Items never duplicate: a photo in two journeys is one object with two relationships.

## 2. Decisions so far (Nov 23 2026)

| Topic | Decision |
|---|---|
| Shapes | Ten in the first set (below); Remember has three styles |
| Choosing a shape | AI picks; the other shapes are one tap away; you can switch later |
| Spaces | Kept. Any space can become a journey. Plain spaces get a visual upgrade (section 7) |
| Mockup review | All journeys at once, fully clickable start to finish — **approved Nov 23 2026** |
| Space upgrade | **Focus only** (S-Focus board): groups that say why, tap → focus view with labelled relationship lanes, unrelated items folded. The Flow + story board (S-Flow) is **not** in scope |
| Visual bar | At least as good as the v1 concept mockups; more interactive and more polished |
| No boards/cards look | Journeys use their own shape, not grids of cards |

## 3. The ten shapes

| # | Shape | Kind of time | For | Progress shown as |
|---|---|---|---|---|
| 1 | **Trail** | Has an end | Projects: renovation, move, job search | Thin bar + "you are here" stop |
| 2 | **Living Tree** | Ongoing, grows | A person or life: grandchild, child, pet, marriage | None; branches and fruit |
| 3 | **Remember** (3 styles, see 3a) | Looks back | Remembering someone who died | None; anniversaries return |
| 4 | **Crossroads** | Converges | Decisions: a purchase, a job offer, a contractor | Options narrowing to one gate |
| 5 | **River** | Deepens | Learning: a skill, hobby, course | Depth meter (Curious → Could teach it) |
| 6 | **Orbit** | Returns | Recurring: taxes, car registration, physicals | Rings per cycle; days to next due |
| 7 | **Shelf** | Accumulates | Collections: recipes, books, wishlist, gift ideas | None; count and "recently added" |
| 8 | **Voyage** | Before · during · after | A trip | Islands per place/day; becomes an album |
| 9 | **Climb** | Toward a target | A measured goal: savings, fitness, recovery | Altitude toward a summit with camps |
| 10 | **Countdown** | To a fixed day | An event: wedding, party, a service | A ring tightening to the day; run of show |

### 3a. Remember — three styles of one shape
Replaces the v1 "Two Lines" concept (judged not personal enough). The AI picks a style; the other two
are one tap away, and all three show the same memories.

| Style | What you see | Grows by | Notes |
|---|---|---|---|
| **Keepsake Room** (default) | A warm room that is *his*: objects chosen from your memories — fishing rod on the wall (Lake Powell), guitar in the corner (garage band), framed photo (wedding toast), his phone on the table (last texts), the obituary as a folded card by the door | A new memory adds a new object | Light follows the time of day; softly lit on his birthday and the anniversary. Needs an illustrated object library (~40 objects) the AI chooses from |
| **Memory Garden** | A garden you tend: photos are flowers, stories are trees, people who share memories plant their own | Planting | Blooms on anniversaries; gentle and hopeful |
| **Living Letter** | A letter to him that writes itself in chapters, photos tucked in like a scrapbook, your words between | Writing | Most intimate and readable; least visual |

Tap any object / plant / chapter → its bundle (Lens) → items. Entry wording is gentle ("Would you like
to keep this somewhere?"); sources are your phone plus the obituary you asked for, nothing else.

Candidates held back: **Thread** (a case with an organization: insurance claim, warranty, dispute —
calls and emails, waiting on them). Can replace any of the ten.

### AI shape picking — signals
- Has a fixed date in the future → Countdown. Has a numeric target → Climb.
- Names a person who died (obituary, "passed") → Remember. Names a living person you keep up with → Living Tree.
- Options / "which" / "vs" / reviews → Crossroads. "Learn / how to / practice" → River.
- Repeats yearly/monthly → Orbit. Places + dates away from home → Voyage.
- Things to keep, no end → Shelf. Otherwise a multi-step goal → Trail.
The pick is shown with a one-line reason ("It has a date: Sat Nov 29") and the alternates.

## 4. Screens every journey has (the full flow)

1. **Start** — after saving a group the box offers "Make this a journey?" (gentle wording for grief:
   "Would you like to keep this somewhere?"). Or: + → New journey (manual), or from a template.
2. **Shape preview** — "Here's how I'd shape it" + reason; swipe to the other nine; live preview of each.
3. **Building** — the shape draws itself; items from your phone flow in with counts ("38 photos from
   your gallery"); nothing is added from the web unless you asked; you can uncheck sources.
4. **Journey home** — the shape, full screen; thin header; AI line; the one input box.
5. **Part view** — a stop / chapter / branch / camp / island / option / ring.
6. **Bundle bloom** — a stack opens into a swipeable fan; best few first; near-duplicates in one pile.
7. **Item viewer** — photo, video, PDF (facts pulled out), link preview, messages, email, note, voice.
   Back to bundle, previous/next, AI line about the item, "Ask about this…".
8. **Adding** — type, talk, attach, share-in, or drop on a part; AI proposes where it goes (with Undo).
9. **Editing the shape** — rename, reorder, add, merge, split parts; change shape (keeps all items).
10. **Lots of data** — semantic zoom; ~12 things on screen max; AI piles (renamable); best few lead;
    raw view + search always reachable.
11. **Finishing / cycling** — Trail done → becomes a memory; Crossroads → verdict; Orbit → next ring;
    Countdown → the day, then an album; Climb → summit; Voyage → album; Tree/Remember/Shelf/River never finish.
12. **Templates** — save any journey's skeleton (parts, not items) as a template; templates learn from
    your edits (e.g. you always add "Permits").
13. **On Home** — each journey appears in its own mini shape with one status line.
14. **Settings per journey** — shape, sources it may pull from, lock (biometric) for private journeys,
    archive, delete.

## 5. Shape-specific parts

| Shape | Parts | Part detail shows |
|---|---|---|
| Trail | Stops (done / here / next / later); roadside items | What's needed to leave this stop; items found here |
| Living Tree | Branches (finite mini-journeys) and fruit (finished moments); buds (ideas) | A branch's own mini trail |
| Remember | Objects (Room) / plants (Garden) / chapters (Letter); people who appear | Bundles per memory; "add a memory only you have" |
| Crossroads | Options (streams) and the gate | Loved / complaints / sources; rule out, bring back, choose |
| River | Sources (tributaries) and attempts (stones) | What you learned from it; next attempt |
| Orbit | Cycles (rings) and recurring items | Last time vs this time; what to watch for |
| Shelf | Shelves (AI-grouped) and items | Item details; "make this" / "buy this" turns into a branch or trail |
| Voyage | Before (plans), islands (places/days), after (album) | Bookings, maps, photos per day |
| Climb | Base, camps (milestones), summit (target) | Measurements over time; what helped |
| Countdown | Days remaining, prep lanes (guests, vendors, run of show) | The day's schedule; who's doing what |

## 6. Lots of data — rules
1. About 12 things on screen at once; everything else lives inside them.
2. AI makes and names piles; rename, split or merge in one tap.
3. Inside a pile, the best few lead; near-duplicates fold away.
4. Nothing is lost: raw view and search jump to any single item.

## 7. Spaces upgrade (plain, non-journey spaces)
Problem today (0.7.1): cards are good, but connecting lines and band groupings can look cluttered and
don't explain how things really interact.
**Approved: Focus** (reference board S-Focus).
- **Readable groups** — no connecting lines in the default view; every band says why it's a group
  ("With Priya · 4 things · 1 late") with a one-line reason under it.
- **Focus view** — tap any item: it moves to the top; everything connected to it is listed in labelled
  lanes by relationship ("Assigned to", "Blocks", "Came from", "For", "Waiting on"), each with a short
  why; unconnected items fold into one "N other things" line. Back returns to groups.
- The AI line reacts to the focused item (e.g. "Ordering seedlings unblocks the price tags").
Not in scope: Flow lanes and Story strip (S-Flow board kept only for reference).

## 8. Mapping to the codebase (initial read of 0.7.1)
- Journey = a `PROJECT` object (space) with a shape detail (e.g. `DetailKind.SHAPE`) and parts.
- Parts (stops/chapters/branches…) = objects or band details linked with `PART_OF`; branches = child
  spaces (spaces already hold spaces).
- Bands (ADR-10, `data/Board.kt`) become the default grouping for plain spaces and seed journey parts.
- Bundles = AI piles over a part's items; "best of" picks stored as details so they're stable.
- Box (`capture/Sentence`, `CatchBox`, `AtlasPanel`) offers "make this a journey" after a save.
- `Brain` gets `pickShape(...)` and `nameParts(...)`; rule-based first, model refines (ADR: code before model).

## 9. Open questions
- Exact list of ten (Thread vs. another)?
- Which sources each shape may pull from by default (e.g. Messages for Remember)?
- Lock/biometric default for Remember journeys?
