# Atlas — build ledger (Nov 23 2026)

Honest status. "Verified" = automated test or inspected screenshot (Robolectric). Nothing has run on a physical Fold 6 yet; items marked **device-only** can only be proven there.

## 0.7.3 — Focus as an option; the board stays

| Asked | Done | Test |
|---|---|---|
| Keep the current workspace as an option when adding new views (Nov 24 2026) | Project spaces get a **Board · Focus** switch at the top. **Board** is the 0.7 board, unchanged, and the default. **Focus** (approved mockup S-Focus) adds a one-line reason under each group, and holding a card shows what it's linked to in labelled lanes with the rest folded. The choice is remembered per space | `focusIsAnOptionBoardStaysDefault` |

## 0.7.2 — load the example later, and a visible "+ New space"

| Asked | Done | Test |
|---|---|---|
| No seed data after starting empty | Settings → Your data → **Load example workspace** (shown while none is loaded; Remove stays where it was) | `emptyStartCanLoadExampleAndMakeSpace` |
| No obvious way to add a space | **+ New space** on Home (inner and cover screens) opens the box with "New space " ready | same |
| Test builds wouldn't update each other | Debug builds are signed with one committed debug key, so each CI APK installs over the last | CI |
| Roll back 0.8.0 | The 0.8.0 space Focus view and band reasons are removed; boards are back to 0.7.1 behaviour | existing board tests |

## 0.7.1 — files, cards that look like what they are, the box on the Fold

| Reported | Cause / decision | Fix | Test |
|---|---|---|---|
| Can't see what I type with the keyboard up | The panel kept its full height, so on the inner screen it pushed the input row under the keyboard | Input row is laid out first and never shrinks; the panel gets only the room left above the keyboard and scrolls; clears the status bar | `longConversationKeepsTheInputOnScreen` (layout; keyboard itself **device-only**) |
| Want to swipe the conversation down | — | Drag the panel's handle/header down, or pull down at the top of the thread: it folds to a "Conversation · N" pill; tap to bring it back | `swipeDownFoldsTheConversation` |
| "1 attachment / 1 attachment" | The fallback text and the file count were both shown | Your bubble lists the files with their format badges | `longConversationKeepsTheInputOnScreen` |
| "How old is Lincoln" → "no access to personal information" | Chat prompt said "private assistant… the person", which Gemma read as a privacy question | Prompt says it answers general knowledge incl. public figures, reads names charitably; turns labelled User/Atlas. Save offers and the caveat show only on the latest answer | — (**device-only**) |
| Cards all look the same / boxy | — | Person: face + name on a pill. File: format badge (red PDF, blue DOC, green XLS, orange PPT…) + name + size. Photo: the photo. Task: tick box you can tap. Idea: bulb on warm paper with a bubble corner. Event: calendar leaf. State colors unchanged | `cardsOpenOnTapAndShowLinksOnLongPress`, `cardsOnTheCover`, `tickingATaskOnItsCard` |
| Tapping an item in a group doesn't open it | Tap selected it (links mode) | Tap opens: files open in the phone's viewer, links in the browser, everything else on its page. Long-press shows its links (as tap did before); while links show, taps move the selection | `cardsOpenOnTapAndShowLinksOnLongPress`, `boardTapOpen` |
| Files: PDF icon, open on tap, ⋮ for details | — | `FileKind` from MIME/extension; ⋮ on file, photo and link cards opens the details page. Open goes straight to the default viewer (no chooser) with the right extension | `fileKinds` |
| File page | Image shown twice, giant file name, empty "Not found" rows, actions that didn't apply | Leads with the file: photo / PDF first page / big badge (tap opens), name + format · pages · size · added, Open · Share (the actual file) · Rename · File in…, then where it's filed, what Atlas read (only if found), text (if any), your note | `aFilePageLeadsWithTheFile`, `aFilePageCover` |
| Files space should feel like a file manager | — | Files opens as a list by default: Recent badges, then This week / This month / Earlier | `cardsOnTheCover` |
| Tasks has no filter | — | Filter on every kind space; on Tasks it lists matches and keeps the time bands otherwise | `tasksHaveAFilter` |
| Find people in phone contacts | Contacts were only reachable from a sheet | Search shows "On your phone" (contacts not yet in Atlas; tap adds and opens). Without permission, a name search offers "Search your phone contacts too · Allow". People has "From contacts" | `searchOffersPhoneContacts`, `peopleCanComeFromContacts` |
| No way home from an item | — | Home button next to Back on item, file, space and All spaces pages once you're two levels in | `cardsOpenOnTapAndShowLinksOnLongPress` |

## 0.7.0 — the Atlas box, search, Now, spaces, color = state

| Asked | Done | Test |
|---|---|---|
| Typing "Priya" wants to add something | Every line is routed first: **find** (a name or a few words), **capture** (a sentence with a verb, date or "add to"), **ask about my things**, **ask anything**, or **chat** (follow-up to an answer). Find shows kind chips, a best-match card (person: Open / Call / Text / Email), grouped results with why they matched, and "Save as a note" last | `routesWhatYouType`, `aNameFindsThings` |
| A full AI experience like Muse | The box keeps a thread: what you said, what Atlas did (with Undo), answers about your things (instant rule answer, refined by the model), and general answers. A general answer offers once: Save to <matching space> / + New space / Keep as a note. "add to X" right after an answer files it | `generalQuestionWithoutAModel`, `answerFilesIntoASpace`, `threadSaysWhatItDidAndUndoes` |
| Too many spaces on Now | Now: core strip (People, Tasks · N slipped, Files, Ideas & notes, Spaces) that scrolls sideways; up to 4 pinned spaces (yours first, then the busiest, marked Auto), "All N spaces". 3+ late tasks fold into one "slipped" row | `nowHasTheCoreStripAndPinnedSpaces`, `nowOnTheCover` |
| Collections into spaces | All spaces screen: filter, Pinned, This week, spaces that hold spaces (House → …), Other, Quiet folded. Pin toggle; tap a mark to pick color (6) and icon (12). Collection is no longer offered in Create | `allSpacesPinsFiltersAndRecolors` |
| Long lists of everything | People / Files / Ideas & notes: filter + List / By space. People: Recent faces, A–Z with letter rail; others by This week / This month / Earlier | `peopleReadAsAList` |
| Card colors | Color is state: coral late or blocked, amber waiting, green done, otherwise white. Type is an icon on a quiet chip; spaces wear their own color + icon; people keep their face color. Bands neutral, Late band faintly coral | `colorIsState` |
| On-phone AI | One `Brain` interface. Gemma (LiteRT-LM, GPU) is one implementation; the model is downloaded once or imported from a file (Settings → AI → Import the model file), never bundled in the APK. No cloud fallback | — (model can't run under Robolectric; **device-only**) |

Changed tests: the box no longer closes with a toast after saving — `catchATaskFromASentence`, `returnKeySends` now check the thread's "did" card; `toastGoesAway` uses another toast.

## 0.6.3 — input box closing by itself; sending from the sentence

| Reported | Cause | Fix | Test |
|---|---|---|---|
| Box closes when typing/choosing (esp. from the widget) | Taps in the gaps of the sheet fell through to the backdrop, which closed it; from the widget the keyboard is still sliding the sheet up when you tap. Closing also discarded the draft | Sheet consumes its own taps; backdrop ignores taps for 0.7 s after opening; with a draft the first backdrop tap only hides the keyboard; drafts (text and attachments) survive closing | `backdropDoesntThrowAwayADraft` |
| Hitting the round send button is awkward | Multi-line fields get a return key, not an action key, so Enter added a newline | Return sends (InputTransformation); a "Save task / Add step / Make space / Save 2" button on the sentence card; "Ask" on the question bubble | `returnKeySends`, `sendButtonOnTheSentence` |

## 0.6.2 — put things in spaces that don't exist yet

| Asked | Done | Test |
|---|---|---|
| Add a photo to "Ankle Surgery" when it doesn't exist | Photos, scans and files picked from the input box wait there (thumbnails, ×) until you send; the line says where they go | `photoGoesToANewSpace` |
| "Add to Ankle Surgery" → bubble if it's not there | "add to / put in / file under / save to X" and `@X` link to X if it exists, else a dashed "+ New space · X" bubble (tap to cross out); made on send, undo removes it | `readsWhereThingsGo`, `lineMakesTheSpaceItNames` |
| Create groups quickly | "New space Ankle Surgery" / "new group called …" / just "Add to Ankle Surgery"; Space is also in the type cycle | `readsWhereThingsGo` |
| — | Link to… offers "Create 'X'" when nothing matches | `linkPickerCreatesASpace` |

## 0.6.1 — input box, toasts, board lines

| Reported | Cause | Fix | Test |
|---|---|---|---|
| Toasts never leave | The timer ran inside `LaunchedEffect(msg)`, which clearing `msg` cancelled | Two effects: copy and clear; the wait is keyed on the toast. Tap to dismiss | `InputUiTest.toastGoesAway` |
| Typing or asking doesn't work | Value-based text field mirrored through a StateFlow; real keyboards desync | `TextFieldState` + `BasicTextField(state=…)` | `catchAndAsk`, `askShowsAnAnswer` |
| Can't add to an existing item | The input box had no idea what was open | Context = open item or selected card. On a task a line becomes a Step; on a person, Remember; on a project, Update; else linked. `@name` mentions; "+ Link to…" picker | `lineOnATaskBecomesAStep`, `ideaOnAPersonIsLinkedToThem`, `mentionAndLinkPicker` |
| Ask felt dead | Waited on the model | Atlas's own answer shows at once; on-device model refines within 45 s or says it didn't | `askShowsAnAnswer` |
| Board lines unclear | Bezier edge to edge, crossing cards | Routed through gutters (fewest cards crossed), rounded elbows, halo and glow, draw-in, labels ("assigned to", "blocks"), dashed when Atlas made the link; link-count badges | `boardLines`, `boardLinesCover` |

## 0.6 — redesign (Home · Space · item page · one input box)
| Area | What works | Evidence |
|---|---|---|
| Home | One briefing sentence with tappable names (late, due soon, who owes you what, next milestone), deterministic and sourced; This week — each task once, late first, tick to complete; spaces (project covers + People, Files, Ideas & notes, Tasks, Everything); quiet Settings icon top right; "to confirm" line when Atlas has suggestions. | `SixTest.briefingSentenceIsSpecificAndSourced`, `CoverUiTest.setupThenHome`, screenshots c01, f01, f12 |
| Space board | Bands by person (one step away), "Not tied to anyone", "Also involved"; most urgent first; status line per band; tiles per type (late/due/waiting labels, steps, document facts, photos, voice notes); tap → lines to linked tiles, others fade, dark bar with Open; selected tile scrolls into view; timeline ribbon (cover: a line of words); band rename; AI names when on. | `SixTest.bandsGroupByPersonAndPutLateFirst`, `bandNamesYouSetWinAndStick`, `FoldUiTest.homeBoardTapPage`, screenshots c04–c05, f02–f03, f13 |
| Special spaces | People/Files/Ideas & notes by space; Tasks by Late/This week/Waiting/Later/No date/Done lately; Everything by kind. | `SixTest.specialSpaces`, screenshots f08–f09 |
| Item page | Hero (kind, title, one line), type actions (Call/Text/Email, Mark done, Open file/link, Open the board), How it got here (origin, Atlas links with Unlink, proposals Yes/No), Connected, then every 0.5 detail section. Two columns unfolded. | `ItemPageUiTest`, screenshots c06, f04, f10, f11, i01–i03 |
| Input box | + · text · mic · send in one row on every main screen. Sentence → Task/Note/Idea with date, space (current space wins), people — every part tappable before saving; Undo after. Questions answered from your items (rules; Gemma when on), sources listed, "Show on the board" rings them. Attach photo/scan/file/copied link. | `SixTest.sentenceParser`, `CoverUiTest.catchATaskFromASentence`, `FoldUiTest.catchAndAsk`, screenshots c02–c03, f05–f07 |
| Removed | Universe map, Insights tab, Library, Now, Inspector, capture sheet, bottom bar/rail. Share and widget captures now report where they went in one line with Undo. | — |
| AI additions | `Ai.answer` (numbered items only, JSON schema, sources validated) and `Ai.nameBands` (1–28 chars, letters only; else default kept). | `SixTest.aiOutputIsValidated`; inference **device-only** |

## 0.5 — what changed
| Area | What works | Evidence |
|---|---|---|
| Home | Now: greeting, deterministic briefing headline, Needs you (overdue + due/deadline today, tick to complete with Undo), Waiting on, today's calendar, Just captured with automatic connections + Undo. | `PhoneUiTest.setupThenNow`, `IntegrationTest.briefingAndSchedule` |
| Navigation | Now · Universe · Capture · Search · Library on the phone bar and the Fold rail. Library list → card side pane on the Fold. | UI tests, screenshots |
| Setup | Welcome → locked to this phone (encrypted, fingerprint) → briefing time + notifications → contacts/calendars (optional) → example or empty. | `PhoneUiTest` |
| Item cards | Person, Project, Task, Document/Image, Link, generic (note/idea/event/decision/collection). Card → actions → why now → type details → connections/history collapsed. | `CardUiTest`, screenshots i01–i12 |
| Editing | Full-screen editors per type: person (repeatable phones/emails/addresses/dates/social with labels, nickname, photo), project, task (plan vs deadline, priority, blocked), document (kind, document date, key facts), link (kind). Custom fields: 10 types, per item or per type, secret masking. | `IntegrationTest.customFieldsPerItemAndPerType` |
| Tasks | Plan date vs deadline, reminder (notification with Done / Snooze / voice reply, mirrored to a paired watch), repeat (rolls forward on complete), steps checklist, block time in calendar. | `IntegrationTest.repeatingTaskRollsForward`; alarms **device-only** |
| Projects | Health (On track/At risk/Off track) via posted updates, progress, next milestone, people & roles, documents, decisions. | `IntegrationTest.projectUpdatesCarryHealth`, `CardUiTest` |
| Documents | Key facts read on the phone (total, valid-until/due, reference no., document date, kind); page-1 preview; summarize on the phone when AI is on. | `IntegrationTest.documentFacts` |
| Auto-connect | Known people/projects connect at once (Origin AUTO, logged, Undo); anything new waits in To review. Setting to turn off. | `IntegrationTest.autoConnectKnownItemsWithUndo` |
| Contacts | Search phone contacts; add/connect with photo and all fields; two-way sync when enabled (writes back; new people saved to the chosen account); phone wins when it changes; deleted contacts unlink, never delete. | **device-only** (Robolectric has no contacts provider) |
| Calendars | Choose calendars; next 14 days in Now, person/project cards and search; insert events into the chosen calendar, or open the calendar app prefilled without permission. | **device-only** |
| Internet | Link previews (title, description, site, author, date, image, readable copy) over HTTPS only, 2 MB/3 MB caps, 5 redirects, private/loopback/link-local/CGNAT addresses refused; copies Wi-Fi only by default. | `IntegrationTest.pagesAreParsedAsPlainText`, `DataTest.networkIsHttpsOnlyAndNeverPrivate` |
| Share | Gmail/Chrome/any app → Atlas: subject becomes the title, a lone URL becomes a Link with preview, source app recorded. | code; **device-only** end to end |
| Widget | One Glance widget: type, voice, scan, paste copied link. | **device-only** |
| Briefing | Notification at the chosen time (inexact 15-min window, weekdays option), rescheduled on boot. | `IntegrationTest.briefingAndSchedule`; delivery **device-only** |
| AI | Settings shows On this phone / My server (coming later) / Off. On-device: Gemma 4 E2B (.litertlm, 2,588,147,712 bytes) downloaded on request, pinned to HF commit 2e12bfd8…, SHA-256 verified; LiteRT-LM 0.19.1 GPU→CPU fallback; JSON-schema output; results validated and only ever proposals. | `IntegrationTest.aiOutputIsValidatedNotTrusted`; inference **device-only** |
| Encryption | Database encrypted with SQLCipher 4.20 (passphrase random, wrapped by Keystore, no-backup storage); 0.4 plain DB converted once via sqlcipher_export. Fingerprint/screen-lock unlock with lock-after delay; blank recents card. | migration of schema `IntegrationTest.v1DatabaseMigratesAndKeepsData`; SQLCipher itself **device-only** |

## Known limits
- Exact alarms aren't used (no special permission): reminders can arrive up to 5 minutes late, the briefing up to 15.
- Gemma 4 E2B on the Fold 6 (Snapdragon 8 Gen 3) is unbenchmarked; expect several seconds per capture.
- "My server" (Ollama over Tailscale) is listed, not built.
- Gmail is share-only by design (read sync needs a full-mailbox credential).
- Single light theme.


## Earlier (0.4)

## 0.4 changes (UI review pass)
- Phone map shows real content (faces, documents, checkable tasks, ideas, projects, milestones) instead of count tiles; same neighbourhood layout as the Fold.
- Phone panel opens on "Why this matters now"; reasons about tasks are checkable in place; next steps no longer repeat them.
- Removed repetition: focus pill, per-row "Example", duplicate type/status chips, explainer subtitles and footnotes. One "Example data" chip removes the example workspace.
- Search bar is a search bar (magnifier, voice); no sparkle/send/attach.
- Capture is a sheet over the current screen; Save sits under the text box; 5 common types plus "More".
- Controls: hamburger menu, + Capture in the phone top bar, Settings gear on wide screens, Map/Insights in the same place on every device, map options (focus, list view, zoom) in one labeled menu, list pages get a labeled "New …" button.
- Naming: "Documents" everywhere. Dates read as words ("Fri, Nov 27").
- Fixed: "1 connections", Tasks filters (Open/Overdue/Today/Upcoming/Blocked/Done), search excerpts cut mid-word, stale snackbar, clipped labels, mid-word line breaks, tabs hidden off-screen (now wrap).


Honest status of this build. "Verified" means covered by an automated test or inspected in a rendered screenshot; nothing here has been run on a physical Fold 6 yet.

## Done and verified
| Area | What works | Evidence |
|---|---|---|
| Storage | SQLite schema v1: foreign keys, cascades, UNIQUE edges, FTS4 search, transactional writes. One document in three projects is one row. Edits never cascade-delete connections (UPDATE-then-INSERT, never REPLACE). | `DataTest` |
| Originals | Photos/scans/files encrypted (AES-256-GCM, Keystore key) before any processing; purge erases the file. | `DataTest.fileOriginalIsEncryptedAndRecoverable` |
| Capture | Text, photo picker, file (PDF/image/text), document scanner, share-to-Atlas. Original saved first; on-device OCR (ML Kit bundled, PdfRenderer for PDFs); then proposals. | `CaptureTest`, `PhoneUiTest.captureReviewAccept` |
| Understanding | Deterministic rules: people (cue words, existing names, "Jamie" ≠ "Jamie Lee"), projects (named, via graph, or by shared topic words — names never count), expected deliverables, explicit actions, possible milestones, dates. Every proposal quotes its evidence. Imported text is data, never instructions. | `CaptureTest` |
| Review | Inbox and inline review; accept/dismiss one or all; accept is transactional and never duplicates a person; tasks connect to the person, project and source note. | `CaptureTest`, UI tests |
| Objects | Type-specific pages (Project: Overview/Connections/Files/Tasks/Timeline/Activity; Person: …/Notes/Projects/Documents; Document: Preview/Text/Connections/Actions/Activity; Idea: Overview/Related/Sources/Actions). Create, edit (dates, status, priority, blocked), pin, trash, restore, delete forever. Manual connect/disconnect; confirm/reject proposed edges. | UI tests, screenshots |
| Universe map | Focal circle, six neighbourhoods matching the target layout, organic blobs, curved connectors with nodes, dashed lines for proposed-only links, interactive task checkboxes, timeline strip, tap-to-refocus with animation, focus history, focus picker, zoom/pan/fit, accessible list alternative. Phone: tiles + representative nodes + persistent inspector sheet. | `PhoneUiTest`, `WideUiTest`, screenshots |
| Inspector | Context ("Why this matters now", rule-based, each line tappable to its source), next steps, Sources (evidence quotes, original capture, open original), Activity. | screenshots |
| Insights | Overdue, due soon, waiting on, review backlog, active project without next step, unconnected files, possible duplicate people — each with sources, scope and time; dismissible. No scores or percentages. | UI tests |
| Lists | People, Projects (status filters), Documents (type filters, list/grid), Ideas, Tasks (Today/Upcoming/All open/Blocked/Completed), Collections, Timeline (planned vs recorded). | screenshots |
| Search | Exact (FTS over titles, notes, people's details, OCR text) and Connected (neighbours of matches, with the path). | `DataTest`, `PhoneUiTest` |
| Adaptive | Phone 411dp, Fold cover 344dp, Fold inner portrait 707dp and landscape 880dp, tablet 1280dp. Focus survives activity recreation. | `WideUiTest.stateSurvivesRecreate`, screenshots |
| Privacy | No INTERNET permission in the release APK (stripped from ML Kit's merged manifest). Excluded from backup/device transfer. Export to JSON via the system file picker. No analytics. | `DataTest.appCannotUseTheNetwork`, `aapt2` |
| Palette | Morph light tokens; no yellow family; text pairs ≥ 4.5:1. | `TokenTest` |

## Not done (by plan)
- **Phase 4 AI**: no summaries, no "ask" answers. The bar says "Search your Atlas…" and searches; it does not pretend to be an assistant.
- **Phase 5 Canvas**: hidden (no placeholder control).
- **Phase 7 hardening**: no app lock, database file not encrypted (SQLCipher). Originals are encrypted; metadata and text are protected only by Android's app sandbox and your screen lock.
- No Google sync, calendar, or import from other apps.
- Single light theme.

## Known limitations
- Rule-based understanding misses phrasing it doesn't know (e.g. names without a cue word, unusual date formats). It errs toward proposing less.
- Example documents have no files behind them (labeled).
- Dictation depends on the phone's speech recognizer app; Atlas asks it to prefer on-device recognition but cannot enforce it.
- Not yet tested on a physical device: real OCR quality, document scanner availability, performance with thousands of items, TalkBack pass.
