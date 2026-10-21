package com.glassnote.data.local.converter

import com.glassnote.domain.model.NoteBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBlockListConverterTest {

    @Test
    fun roundTrip_preservesAllBlockKinds() {
        val blocks = listOf(
            NoteBlock.TextBlock(id = "t1", content = "hello\nworld"),
            NoteBlock.ImageBlock(id = "i1", localPath = "/files/a.png"),
            NoteBlock.FileBlock(
                id = "f1",
                localPath = "/files/a.pdf",
                fileName = "a.pdf",
                fileSize = "12 KB",
            ),
            NoteBlock.LocationBlock(
                id = "l1",
                latitude = 39.9,
                longitude = 116.4,
                address = "未知位置",
            ),
        )

        val decoded = NoteBlockListConverter.decode(NoteBlockListConverter.encode(blocks))

        assertEquals(blocks, decoded)
    }

    @Test
    fun legacyContent_isWrappedInOneTextBlock() {
        val blocks = NoteBlockListConverter.legacyContentBlocks("old note")

        assertEquals(1, blocks.size)
        assertTrue(blocks.first() is NoteBlock.TextBlock)
        assertEquals("old note", (blocks.first() as NoteBlock.TextBlock).content)
    }

    @Test
    fun malformedJson_returnsEmptyList() {
        assertTrue(NoteBlockListConverter.decode("{not-json").isEmpty())
    }
}
