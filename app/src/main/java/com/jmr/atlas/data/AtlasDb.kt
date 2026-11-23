package com.jmr.atlas.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteDatabase.CONFLICT_NONE
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.jmr.atlas.model.*
import org.json.JSONArray
import java.time.LocalDate

/**
 * Atlas schema v2. SQLite with foreign keys, unique constraints, indexes and FTS4, opened through the androidx
 * SupportSQLite interface so the same code runs on SQLCipher (encrypted, on the phone) and on the framework
 * driver (unit tests). See docs/ARCHITECTURE.md ADR-3 and ADR-7.
 */
class AtlasDb(context: Context, name: String? = "atlas.db", factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()) {

    var ftsAvailable = true
        private set

    private val helper: SupportSQLiteOpenHelper = factory.create(
        SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(VERSION) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) = create(db)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = upgrade(db, oldVersion)
            override fun onOpen(db: SupportSQLiteDatabase) {
                ftsAvailable = runCatching { db.query("SELECT count(*) FROM objects_fts").use { it.moveToFirst() } }.isSuccess
            }
        }).build()
    )

    val db: SupportSQLiteDatabase get() = helper.writableDatabase
    fun close() = helper.close()

    private fun create(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE objects(
            id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL DEFAULT '',
            created INTEGER NOT NULL, updated INTEGER NOT NULL, trashed INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0,
            favorite INTEGER NOT NULL DEFAULT 0, origin TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT '', org TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',
            project_status TEXT, start_date TEXT, target_date TEXT,
            due TEXT, done INTEGER NOT NULL DEFAULT 0, completed_at INTEGER, priority TEXT NOT NULL DEFAULT 'NONE', blocked INTEGER NOT NULL DEFAULT 0,
            mime TEXT NOT NULL DEFAULT '', vault_file TEXT NOT NULL DEFAULT '', url TEXT NOT NULL DEFAULT '', size_bytes INTEGER NOT NULL DEFAULT 0,
            extracted TEXT NOT NULL DEFAULT '', event_date TEXT)""")
        db.execSQL("CREATE INDEX idx_obj_type ON objects(type, trashed)")
        db.execSQL("CREATE INDEX idx_obj_due ON objects(due)")
        db.execSQL("""CREATE TABLE relationships(
            id TEXT PRIMARY KEY NOT NULL,
            source_id TEXT NOT NULL REFERENCES objects(id) ON DELETE CASCADE,
            target_id TEXT NOT NULL REFERENCES objects(id) ON DELETE CASCADE,
            type TEXT NOT NULL, origin TEXT NOT NULL, review TEXT NOT NULL, evidence TEXT NOT NULL DEFAULT '', created INTEGER NOT NULL,
            UNIQUE(source_id, target_id, type), CHECK(source_id <> target_id))""")
        db.execSQL("CREATE INDEX idx_rel_source ON relationships(source_id)")
        db.execSQL("CREATE INDEX idx_rel_target ON relationships(target_id)")
        db.execSQL("""CREATE TABLE captures(
            id TEXT PRIMARY KEY NOT NULL, text TEXT NOT NULL,
            note_id TEXT REFERENCES objects(id) ON DELETE SET NULL,
            attachment_id TEXT REFERENCES objects(id) ON DELETE SET NULL,
            created INTEGER NOT NULL, status TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE suggestions(
            id TEXT PRIMARY KEY NOT NULL, capture_id TEXT NOT NULL REFERENCES captures(id) ON DELETE CASCADE,
            kind TEXT NOT NULL, obj_type TEXT, title TEXT NOT NULL, target_id TEXT REFERENCES objects(id) ON DELETE CASCADE,
            rel_type TEXT NOT NULL, evidence TEXT NOT NULL, due TEXT, status TEXT NOT NULL, link_to TEXT NOT NULL DEFAULT '[]')""")
        db.execSQL("CREATE INDEX idx_sug_capture ON suggestions(capture_id)")
        db.execSQL("""CREATE TABLE activity(
            id TEXT PRIMARY KEY NOT NULL, object_id TEXT NOT NULL REFERENCES objects(id) ON DELETE CASCADE,
            at INTEGER NOT NULL, kind TEXT NOT NULL, text TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX idx_act_obj ON activity(object_id, at)")
        createFts(db)
        upgrade(db, 1)
    }

    /** Versioned, additive migrations. Each step only adds; nothing is rewritten or dropped. */
    private fun upgrade(db: SupportSQLiteDatabase, from: Int) {
        if (from < 2) {
            V2_COLUMNS.forEach { (col, def) -> db.execSQL("ALTER TABLE objects ADD COLUMN $col $def") }
            db.execSQL("""CREATE TABLE details(
                id TEXT PRIMARY KEY NOT NULL, object_id TEXT NOT NULL REFERENCES objects(id) ON DELETE CASCADE,
                kind TEXT NOT NULL, label TEXT NOT NULL DEFAULT '', value TEXT NOT NULL DEFAULT '', ftype TEXT NOT NULL DEFAULT 'TEXT',
                sort INTEGER NOT NULL DEFAULT 0, flag INTEGER NOT NULL DEFAULT 0, extra TEXT NOT NULL DEFAULT '', created INTEGER NOT NULL,
                origin TEXT NOT NULL DEFAULT 'USER')""")
            db.execSQL("CREATE INDEX idx_det_obj ON details(object_id, kind)")
            db.execSQL("""CREATE TABLE field_defs(
                id TEXT PRIMARY KEY NOT NULL, obj_type TEXT NOT NULL, label TEXT NOT NULL, ftype TEXT NOT NULL,
                options TEXT NOT NULL DEFAULT '[]', secret INTEGER NOT NULL DEFAULT 0, sort INTEGER NOT NULL DEFAULT 0,
                UNIQUE(obj_type, label))""")
            db.execSQL("CREATE INDEX idx_obj_contact ON objects(contact_key)")
            // People already carrying a single email/phone get them as repeatable details.
            db.query("SELECT id, email, phone FROM objects WHERE type = 'PERSON'").use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    c.getString(1)?.takeIf { it.isNotBlank() }?.let { db.insert("details", CONFLICT_NONE, Detail(objectId = id, kind = DetailKind.EMAIL, label = "Email", value = it).values()) }
                    c.getString(2)?.takeIf { it.isNotBlank() }?.let { db.insert("details", CONFLICT_NONE, Detail(objectId = id, kind = DetailKind.PHONE, label = "Mobile", value = it).values()) }
                }
            }
        }
    }

    private fun createFts(db: SupportSQLiteDatabase) {
        ftsAvailable = runCatching { db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS objects_fts USING fts4(id, content, notindexed=id, tokenize=unicode61)") }.isSuccess
    }

    // ---------------------------------------------------------------- read
    fun loadAll(): Snapshot {
        val d = db
        val objs = d.query("SELECT * FROM objects").use { c -> buildMap { while (c.moveToNext()) c.toObject().let { put(it.id, it) } } }
        val rels = d.query("SELECT * FROM relationships").use { c -> buildList { while (c.moveToNext()) add(c.toRelation()) } }
        val caps = d.query("SELECT * FROM captures ORDER BY created DESC").use { c -> buildList { while (c.moveToNext()) add(c.toCapture()) } }
        val sugs = d.query("SELECT * FROM suggestions").use { c -> buildList { while (c.moveToNext()) add(c.toSuggestion()) } }
        val acts = d.query("SELECT * FROM activity ORDER BY at DESC LIMIT 3000").use { c -> buildList { while (c.moveToNext()) add(c.toActivity()) } }
        val dets = d.query("SELECT * FROM details").use { c -> buildList { while (c.moveToNext()) add(c.toDetail()) } }
        val defs = d.query("SELECT * FROM field_defs ORDER BY sort").use { c -> buildList { while (c.moveToNext()) add(c.toFieldDef()) } }
        return Snapshot(objs, rels, caps, sugs, acts, dets, defs)
    }

    fun keyword(query: String, limit: Int = 60): Map<String, Float> {
        val terms = tokens(query); if (terms.isEmpty()) return emptyMap()
        if (ftsAvailable) runCatching {
            val match = terms.joinToString(" OR ") { "$it*" }
            return db.query("SELECT id, content FROM objects_fts WHERE objects_fts MATCH ? LIMIT ?", arrayOf<Any>(match, limit * 3)).use { c ->
                buildMap { while (c.moveToNext()) { val t = c.getString(1).lowercase(); put(c.getString(0), terms.count { t.contains(it) }.toFloat() / terms.size) } }
            }.entries.sortedByDescending { it.value }.take(limit).associate { it.key to it.value }
        }
        return emptyMap()
    }

    // ---------------------------------------------------------------- write (callers wrap in tx())
    fun <T> tx(block: SupportSQLiteDatabase.() -> T): T {
        val d = db
        d.beginTransaction()
        try { val r = d.block(); d.setTransactionSuccessful(); return r } finally { d.endTransaction() }
    }

    fun SupportSQLiteDatabase.putObject(o: KObject) {
        upsert("objects", o.id, o.values())
        if (ftsAvailable) {
            delete("objects_fts", "id = ?", arrayOf(o.id))
            if (!o.trashed) insert("objects_fts", CONFLICT_NONE, ContentValues().apply { put("id", o.id); put("content", o.searchText()) })
        }
    }
    fun SupportSQLiteDatabase.deleteObject(id: String) {
        delete("objects", "id = ?", arrayOf(id))
        if (ftsAvailable) delete("objects_fts", "id = ?", arrayOf(id))
    }
    /** Returns false if an equivalent relationship already exists (UNIQUE) — prevents duplicate edges. */
    fun SupportSQLiteDatabase.putRelation(r: Relation): Boolean = insert("relationships", CONFLICT_IGNORE, r.values()) != -1L
    fun SupportSQLiteDatabase.updateRelation(r: Relation) { update("relationships", CONFLICT_NONE, r.values(), "id = ?", arrayOf(r.id)) }
    fun SupportSQLiteDatabase.deleteRelation(id: String) { delete("relationships", "id = ?", arrayOf(id)) }
    fun SupportSQLiteDatabase.putCapture(c: Capture) { upsert("captures", c.id, c.values()) }
    fun SupportSQLiteDatabase.putSuggestion(s: Suggestion) { upsert("suggestions", s.id, s.values()) }
    fun SupportSQLiteDatabase.putActivity(a: Activity) { upsert("activity", a.id, a.values()) }
    fun SupportSQLiteDatabase.putDetail(d: Detail) { upsert("details", d.id, d.values()) }
    fun SupportSQLiteDatabase.deleteDetail(id: String) { delete("details", "id = ?", arrayOf(id)) }
    fun SupportSQLiteDatabase.putFieldDef(f: FieldDef) { upsert("field_defs", f.id, f.values()) }
    fun SupportSQLiteDatabase.deleteFieldDef(id: String) { delete("field_defs", "id = ?", arrayOf(id)) }

    /** UPDATE-then-INSERT. Never INSERT OR REPLACE: REPLACE deletes the parent row first, which fires ON DELETE CASCADE on its children. */
    private fun SupportSQLiteDatabase.upsert(table: String, id: String, v: ContentValues) {
        if (update(table, CONFLICT_NONE, v, "id = ?", arrayOf(id)) == 0) insert(table, CONFLICT_NONE, v).also { if (it == -1L) error("insert into $table failed") }
    }

    fun wipe() = tx {
        listOf("suggestions", "captures", "activity", "details", "field_defs", "relationships", "objects").forEach { delete(it, null, null) }
        if (ftsAvailable) delete("objects_fts", null, null)
    }

    companion object {
        const val VERSION = 2
        private val V2_COLUMNS = listOf(
            "photo" to "TEXT NOT NULL DEFAULT ''", "contact_key" to "TEXT NOT NULL DEFAULT ''", "nickname" to "TEXT NOT NULL DEFAULT ''",
            "kind" to "TEXT NOT NULL DEFAULT ''", "doc_date" to "TEXT", "deadline" to "TEXT", "remind_at" to "INTEGER", "repeat" to "TEXT NOT NULL DEFAULT 'NONE'",
            "health" to "TEXT", "summary" to "TEXT NOT NULL DEFAULT ''", "site" to "TEXT NOT NULL DEFAULT ''", "author" to "TEXT NOT NULL DEFAULT ''",
            "published" to "TEXT", "read_minutes" to "INTEGER NOT NULL DEFAULT 0", "is_read" to "INTEGER NOT NULL DEFAULT 0",
            "cover_file" to "TEXT NOT NULL DEFAULT ''", "preview_at" to "INTEGER", "calendar_event_id" to "INTEGER",
        )
        private val STOP = setOf("the", "a", "an", "is", "are", "was", "what", "when", "where", "who", "how", "did", "do", "my", "me", "i", "of", "to", "for", "in", "on", "at", "and", "or", "about", "that", "this", "it", "with", "from")
        fun tokens(q: String) = q.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").split(Regex("\\s+")).filter { it.length >= 2 && it !in STOP }.distinct()

        // ---------------------------------------------------------------- mapping
        private val Boolean.i get() = if (this) 1 else 0
        private fun KObject.values() = ContentValues().apply {
            put("id", id); put("type", type.name); put("title", title); put("body", body); put("created", created); put("updated", updated)
            put("trashed", trashed.i); put("pinned", pinned.i); put("favorite", favorite.i); put("origin", origin.name)
            put("role", role); put("org", org); put("email", email); put("phone", phone)
            put("project_status", projectStatus?.name); put("start_date", startDate?.toString()); put("target_date", targetDate?.toString())
            put("due", due?.toString()); put("done", done.i); put("completed_at", completedAt); put("priority", priority.name); put("blocked", blocked.i)
            put("mime", mime); put("vault_file", vaultFile); put("url", url); put("size_bytes", sizeBytes); put("extracted", extracted); put("event_date", eventDate?.toString())
            put("photo", photo); put("contact_key", contactKey); put("nickname", nickname); put("kind", kind); put("doc_date", docDate?.toString())
            put("deadline", deadline?.toString()); put("remind_at", remindAt); put("repeat", repeat.name); put("health", health?.name); put("summary", summary)
            put("site", site); put("author", author); put("published", published?.toString()); put("read_minutes", readMinutes); put("is_read", isRead.i)
            put("cover_file", coverFile); put("preview_at", previewAt); put("calendar_event_id", calendarEventId)
        }
        private fun Relation.values() = ContentValues().apply {
            put("id", id); put("source_id", sourceId); put("target_id", targetId); put("type", type.name); put("origin", origin.name)
            put("review", review.name); put("evidence", evidence); put("created", created)
        }
        private fun Capture.values() = ContentValues().apply {
            put("id", id); put("text", text); put("note_id", noteId); put("attachment_id", attachmentId); put("created", created); put("status", status.name)
        }
        private fun Suggestion.values() = ContentValues().apply {
            put("id", id); put("capture_id", captureId); put("kind", kind.name); put("obj_type", objType?.name); put("title", title); put("target_id", targetId)
            put("rel_type", relType.name); put("evidence", evidence); put("due", due?.toString()); put("status", status.name); put("link_to", JSONArray(linkTo).toString())
        }
        private fun Activity.values() = ContentValues().apply { put("id", id); put("object_id", objectId); put("at", at); put("kind", kind); put("text", text) }
        private fun Detail.values() = ContentValues().apply {
            put("id", id); put("object_id", objectId); put("kind", kind.name); put("label", label); put("value", value); put("ftype", type.name)
            put("sort", sort); put("flag", flag.i); put("extra", extra); put("created", created); put("origin", origin.name)
        }
        private fun FieldDef.values() = ContentValues().apply {
            put("id", id); put("obj_type", objType.name); put("label", label); put("ftype", type.name); put("options", JSONArray(options).toString()); put("secret", secret.i); put("sort", sort)
        }

        private fun Cursor.s(n: String): String? = getColumnIndex(n).let { if (it < 0 || isNull(it)) null else getString(it) }
        private fun Cursor.l(n: String): Long? = getColumnIndex(n).let { if (it < 0 || isNull(it)) null else getLong(it) }
        private fun Cursor.b(n: String) = l(n) == 1L
        private fun Cursor.d(n: String) = s(n)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        private inline fun <reified E : Enum<E>> Cursor.e(n: String, def: E): E = s(n)?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: def
        private inline fun <reified E : Enum<E>> Cursor.en(n: String): E? = s(n)?.let { v -> enumValues<E>().firstOrNull { it.name == v } }

        private fun Cursor.toObject() = KObject(
            id = s("id")!!, type = e("type", ObjType.NOTE), title = s("title").orEmpty(), body = s("body").orEmpty(), created = l("created") ?: 0, updated = l("updated") ?: 0,
            trashed = b("trashed"), pinned = b("pinned"), favorite = b("favorite"), origin = e("origin", Origin.USER),
            role = s("role").orEmpty(), org = s("org").orEmpty(), email = s("email").orEmpty(), phone = s("phone").orEmpty(),
            projectStatus = en<ProjectStatus>("project_status"), startDate = d("start_date"), targetDate = d("target_date"),
            due = d("due"), done = b("done"), completedAt = l("completed_at"), priority = e("priority", Priority.NONE), blocked = b("blocked"),
            mime = s("mime").orEmpty(), vaultFile = s("vault_file").orEmpty(), url = s("url").orEmpty(), sizeBytes = l("size_bytes") ?: 0, extracted = s("extracted").orEmpty(),
            eventDate = d("event_date"), photo = s("photo").orEmpty(), contactKey = s("contact_key").orEmpty(), nickname = s("nickname").orEmpty(), kind = s("kind").orEmpty(),
            docDate = d("doc_date"), deadline = d("deadline"), remindAt = l("remind_at"), repeat = e("repeat", Repeat.NONE), health = en<Health>("health"),
            summary = s("summary").orEmpty(), site = s("site").orEmpty(), author = s("author").orEmpty(), published = d("published"), readMinutes = (l("read_minutes") ?: 0).toInt(),
            isRead = b("is_read"), coverFile = s("cover_file").orEmpty(), previewAt = l("preview_at"), calendarEventId = l("calendar_event_id"),
        )
        private fun Cursor.toRelation() = Relation(s("id")!!, s("source_id")!!, s("target_id")!!, e("type", RelType.RELATED_TO), e("origin", Origin.USER), e("review", Review.CONFIRMED), s("evidence").orEmpty(), l("created") ?: 0)
        private fun Cursor.toCapture() = Capture(s("id")!!, s("text").orEmpty(), s("note_id"), s("attachment_id"), l("created") ?: 0, e("status", CaptureStatus.SAVED))
        private fun Cursor.toSuggestion() = Suggestion(s("id")!!, s("capture_id")!!, e("kind", SuggestionKind.LINK_EXISTING), en<ObjType>("obj_type"),
            s("title").orEmpty(), s("target_id"), e("rel_type", RelType.RELATED_TO), s("evidence").orEmpty(), d("due"), e("status", SuggestionStatus.PENDING),
            runCatching { JSONArray(s("link_to") ?: "[]").let { a -> (0 until a.length()).map { a.getString(it) } } }.getOrDefault(emptyList()))
        private fun Cursor.toActivity() = Activity(s("id")!!, s("object_id")!!, l("at") ?: 0, s("kind").orEmpty(), s("text").orEmpty())
        private fun Cursor.toDetail() = Detail(s("id")!!, s("object_id")!!, e("kind", DetailKind.CUSTOM), s("label").orEmpty(), s("value").orEmpty(), e("ftype", FieldType.TEXT),
            (l("sort") ?: 0).toInt(), b("flag"), s("extra").orEmpty(), l("created") ?: 0, e("origin", Origin.USER))
        private fun Cursor.toFieldDef() = FieldDef(s("id")!!, e("obj_type", ObjType.NOTE), s("label").orEmpty(), e("ftype", FieldType.TEXT),
            runCatching { JSONArray(s("options") ?: "[]").let { a -> (0 until a.length()).map { a.getString(it) } } }.getOrDefault(emptyList()), b("secret"), (l("sort") ?: 0).toInt())
    }
}
