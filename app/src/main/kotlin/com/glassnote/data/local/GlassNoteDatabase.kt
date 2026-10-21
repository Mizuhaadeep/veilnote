package com.glassnote.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.glassnote.data.local.converter.NoteBlockListConverter
import com.glassnote.data.local.dao.NoteDao
import com.glassnote.data.local.dao.TagDao
import com.glassnote.data.local.entity.NoteEntity
import com.glassnote.data.local.entity.NoteTagCrossRef
import com.glassnote.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(NoteBlockListConverter::class)
abstract class GlassNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "glass-note.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN is_pinned_bottom INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN is_encrypted INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN password_hash TEXT",
                )
            }
        }

        /**
         * Replaces the flat `content` column with the Gson-backed `blocks`
         * column. We rebuild the table instead of leaving `content` behind, so
         * Room schema validation stays exact and legacy databases keep working.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS notes_new")

                db.execSQL(
                    """
                    CREATE TABLE notes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        blocks TEXT NOT NULL,
                        ambience_color TEXT NOT NULL,
                        is_pinned INTEGER NOT NULL,
                        is_pinned_bottom INTEGER NOT NULL DEFAULT 0,
                        is_encrypted INTEGER NOT NULL DEFAULT 0,
                        password_hash TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                db.query("SELECT * FROM notes").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val titleIndex = cursor.getColumnIndexOrThrow("title")
                    val contentIndex = cursor.getColumnIndexOrThrow("content")
                    val ambienceIndex = cursor.getColumnIndexOrThrow("ambience_color")
                    val pinnedIndex = cursor.getColumnIndexOrThrow("is_pinned")
                    val pinnedBottomIndex = cursor.getColumnIndexOrThrow("is_pinned_bottom")
                    val encryptedIndex = cursor.getColumnIndexOrThrow("is_encrypted")
                    val passwordIndex = cursor.getColumnIndexOrThrow("password_hash")
                    val createdIndex = cursor.getColumnIndexOrThrow("created_at")
                    val updatedIndex = cursor.getColumnIndexOrThrow("updated_at")

                    while (cursor.moveToNext()) {
                        val oldContent = cursor.getString(contentIndex).orEmpty()
                        val values = ContentValues().apply {
                            put("id", cursor.getLong(idIndex))
                            put("title", cursor.getString(titleIndex).orEmpty())
                            put(
                                "blocks",
                                NoteBlockListConverter.encode(
                                    NoteBlockListConverter.legacyContentBlocks(oldContent),
                                ),
                            )
                            put("ambience_color", cursor.getString(ambienceIndex))
                            put("is_pinned", cursor.getInt(pinnedIndex))
                            put("is_pinned_bottom", cursor.getInt(pinnedBottomIndex))
                            put("is_encrypted", cursor.getInt(encryptedIndex))
                            if (cursor.isNull(passwordIndex)) {
                                putNull("password_hash")
                            } else {
                                put("password_hash", cursor.getString(passwordIndex))
                            }
                            put("created_at", cursor.getLong(createdIndex))
                            put("updated_at", cursor.getLong(updatedIndex))
                        }
                        db.insert("notes_new", SQLiteDatabase.CONFLICT_ABORT, values)
                    }
                }

                // Preserve tag relations while the notes table is rebuilt.
                db.execSQL("DROP TABLE IF EXISTS note_tag_cross_ref_backup")
                db.execSQL(
                    """
                    CREATE TABLE note_tag_cross_ref_backup AS
                    SELECT note_id, tag_id FROM note_tag_cross_ref
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE note_tag_cross_ref")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                        note_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        PRIMARY KEY(note_id, tag_id),
                        FOREIGN KEY(note_id) REFERENCES notes(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(tag_id) REFERENCES tags(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO note_tag_cross_ref(note_id, tag_id)
                    SELECT note_id, tag_id FROM note_tag_cross_ref_backup
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE note_tag_cross_ref_backup")

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_is_pinned_updated_at " +
                        "ON notes(is_pinned, updated_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_tag_id " +
                        "ON note_tag_cross_ref(tag_id)",
                )
            }
        }

        @Volatile
        private var instance: GlassNoteDatabase? = null

        fun getInstance(context: Context): GlassNoteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlassNoteDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
