package com.k2s.listennest.ui.screens.library

import androidx.lifecycle.ViewModel

data class LibraryUiState(
    val books: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val selectedFolderLabel: String? = null,
)

class LibraryViewModel : ViewModel() {
    val uiState: LibraryUiState = LibraryUiState()
}
