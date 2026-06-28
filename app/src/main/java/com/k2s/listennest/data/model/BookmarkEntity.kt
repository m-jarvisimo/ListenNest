package com.k2s.listennest.data.model

data class BookmarkEntity(
    val id: Long = 0L,
    val bookId: Long = 0L,
    val positionMs: Long = 0L,
    val label: String? = null,
    val createdAt: Long = 0L,
)
