package com.k2s.listennest.data.resume

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaybackResumeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PlaybackResumeDatabase : RoomDatabase() {
    abstract fun playbackResumeDao(): PlaybackResumeDao

    companion object {
        @Volatile
        private var INSTANCE: PlaybackResumeDatabase? = null

        fun getInstance(context: Context): PlaybackResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlaybackResumeDatabase::class.java,
                    "listenest_resume.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
