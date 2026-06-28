package com.k2s.listennest.data.model

data class BookFileEntity(
    val id: Long = 0L,
    val bookId: Long = 0L,
    val displayName: String = "",
    val uri: String = "",
    val trackIndex: Int = 0,
    val durationMs: Long = 0L,
)
