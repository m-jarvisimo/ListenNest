package com.k2s.listennest.data.resume

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackResumeDao {
    @Query("SELECT * FROM book_resume_state WHERE bookKey = :bookKey LIMIT 1")
    fun findByBookKey(bookKey: String): PlaybackResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PlaybackResumeEntity)
}
