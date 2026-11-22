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
| `ui.shell` | Adaptive navigation: bottom bar (compact) or rail + workspace toolbar + inspector (medium/expanded). |
| `ui.universe` | Focal knowledge map, cluster previews, refocus history, zoom/pan/fit, list alternative. |
| `ui.screens` | People, Projects, Documents, Ideas, Tasks, Collections, Timeline, Search, Review, Capture, Settings, Onboarding. |

## Decision records
**ADR-1 Native Kotlin + Compose + Material 3.** Per spec. Compose BOM 2026.11.01, AGP 9.5.2 with built-in Kotlin, compileSdk 37.

**ADR-2 Window-based adaptivity via Compose Material 3 Adaptive 1.3.0 (stable, Oct 2026).** `currentWindowAdaptiveInfo()` drives layout from actual window size class, never device model. Compact → bottom bar; medium/expanded → rail + panes. Panes are hand-composed (map + inspector, list + detail) so the map keeps full control of gestures.

**ADR-3 Plain SQLite (`SQLiteOpenHelper`), not Room, for now.** Room 3 requires KSP; adding a second code generator to AGP 9 built-in Kotlin is the riskiest build change. The schema uses real tables, foreign keys (`ON DELETE CASCADE`), unique constraints, indexes, FTS4 and versioned `onUpgrade` migrations, so moving to Room later is mechanical. Revisit in Phase 7.

**ADR-4 Encryption.** Attachments: AES-256-GCM with a non-exportable Android Keystore key, stored in app-private `vault/`. Database: app-private storage protected by Android file-based encryption; SQLCipher evaluated for Phase 7 (cost: native library size, ~10–20% query overhead). `allowBackup=false` so plaintext never reaches cloud backup.

**ADR-5 AI is optional and comes in Phase 4.** This build extracts with deterministic on-device rules and labels them as such. The provider abstraction (on-device LiteRT-LM / Gemini Nano where available / user-configured external provider with explicit consent, never silent fallback) arrives in Phase 4. Nothing in the app pretends to be AI.

**ADR-6 One record, many relationships.** A document linked to three projects is one row plus three `relationships` rows. Proposed (unreviewed) relationships are stored with `review=PROPOSED` and drawn dashed; they are never treated as facts.

**ADR-7 No placeholder controls.** Canvas is not shown until it works (Phase 5). Every visible button has a tested purpose.
