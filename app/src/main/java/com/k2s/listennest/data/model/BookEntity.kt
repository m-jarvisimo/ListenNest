package com.k2s.listennest.data.model

data class BookEntity(
    val id: Long = 0L,
    val title: String = "",
    val author: String? = null,
    val folderUri: String = "",
    val coverArtUri: String? = null,
    val lastPlayedAt: Long? = null,
    val lastPositionMs: Long = 0L,
)
