package com.k2s.listennest.ui.screens.settings

import androidx.lifecycle.ViewModel

data class SettingsUiState(
    val libraryFolderLabel: String? = null,
    val useDarkTheme: Boolean = true,
)

class SettingsViewModel : ViewModel() {
    val uiState: SettingsUiState = SettingsUiState()
}
