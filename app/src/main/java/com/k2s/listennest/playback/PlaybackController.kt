package com.k2s.listennest.playback

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.Intent
import android.os.IBinder
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.player.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackController(application: Application) {
    private val appContext = application.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingActions = ArrayDeque<(PlaybackService) -> Unit>()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var service: PlaybackService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? PlaybackService.LocalBinder ?: return
            service = localBinder.getService()
            scope.launch {
                service!!.uiState.collect { _state.value = it }
            }
            while (pendingActions.isNotEmpty()) {
                pendingActions.removeFirst().invoke(service!!)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    init {
        bindService()
    }

    fun loadBook(book: LibraryBookItem?) = withService { it.loadBook(book) }

    fun togglePlayback() = withService { it.togglePlayback() }

    fun rewindOneMinute() = withService { it.rewindOneMinute() }

    fun rewindTenSeconds() = withService { it.rewindTenSeconds() }

    fun forwardTenSeconds() = withService { it.forwardTenSeconds() }

    fun forwardOneMinute() = withService { it.forwardOneMinute() }

    fun previousTrack() = withService { it.previousTrack() }

    fun nextTrack() = withService { it.nextTrack() }

    fun seekToTrack(index: Int) = withService { it.seekToTrack(index) }

    fun close() {
        if (bound) {
            runCatching { appContext.unbindService(connection) }
            bound = false
        }
        scope.cancel()
    }

    private fun withService(action: (PlaybackService) -> Unit) {
        service?.let(action) ?: run {
            pendingActions.add(action)
            bindService()
        }
    }

    private fun bindService() {
        if (bound) return
        val intent = Intent(appContext, PlaybackService::class.java)
        bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}
