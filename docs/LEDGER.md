# Atlas 0.5 — build ledger (Nov 22 2026)

Honest status. "Verified" = automated test or inspected screenshot (Robolectric). Nothing has run on a physical Fold 6 yet; items marked **device-only** can only be proven there.

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
