package com.k2s.listennest.ui.screens.player

import androidx.lifecycle.ViewModel

data class PlayerUiState(
    val title: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val isPlaying: Boolean = false,
)

class PlayerViewModel : ViewModel() {
    val uiState: PlayerUiState = PlayerUiState()
}
