package com.k2s.listennest.data.resume

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_resume_state")
data class PlaybackResumeEntity(
    @PrimaryKey val bookKey: String,
    val trackIndex: Int,
    val positionMs: Long,
    val updatedAtMs: Long,
)
