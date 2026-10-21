package com.glassnote.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.glassnote.data.local.GlassNoteDatabase
import com.glassnote.data.local.dao.NoteDao
import com.glassnote.data.local.dao.TagDao
import com.glassnote.data.local.entity.NoteEntity
import com.glassnote.data.local.entity.NoteTagCrossRef
import com.glassnote.data.local.entity.TagEntity
import com.glassnote.data.local.mapper.toDomain
import com.glassnote.data.local.mapper.toEntity
import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.NoteBlock
import com.glassnote.domain.model.isBlankDocument
import com.glassnote.domain.model.plainText
import com.glassnote.domain.model.Tag
import com.glassnote.domain.model.TagWriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineNoteRepository(
    private val database: GlassNoteDatabase,
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : NoteRepository {

    override fun observeNotes(searchQuery: String, tagId: Long?): Flow<List<Note>> {
        val query = searchQuery.trim()
        return noteDao.observeNotes(
            searchQuery = "",
            tagId = tagId,
        ).map { notes ->
            notes.map { it.toDomain() }.filter { note ->
                query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            }
        }
    }

    override fun observeNote(noteId: Long): Flow<Note?> =
        noteDao.observeNote(noteId).map { it?.toDomain() }

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { tags -> tags.map(TagEntity::toDomain) }

    override suspend fun getNote(noteId: Long): Note? = noteDao.getNote(noteId)?.toDomain()

    override suspend fun importNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        database.withTransaction {
            notes.forEach { snapshot ->
                if (snapshot.id <= 0L) return@forEach

                noteDao.upsert(snapshot.toEntity())
                noteDao.clearTagRefsForNote(snapshot.id)

                val uniqueTagNames = snapshot.tags
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                uniqueTagNames.forEach { tagName ->
                    val existing = tagDao.findByName(tagName)
                    val tagId = existing?.id ?: tagDao.insert(
                        TagEntity(
                            name = tagName,
                            createdAt = snapshot.updatedAt,
                        ),
                    )
                    noteDao.insertTagRefs(
                        listOf(NoteTagCrossRef(noteId = snapshot.id, tagId = tagId)),
                    )
                }
            }
        }
    }

    override suspend fun createNote(
        title: String,
        blocks: List<NoteBlock>,
        ambienceColor: AmbienceColor,
        tagIds: Set<Long>,
    ): Long {
        // A brand-new editor with no text must never create a database row.
        if (title.isBlank() && blocks.isBlankDocument()) return 0L
        return database.withTransaction {
            val timestamp = clock()
            val noteId = noteDao.insert(
                NoteEntity(
                    title = title,
                    blocks = blocks,
                    ambienceColor = ambienceColor.storageKey,
                    isPinned = false,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )
            attachExistingTags(noteId, tagIds)
            noteId
        }
    }

    override suspend fun saveDraft(
        noteId: Long,
        title: String,
        blocks: List<NoteBlock>,
        ambienceColor: AmbienceColor,
    ): Boolean {
        // Empty cards are not valid notes. The editor deletes an existing row
        // when its draft becomes blank; this guard also protects the data layer
        // from any future caller trying to persist an empty update.
        if (title.isBlank() && blocks.isBlankDocument()) return false
        return noteDao.updateDraft(
            noteId = noteId,
            title = title,
            blocks = blocks,
            ambienceColor = ambienceColor.storageKey,
            updatedAt = clock(),
        ) > 0
    }

    override suspend fun setNoteTags(noteId: Long, tagIds: Set<Long>): Boolean =
        database.withTransaction {
            if (!noteDao.exists(noteId)) return@withTransaction false

            val uniqueTagIds = tagIds.toList().distinct()
            val existingTagIds = if (uniqueTagIds.isEmpty()) {
                emptyList()
            } else {
                tagDao.getExistingIds(uniqueTagIds)
            }

            noteDao.clearTagRefsForNote(noteId)
            if (existingTagIds.isNotEmpty()) {
                noteDao.insertTagRefs(existingTagIds.map { tagId ->
                    NoteTagCrossRef(noteId = noteId, tagId = tagId)
                })
            }
            true
        }

    override suspend fun setPinned(noteId: Long, pinned: Boolean): Boolean =
        noteDao.updatePinned(noteId = noteId, pinned = pinned) > 0

    override suspend fun setPinnedBottom(noteId: Long, pinnedBottom: Boolean): Boolean =
        noteDao.updatePinnedBottom(noteId = noteId, pinnedBottom = pinnedBottom) > 0

    override suspend fun setEncryption(
        noteId: Long,
        isEncrypted: Boolean,
        passwordHash: String?,
    ): Boolean = noteDao.updateEncryption(
        noteId = noteId,
        isEncrypted = isEncrypted,
        passwordHash = if (isEncrypted) passwordHash else null,
    ) > 0

    override suspend fun deleteNote(noteId: Long): Note? = database.withTransaction {
        val snapshot = noteDao.getNote(noteId)?.toDomain() ?: return@withTransaction null
        noteDao.deleteById(noteId)
        snapshot
    }

    override suspend fun deleteNotes(noteIds: Set<Long>): Int {
        if (noteIds.isEmpty()) return 0
        return noteDao.deleteByIds(noteIds.toList())
    }

    override suspend fun restoreNote(note: Note): Boolean = database.withTransaction {
        if (note.id <= 0L) return@withTransaction false
        if (noteDao.exists(note.id)) return@withTransaction false

        noteDao.insert(note.toEntity())

        // A tag may have been deleted between the initial delete and undo.
        // Restore the note with whichever selected tags still exist.
        val tagIds = note.tags.map(Tag::id).distinct()
        val existingTagIds = if (tagIds.isEmpty()) emptyList() else tagDao.getExistingIds(tagIds)
        if (existingTagIds.isNotEmpty()) {
            noteDao.insertTagRefs(existingTagIds.map { tagId ->
                NoteTagCrossRef(noteId = note.id, tagId = tagId)
            })
        }
        true
    }

    override suspend fun createTag(name: String): TagWriteResult {
        val normalizedName = normalizeTagName(name) ?: return TagWriteResult.BlankName
        if (tagDao.findByName(normalizedName) != null) {
            return TagWriteResult.DuplicateName
        }

        val timestamp = clock()
        return try {
            val id = tagDao.insert(
                TagEntity(name = normalizedName, createdAt = timestamp),
            )
            TagWriteResult.Success(
                Tag(id = id, name = normalizedName, createdAt = timestamp),
            )
        } catch (_: SQLiteConstraintException) {
            // Unique index protects against a duplicate created concurrently.
            TagWriteResult.DuplicateName
        }
    }

    override suspend fun renameTag(tagId: Long, newName: String): TagWriteResult {
        val normalizedName = normalizeTagName(newName) ?: return TagWriteResult.BlankName
        val current = tagDao.getById(tagId) ?: return TagWriteResult.NotFound

        val duplicate = tagDao.findByName(normalizedName)
        if (duplicate != null && duplicate.id != tagId) {
            return TagWriteResult.DuplicateName
        }

        val updated = current.copy(name = normalizedName)
        return try {
            if (tagDao.update(updated) == 0) {
                TagWriteResult.NotFound
            } else {
                TagWriteResult.Success(updated.toDomain())
            }
        } catch (_: SQLiteConstraintException) {
            TagWriteResult.DuplicateName
        }
    }

    override suspend fun deleteTag(tagId: Long): Boolean = tagDao.deleteById(tagId) > 0

    private suspend fun attachExistingTags(noteId: Long, tagIds: Collection<Long>) {
        val uniqueTagIds = tagIds.distinct()
        if (uniqueTagIds.isEmpty()) return

        val existingTagIds = tagDao.getExistingIds(uniqueTagIds)
        if (existingTagIds.isNotEmpty()) {
            noteDao.insertTagRefs(existingTagIds.map { tagId ->
                NoteTagCrossRef(noteId = noteId, tagId = tagId)
            })
        }
    }

    private fun normalizeTagName(rawName: String): String? {
        val normalized = rawName.trim().replace(WhitespaceRegex, " ")
        return normalized.takeIf { it.isNotEmpty() }
    }

    /**
     * Escapes `%`, `_` and `\` so user input is always treated as literal text
     * by the `LIKE` expressions in [NoteDao].
     */
    private fun String.escapeLikePattern(): String = buildString(length) {
        for (character in this@escapeLikePattern) {
            if (character == '\\' || character == '%' || character == '_') {
                append('\\')
            }
            append(character)
        }
    }

    private companion object {
        val WhitespaceRegex = Regex("\\s+")
    }
}
