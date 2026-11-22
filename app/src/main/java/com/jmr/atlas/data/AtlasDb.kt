package com.jmr.atlas.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jmr.atlas.model.*
import org.json.JSONArray
import java.time.LocalDate

/**
 * Atlas schema v1. Plain SQLite with foreign keys, unique constraints, indexes and FTS4 (see docs/ARCHITECTURE.md ADR-3).
 */
class AtlasDb(context: Context, name: String? = "atlas.db") : SQLiteOpenHelper(context, name, null, VERSION) {

    var ftsAvailable = true
        private set

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
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
    }

    private fun createFts(db: SQLiteDatabase) {
        ftsAvailable = runCatching { db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS objects_fts USING fts4(id, content, notindexed=id, tokenize=unicode61)") }.isSuccess
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ftsAvailable = runCatching { db.rawQuery("SELECT count(*) FROM objects_fts", null).use { it.moveToFirst() } }.isSuccess
    }

    /** Versioned migrations go here (v1 is the first schema). */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ---------------------------------------------------------------- read
    fun loadAll(): Snapshot {
        val db = readableDatabase
        val objs = db.rawQuery("SELECT * FROM objects", null).use { c -> buildMap { while (c.moveToNext()) c.toObject().let { put(it.id, it) } } }
        val rels = db.rawQuery("SELECT * FROM relationships", null).use { c -> buildList { while (c.moveToNext()) add(c.toRelation()) } }
        val caps = db.rawQuery("SELECT * FROM captures ORDER BY created DESC", null).use { c -> buildList { while (c.moveToNext()) add(c.toCapture()) } }
        val sugs = db.rawQuery("SELECT * FROM suggestions", null).use { c -> buildList { while (c.moveToNext()) add(c.toSuggestion()) } }
        val acts = db.rawQuery("SELECT * FROM activity ORDER BY at DESC LIMIT 2000", null).use { c -> buildList { while (c.moveToNext()) add(c.toActivity()) } }
        return Snapshot(objs, rels, caps, sugs, acts)
    }

    fun keyword(query: String, limit: Int = 60): Map<String, Float> {
        val terms = tokens(query); if (terms.isEmpty()) return emptyMap()
        if (ftsAvailable) runCatching {
            val match = terms.joinToString(" OR ") { "$it*" }
            return readableDatabase.rawQuery("SELECT id, content FROM objects_fts WHERE objects_fts MATCH ? LIMIT ?", arrayOf(match, (limit * 3).toString())).use { c ->
                buildMap { while (c.moveToNext()) { val t = c.getString(1).lowercase(); put(c.getString(0), terms.count { t.contains(it) }.toFloat() / terms.size) } }
            }.entries.sortedByDescending { it.value }.take(limit).associate { it.key to it.value }
        }
        return emptyMap()
    }

    // ---------------------------------------------------------------- write (callers wrap in tx())
    fun <T> tx(block: SQLiteDatabase.() -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try { val r = db.block(); db.setTransactionSuccessful(); return r } finally { db.endTransaction() }
    }

    fun SQLiteDatabase.putObject(o: KObject) {
        upsert("objects", o.id, o.values())
        if (ftsAvailable) {
            delete("objects_fts", "id = ?", arrayOf(o.id))
            if (!o.trashed) insert("objects_fts", null, ContentValues().apply { put("id", o.id); put("content", o.searchText()) })
        }
    }
    fun SQLiteDatabase.deleteObject(id: String) {
        delete("objects", "id = ?", arrayOf(id))
        if (ftsAvailable) delete("objects_fts", "id = ?", arrayOf(id))
    }
    /** Returns false if an equivalent relationship already exists (UNIQUE) — prevents duplicate edges. */
    fun SQLiteDatabase.putRelation(r: Relation): Boolean = insertWithOnConflict("relationships", null, r.values(), SQLiteDatabase.CONFLICT_IGNORE) != -1L
    fun SQLiteDatabase.updateRelation(r: Relation) { update("relationships", r.values(), "id = ?", arrayOf(r.id)) }
    fun SQLiteDatabase.deleteRelation(id: String) { delete("relationships", "id = ?", arrayOf(id)) }
    fun SQLiteDatabase.putCapture(c: Capture) { upsert("captures", c.id, c.values()) }
    fun SQLiteDatabase.putSuggestion(s: Suggestion) { upsert("suggestions", s.id, s.values()) }
    fun SQLiteDatabase.putActivity(a: Activity) { upsert("activity", a.id, a.values()) }

    /** UPDATE-then-INSERT. Never INSERT OR REPLACE: REPLACE deletes the parent row first, which fires ON DELETE CASCADE on its children. */
    private fun SQLiteDatabase.upsert(table: String, id: String, v: ContentValues) {
        if (update(table, v, "id = ?", arrayOf(id)) == 0) insertOrThrow(table, null, v)
    }

    fun wipe() = tx {
        listOf("suggestions", "captures", "activity", "relationships", "objects").forEach { delete(it, null, null) }
        if (ftsAvailable) delete("objects_fts", null, null)
    }

    companion object {
        const val VERSION = 1
        private val STOP = setOf("the", "a", "an", "is", "are", "was", "what", "when", "where", "who", "how", "did", "do", "my", "me", "i", "of", "to", "for", "in", "on", "at", "and", "or", "about", "that", "this", "it", "with", "from")
        fun tokens(q: String) = q.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").split(Regex("\\s+")).filter { it.length >= 2 && it !in STOP }.distinct()
    }

    // ---------------------------------------------------------------- mapping
    private fun KObject.values() = ContentValues().apply {
        put("id", id); put("type", type.name); put("title", title); put("body", body); put("created", created); put("updated", updated)
        put("trashed", trashed.i); put("pinned", pinned.i); put("favorite", favorite.i); put("origin", origin.name)
        put("role", role); put("org", org); put("email", email); put("phone", phone)
        put("project_status", projectStatus?.name); put("start_date", startDate?.toString()); put("target_date", targetDate?.toString())
        put("due", due?.toString()); put("done", done.i); put("completed_at", completedAt); put("priority", priority.name); put("blocked", blocked.i)
        put("mime", mime); put("vault_file", vaultFile); put("url", url); put("size_bytes", sizeBytes); put("extracted", extracted); put("event_date", eventDate?.toString())
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

    private val Boolean.i get() = if (this) 1 else 0
    private fun Cursor.s(n: String): String? = getColumnIndex(n).let { if (it < 0 || isNull(it)) null else getString(it) }
    private fun Cursor.l(n: String): Long? = getColumnIndex(n).let { if (it < 0 || isNull(it)) null else getLong(it) }
    private fun Cursor.b(n: String) = l(n) == 1L
    private fun Cursor.d(n: String) = s(n)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    private inline fun <reified E : Enum<E>> Cursor.e(n: String, def: E): E = s(n)?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: def

    private fun Cursor.toObject() = KObject(
        id = s("id")!!, type = e("type", ObjType.NOTE), title = s("title").orEmpty(), body = s("body").orEmpty(), created = l("created") ?: 0, updated = l("updated") ?: 0,
        trashed = b("trashed"), pinned = b("pinned"), favorite = b("favorite"), origin = e("origin", Origin.USER),
        role = s("role").orEmpty(), org = s("org").orEmpty(), email = s("email").orEmpty(), phone = s("phone").orEmpty(),
        projectStatus = s("project_status")?.let { v -> ProjectStatus.entries.firstOrNull { it.name == v } }, startDate = d("start_date"), targetDate = d("target_date"),
        due = d("due"), done = b("done"), completedAt = l("completed_at"), priority = e("priority", Priority.NONE), blocked = b("blocked"),
        mime = s("mime").orEmpty(), vaultFile = s("vault_file").orEmpty(), url = s("url").orEmpty(), sizeBytes = l("size_bytes") ?: 0, extracted = s("extracted").orEmpty(),
        eventDate = d("event_date"),
    )
    private fun Cursor.toRelation() = Relation(s("id")!!, s("source_id")!!, s("target_id")!!, e("type", RelType.RELATED_TO), e("origin", Origin.USER), e("review", Review.CONFIRMED), s("evidence").orEmpty(), l("created") ?: 0)
    private fun Cursor.toCapture() = Capture(s("id")!!, s("text").orEmpty(), s("note_id"), s("attachment_id"), l("created") ?: 0, e("status", CaptureStatus.SAVED))
    private fun Cursor.toSuggestion() = Suggestion(s("id")!!, s("capture_id")!!, e("kind", SuggestionKind.LINK_EXISTING), s("obj_type")?.let { v -> ObjType.entries.firstOrNull { it.name == v } },
        s("title").orEmpty(), s("target_id"), e("rel_type", RelType.RELATED_TO), s("evidence").orEmpty(), d("due"), e("status", SuggestionStatus.PENDING),
        runCatching { JSONArray(s("link_to") ?: "[]").let { a -> (0 until a.length()).map { a.getString(it) } } }.getOrDefault(emptyList()))
    private fun Cursor.toActivity() = Activity(s("id")!!, s("object_id")!!, l("at") ?: 0, s("kind").orEmpty(), s("text").orEmpty())
}
