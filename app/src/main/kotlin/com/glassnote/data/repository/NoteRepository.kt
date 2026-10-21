package com.glassnote.data.repository

import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.NoteBlock
import com.glassnote.domain.model.Tag
import com.glassnote.domain.model.TagWriteResult
import kotlinx.coroutines.flow.Flow

/**
 * Single local source of truth for notes, note/tag relations and tag CRUD.
 *
 * All methods are safe to call from a ViewModel's `viewModelScope`. There is no
 * debounce in this layer: the editor ViewModel debounces [saveDraft] so a
 * keystroke never triggers its own database write.
 */
interface NoteRepository {

    fun observeNotes(searchQuery: String = "", tagId: Long? = null): Flow<List<Note>>

    fun observeNote(noteId: Long): Flow<Note?>

    fun observeTags(): Flow<List<Tag>>

    suspend fun getNote(noteId: Long): Note?

    /** Imports note snapshots from public `.veil` files into the Room cache. */
    suspend fun importNotes(notes: List<Note>)

    /**
     * Creates a note immediately and returns its generated id.
     *
     * Returns 0 without inserting anything when both title and content are
     * blank. Tag ids that no longer exist are ignored.
     */
    suspend fun createNote(
        title: String = "",
        blocks: List<NoteBlock> = emptyList(),
        ambienceColor: AmbienceColor = AmbienceColor.DEFAULT,
        tagIds: Set<Long> = emptySet(),
    ): Long

    /**
     * Saves title/blocks/ambience without touching tag relations. Returns
     * false when the note no longer exists or when the whole document is blank.
     */
    suspend fun saveDraft(
        noteId: Long,
        title: String,
        blocks: List<NoteBlock>,
        ambienceColor: AmbienceColor,
    ): Boolean

    /** Replaces the note's complete tag selection. */
    suspend fun setNoteTags(noteId: Long, tagIds: Set<Long>): Boolean

    suspend fun setPinned(noteId: Long, pinned: Boolean): Boolean

    suspend fun setPinnedBottom(noteId: Long, pinnedBottom: Boolean): Boolean

    /**
     * Locks/unlocks a note. [passwordHash] must already be a SHA-256 hex string;
     * null means the note is unencrypted.
     */
    suspend fun setEncryption(
        noteId: Long,
        isEncrypted: Boolean,
        passwordHash: String?,
    ): Boolean

    /**
     * Hard-deletes a note and returns the deleted snapshot. The editor screen
     * keeps the snackbar visible for a short grace period before invoking this
     * method. [restoreNote] is available as an additional safety net.
     */
    suspend fun deleteNote(noteId: Long): Note?

    /** Batch delete used by multi-select. Returns the number of removed rows. */
    suspend fun deleteNotes(noteIds: Set<Long>): Int

    /** Restores a snapshot returned by [deleteNote], including its tags. */
    suspend fun restoreNote(note: Note): Boolean

    suspend fun createTag(name: String): TagWriteResult

    suspend fun renameTag(tagId: Long, newName: String): TagWriteResult

    suspend fun deleteTag(tagId: Long): Boolean
}
