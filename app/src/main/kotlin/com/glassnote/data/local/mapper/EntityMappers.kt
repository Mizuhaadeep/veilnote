package com.glassnote.data.local.mapper

import com.glassnote.data.local.entity.NoteEntity
import com.glassnote.data.local.entity.TagEntity
import com.glassnote.data.local.relation.NoteWithTags
import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.Tag

internal fun NoteEntity.toDomain(tags: List<TagEntity>): Note = Note(
    id = id,
    title = title,
    blocks = blocks,
    ambienceColor = AmbienceColor.fromStorageKey(ambienceColor),
    pinned = isPinned,
    isPinnedBottom = isPinnedBottom,
    isEncrypted = isEncrypted,
    passwordHash = passwordHash,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags.map(TagEntity::toDomain).sortedBy { it.name.lowercase() },
)

internal fun NoteWithTags.toDomain(): Note = note.toDomain(tags)

internal fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    createdAt = createdAt,
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    blocks = blocks,
    ambienceColor = ambienceColor.storageKey,
    isPinned = pinned,
    isPinnedBottom = isPinnedBottom,
    isEncrypted = isEncrypted,
    passwordHash = passwordHash,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
