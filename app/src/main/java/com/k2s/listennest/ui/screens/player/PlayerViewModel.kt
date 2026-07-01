package com.k2s.listennest.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.k2s.listennest.data.resume.PlaybackResumeStore
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DEFAULT_TRACK_DURATION_MS = 30 * 60 * 1000L
private const val SAVE_PROGRESS_THRESHOLD_MS = 5_000L

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
    private val resumeStore = PlaybackResumeStore(application)
    private val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncFromPlayer(if (isPlaying) "Playing" else "Paused", persist = !isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncFromPlayer("Track changed", persist = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncFromPlayer(
                    when (playbackState) {
                        Player.STATE_BUFFERING -> "Buffering…"
                        Player.STATE_READY -> if (isPlaying) "Playing" else "Ready to play"
                        Player.STATE_ENDED -> "Playback complete"
                        else -> "Loading"
                    },
                    persist = playbackState == Player.STATE_ENDED,
                )
            }
        })
    }

    private var progressJob: Job? = null
    private var lastPersistedTrackIndex: Int = -1
    private var lastPersistedPositionMs: Long = -1L
    private var currentBookKey: String? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun playerForView(): ExoPlayer = player

    init {
        progressJob = viewModelScope.launch {
            while (isActive) {
                syncFromPlayer()
                delay(250)
            }
        }
    }

    fun loadBook(book: LibraryBookItem?) {
        if (book == null) {
            persistProgress(force = true)
            player.stop()
            player.clearMediaItems()
            currentBookKey = null
            lastPersistedTrackIndex = -1
            lastPersistedPositionMs = -1L
            _uiState.value = PlayerUiState()
            return
        }

        currentBookKey = book.folderUri
        lastPersistedTrackIndex = -1
        lastPersistedPositionMs = -1L

        val resume = resumeStore.load(book.folderUri)
        val resumeTrackIndex = resume?.trackIndex?.coerceIn(0, maxOf(0, book.tracks.lastIndex)) ?: book.resumeTrackIndex.coerceIn(0, maxOf(0, book.tracks.lastIndex))
        val resumePosition = resume?.positionMs ?: book.resumePositionMs
        val mediaItems = book.tracks.map { track -> MediaItem.fromUri(track.uri) }
        val chapterPlaceholders = if (book.tracks.isEmpty()) {
            listOf("Chapter placeholder 1", "Chapter placeholder 2", "Chapter placeholder 3")
        } else {
            book.tracks.mapIndexed { index, _ -> "Chapter placeholder ${index + 1}" }
        }

        player.setMediaItems(mediaItems, /* resetPosition = */ true)
        player.prepare()
        if (mediaItems.isNotEmpty()) {
            player.seekTo(resumeTrackIndex, resumePosition)
        }
        player.playWhenReady = false

        _uiState.update {
            it.copy(
                bookTitle = book.title,
                folderUri = book.folderUri,
                coverArtUri = book.coverArtUri,
                tracks = book.tracks.map { track -> track.title },
                currentTrackIndex = resumeTrackIndex,
                positionMs = resumePosition,
                durationMs = DEFAULT_TRACK_DURATION_MS,
                isPlaying = false,
                chapterPlaceholders = chapterPlaceholders,
                statusMessage = if (resumePosition > 0) {
                    "Resume available at ${formatTime(resumePosition)}"
                } else {
                    "Ready to play"
                },
            )
        }

        persistProgress(force = true)
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        syncFromPlayer()
    }

    fun previousTrack() {
        if (player.mediaItemCount == 0) return
        player.seekToPreviousMediaItem()
        player.playWhenReady = true
        syncFromPlayer("Previous track", persist = true)
    }

    fun nextTrack() {
        if (player.mediaItemCount == 0) return
        player.seekToNextMediaItem()
        player.playWhenReady = true
        syncFromPlayer("Next track", persist = true)
    }

    fun rewindTenSeconds() {
        val next = (player.currentPosition - 10_000L).coerceAtLeast(0L)
        player.seekTo(next)
        syncFromPlayer("Rewind 10s", persist = true)
    }

    fun forwardTenSeconds() {
        val duration = player.duration.takeIf { it > 0L } ?: DEFAULT_TRACK_DURATION_MS
        val next = (player.currentPosition + 10_000L).coerceAtMost(duration)
        player.seekTo(next)
        syncFromPlayer("Forward 10s", persist = true)
    }

    fun rewindOneMinute() {
        val next = (player.currentPosition - 60_000L).coerceAtLeast(0L)
        player.seekTo(next)
        syncFromPlayer("Rewind 1m", persist = true)
    }

    fun forwardOneMinute() {
        val duration = player.duration.takeIf { it > 0L } ?: DEFAULT_TRACK_DURATION_MS
        val next = (player.currentPosition + 60_000L).coerceAtMost(duration)
        player.seekTo(next)
        syncFromPlayer("Forward 1m", persist = true)
    }

    fun seekToTrack(index: Int) {
        if (index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0L)
        player.playWhenReady = true
        syncFromPlayer("Track selected", persist = true)
    }

    private fun syncFromPlayer(statusMessage: String? = null, persist: Boolean = false) {
        val mediaIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        val labels = _uiState.value.tracks
        val duration = player.duration.takeIf { it > 0L && it != Long.MIN_VALUE } ?: DEFAULT_TRACK_DURATION_MS
        val position = player.currentPosition.coerceAtLeast(0L)
        _uiState.update { current ->
            current.copy(
                currentTrackIndex = mediaIndex,
                positionMs = position,
                durationMs = duration,
                speed = player.playbackParameters.speed,
                isPlaying = player.isPlaying,
                statusMessage = statusMessage ?: current.statusMessage,
                tracks = labels,
            )
        }

        maybePersistProgress(mediaIndex, position, persist)
    }

    private fun maybePersistProgress(trackIndex: Int, positionMs: Long, force: Boolean) {
        val bookKey = currentBookKey ?: return
        val shouldPersist = force ||
            lastPersistedTrackIndex != trackIndex ||
            kotlin.math.abs(positionMs - lastPersistedPositionMs) >= SAVE_PROGRESS_THRESHOLD_MS

        if (shouldPersist) {
            resumeStore.save(bookKey, trackIndex, positionMs)
            lastPersistedTrackIndex = trackIndex
            lastPersistedPositionMs = positionMs
        }
    }

    private fun persistProgress(force: Boolean = false) {
        maybePersistProgress(
            trackIndex = player.currentMediaItemIndex.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            force = force,
        )
    }

    override fun onCleared() {
        persistProgress(force = true)
        progressJob?.cancel()
        player.release()
        super.onCleared()
    }

    private fun formatTime(positionMs: Long): String {
        val totalSeconds = positionMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
