package com.glassnote.domain.model

import java.util.UUID

/**
 * One piece of a note. Text is the only editable primitive; the other blocks
 * are inserted as attachments and rendered in document order.
 */
sealed interface NoteBlock {
    val id: String

    data class TextBlock(
        override val id: String = UUID.randomUUID().toString(),
        val content: String,
    ) : NoteBlock

    data class ImageBlock(
        override val id: String = UUID.randomUUID().toString(),
        val localPath: String,
    ) : NoteBlock

    data class FileBlock(
        override val id: String,
        val localPath: String,
        val fileName: String,
        val fileSize: String,
    ) : NoteBlock

    data class LocationBlock(
        override val id: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
    ) : NoteBlock
}

/** Plain text projection used by search, cards and legacy save paths. */
fun List<NoteBlock>.plainText(): String = filterIsInstance<NoteBlock.TextBlock>()
    .joinToString("\n") { it.content }

fun List<NoteBlock>.isBlankDocument(): Boolean = all { block ->
    block is NoteBlock.TextBlock && block.content.isBlank()
}
