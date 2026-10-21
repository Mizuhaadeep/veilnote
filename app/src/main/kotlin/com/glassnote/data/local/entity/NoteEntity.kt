package com.glassnote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.glassnote.domain.model.NoteBlock

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["is_pinned", "updated_at"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "blocks")
    val blocks: List<NoteBlock>,
    /** [com.glassnote.domain.model.AmbienceColor.storageKey] */
    @ColumnInfo(name = "ambience_color")
    val ambienceColor: String,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,
    @ColumnInfo(name = "is_pinned_bottom", defaultValue = "0")
    val isPinnedBottom: Boolean = false,
    @ColumnInfo(name = "is_encrypted", defaultValue = "0")
    val isEncrypted: Boolean = false,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
