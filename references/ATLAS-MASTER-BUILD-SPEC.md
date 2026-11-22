# ATLAS — Your Personal Knowledge Universe
## Complete AI development handoff | 21 November 2026

**Product vision:** Capture anything. Understand everything. Connect what matters. Make it useful when you need it.

This is the consolidated, implementation-ready specification from the Atlas design discussion. It includes the master development prompt, surrounding design rationale, visual direction, exact starting color tokens, screen inventory, device adaptations, architecture, AI behavior, security, tests and delivery requirements. Treat the requirements in this document as authoritative over incidental text and colors in exploratory mockups. Mockup data is fictitious demonstration content, not actual user data or validated estimates.

## How to use this handoff

Give the coding AI this entire package, including the `references/primary/` directory. Read this document first, then inspect the three original reference images and the generated visual-direction diagrams. The design tokens are also supplied in machine-readable JSON. Do not implement directly from screenshots alone; use the written requirements for behavior, data and security. Earlier layout-only images contain disallowed lime/yellow-green accents, and the exploratory dark boards are not the chosen theme. They are retained only for structural reference.

### Primary visual references

1. **Original Atlas:** authoritative inspiration for spatial interactions, navigation, map clusters, mobile bottom sheet and expanded inspector.

![Original Atlas phone and expanded Fold](references/primary/01-original-atlas.png)

2. **Original Morph:** inspiration for expressive light colors, shapes, typography and visual warmth only. Remove every yellow, lime and yellow-green interface color. Do not adopt Morph's information architecture in place of Atlas.

![Original Morph phone and expanded Fold](references/primary/02-original-morph.png)

3. **Light Atlas screen board:** examples of the light-theme interpretation across Universe, People, Projects, Documents and Ideas. Use as a secondary visual reference; the actual screen requirements below supersede the mockup composition.

![Light Atlas design board](references/primary/03-light-atlas-screen-board.png)

4. **Approved visual direction illustration:** the original spatial idea with the proposed no-yellow light palette, recreated as a portable diagram.

![Atlas visual direction](references/primary/04-visual-direction.png)

5. **Color palette:** exact proposed starting values, subject to contrast verification. The written and JSON values are authoritative.

![Atlas color palette](references/primary/05-color-palette.png)

## Product design rationale

Atlas is an AI-native personal knowledge and productivity operating system designed primarily for Android phones, foldables, tablets and expanded desktop-style Android environments. It must not feel like a traditional productivity app with AI added, or a chatbot with file storage. It should become the place where a person puts virtually anything from daily life and trusts that they can find it, understand it, connect it and act on it later.

The chosen direction combines the original Atlas spatial knowledge graph with Morph's expressive pastel light theme. Absolutely no yellow, lime, chartreuse, mustard, gold, amber or yellow-green interface accents. The design must be light, expressive, sophisticated, soft and inviting without looking childish. Build a native Android application rather than wrapping a website inside an APK.

### Recommended development foundation

| Component | Recommendation |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 customized for Atlas |
| Foldables | Compose Material 3 Adaptive |
| Database | Room / SQLite |
| Architecture | Modular layered architecture, unidirectional data flow |
| Background work | WorkManager |
| Spatial map | Custom Compose graphics and graph layout engine |
| Files | Encrypted application-private storage |
| Search | Local full-text search, optional semantic vector retrieval |
| AI | Capability-based on-device and configurable providers |
| Authentication | Android biometric and device authentication |
| Development | Android Studio and Gradle |
| Distribution | Installable APK and Android App Bundle |

Android's adaptive libraries support navigation rails, multi-pane layouts, resizing and folding postures. On-device AI varies by device and model, so core Atlas must never depend entirely on it. Verify current stable dependencies and capabilities at implementation time.

---

# MASTER AI DEVELOPMENT PROMPT

## 1. Development mission

Act as a senior Android software architect, mobile UX designer, AI systems engineer, database architect, security engineer and quality assurance engineer.

Design and build a complete, production-quality native Android application named **Atlas**. Atlas is an AI-native personal knowledge, information management, spatial exploration and productivity application. It must be fully functional, not simply a visual prototype, collection of static screens, website wrapped in an APK, or demonstration containing simulated functionality.

Build Atlas as a next-generation application intended for 2027 and beyond, with particular emphasis on Android smartphones, Samsung Galaxy Z Fold devices, Android tablets and expanded desktop-style Android environments. Use the supplied Atlas reference images as the foundation for information architecture, application structure and spatial interaction. Use the Morph reference images as inspiration for light-mode visual design, typography, expressive shapes and colors, excluding all yellow and yellow-green colors. Do not reproduce the dark Atlas theme as the primary interface.

## 2. Product identity

**Name:** Atlas  
**Tagline:** Your Personal Knowledge Universe.  
**Core philosophy:** Capture anything once. Make it useful everywhere.

Atlas is a single-user personal intelligence system that allows a person to capture virtually any information, understand its meaning, establish relationships with existing information, retrieve it intelligently and transform it into meaningful actions. Eliminate the traditional requirement to manually decide where information belongs before saving it.

Example user input:

> Talked with Jamie today about replacing the siding. He said he could start next month and will send me an updated estimate Friday. Remember to follow up.

Atlas should recognize Jamie as an existing or potentially new person, a relationship to an existing home renovation project, a discussion involving siding replacement, a potential upcoming milestone, an expected contractor estimate, a proposed Friday follow-up task, a new project activity entry, and related existing documents and notes. Preserve the original text and allow the user to review, accept, modify or reject suggested relationships and actions. Make the information useful across the entire application without requiring duplicate entry.

## 3. What Atlas is and is not

Atlas combines personal knowledge management, intelligent document management, personal memory, AI-powered retrieval, spatial exploration, personal contact and relationship management, task and project management, brainstorming and visual canvases, context-aware assistance and intelligent information resurfacing.

Atlas is not a traditional folder-based file manager, generic chatbot, conventional notes application, business collaboration platform, complicated enterprise project management system, application requiring manual construction of an entire graph, or disconnected tools sharing a navigation menu. All features operate on one connected information architecture.

## 4. Foundational user experience

1. **Capture:** Bring information into Atlas.
2. **Understand:** Analyze and identify meaningful information.
3. **Connect:** Establish relationships with existing knowledge.
4. **Explore:** Navigate visually or traditionally.
5. **Recall:** Retrieve relevant information at the appropriate time.
6. **Act:** Convert knowledge into useful outcomes.

All six activities must be accessible without forcing a traditional AI chat session. AI participates naturally throughout the experience. Every saved item has durable identity, creation history, original source, relevant relationships and editable metadata. A document linked to three projects remains one document with three associations, not three copies.

![Connected intelligence architecture](references/primary/06-connected-intelligence.png)

## 5. Visual design system

### 5.1 Overall aesthetic

Create a modern, premium, expressive and approachable Android application combining original Atlas spatial knowledge, Morph expressive pastels, Material 3 adaptive interaction, generous whitespace, soft organic shapes, strong typography, calm structured presentation, subtle depth, meaningful motion and excellent accessibility. Sophisticated for professional work, inviting for personal use. Avoid generic AI-generated SaaS dashboards. Each screen's layout must suit its actual purpose while retaining consistent identity.

### 5.2 Mandatory light-mode palette

| Token | Hex | Purpose |
|---|---|---|
| Background | #FBF7FC | Main application background |
| Surface | #FFFFFF | Cards, editors and document surfaces |
| Surface secondary | #F4EDF8 | Secondary interface regions |
| Primary | #7443B1 | Primary purple actions |
| Primary container | #E7D8FC | Selected navigation and highlights |
| Orchid | #C3A6F4 | Expressive visual accents |
| Peach | #FFDDD3 | Ideas and warm accent surfaces |
| Coral | #C75D58 | Important attention indicators |
| Cool mint | #D5EFEB | Tasks and positive information |
| Deep teal | #286D65 | Positive actions and indicators |
| Powder blue | #DCE8FA | Documents and informational surfaces |
| Deep plum | #352044 | Primary text |
| Secondary text | #61566B | Secondary text |
| Border | #DDD4E5 | Dividers and boundaries |
| Error | #B3263E | Errors and destructive actions |

Tokens are design starting points. Validate every foreground/background combination for accessible contrast; adjust darker foreground tokens where necessary. Use the standard palette rather than Android wallpaper-derived dynamic colors that could introduce banned hues.

**Strict prohibited colors:** yellow, lemon, lime, chartreuse, fluorescent yellow-green, mustard, gold, amber and yellow-green gradients. Do not use for status, warnings, charts, illustrations, highlights, backgrounds, selected states, icons or decorative interface elements. Use peach, coral, plum, teal or accessible neutrals instead. This restriction applies to designed UI, not original user-uploaded photos or documents.

### 5.3 Object colors

People: lavender. Projects: purple. Documents: powder blue. Tasks: cool mint. Ideas: peach. Notes: soft orchid. Links: muted blue. Images: soft coral. Audio recordings: rose-lavender. Collections: purple-lavender. Timeline events: teal and plum. Colors supplement, not replace, icons and labels.

### 5.4 Typography

Use a high-quality variable sans-serif font, preferably Roboto Flex or another appropriately licensed Android-compatible typeface. Display 34–40sp; page titles 28–32sp; section headings 20–24sp; card headings 16–18sp; body 16sp; secondary 13–14sp; small metadata 12sp minimum where practical. Support system font scaling without clipping. Confident titles, readable content.

### 5.5 Shapes and spacing

Underlying 4dp spacing grid. Page padding 16–24dp; card spacing 12–16dp; small surface radius 12dp; standard cards 18–22dp; expressive containers 24–32dp; appropriately rounded buttons/chips; touch targets at least 48dp. Graph clusters may have organic irregular rounded shapes if readable. Avoid excessively rounding every element; documents, data tables and editors retain structure.

### 5.6 Shadows and backgrounds

Soft restrained elevation and low-opacity shadows. Subtle lavender, peach or mint organic background shapes may be used without interfering with reading. Avoid heavy glassmorphism, translucent text containers, neon effects, excessive gradients and glowing borders. White or porcelain backgrounds for content-heavy screens.

### 5.7 Icons

Consistent recognizable icon family with meaningful accessibility descriptions. Atlas logo is a distinctive cluster of interconnected dots, representing a personal knowledge universe. Simple outline navigation icons with filled/highlighted selected states.

### 5.8 Motion

Motion communicates relationships, navigation and state: smooth node expansion, animated focal-object transitions, shared-element transitions where suitable, spring-based bottom sheets, smooth compact-to-expanded transitions, subtle confirmation and drag-and-drop feedback. No continuous purely decorative animation. Support reduced motion and a low-motion map setting.

## 6. Adaptive Android architecture

Do not simply scale one screen. Support standard phones, narrow Fold cover display, unfolded inner display, portrait/landscape, tablets, Samsung DeX and Android desktop-style windows, resizable windows and split-screen multitasking. Use actual window dimensions and posture, not model/manufacturer checks.

### 6.1 Compact phones

Compact top app bar, full-width primary content, bottom navigation, expandable contextual sheets, full-screen detail for complex objects, thumb-accessible actions, persistent or easily accessible universal capture. No permanent desktop sidebar on narrow screens.

### 6.2 Unfolded Fold

Use width intelligently: navigation rail, primary workspace, context inspector/detail panel and AI command input as space permits. Universe: map beside selected object. Documents: list/detail or reader/AI side by side. Do not force three unreadable panes just because unfolded.

### 6.3 Expanded tablet and desktop

Navigation rail/expanded sidebar, primary content, optional supporting pane, inspector, resizable panes where practical. Keyboard shortcuts, mouse, wheel scrolling, hover, right-click context menus and drag-and-drop. Desktop here means expanded Android/DeX/windowed layouts, not a separate native Windows/macOS product.

### 6.4 Screen continuity

Preserve destination, selected object, graph focal point and viewport, active document and page, search, unsaved input, recording, canvas selection, scroll, AI conversation and pending approvals during folding, unfolding, rotation or resizing. Never reset to Home just because configuration changes.

## 7. Navigation

### 7.1 Primary destinations

Universe, People, Projects, Documents, Ideas, Tasks, Collections.

### 7.2 Workspace modes

Map, Canvas, Insights. These are contextual modes for the currently selected topic/object. Map is the default connected knowledge visualization. Canvas is a manually editable spatial workspace. Insights is AI-supported contextual analysis.

### 7.3 Compact navigation

Recommended bottom bar: Universe — Tasks — Capture — Search — More. More exposes People, Projects, Documents, Ideas, Collections, Canvas, Insights, Timeline and Settings. Optionally allow configuration of frequently used destinations. Global Capture stays prominent.

### 7.4 Expanded navigation

Original Atlas rail/sidebar with all seven destinations; Map, Canvas, Insights in workspace toolbar; global Search, AI, Notifications, Profile controls; universal Add/Capture near bottom of rail.

### 7.5 Contextual navigation

- Project: Overview, Connections, Files, Tasks, Timeline, Activity.
- Person: Overview, Connections, Notes, Projects, Documents, Activity.
- Document: Preview, Summary, Key Insights, Connections, Actions, Activity.
- Idea: Overview, Related Ideas, Sources, Canvas, Actions.

Do not force identical tabs on all object types.

## 8. Screen-by-screen requirements

### Screen 01 — Onboarding

**Purpose:** Introduce Atlas without complicated configuration. Briefly explain capture, connection and retrieval. Create a local personal workspace, configure optional AI, explain privacy settings, offer biometric protection and let users start immediately. Skip wherever possible; no external account required for local functionality.

**Phone:** Short expressive onboarding sequence. **Expanded:** Visual explanation alongside setup choices.

### Screen 02 — Universe

**Purpose:** Primary home and living representation of connected knowledge; Atlas's defining experience. Interactive map centered on the selected focal object: entire personal universe, project, person, idea, document, collection, question or active task.

Example Home Renovation shows People, Documents, Tasks, Ideas, Timeline and Related Projects clusters. Clusters show actual previews, not just category names: interactive task checkboxes, real document thumbnails, saved photos/initials. Selecting an object smoothly reorganizes the map around it.

Support focal selection, relationship and object-type filters, within-map search, zoom, pan, fit, pinning, expansion/collapse, back to previous focal object, accessible list representation, and opening in Canvas/Insights. Progressively expand neighboring relationships instead of thousands of visible nodes.

**Phone:** Map occupies most of display with expandable contextual sheet. **Fold/Desktop:** Central map plus Context/Sources/Activity inspector.

### Screen 03 — Contextual object inspector

**Purpose:** Access selected object's important information without losing context. Title, type, thumbnail/icon, status, description, connection count, metadata, contextual tabs, related objects and actions. Context explains why it matters using verified saved information. Sources shows documents, notes, links and evidence. Activity shows meaningful changes/interactions. Pin or dismiss inspector.

**Phone:** Draggable bottom sheet (collapsed, expanded, full-screen). **Fold/Desktop:** Dedicated supporting pane.

### Screen 04 — People

**Purpose:** Personal knowledge about people and their relationships, more than contacts. Associate people with projects, documents, notes, discussions, tasks, important dates, contact details, ideas, related people and activity. Search, filter, sort, favorites, duplicate detection. Editable photo/initials, name, role, organization, contact methods, related content and recent activity. AI may suggest duplicates but never merge without approval. No meaningless relationship scores.

**Phone:** Searchable list and full-screen profile. **Fold/Desktop:** List/grid, selected profile and related knowledge.

### Screen 05 — Projects

**Purpose:** Organize personal and professional goals (renovation, research, travel, software, book writing). Name, description, cover, status, start/target dates, tasks, files, people, ideas, notes, links, milestones, related projects and optional budget. Filters Active, Planning, On Hold, Completed, Archived. Progress based on actual task/milestone data or explicitly manually entered. Never invent budgets or completion estimates.

**Phone:** List/cards and project pages. **Fold/Desktop:** Grid/list beside selected project overview.

### Screen 06 — Project detail

**Purpose:** Complete project-specific working environment. Overview, description, progress, next milestone, open tasks, decisions, people, files/images, recent activity, timeline, related projects, AI insights and project map. Create, edit, archive, delete. Contextual AI examples: compare all contractor estimates; outstanding decisions; blockers; find permit documents. Answers cite underlying information.

### Screen 07 — Documents

**Purpose:** Unified library of captured and created information. Although labeled Documents in original navigation, includes PDFs, Word, Excel, PowerPoint, text/Markdown, notes, images, screenshots, audio, saved websites, links and supported attachments. List/grid, search, sort, filter, multi-selection. Filter by type, project, person, collection, date, processing status. Preview, title, type, metadata. Documents remain accessible when AI fails.

**Phone:** Searchable list/gallery. **Fold/Desktop:** Library plus chosen preview/detail.

### Screen 08 — Document reader and intelligence

**Purpose:** Original reader plus intelligence. PDFs: scroll, zoom, page navigation, extraction and OCR when supported. Office files: safe appropriate previews and extraction; open original in compatible app. Do not claim full Office editing fidelity unless implemented/tested.

Original file, extracted text, summary, findings, connected items, proposed tasks, source references, annotations, processing history. Link AI answers to pages, sections, paragraphs, sheets or cells when supported.

**Phone:** Full-screen reading and intelligence sections. **Fold/Desktop:** Side-by-side original and AI analysis.

### Screen 09 — Ideas

**Purpose:** Capture unstructured thoughts and develop over time. One sentence, image, recording, sketch or references. Quick capture, rich notes, sources, images, links, status, optional tags, connected ideas, project conversion, task creation and brainstorming. AI suggests related past ideas/next steps; do not automatically turn every idea into a project/task.

**Phone:** Expressive cards and focused editor. **Fold/Desktop:** Visual gallery, selected editor or brainstorming canvas and related material.

### Screen 10 — Tasks

**Purpose:** Actionable work across the universe. Independent or linked tasks; title, description, status, due date, priority, project, related person, documents, notes/links, subtasks, recurrence, dependencies, reminders, completion history. Today, Upcoming, All, Blocked, Completed; list, board, calendar as appropriate. AI-created tasks require approval unless the user specifically authorized that automation.

**Phone:** Accessible lists grouped by time/status. **Fold/Desktop:** Board/list/calendar and task inspector. Priority indicators in plum/coral/teal or approved neutrals, never yellow.

### Screen 11 — Timeline

**Purpose:** History, milestones and upcoming events. Historical activity and planned task/project timeline are distinct. Include notes, document additions, changes, completions, discussions, user events, deadlines, milestones. Filter date, project, person, collection and object type. AI questions: what was I working on last February; when did I change materials; what changed in the last two weeks? Distinguish recorded events from plans and suggestions.

**Phone:** Vertical chronological view. **Fold/Desktop:** Expanded timeline, calendar or interactive project timeline.

### Screen 12 — Collections

**Purpose:** Flexible grouping without traditional folders. Home, Work, Family, Travel, Learning, Research, Personal Ideas. Mixed object types; multiple memberships without duplicate records. Manual and smart collections, favorites, custom ordering, covers, descriptions, connected maps, scoped search.

**Phone:** Gallery/list. **Fold/Desktop:** Multi-column gallery and selected contents.

### Screen 13 — Map

**Purpose:** Deliberate exploration of relationships using the same graph as Universe. Direct/indirect connections, topics, shared people/docs, dependencies, evidence, related ideas. Select edges to explain why connected (e.g. shared contractor and estimate). User-confirmed and AI-proposed edges visually distinct (dashed plus accessible labels). Create/edit/remove/approve relationships; reject invalid endpoints and unintended duplicates. Accessible non-spatial alternative.

### Screen 14 — Canvas

**Purpose:** Freeform visual planning and brainstorming, distinct from the automatically arranged Map. Infinite or practically unbounded workspace, smooth pan/zoom, drag, multi-select, resize, connect, text, sticky notes, images, document previews, task/project/idea/person cards, lists, shapes, supported drawing, undo/redo, fit, persistent layouts.

Objects reference existing records or canvas-local content; promote local content into durable knowledge. No duplicate records for multiple placements. AI can propose layout but must not move user placements without authorization.

**Phone:** Touch canvas and collapsible tools. **Fold/Desktop:** Expanded canvas, creation toolbar and optional inspector.

### Screen 15 — Insights

**Purpose:** Useful contextual understanding based on evidence. Project summaries, important changes, outstanding decisions, upcoming deadlines, conflicts, duplicates, related knowledge, forgotten relevant notes, missing info, proposed actions and sourced comparisons. No meaningless statistics or fabricated health/risk/budget scores. Every insight identifies source, scope and generation time. Dismiss/save/correct/act.

**Phone:** Prioritized scrolling feed. **Fold/Desktop:** Organized analysis with evidence panes.

### Screen 16 — Universal Capture

**Purpose:** Minimal friction; one of the most important screens. Universal input with restrained expressive styling. Type, paste text/URL, upload one/multiple files, images, photographs, scanning, voice recording, supported audio import, Android Share menu, deliberate structured creation. No category required before saving. Quick and structured capture both supported.

Persist originals immediately after submission; AI processing separate from successful capture. States Saved, Processing, Ready for Review, Connected, AI Unavailable, Processing Failed. Retry/cancel; preserve original on failure.

**Phone:** Thumb-accessible sheet/full editor. **Fold/Desktop:** Capture plus attachment preview and proposed links.

### Screen 17 — Capture review inbox

**Purpose:** Control over interpretation. Original content, proposed title/types, extracted people/orgs/dates, projects, relationships, tasks, duplicates and evidence. Approve all/individually, edit, reject, defer. Unreviewed suggestions are not user-confirmed facts. Explain proposed changes precisely.

### Screen 18 — Global Search

**Purpose:** Retrieve any captured supported information. Titles, extracted full text, notes, people, projects, tasks, dates, tags, associated items, exact phrases, optional semantic meaning. Examples: PDF about security controls last summer; estimate including garage; what Jamie said about materials; garden app idea. Combine exact, full-text, graph and optional semantic retrieval. Distinguish exact matches, related records and AI interpretations. Filters, sorting.

**Phone:** Full-screen grouped results. **Fold/Desktop:** Results and previews/optional explanations.

### Screen 19 — Ask Atlas

**Purpose:** Conversational actionable intelligence grounded in personal knowledge, globally accessible. Scope all knowledge, current project/person/document/collection/canvas or selected sources. Text, voice, attachments. Return prose, sources, checklists, comparison tables, timelines, related info, plans, drafts and suggested maps. Save outputs as notes, proposed tasks/projects or canvas items. Use validated predefined UI component schemas, never arbitrary AI-generated executable UI code. Acknowledge insufficient evidence.

### Screen 20 — Notifications and reminders

**Purpose:** Meaningful resurfacing without interruption overload. User reminders, upcoming tasks, milestones, processing completion, review requests, optional suggested reminders. Category/frequency/quiet-hour control. Sensitive content hidden on lock screen by default. Use supported Android scheduling and disclose limits on exact delivery.

### Screen 21 — Profile and Settings

**Purpose:** Control appearance, intelligence and privacy. Profile, appearance, accessibility, navigation, providers/permissions, capture, document handling, notifications, storage, backup/restore, privacy/security, import/export, trash, about/diagnostics. Light is default; optional dark architecture, not the priority. No yellow accents introduced by customization.

### Screen 22 — AI settings

**Purpose:** Transparent intelligence. Providers, active provider, on-device availability, model readiness, download/setup, capabilities, status, limits, recent AI activity. Disable AI entirely. Separate content analysis, semantic indexing, relationship suggestions, proactive insights, external processing permissions. External transmission only with explicit config and consent; no silent cloud fallback.

### Screen 23 — Backup, restore and data management

**Purpose:** User ownership. Full workspace, original files, structured knowledge, relationships and canvas exports; import; encrypted backup; integrity verification; restore preview; usage; trash. Document portable JSON/Markdown/CSV and original file formats. Preserve IDs, links, attachments. Never silently overwrite existing workspace.

### Screen layout completion notes

For screens that have no separately prescribed layout in the original specification, apply the same compact/expanded pattern: phone uses a single task-focused column or full-screen editor; expanded uses a navigation rail and list/detail or primary workspace with a supporting pane. Related Projects is a navigable filtered relationship view, not a separate copy of the project store. Overview, Connections, Files, Tasks, Context, Sources and Activity visible in the reference images must all be functional destinations or tabs, not decorative labels. Search, AI sparkle, notification bell, avatar/profile, plus/add, inspector expand, overflow, attachments and microphone must have working accessible interactions.

## 9. Connected knowledge architecture

### 9.1 Objects

Unified persistent identity. Types: Person, Project, Task, Idea, Note, Document, Image, Website, Link, Audio recording, Collection, Event, Decision, Canvas. Common identity, type, title, timestamps, lifecycle, revision, provenance. Validated type-specific models; avoid arbitrary JSON for all important records.

### 9.2 Relationships

First-class persistent records: Related to, Belongs to, References, Created by, Discussed with, Supports, Depends on, Assigned to, Derived from, Part of, Contradicts, Follow-up to, Duplicate of, Associated with. Fields: unique ID, source, target, type, optional description, creation time, origin, review status, optional evidence and appropriate revisions. Origins user-created/extracted/imported/AI-suggested. Confidence is not verified probability of truth.

### 9.3 Original versus derived

Distinguish original capture, user-authored data, extracted information, AI-generated information and approved suggestions. Summaries never replace originals. Preserve source references/versions. Mark derived information potentially stale after source changes.

### 9.4 Database entities

At least KnowledgeObject, Person, Project, Task, Note, Idea, Document, Attachment, Relationship, Collection, CollectionMembership, SourceReference, ContentChunk, EmbeddingRecord, Capture, ProcessingJob, AISuggestion, AIInsight, AIConversation, AIMessage, TimelineEvent, Canvas, CanvasItem, CanvasConnector, Reminder, Revision, AuditEvent, ApplicationSettings. Use transactions, foreign keys, indexes, migrations and repository boundaries. Prevent orphaned records and accidental deletion of shared attachments.

## 10. AI intelligence architecture

### 10.1 Capability layer

Provider abstraction, never tightly coupled to one model/vendor/cloud. Advertise actual text generation, structured extraction, image understanding, transcription, summarization, embeddings, contextual QA and tool-calling capabilities. Use applicable Gemini Nano/ML Kit on-device APIs where available. External/private providers optional. Degrade gracefully and never fake successful inference.

### 10.2 Capture processing pipeline

1. Capture original information.
2. Validate and persist original.
3. Identify format.
4. Extract supported text/metadata.
5. OCR/transcribe when supported.
6. Create searchable chunks.
7. Generate embeddings when configured/supported.
8. Identify relevant knowledge.
9. Request structured AI extraction if authorized.
10. Propose objects and relationships.
11. Validate proposals.
12. Present review.
13. Apply approved changes transactionally.
14. Update indexes and dependent views.

Durable retryable jobs surviving restarts and interruption; idempotent to prevent duplicates.

### 10.3 Retrieval-augmented intelligence

Full text, filters, optional semantic retrieval, graph traversal, relationship relevance and meaningful recency. Do not send whole database for every request. Select relevant excerpts within model context limits. Answers link to originals; history and evidence distinguishable.

### 10.4 Structured output

Schema validation and allowlisted actions for all proposed application changes. No unrestricted model access to database, filesystem, OS or arbitrary network. Validate permissions, ownership, identifiers, types and proposed changes. Never directly execute AI-generated SQL/code.

### 10.5 Proactive intelligence

Optional, scoped and frequency-controlled. Examples: remind follow-up with Jamie and surface estimate; three documents discuss the same requirement; similar idea from six months ago; two tasks depend on unresolved decision. Avoid continuous unnecessary background AI; respect battery, thermal, network and Android restrictions.

## 11. Local-first storage and privacy

Core works offline: text capture, local import, recording, record CRUD, local document viewing, tasks, saved graph, canvas, full-text search and prior AI results. Unavailable AI clearly labeled, core remains usable.

Use Android Keystore-backed key management, appropriately encrypted sensitive DB and files, private directories, secure temp files, optional biometric lock, validated imports, documented secure deletion limits, restricted exports and explicit transfer consent. No ads, analytics tracking or telemetry by default. No secrets/documents in logs. Disable/protect system backup of sensitive plaintext. Never execute imported macros/scripts or unsafe content. Treat imported material as untrusted data, not AI instructions. Prevent document prompt injection from authorizing actions.

## 12. Android implementation requirements

Native Kotlin, Compose, Material 3, Compose Adaptive, WindowManager where needed, Android Navigation, ViewModel, Coroutines/Flow, Room/SQLite, WorkManager, DataStore for non-sensitive prefs, Keystore, Biometrics, Storage Access Framework, Photo Picker, CameraX or appropriate capture APIs.

Current stable mutually compatible dependencies; isolate experimental libraries behind adapters with stable fallbacks. Verify library availability, licenses and devices. Runtime feature detection for on-device AI. Consider API 26 initial minimum only if compatible with chosen stable dependencies and security; raise with documented rationale. Target current stable SDK and satisfy distribution requirements.

Coherent modules/bounded packages: app shell/navigation, design system, adaptive layout, models, database, secure storage, repositories, capture/ingestion, document processing, AI provider abstraction/orchestration, search/indexing, graph, canvas, Universe, People, Projects, Documents, Ideas, Tasks, Collections, Insights, Timeline, Settings/security, shared tests. Unidirectional flow and well-defined interfaces; avoid unnecessary layers.

## 13. Performance

Lazy loading, pagination, cached thumbnails, DB indexes, background extraction, incremental indexing, cancellation, viewport culling, progressive graph expansion, spatial hit testing, bounded memory, proper image decoding, battery-conscious jobs. Never render entire graph at once. Cache stable layouts, preserve pins. Benchmark real devices or representative emulators. Set measurable capture responsiveness, map smoothness and search-latency targets; report measured results, not unsupported guarantees.

## 14. Accessibility

Target WCAG 2.2 AA principles where applicable to Android and Android guidance. TalkBack, labels, logical traversal, text scaling, contrast, reduced motion, large touch targets, keyboard navigation, accessible errors, non-color status and non-spatial alternatives. Graph cannot be the only route to any record or relationship; users unable to manipulate canvas must still access underlying info.

## 15. Core acceptance scenario

User creates Home Renovation, adds contractor Jamie, uploads two PDF estimates, photographs house, records a discussion, saves siding website and enters follow-up reminder. Atlas preserves originals, indexes supported content, proposes reviewable links, recognizes existing people/project when evidence supports it, proposes nonduplicate tasks, displays links in Universe, person info in People, files in Documents, approved tasks in Tasks, activity in Timeline, allows existing objects on Canvas and gives sourced project answers.

User asks: “Compare the two estimates, tell me what is different, and show me what still needs to be done.” Atlas retrieves evidence, renders structured comparison with references and next-step proposals. Unfold during review: question, documents and selection persist into expanded layout. Restart: approved data and links persist. Repeat with AI off; manual workflows remain operational.

## 16. Testing and quality gates

A screen is not complete because it looks like a reference; every control has real tested purpose.

Test CRUD, relationship integrity, duplicates, migrations, search correctness, imports, failed extraction, interrupted AI, unsupported AI devices, invalid AI output, injection resistance, backup integrity, restore, offline, notification permission, canvas persistence, undo/redo, graph interaction, fold/unfold, rotation/resize, screen reader, text sizes, backgrounding, process death/restoration, denied storage permission, large attachments, memory/performance.

Visual verification on compact, narrow Fold cover, unfolded Fold, tablet and expanded Android windows. Screenshot regression tests; validate light palette and automated design-token checks for yellow/yellow-green. Review security, dependencies and data protection. No placeholder controls masking broken functionality.

## 17. Implementation phases

Vertical functional increments, not all screens first and behavior later.

- **Phase 0 — Planning & architecture:** Requirements, ADRs, data model, navigation map, tokens, screen inventory, threat model, test strategy, backlog, Git.
- **Phase 1 — Native foundation:** Native project, light design system, adaptive navigation, persistent DB, secure attachments, app state, basic settings. Verify compact/expanded.
- **Phase 2 — Capture & knowledge:** Capture, notes, imports/images, people/projects/tasks, common records, manual relationships, local search. Verify persistence.
- **Phase 3 — Universe & spatial:** Graph, visual links, node interaction, inspector, filters, focal navigation, Fold layouts. Verify state/performance.
- **Phase 4 — AI:** Provider adapters, capabilities, extraction, summaries, structured entity extraction, link suggestions, review, contextual QA, sources. No mandatory cloud provider.
- **Phase 5 — Extended workspaces:** Canvas, collections, timeline, ideas, advanced projects, task boards, expanded reader/intelligence, Insights.
- **Phase 6 — Personal intelligence:** Optional proactive insights, reminders, resurfacing, supported semantic retrieval, structured responses. Verify consent for consequential actions.
- **Phase 7 — Security & release:** Hardening, accessibility, benchmarks, backup/restore, compatibility, visual regressions, docs, appropriate owner-controlled signing.

## 18. Required deliverables

Complete native source, reproducible Gradle project, debug APK, release configuration, AAB or unsigned release artifact as appropriate, schemas/migrations, design documentation, architecture and navigation documentation, AI configuration, security/privacy and backup guides, automated tests, reports, known limitations, dependency/licensing inventory, build instructions, implementation ledger and Git history. Each verified checkpoint: updated source archive with Git history, status and test evidence. Never claim unexecuted tests or production readiness with mocked, incomplete or untested critical functions.

## 19. Non-negotiable requirements

1. Real native Android app.
2. Original Atlas spatial identity.
3. Morph-inspired light design.
4. No yellow or yellow-green UI colors.
5. Compact phones and Fold layouts.
6. Adaptive tablet/desktop Android windows.
7. Capture supported information.
8. Preserve originals.
9. Persistent relationships.
10. Manual editing and correction.
11. Optional transparent AI.
12. Useful offline functionality.
13. Source-backed intelligence.
14. Privacy protection.
15. State continuity across configuration changes.
16. Functional interactions.
17. Complete data export and recovery.
18. Documented functional, persistence, security, accessibility and visual test gates.

**Atlas should become more useful as the user's knowledge grows, without becoming more complicated to operate.**

---

# Supporting product direction and design rationale

Atlas pages must not be separate applications. They operate on a shared persistent knowledge universe: originals, objects and relationships. Capture anything → understand and index → review and connect → persistent Atlas knowledge → explore (Map & Canvas), recall (Search & AI), act (Tasks & Work).

Imagine photographing something interesting when shopping, recording a thought while driving safely, saving a PDF during work, typing a reminder at night and bookmarking a website next morning. Most apps make these unrelated pieces. Atlas could connect them and retrieve them when a decision needs them. The distinction is not the graph itself; it is making information useful without constant manual management and organization.

Protect four areas during development:

| Area | Why it matters |
|---|---|
| Universal capture | Inconvenient capture undermines the entire system. |
| Knowledge relationships | Makes Atlas more than file and note management. |
| Intelligent retrieval | Users find information even without remembering its name. |
| Adaptive spatial interface | Particularly compelling on Fold devices. |

Resist adding dozens of features until these four work exceptionally well together. Implement and verify one functional increment at a time rather than generating all 23 screens and all AI features in one unverified pass.

## Official technical references

- [Android adaptive application development](https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps)
- [Adaptive list-detail layouts](https://developer.android.com/develop/adaptive-apps/guides/list-detail)
- [Compose Material 3 Adaptive releases](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive)
- [Offline-first Android architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Google ML Kit generative AI](https://developers.google.com/ml-kit/genai/)
- [Gemini Nano on Android](https://developer.android.com/ai/gemini-nano)

Some on-device AI features may have experimental APIs, hardware restrictions or availability limitations. Verify the current stable documentation during implementation. Isolate capabilities behind adapters so changing one AI library cannot break the app.

## Reference priority and known visual discrepancies

- `references/primary/01-original-atlas.png`: original user-selected Atlas structure. Dark palette is NOT the requested final design.
- `references/primary/02-original-morph.png`: colors/shapes inspiration. Yellow/lime in this reference is explicitly forbidden in Atlas.
- `references/primary/03-light-atlas-screen-board.png`: closest supplied multi-screen light design, not an exhaustive screen contract.
- `references/primary/04-visual-direction.png` and `.svg`: no-yellow spatial direction illustration recreated from the chat diagram.
- `references/primary/05-color-palette.png` and `.svg`: palette chart; exact values in this document and `design-tokens.json`.
- `references/primary/06-connected-intelligence.png` and `.svg`: connected intelligence flow recreated from the chat diagram.
- `references/layout-only/`: six earlier screen mockups; retain structural ideas only. They contain disallowed yellow-green hues and a too-wide compact navigation rail. Do not copy these defects.
- `references/explorations/`: earlier dark and/or mixed design boards. Not approved final styling; optional screen inventory inspiration only. Do not copy neon, dark backgrounds or heavy glass effects.

This document is a consolidated version of the prior conversation specification, with the same design and behavioral requirements and portable references. It is not a claim that an Android app has already been built or that every mockup is an approved implementation.
