package com.glassnote.domain.model

/**
 * A note and its selected tags.
 *
 * [blocks] is the document source of truth. [content] is a read-only plain-text
 * projection used by list cards, search and legacy code paths.
 */
data class Note(
    val id: Long,
    val title: String,
    val blocks: List<NoteBlock> = emptyList(),
    val ambienceColor: AmbienceColor = AmbienceColor.DEFAULT,
    val pinned: Boolean = false,
    val isPinnedBottom: Boolean = false,
    val isEncrypted: Boolean = false,
    val passwordHash: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
) {
    val content: String
        get() = blocks.plainText()
}
