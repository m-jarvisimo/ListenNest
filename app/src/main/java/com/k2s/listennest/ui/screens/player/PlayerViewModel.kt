package com.k2s.listennest.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.k2s.listennest.playback.PlaybackController
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import kotlinx.coroutines.flow.StateFlow

private const val DEFAULT_TRACK_DURATION_MS = 30 * 60 * 1000L

// Reused by the playback service and UI to keep notification/content state in sync.
data class PlayerUiState(
    val bookTitle: String = "No book selected",
    val folderUri: String = "",
    val coverArtUri: String? = null,
    val tracks: List<String> = emptyList(),
    val currentTrackIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = DEFAULT_TRACK_DURATION_MS,
    val speed: Float = 1.0f,
    val isPlaying: Boolean = false,
    val chapterPlaceholders: List<String> = emptyList(),
    val statusMessage: String = "Choose a book from the Library tab.",
) {
    val currentTrackLabel: String
        get() = tracks.getOrNull(currentTrackIndex) ?: "No track selected"
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val playbackController = PlaybackController(application)
    val uiState: StateFlow<PlayerUiState> = playbackController.state

    fun loadBook(book: LibraryBookItem?) {
        playbackController.loadBook(book)
    }

    fun togglePlayback() {
        playbackController.togglePlayback()
    }

    fun rewindOneMinute() {
        playbackController.rewindOneMinute()
    }

    fun rewindTenSeconds() {
        playbackController.rewindTenSeconds()
    }

    fun forwardTenSeconds() {
        playbackController.forwardTenSeconds()
    }

    fun forwardOneMinute() {
        playbackController.forwardOneMinute()
    }

    fun previousTrack() {
        playbackController.previousTrack()
    }

    fun nextTrack() {
        playbackController.nextTrack()
    }

    fun seekToTrack(index: Int) {
        playbackController.seekToTrack(index)
    }

    override fun onCleared() {
        playbackController.close()
        super.onCleared()
    }
}
