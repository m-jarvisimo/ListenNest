package com.k2s.listennest.ui.screens.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k2s.listennest.data.resume.PlaybackResumeStore
import com.k2s.listennest.domain.scanner.LibraryFolderScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryTrackItem(
    val title: String,
    val uri: String,
)

private const val SELECT_A_FOLDER_PROMPT = "Choose a folder to start scanning"

data class LibraryBookItem(
    val title: String,
    val folderUri: String,
    val tracks: List<LibraryTrackItem>,
    val resumeTrackIndex: Int = 0,
    val resumePositionMs: Long = 0L,
) {
    val trackCount: Int get() = tracks.size
}

data class LibraryUiState(
    val selectedFolderUri: String? = null,
    val selectedFolderLabel: String? = null,
    val books: List<LibraryBookItem> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String = SELECT_A_FOLDER_PROMPT,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val resumeStore = PlaybackResumeStore(application)
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun onFolderSelected(folderUri: String, folderLabel: String) {
        _uiState.update { current ->
            current.copy(
                selectedFolderUri = folderUri,
                selectedFolderLabel = folderLabel,
                statusMessage = "Folder selected. Tap Scan library to import books.",
            )
        }
    }

    fun scanLibrary() {
        val rootUriString = _uiState.value.selectedFolderUri
        if (rootUriString.isNullOrBlank()) {
            _uiState.update { it.copy(statusMessage = SELECT_A_FOLDER_PROMPT) }
            return
        }

        val context = getApplication<Application>()
        val rootUri = Uri.parse(rootUriString)

        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, statusMessage = "Scanning selected folder…") }
            val books = withContext(Dispatchers.IO) {
                LibraryFolderScanner.scanLibraryTree(context, rootUri)
                    .map { book ->
                        val resume = resumeStore.load(book.folderUri)
                        if (resume == null) {
                            book
                        } else {
                            book.copy(
                                resumeTrackIndex = resume.trackIndex.coerceIn(0, maxOf(0, book.trackCount - 1)),
                                resumePositionMs = resume.positionMs,
                            )
                        }
                    }
            }

            _uiState.update { current ->
                current.copy(
                    books = books,
                    isScanning = false,
                    statusMessage = when {
                        books.isEmpty() -> "No supported audiobook folders were found."
                        else -> "Found ${books.size} book${if (books.size == 1) "" else "s"}."
                    },
                )
            }
        }
    }
}
