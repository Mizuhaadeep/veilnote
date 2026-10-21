package com.glassnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.glassnote.data.local.entity.NoteEntity
import com.glassnote.data.local.entity.NoteTagCrossRef
import com.glassnote.data.local.relation.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /**
     * Notes sorted with pinned-to-top items first, regular notes by latest edit,
     * and pinned-to-bottom items last.
     *
     * [searchQuery] must already be escaped for SQLite `LIKE` wildcards by the
     * repository. An empty string means "match everything". A null [tagId]
     * disables tag filtering.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE (
            :searchQuery = ''
            OR title LIKE '%' || :searchQuery || '%' ESCAPE '\'
        )
        AND (
            :tagId IS NULL
            OR EXISTS (
                SELECT 1 FROM note_tag_cross_ref
                WHERE note_tag_cross_ref.note_id = notes.id
                  AND note_tag_cross_ref.tag_id = :tagId
            )
        )
        ORDER BY is_pinned DESC, is_pinned_bottom ASC, updated_at DESC
        """,
    )
    fun observeNotes(searchQuery: String, tagId: Long?): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun observeNote(noteId: Long): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNote(noteId: Long): NoteWithTags?

    @Query("SELECT EXISTS(SELECT 1 FROM notes WHERE id = :noteId)")
    suspend fun exists(noteId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    /** Import path for `.veil` files: replace an existing row with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(note: NoteEntity): Int

    /** Block-document save used by the debounced editor. */
    @Query(
        """
        UPDATE notes
        SET title = :title,
            blocks = :blocks,
            ambience_color = :ambienceColor,
            updated_at = :updatedAt
        WHERE id = :noteId
        """,
    )
    suspend fun updateDraft(
        noteId: Long,
        title: String,
        blocks: List<com.glassnote.domain.model.NoteBlock>,
        ambienceColor: String,
        updatedAt: Long,
    ): Int

    /** Pin state changes do not count as content edits. */
    @Query(
        """
        UPDATE notes
        SET is_pinned = :pinned,
            is_pinned_bottom = CASE WHEN :pinned = 1 THEN 0 ELSE is_pinned_bottom END
        WHERE id = :noteId
        """,
    )
    suspend fun updatePinned(noteId: Long, pinned: Boolean): Int

    /** Bottom placement is mutually exclusive with top pinning. */
    @Query(
        """
        UPDATE notes
        SET is_pinned_bottom = :pinnedBottom,
            is_pinned = CASE WHEN :pinnedBottom = 1 THEN 0 ELSE is_pinned END
        WHERE id = :noteId
        """,
    )
    suspend fun updatePinnedBottom(noteId: Long, pinnedBottom: Boolean): Int

    @Query(
        """
        UPDATE notes
        SET is_encrypted = :isEncrypted,
            password_hash = :passwordHash
        WHERE id = :noteId
        """,
    )
    suspend fun updateEncryption(
        noteId: Long,
        isEncrypted: Boolean,
        passwordHash: String?,
    ): Int

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long): Int

    @Query("DELETE FROM notes WHERE id IN (:noteIds)")
    suspend fun deleteByIds(noteIds: List<Long>): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE note_id = :noteId")
    suspend fun clearTagRefsForNote(noteId: Long)
}
