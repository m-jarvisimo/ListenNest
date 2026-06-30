package com.k2s.listennest.ui.screens.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k2s.listennest.data.resume.PlaybackResumeStore
import com.k2s.listennest.domain.scanner.LibraryFolderScanner
import com.k2s.listennest.domain.settings.LibrarySettingsStore
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
    val isSelected: Boolean = false,
) {
    val trackCount: Int get() = tracks.size
}

data class LibraryUiState(
    val selectedFolderUri: String? = null,
    val selectedFolderLabel: String? = null,
    val savedBooks: List<LibraryBookItem> = emptyList(),
    val discoveredBooks: List<LibraryBookItem> = emptyList(),
    val pendingSelectionUris: Set<String> = emptySet(),
    val pendingRemovalBook: LibraryBookItem? = null,
    val isScanning: Boolean = false,
    val statusMessage: String = SELECT_A_FOLDER_PROMPT,
) {
    val hasDiscoveredBooks: Boolean get() = discoveredBooks.isNotEmpty()
    val hasSavedBooks: Boolean get() = savedBooks.isNotEmpty()
    val hasFolderSelected: Boolean get() = !selectedFolderUri.isNullOrBlank()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val resumeStore = PlaybackResumeStore(application)
    private val settingsStore = LibrarySettingsStore(application)
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        restoreSavedLibrary()
    }

    fun onFolderSelected(folderUri: String, folderLabel: String) {
        _uiState.update { current ->
            current.copy(
                selectedFolderUri = folderUri,
                selectedFolderLabel = folderLabel,
                discoveredBooks = emptyList(),
                pendingSelectionUris = emptySet(),
                pendingRemovalBook = null,
                statusMessage = "Folder selected. Tap Scan library to discover books.",
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
            val scannedBooks = withContext(Dispatchers.IO) {
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
            val savedSelection = settingsStore.loadSelection().selectedBookUris
            val pendingSelection = if (savedSelection.isNotEmpty()) {
                scannedBooks.filter { it.folderUri in savedSelection }.mapTo(mutableSetOf()) { it.folderUri }
            } else {
                emptySet()
            }
            val visibleSavedBooks = scannedBooks.filter { it.folderUri in savedSelection }.map { book ->
                book.copy(isSelected = true)
            }
            val visibleDiscoveredBooks = scannedBooks.map { book ->
                book.copy(isSelected = book.folderUri in pendingSelection)
            }

            _uiState.update { current ->
                current.copy(
                    savedBooks = visibleSavedBooks,
                    discoveredBooks = visibleDiscoveredBooks,
                    pendingSelectionUris = pendingSelection,
                    isScanning = false,
                    statusMessage = when {
                        scannedBooks.isEmpty() -> "No supported audiobook folders were found."
                        visibleSavedBooks.isNotEmpty() -> "Found ${visibleSavedBooks.size} saved book${if (visibleSavedBooks.size == 1) "" else "s"}."
                        else -> "Found ${scannedBooks.size} book${if (scannedBooks.size == 1) "" else "s"}. Select the ones you want and save them."
                    },
                )
            }
        }
    }

    fun toggleBookSelection(folderUri: String) {
        _uiState.update { current ->
            val newSelection = current.pendingSelectionUris.toMutableSet().apply {
                if (!add(folderUri)) {
                    remove(folderUri)
                }
            }
            current.copy(
                pendingSelectionUris = newSelection,
                discoveredBooks = current.discoveredBooks.map { book ->
                    if (book.folderUri == folderUri) {
                        book.copy(isSelected = folderUri in newSelection)
                    } else {
                        book
                    }
                },
            )
        }
    }

    fun saveSelectedBooks() {
        val current = _uiState.value
        val folderUri = current.selectedFolderUri ?: return
        val folderLabel = current.selectedFolderLabel ?: "Selected folder"
        val selectedUris = current.pendingSelectionUris
        val discoveredBooks = current.discoveredBooks
        if (selectedUris.isEmpty() || discoveredBooks.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one book before saving.") }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                settingsStore.saveSelection(folderUri, folderLabel, selectedUris)
            }
            val savedBooks = discoveredBooks.filter { it.folderUri in selectedUris }.map { it.copy(isSelected = true) }
            _uiState.update { state ->
                state.copy(
                    selectedFolderUri = folderUri,
                    selectedFolderLabel = folderLabel,
                    savedBooks = savedBooks,
                    discoveredBooks = emptyList(),
                    pendingSelectionUris = selectedUris,
                    statusMessage = "Saved ${savedBooks.size} book${if (savedBooks.size == 1) "" else "s"} to your library.",
                )
            }
        }
    }

    fun requestRemoveBook(book: LibraryBookItem) {
        _uiState.update { current ->
            requestRemoveBookConfirmation(current, book)
        }
    }

    fun cancelRemoveBook() {
        _uiState.update { current ->
            cancelRemoveBookConfirmation(current)
        }
    }

    fun confirmRemoveBook() {
        val current = _uiState.value
        val updatedState = confirmRemoveBookRemoval(current)
        if (updatedState === current) return

        val folderToPersist = updatedState.selectedFolderUri ?: return
        val folderLabelToPersist = updatedState.selectedFolderLabel ?: "Selected folder"

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                settingsStore.saveSelection(folderToPersist, folderLabelToPersist, updatedState.pendingSelectionUris)
            }
            _uiState.update { updatedState }
        }
    }

    private fun restoreSavedLibrary() {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { settingsStore.loadSelection() }
            val folderUri = snapshot.sourceFolderUri
            if (folderUri.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        selectedFolderLabel = snapshot.sourceFolderLabel,
                        statusMessage = SELECT_A_FOLDER_PROMPT,
                    )
                }
                return@launch
            }

            val rootUri = Uri.parse(folderUri)
            val context = getApplication<Application>()
            val books = withContext(Dispatchers.IO) {
                LibraryFolderScanner.scanLibraryTree(context, rootUri)
                    .map { book ->
                        val resume = resumeStore.load(book.folderUri)
                        val withResume = if (resume == null) {
                            book
                        } else {
                            book.copy(
                                resumeTrackIndex = resume.trackIndex.coerceIn(0, maxOf(0, book.trackCount - 1)),
                                resumePositionMs = resume.positionMs,
                            )
                        }
                        withResume.copy(isSelected = withResume.folderUri in snapshot.selectedBookUris)
                    }
            }

            val savedBooks = books.filter { it.isSelected }
            _uiState.update {
                it.copy(
                    selectedFolderUri = folderUri,
                    selectedFolderLabel = snapshot.sourceFolderLabel ?: "Selected folder",
                    savedBooks = savedBooks,
                    discoveredBooks = emptyList(),
                    pendingSelectionUris = snapshot.selectedBookUris.intersect(books.map { book -> book.folderUri }.toSet()),
                    statusMessage = if (savedBooks.isEmpty()) {
                        "Your saved library is empty. Tap Scan library to choose books."
                    } else {
                        "Loaded ${savedBooks.size} saved book${if (savedBooks.size == 1) "" else "s"}."
                    },
                )
            }
        }
    }
}
