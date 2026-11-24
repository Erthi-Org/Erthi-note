package org.erthi.note

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Note(
    val id: Long,
    val body: String,
    val updatedAt: Long,
    val color: Int = 0,
    val pinned: Boolean = false,
    val reminderAt: Long? = null,
    val customTitle: String = "",
    val richHtml: String = "",
    val titleSizeSp: Int = 20,
    val titleInk: Int = 0,
    val tags: String = "",
    val archived: Boolean = false,
    val favorite: Boolean = false,
    val folderId: Long? = null,
) {
    val title: String get() = customTitle.ifBlank { NoteText.title(body) }
}

data class Folder(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val createdAt: Long,
    val noteCount: Int = 0,
)

object NoteText {
    fun title(body: String): String = body.lineSequence().map { it.trim() }
        .firstOrNull { it.isNotEmpty() }?.take(70) ?: ""
    fun preview(body: String): String = body.trim().replace('\n', ' ').take(150)
    fun matches(body: String, query: String): Boolean =
        body.contains(query.trim(), ignoreCase = true)
}

object NoteColors {
    val backgrounds = intArrayOf(
        0xFFFFF4CD.toInt(), 0xFFDDF5E9.toInt(), 0xFFFFE5DB.toInt(),
        0xFFE4EDFF.toInt(), 0xFFF5E5F6.toInt()
    )
    val accents = intArrayOf(
        0xFFAC7100.toInt(), 0xFF167E60.toInt(), 0xFFB64D4B.toInt(),
        0xFF3C65BA.toInt(), 0xFF8C50AA.toInt()
    )
    fun valid(index: Int): Int = index.coerceIn(0, backgrounds.lastIndex)
}

/** Versioned migrations preserve all existing user notes, including 0.1.0 databases. */
class NoteStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "floating_notes.db", null, 9) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "body TEXT NOT NULL, updated_at INTEGER NOT NULL, " +
                "color INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0, " +
                "reminder_at INTEGER, title TEXT NOT NULL DEFAULT '', " +
                "rich_html TEXT NOT NULL DEFAULT '', deleted_at INTEGER, " +
                "title_size INTEGER NOT NULL DEFAULT 20, title_ink INTEGER NOT NULL DEFAULT 0, " +
                "tags TEXT NOT NULL DEFAULT '', archived_at INTEGER, " +
                "favorite INTEGER NOT NULL DEFAULT 0, folder_id INTEGER)"
        )
        db.execSQL("CREATE INDEX notes_updated_idx ON notes(updated_at DESC)")
        db.execSQL("CREATE INDEX notes_folder_idx ON notes(folder_id)")
        db.execSQL(
            "CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, parent_id INTEGER, created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN reminder_at INTEGER")
        }
        if (oldVersion < 3) db.execSQL("ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 4) db.execSQL("ALTER TABLE notes ADD COLUMN rich_html TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 5) db.execSQL("ALTER TABLE notes ADD COLUMN deleted_at INTEGER")
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE notes ADD COLUMN title_size INTEGER NOT NULL DEFAULT 20")
            db.execSQL("ALTER TABLE notes ADD COLUMN title_ink INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 7) db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE notes ADD COLUMN archived_at INTEGER")
            db.execSQL("ALTER TABLE notes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE notes ADD COLUMN folder_id INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS notes_folder_idx ON notes(folder_id)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, parent_id INTEGER, created_at INTEGER NOT NULL)"
            )
        }
    }

    fun create(body: String = "", color: Int = 0, title: String = ""): Long {
        val values = ContentValues().apply {
            put("body", body)
            put("title", title)
            put("updated_at", System.currentTimeMillis())
            put("color", NoteColors.valid(color))
        }
        return writableDatabase.insertOrThrow("notes", null, values)
    }

    fun update(id: Long, body: String): Boolean {
        val values = ContentValues().apply {
            put("body", body)
            put("updated_at", System.currentTimeMillis())
        }
        return writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString())) > 0
    }

    /** Atomically persist the independent title and body in one autosave operation. */
    fun updateNote(id: Long, title: String, body: String): Boolean {
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("updated_at", System.currentTimeMillis())
        }
        return writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun updateRich(id: Long, title: String, body: String, richHtml: String): Boolean {
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("rich_html", richHtml)
            put("updated_at", System.currentTimeMillis())
        }
        return writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun setTitleStyle(id: Long, sizeSp: Int, ink: Int): Boolean =
        change(id, ContentValues().apply {
            put("title_size", sizeSp.coerceIn(14, 36))
            put("title_ink", ink)
        })

    fun setColor(id: Long, color: Int): Boolean =
        change(id, ContentValues().apply { put("color", NoteColors.valid(color)) })

    fun setPinned(id: Long, pinned: Boolean): Boolean =
        change(id, ContentValues().apply { put("pinned", if (pinned) 1 else 0) })

    fun setTags(id: Long, tags: String): Boolean =
        change(id, ContentValues().apply { put("tags", tags.take(500)) })

    fun setFavorite(id: Long, favorite: Boolean): Boolean =
        change(id, ContentValues().apply { put("favorite", if (favorite) 1 else 0) })

    fun setArchived(id: Long, archived: Boolean): Boolean =
        writableDatabase.update("notes",
            ContentValues().apply {
                if (archived) put("archived_at", System.currentTimeMillis())
                else putNull("archived_at")
            },
            "id = ? AND deleted_at IS NULL", arrayOf(id.toString())) > 0

    fun allArchived(): List<Note> {
        val result = mutableListOf<Note>()
        readableDatabase.query("notes", columns, "archived_at IS NOT NULL AND deleted_at IS NULL",
            null, null, null, "archived_at DESC, id DESC")
            .use { c -> while (c.moveToNext()) result.add(row(c)) }
        return result
    }

    fun restoreArchived(id: Long): Boolean =
        writableDatabase.update("notes",
            ContentValues().apply { putNull("archived_at") },
            "id = ? AND archived_at IS NOT NULL", arrayOf(id.toString())) > 0


    fun setReminder(id: Long, at: Long?): Boolean =
        change(id, ContentValues().apply {
            if (at == null) putNull("reminder_at") else put("reminder_at", at)
        })

    /** Duplicate a note, returning the new note's id or -1 on failure. */
    fun duplicate(id: Long): Long {
        val note = get(id) ?: return -1L
        val values = ContentValues().apply {
            put("title", note.title.take(500))
            put("body", note.body)
            put("rich_html", note.richHtml)
            put("color", note.color)
            put("tags", note.tags)
            put("updated_at", System.currentTimeMillis())
            put("title_size", note.titleSizeSp)
            put("title_ink", note.titleInk)
        }
        return try { writableDatabase.insertOrThrow("notes", null, values) } catch (_: Exception) { -1L }
    }

    private fun change(id: Long, values: ContentValues): Boolean =
        writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString())) > 0

    /** Recoverable soft deletion, including after process restart. */
    fun delete(id: Long): Boolean =
        writableDatabase.update("notes",
            ContentValues().apply { put("deleted_at", System.currentTimeMillis()) },
            "id = ? AND deleted_at IS NULL", arrayOf(id.toString())) > 0

    fun lastDeleted(): Note? {
        readableDatabase.query("notes", columns, "deleted_at IS NOT NULL", null, null,
            null, "deleted_at DESC, id DESC", "1").use { cursor ->
            return if (cursor.moveToFirst()) row(cursor) else null
        }
    }

    fun undoLastDelete(): Note? {
        val last = lastDeleted() ?: return null
        val count = writableDatabase.update("notes",
            ContentValues().apply { putNull("deleted_at") },
            "id = ? AND deleted_at IS NOT NULL", arrayOf(last.id.toString()))
        return if (count == 1) get(last.id) else null
    }

    private val columns = arrayOf("id", "body", "updated_at", "color", "pinned", "reminder_at", "title", "rich_html", "title_size", "title_ink", "tags", "archived_at", "favorite", "folder_id")

    private fun row(c: android.database.Cursor) = Note(
        c.getLong(0), c.getString(1), c.getLong(2), NoteColors.valid(c.getInt(3)),
        c.getInt(4) != 0, if (c.isNull(5)) null else c.getLong(5), c.getString(6), c.getString(7), c.getInt(8), c.getInt(9), c.getString(10),
        !c.isNull(11), c.getInt(12) != 0,
        if (c.isNull(13)) null else c.getLong(13)
    )

    fun get(id: Long): Note? {
        readableDatabase.query(
            "notes", columns, "id = ? AND deleted_at IS NULL", arrayOf(id.toString()), null, null, null, "1"
        ).use { c ->
            return if (c.moveToFirst()) row(c) else null
        }
    }

    fun all(): List<Note> {
        val result = mutableListOf<Note>()
        readableDatabase.query(
            "notes", columns, "deleted_at IS NULL AND archived_at IS NULL", null, null, null,
            "favorite DESC, pinned DESC, updated_at DESC, id DESC"
        ).use { c -> while (c.moveToNext()) result.add(row(c)) }
        return result
    }

    fun search(query: String): List<Note> =
        if (query.isBlank()) all() else all().filter { NoteText.matches(it.body, query) || NoteText.matches(it.customTitle, query) || NoteText.matches(it.tags, query) }

    /** Validate archive before calling. Use a transaction so failures never leave
     * a partially imported archive. Merge; do NOT wipe existing notes. */
    fun allDeleted(): List<Note> {
        val result = mutableListOf<Note>()
        readableDatabase.query("notes", columns, "deleted_at IS NOT NULL", null, null,
            null, "deleted_at DESC, id DESC").use { c -> while (c.moveToNext()) result.add(row(c)) }
        return result
    }

    fun restore(id: Long): Boolean =
        writableDatabase.update("notes",
            ContentValues().apply { putNull("deleted_at") },
            "id = ? AND deleted_at IS NOT NULL", arrayOf(id.toString())) > 0

    fun permanentlyDelete(id: Long): Boolean =
        writableDatabase.delete("notes", "id = ? AND deleted_at IS NOT NULL",
            arrayOf(id.toString())) > 0

    fun emptyTrash(): Int =
        writableDatabase.delete("notes", "deleted_at IS NOT NULL", null)

    // ── Folder queries ────────────────────────────────────────────────────────

    fun getFolders(parentId: Long? = null): List<Folder> {
        val result = mutableListOf<Folder>()
        val (sel, args) = if (parentId == null)
            "parent_id IS NULL" to emptyArray()
        else
            "parent_id = ?" to arrayOf(parentId.toString())
        readableDatabase.query("folders", arrayOf("id", "name", "parent_id", "created_at"),
            sel, args.takeIf { it.isNotEmpty() }, null, null, "name ASC").use { c ->
            while (c.moveToNext()) {
                val fid = c.getLong(0)
                val count = noteCountInFolder(fid)
                result += Folder(fid, c.getString(1), if (c.isNull(2)) null else c.getLong(2),
                    c.getLong(3), count)
            }
        }
        return result
    }

    private fun noteCountInFolder(folderId: Long): Int {
        readableDatabase.query("notes", arrayOf("COUNT(*)"),
            "folder_id = ? AND deleted_at IS NULL AND archived_at IS NULL",
            arrayOf(folderId.toString()), null, null, null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun notesInFolder(folderId: Long?): List<Note> {
        val result = mutableListOf<Note>()
        val (sel, args) = if (folderId == null)
            "folder_id IS NULL AND deleted_at IS NULL AND archived_at IS NULL" to null
        else
            "folder_id = ? AND deleted_at IS NULL AND archived_at IS NULL" to arrayOf(folderId.toString())
        readableDatabase.query("notes", columns, sel, args, null, null,
            "favorite DESC, pinned DESC, updated_at DESC, id DESC").use { c ->
            while (c.moveToNext()) result.add(row(c))
        }
        return result
    }

    fun createFolder(name: String, parentId: Long? = null): Long {
        val values = ContentValues().apply {
            put("name", name.trim().take(200))
            if (parentId != null) put("parent_id", parentId) else putNull("parent_id")
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("folders", null, values)
    }

    fun renameFolder(id: Long, name: String): Boolean =
        writableDatabase.update("folders",
            ContentValues().apply { put("name", name.trim().take(200)) },
            "id = ?", arrayOf(id.toString())) > 0

    fun deleteFolder(id: Long): Boolean {
        // Move contained notes to parent folder (or root) before deleting
        val parentId: Long? = readableDatabase.query("folders", arrayOf("parent_id"),
            "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
        val cv = ContentValues().apply {
            if (parentId != null) put("folder_id", parentId) else putNull("folder_id")
        }
        writableDatabase.update("notes", cv, "folder_id = ?", arrayOf(id.toString()))
        // Reparent subfolders
        val subCv = ContentValues().apply {
            if (parentId != null) put("parent_id", parentId) else putNull("parent_id")
        }
        writableDatabase.update("folders", subCv, "parent_id = ?", arrayOf(id.toString()))
        return writableDatabase.delete("folders", "id = ?", arrayOf(id.toString())) > 0
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long?): Boolean =
        writableDatabase.update("notes",
            ContentValues().apply { if (folderId != null) put("folder_id", folderId) else putNull("folder_id") },
            "id = ? AND deleted_at IS NULL", arrayOf(noteId.toString())) > 0

    // ── Bulk operations ───────────────────────────────────────────────────────

    fun bulkDelete(ids: Set<Long>): Int {
        if (ids.isEmpty()) return 0
        val placeholders = ids.joinToString(",") { "?" }
        return writableDatabase.update("notes",
            ContentValues().apply { put("deleted_at", System.currentTimeMillis()) },
            "id IN ($placeholders) AND deleted_at IS NULL",
            ids.map { it.toString() }.toTypedArray())
    }

    fun bulkArchive(ids: Set<Long>, archive: Boolean): Int {
        if (ids.isEmpty()) return 0
        val placeholders = ids.joinToString(",") { "?" }
        val cv = ContentValues().apply {
            if (archive) put("archived_at", System.currentTimeMillis()) else putNull("archived_at")
        }
        return writableDatabase.update("notes", cv,
            "id IN ($placeholders) AND deleted_at IS NULL",
            ids.map { it.toString() }.toTypedArray())
    }

    fun bulkFavorite(ids: Set<Long>, favorite: Boolean): Int {
        if (ids.isEmpty()) return 0
        val placeholders = ids.joinToString(",") { "?" }
        return writableDatabase.update("notes",
            ContentValues().apply { put("favorite", if (favorite) 1 else 0) },
            "id IN ($placeholders) AND deleted_at IS NULL",
            ids.map { it.toString() }.toTypedArray())
    }

    fun bulkMoveToFolder(ids: Set<Long>, folderId: Long?): Int {
        if (ids.isEmpty()) return 0
        val placeholders = ids.joinToString(",") { "?" }
        val cv = ContentValues().apply {
            if (folderId != null) put("folder_id", folderId) else putNull("folder_id")
        }
        return writableDatabase.update("notes", cv,
            "id IN ($placeholders) AND deleted_at IS NULL",
            ids.map { it.toString() }.toTypedArray())
    }

    // ── Import ────────────────────────────────────────────────────────────────

    fun importNotes(items: List<Note>): List<Long> {
        val db = writableDatabase
        val ids = mutableListOf<Long>()
        // Import is idempotent for identical notes. Keep metadata and timestamp in identity.
        val now = System.currentTimeMillis()
        fun signature(note: Note): List<Any?> = listOf(note.customTitle, note.body, note.richHtml, note.titleSizeSp, note.titleInk, note.updatedAt,
            NoteColors.valid(note.color), note.pinned,
            note.reminderAt?.takeIf { it > now })
        val seen = all().map(::signature).toMutableSet()
        db.beginTransaction()
        try {
            for (n in items) {
                if (!seen.add(signature(n))) continue
                val values = ContentValues().apply {
                    put("body", n.body)
                    put("title", n.customTitle)
                    put("rich_html", n.richHtml)
                    put("title_size", n.titleSizeSp.coerceIn(14, 36))
                    put("title_ink", n.titleInk)
                    put("tags", n.tags.take(500))
                    put("favorite", if (n.favorite) 1 else 0)
                    put("updated_at", n.updatedAt)
                    put("color", NoteColors.valid(n.color))
                    put("pinned", if (n.pinned) 1 else 0)
                    if (n.reminderAt != null && n.reminderAt > System.currentTimeMillis()) {
                        put("reminder_at", n.reminderAt)
                    }
                }
                ids.add(db.insertOrThrow("notes", null, values))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ids
    }
}
