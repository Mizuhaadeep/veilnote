package com.glassnote.domain.model

/** A user-created, reusable label. */
data class Tag(
    val id: Long,
    val name: String,
    val createdAt: Long,
)
