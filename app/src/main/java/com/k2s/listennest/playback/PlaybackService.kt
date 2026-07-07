package com.k2s.listennest.playback

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.k2s.listennest.MainActivity
import com.k2s.listennest.R
import com.k2s.listennest.data.resume.PlaybackResumeStore
import com.k2s.listennest.domain.settings.PlaybackSettingsStore
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryTrackItem
import com.k2s.listennest.ui.screens.player.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 2048
private const val NOTIFICATION_CHANNEL_ID = "listennest_playback"
private const val DEFAULT_TRACK_DURATION_MS = 30 * 60 * 1000L
private const val SAVE_PROGRESS_THRESHOLD_MS = 5_000L
private const val REWIND_INCREMENT_MS = 10_000L
private const val FAST_FORWARD_INCREMENT_MS = 30_000L
private const val PHONE_CALL_PAUSE_ENABLED_KEY = "phone_call_pause_enabled"

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var resumeStore: PlaybackResumeStore
    private lateinit var playbackSettingsStore: PlaybackSettingsStore
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: PlayerNotificationManager

    private var progressJob: Job? = null
    private var lastPersistedTrackIndex: Int = -1
    private var lastPersistedPositionMs: Long = -1L
    private var currentBookKey: String? = null
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var legacyCallStateListener: Any? = null
    private var resumeAfterCall: Boolean = false
    private val phoneStatePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PHONE_CALL_PAUSE_ENABLED_KEY) {
            updatePhoneCallPauseRegistration()
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        resumeStore = PlaybackResumeStore(applicationContext)
        playbackSettingsStore = PlaybackSettingsStore(applicationContext)
        player = ExoPlayer.Builder(applicationContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus= */ true,
            )
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

        mediaSession = MediaSession.Builder(this, player)
            .setId("listennest-playback")
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession.setSessionActivity(sessionActivityIntent)

        notificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
            .setChannelNameResourceId(R.string.playback_notification_channel_name)
            .setChannelDescriptionResourceId(R.string.playback_notification_channel_description)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence =
                    _uiState.value.bookTitle

                override fun createCurrentContentIntent(player: Player): PendingIntent? =
                    sessionActivityIntent

                override fun getCurrentContentText(player: Player): CharSequence? =
                    _uiState.value.currentTrackLabel

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback,
                ): Bitmap? {
                    val coverArtUri = _uiState.value.coverArtUri
                    return when {
                        !coverArtUri.isNullOrBlank() -> decodeBitmapFromUri(Uri.parse(coverArtUri))
                        else -> drawableToBitmap(R.drawable.cover_art_fallback)
                    }
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean,
                ) {
                    if (ongoing) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            startForeground(
                                notificationId,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            startForeground(notificationId, notification)
                        }
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    if (!player.isPlaying) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
            })
            .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
            .build()
        notificationManager.setUsePlayPauseActions(true)
        notificationManager.setUseRewindAction(true)
        notificationManager.setUseFastForwardAction(true)
        notificationManager.setUsePreviousAction(false)
        notificationManager.setUseNextAction(false)
        notificationManager.setUseStopAction(false)
        notificationManager.setUseChronometer(true)
        notificationManager.setMediaSessionToken(mediaSession.platformToken)
        notificationManager.setPlayer(player)

        playbackSettingsStore.registerListener(phoneStatePreferenceListener)
        updatePhoneCallPauseRegistration()

        progressJob = serviceScope.launch {
            while (isActive) {
                syncFromPlayer()
                delay(250)
            }
        }
    }

    override fun onDestroy() {
        persistProgress(force = true)
        progressJob?.cancel()
        unregisterPhoneStateListener()
        if (this::playbackSettingsStore.isInitialized) {
            playbackSettingsStore.unregisterListener(phoneStatePreferenceListener)
        }
        if (this::notificationManager.isInitialized) {
            notificationManager.setPlayer(null)
        }
        if (this::mediaSession.isInitialized) {
            mediaSession.release()
        }
        if (this::player.isInitialized) {
            player.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updatePhoneCallPauseRegistration() {
        if (!this::playbackSettingsStore.isInitialized || !this::player.isInitialized) return
        unregisterPhoneStateListener()
        if (playbackSettingsStore.isPhoneCallPauseEnabled()) {
            registerPhoneStateListener()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_PHONE_STATE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
        val manager = telephonyManager ?: return
        resumeAfterCall = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                @Suppress("DEPRECATION")
                override fun onCallStateChanged(state: Int) {
                    if (!this@PlaybackService::player.isInitialized) return
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING,
                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            resumeAfterCall = player.isPlaying
                            if (resumeAfterCall) {
                                player.pause()
                                syncFromPlayer("Phone call started", persist = false)
                            }
                        }
                        TelephonyManager.CALL_STATE_IDLE -> {
                            if (resumeAfterCall) {
                                resumeAfterCall = false
                                player.play()
                                syncFromPlayer("Phone call ended", persist = false)
                            }
                        }
                    }
                }
            }
            telephonyCallback = callback
            manager.registerTelephonyCallback(ContextCompat.getMainExecutor(applicationContext), callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                @Suppress("DEPRECATION")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (!this@PlaybackService::player.isInitialized) return
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING,
                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            resumeAfterCall = player.isPlaying
                            if (resumeAfterCall) {
                                player.pause()
                                syncFromPlayer("Phone call started", persist = false)
                            }
                        }
                        TelephonyManager.CALL_STATE_IDLE -> {
                            if (resumeAfterCall) {
                                resumeAfterCall = false
                                player.play()
                                syncFromPlayer("Phone call ended", persist = false)
                            }
                        }
                    }
                }
            }
            legacyCallStateListener = listener
            @Suppress("DEPRECATION")
            manager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun unregisterPhoneStateListener() {
        val manager = telephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { manager.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            val listener = legacyCallStateListener as? android.telephony.PhoneStateListener
            if (listener != null) {
                @Suppress("DEPRECATION")
                manager.listen(listener, android.telephony.PhoneStateListener.LISTEN_NONE)
            }
            legacyCallStateListener = null
        }

        telephonyManager = null
        resumeAfterCall = false
    }

    fun loadBook(book: LibraryBookItem?) {
        if (!this::player.isInitialized) return

        if (book == null) {
            persistProgress(force = true)
            player.stop()
            player.clearMediaItems()
            currentBookKey = null
            lastPersistedTrackIndex = -1
            lastPersistedPositionMs = -1L
            _uiState.value = PlayerUiState()
            notificationManager.invalidate()
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        currentBookKey = book.folderUri
        lastPersistedTrackIndex = -1
        lastPersistedPositionMs = -1L

        val resume = resumeStore.load(book.folderUri)
        val resumeTrackIndex = resume?.trackIndex?.coerceIn(0, maxOf(0, book.tracks.lastIndex))
            ?: book.resumeTrackIndex.coerceIn(0, maxOf(0, book.tracks.lastIndex))
        val resumePosition = resume?.positionMs ?: book.resumePositionMs
        val mediaItems = book.tracks.map { track -> track.toMediaItem(book) }

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
                chapterPlaceholders = if (book.tracks.isEmpty()) {
                    listOf("Chapter placeholder 1", "Chapter placeholder 2", "Chapter placeholder 3")
                } else {
                    book.tracks.mapIndexed { index, _ -> "Chapter placeholder ${index + 1}" }
                },
                statusMessage = if (resumePosition > 0) {
                    "Resume available at ${formatTime(resumePosition)}"
                } else {
                    "Ready to play"
                },
            )
        }

        notificationManager.invalidate()
        persistProgress(force = true)
    }

    fun togglePlayback() {
        if (!this::player.isInitialized) return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        syncFromPlayer()
    }

    fun previousTrack() {
        if (!this::player.isInitialized || player.mediaItemCount == 0) return
        player.seekToPreviousMediaItem()
        player.playWhenReady = true
        syncFromPlayer("Previous track", persist = true)
    }

    fun nextTrack() {
        if (!this::player.isInitialized || player.mediaItemCount == 0) return
        player.seekToNextMediaItem()
        player.playWhenReady = true
        syncFromPlayer("Next track", persist = true)
    }

    fun rewindTenSeconds() {
        if (!this::player.isInitialized) return
        val next = (player.currentPosition - REWIND_INCREMENT_MS).coerceAtLeast(0L)
        player.seekTo(next)
        syncFromPlayer("Rewind 10s", persist = true)
    }

    fun forwardTenSeconds() {
        if (!this::player.isInitialized) return
        val duration = player.duration.takeIf { it > 0L } ?: DEFAULT_TRACK_DURATION_MS
        val next = (player.currentPosition + 10_000L).coerceAtMost(duration)
        player.seekTo(next)
        syncFromPlayer("Forward 10s", persist = true)
    }

    fun rewindOneMinute() {
        if (!this::player.isInitialized) return
        val next = (player.currentPosition - 60_000L).coerceAtLeast(0L)
        player.seekTo(next)
        syncFromPlayer("Rewind 1m", persist = true)
    }

    fun forwardOneMinute() {
        if (!this::player.isInitialized) return
        val duration = player.duration.takeIf { it > 0L } ?: DEFAULT_TRACK_DURATION_MS
        val next = (player.currentPosition + 60_000L).coerceAtMost(duration)
        player.seekTo(next)
        syncFromPlayer("Forward 1m", persist = true)
    }

    fun seekToPosition(positionMs: Long) {
        if (!this::player.isInitialized) return
        val duration = player.duration.takeIf { it > 0L } ?: DEFAULT_TRACK_DURATION_MS
        val next = positionMs.coerceIn(0L, duration)
        player.seekTo(next)
        syncFromPlayer("Seek", persist = true)
    }

    fun seekToTrack(index: Int) {
        if (!this::player.isInitialized || index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0L)
        player.playWhenReady = true
        syncFromPlayer("Track selected", persist = true)
    }

    private fun syncFromPlayer(statusMessage: String? = null, persist: Boolean = false) {
        if (!this::player.isInitialized) return

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

        if (player.isPlaying) {
            notificationManager.invalidate()
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
        if (!this::player.isInitialized) return
        maybePersistProgress(
            trackIndex = player.currentMediaItemIndex.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            force = force,
        )
    }

    private fun LibraryTrackItem.toMediaItem(book: LibraryBookItem): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setAlbumTitle(book.title)
            .setArtist(book.title)
        book.coverArtUri?.takeIf { it.isNotBlank() }?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(@DrawableRes drawableResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(applicationContext, drawableResId)
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun formatTime(positionMs: Long): String {
        val totalSeconds = positionMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
