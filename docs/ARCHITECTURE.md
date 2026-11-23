# Atlas — architecture and decisions (Phase 0)

Atlas is a single-user, local-first Android app. One connected store (objects + relationships + originals) feeds every screen.

```
Capture ─▶ persist original (Note/Document + Capture row) ─▶ extract (on-device rules; AI adapter in Phase 4)
        ─▶ proposals (Suggestion rows) ─▶ Review inbox ─▶ approved objects/relationships (transaction)
                                                     ─▶ Universe map · People · Projects · Tasks · Search · Insights
```

## Packages
| Package | Responsibility |
|---|---|
| `model` | Object/relationship/capture/suggestion types. No Android UI. |
| `data` | `AtlasDb` (SQLite schema, FKs, indexes, FTS4, migrations), `Repository` (single source of truth, transactions, StateFlow snapshot), `SecureFiles` (Keystore AES-GCM vault). |
| `capture` | `Extractor` (people, projects, tasks, dates, milestones with quoted evidence), `CaptureEngine` (persist-first pipeline). |
| `search` | Full-text + graph-neighbour retrieval. |
| `insight` | Evidence-backed rule insights (every insight lists its sources). |
| `ui.theme` / `ui.components` | Atlas light design system (tokens from `references/design-tokens.json`). |
| `ui` (Root) | 0.6 shell: Home → Space → item page, one input box at the bottom of all three, no tab bar or rail. `LocalWide` (≥600 dp) switches screens to two columns. |
| `ui.six` | 0.6 screens: `Home` (briefing sentence, This week, spaces), `Space` (banded board, threads, timeline ribbon), `Tiles`, `Item` (hero, how it got here, connected, embedded details), `CatchBox` (input box, preview, answers, selection bar), `Bits`. |
| `ui.cards` | 0.5 per-type detail sections, embedded under the item page (`LocalEmbedded`); editors and sheets. |
| `ui.screens` | Review, Trash, History, Settings (AI, integrations, security), Onboarding. |
| `data.Board` | Band grouping, special spaces, timeline marks, cover lines. Pure, unit-tested. |
| `capture.Sentence` | Reads what you type into kind · title · date · space · people, or recognises a question. Pure, unit-tested. |

## Decision records
**ADR-1 Native Kotlin + Compose + Material 3.** Per spec. Compose BOM 2026.11.01, AGP 9.5.2 with built-in Kotlin, compileSdk 37.

**ADR-2 Window-based adaptivity from measured width (revised in Phase 1).** Layout is chosen from the window's actual width (`BoxWithConstraints`), never the device model: <600dp compact, 600–759dp rail + map + inspector sheet (Fold 6 inner, portrait), ≥760dp rail + map + side inspector (inner landscape, tablets). The Material 3 Adaptive library was dropped: the Fold 6 inner screen is "medium" in both orientations, so size classes alone could not separate the two layouts. The map is laid out at a minimum design size and scaled to fit, so narrow panes never clip it. The phone has no bottom bar, matching the target design: destinations live in a drawer opened from the Atlas wordmark.

**ADR-3 Plain SQLite (`SQLiteOpenHelper`), not Room, for now.** Room 3 requires KSP; adding a second code generator to AGP 9 built-in Kotlin is the riskiest build change. The schema uses real tables, foreign keys (`ON DELETE CASCADE`), unique constraints, indexes, FTS4 and versioned `onUpgrade` migrations, so moving to Room later is mechanical. Revisit in Phase 7.

**ADR-4 Encryption.** Attachments: AES-256-GCM with a non-exportable Android Keystore key, stored in app-private `vault/`. Database: app-private storage protected by Android file-based encryption; SQLCipher evaluated for Phase 7 (cost: native library size, ~10–20% query overhead). `allowBackup=false` so plaintext never reaches cloud backup.

**ADR-5 AI is optional and comes in Phase 4.** This build extracts with deterministic on-device rules and labels them as such. The provider abstraction (on-device LiteRT-LM / Gemini Nano where available / user-configured external provider with explicit consent, never silent fallback) arrives in Phase 4. Nothing in the app pretends to be AI.

**ADR-6 One record, many relationships.** A document linked to three projects is one row plus three `relationships` rows. Proposed (unreviewed) relationships are stored with `review=PROPOSED` and drawn dashed; they are never treated as facts.

**ADR-7 No placeholder controls.** Canvas is not shown until it works (Phase 5). Every visible button has a tested purpose.


### ADR-7 (0.5): SupportSQLite + SQLCipher
AtlasDb talks to `SupportSQLiteDatabase`. Devices use `net.zetetic:sqlcipher-android` (SupportOpenHelperFactory); unit tests use the framework helper, so the same SQL is tested. Schema v2 adds per-type columns, `details` (repeatable fields: phones, facts, steps, updates, highlights, custom values) and `field_defs` (type-wide custom fields). Migrations are additive.

### ADR-8 (0.5): Integrations through the phone, not accounts
Contacts and calendars go through ContactsContract and CalendarContract, so every account the phone already syncs works without OAuth or a server. Gmail is share-in only. Link previews use HTTPS fetches with hard limits.

### ADR-9 (0.5): AI provider
`AiMode` = ON_DEVICE | SERVER | OFF. ON_DEVICE runs Gemma 4 E2B with LiteRT-LM (the Fold 6 isn't supported by ML Kit's Gemini Nano). AI never auto-accepts: deterministic rules auto-connect known items; AI only adds proposals. SERVER (OpenAI-compatible endpoint over Tailscale) is planned.

### ADR-10 (0.6): Bands, not a graph; one input box
Research (Nov 2026) showed whole-graph views go unused and infinite canvases don't work on a phone, while the local graph — what one thing is linked to — is valued. So a project is a **board grouped into bands**: each person anchors the items linked to them (directly, or one step away); items linked to nobody share a band; people with nothing of their own share one. Bands are ordered by urgency, positions are deterministic, and connections are drawn only on tap (lines between tiles, everything else fades). Band names: your rename (`DetailKind.BAND`, origin USER) > on-device model (origin AI) > default ("With Priya").
Creating things moves from type forms to one sentence (`Sentence.parse`), shown back as tappable parts before saving. Questions go to the same box: Atlas's own rules answer (milestone risk, late, due, waiting, else search) and, with on-device AI on, Gemma writes the answer from numbered items only; either way the sources are listed and can be ringed on the board.
