package com.k2s.listennest.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k2s.listennest.domain.settings.PlaybackSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val libraryFolderLabel: String? = null,
    val useDarkTheme: Boolean = true,
    val phoneCallPauseEnabled: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val playbackSettingsStore = PlaybackSettingsStore(application)
    private val _uiState = MutableStateFlow(
        SettingsUiState(phoneCallPauseEnabled = playbackSettingsStore.isPhoneCallPauseEnabled()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setPhoneCallPauseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playbackSettingsStore.setPhoneCallPauseEnabled(enabled)
            _uiState.value = _uiState.value.copy(phoneCallPauseEnabled = enabled)
        }
    }
}
