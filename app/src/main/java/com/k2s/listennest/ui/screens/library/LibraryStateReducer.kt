package com.k2s.listennest.ui.screens.library

internal fun requestBookActionsMenu(
    state: LibraryUiState,
    book: LibraryBookItem,
): LibraryUiState = state.copy(
    pendingBookMenuBook = book,
    pendingRemovalBook = null,
)

internal fun cancelBookActionsMenu(state: LibraryUiState): LibraryUiState =
    state.copy(pendingBookMenuBook = null)

internal fun chooseDeleteFromBookActionsMenu(state: LibraryUiState): LibraryUiState {
    val pendingBook = state.pendingBookMenuBook ?: return state
    return state.copy(
        pendingBookMenuBook = null,
        pendingRemovalBook = pendingBook,
    )
}

internal fun requestRemoveBookConfirmation(
    state: LibraryUiState,
    book: LibraryBookItem,
): LibraryUiState = state.copy(
    pendingBookMenuBook = null,
    pendingRemovalBook = book,
)

internal fun cancelRemoveBookConfirmation(state: LibraryUiState): LibraryUiState =
    state.copy(pendingRemovalBook = null)

internal fun confirmRemoveBookRemoval(state: LibraryUiState): LibraryUiState {
    val pendingBook = state.pendingRemovalBook ?: return state
    val clearedState = state.copy(pendingRemovalBook = null)
    return removeSavedBook(clearedState, pendingBook.folderUri, pendingBook.title)
}

internal fun removeSavedBook(
    state: LibraryUiState,
    folderUri: String,
    bookTitle: String,
): LibraryUiState {
    val updatedSavedBooks = state.savedBooks.filterNot { it.folderUri == folderUri }
    val removed = updatedSavedBooks.size != state.savedBooks.size
    if (!removed) return state

    val updatedPendingSelection = state.pendingSelectionUris - folderUri
    val updatedDiscoveredBooks = state.discoveredBooks.map { book ->
        if (book.folderUri == folderUri) {
            book.copy(isSelected = false)
        } else {
            book
        }
    }

    val removalMessage = if (updatedSavedBooks.isEmpty()) {
        "Removed \"$bookTitle\". Your library is now empty."
    } else {
        "Removed \"$bookTitle\" from your library."
    }

    return state.copy(
        savedBooks = updatedSavedBooks,
        discoveredBooks = updatedDiscoveredBooks,
        pendingSelectionUris = updatedPendingSelection,
        statusMessage = removalMessage,
    )
}
