package com.k2s.listennest.data.resume

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

data class PlaybackResume(
    val trackIndex: Int,
    val positionMs: Long,
)

class PlaybackResumeStore(context: Context) {
    private val dao = PlaybackResumeDatabase.getInstance(context).playbackResumeDao()

    fun load(bookKey: String): PlaybackResume? = runBlocking(Dispatchers.IO) {
        dao.findByBookKey(bookKey)?.let { entity ->
            PlaybackResume(
                trackIndex = entity.trackIndex,
                positionMs = entity.positionMs,
            )
        }
    }

    fun save(bookKey: String, trackIndex: Int, positionMs: Long) {
        runBlocking(Dispatchers.IO) {
            dao.upsert(
                PlaybackResumeEntity(
                    bookKey = bookKey,
                    trackIndex = trackIndex.coerceAtLeast(0),
                    positionMs = positionMs.coerceAtLeast(0L),
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }
}
