# Data model (schema v1)

| Table | Key columns | Notes |
|---|---|---|
| `objects` | id PK, type, title, body, created, updated, trashed, pinned, favorite, origin; person: role, org, email, phone; project: project_status, start_date, target_date; task: due, done, completed_at, priority, blocked; files: mime, vault_file, url, size_bytes, extracted; event: event_date | One durable identity per knowledge object. Types: PERSON, PROJECT, TASK, IDEA, NOTE, DOCUMENT, IMAGE, LINK, COLLECTION, EVENT, DECISION. |
| `relationships` | id PK, source_id FK→objects, target_id FK→objects, type, origin, review, evidence, created | UNIQUE(source,target,type). Cascade delete. Origins: USER, EXTRACTED, IMPORTED, AI. Review: CONFIRMED, PROPOSED, REJECTED. |
| `captures` | id PK, text, note_id FK, attachment_id FK, created, status | Original input kept verbatim. Status: SAVED, READY_FOR_REVIEW, CONNECTED, NOTHING_FOUND, FAILED. |
| `suggestions` | id PK, capture_id FK, kind, obj_type, title, target_id, rel_type, evidence, due, status | Kinds: NEW_PERSON, LINK_EXISTING, NEW_TASK, NEW_EVENT. Status: PENDING, ACCEPTED, REJECTED. |
| `activity` | id PK, object_id FK, at, kind, text | Meaningful changes; feeds Activity tabs and Timeline. |
| `objects_fts` | FTS4(id, content) | title + body + extracted text + person fields. |

Collections are `objects` of type COLLECTION with `PART_OF` relationships, so membership never duplicates records.
