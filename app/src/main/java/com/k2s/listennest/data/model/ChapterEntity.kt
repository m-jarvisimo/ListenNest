package com.k2s.listennest.data.model

data class ChapterEntity(
    val id: Long = 0L,
    val bookId: Long = 0L,
    val title: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val index: Int = 0,
)
