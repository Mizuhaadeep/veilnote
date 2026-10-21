package com.glassnote.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.glassnote.data.local.entity.NoteEntity
import com.glassnote.data.local.entity.NoteTagCrossRef
import com.glassnote.data.local.entity.TagEntity

/** Room projection for a note together with all of its tags. */
data class NoteWithTags(
    @Embedded
    val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "note_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,
)
