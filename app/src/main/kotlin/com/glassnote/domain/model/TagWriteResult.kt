package com.glassnote.domain.model

/** Result returned by tag create/rename operations. */
sealed interface TagWriteResult {
    data class Success(val tag: Tag) : TagWriteResult

    data object BlankName : TagWriteResult

    data object DuplicateName : TagWriteResult

    data object NotFound : TagWriteResult
}
