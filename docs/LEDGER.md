# Atlas 0.3 — build ledger (Nov 22 2026)

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
