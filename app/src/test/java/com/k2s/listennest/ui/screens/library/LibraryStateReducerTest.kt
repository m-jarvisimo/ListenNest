package com.k2s.listennest.ui.screens.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryStateReducerTest {
    @Test
    fun requestRemoveBookConfirmation_setsPendingRemoval() {
        val book = LibraryBookItem(
            title = "Book One",
            folderUri = "content://books/one",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/1")),
            isSelected = true,
        )
        val state = LibraryUiState(
            selectedFolderUri = "content://root",
            selectedFolderLabel = "Audiobooks",
            savedBooks = listOf(book),
            pendingSelectionUris = setOf(book.folderUri),
        )

        val updated = requestRemoveBookConfirmation(state, book)

        assertEquals(book, updated.pendingRemovalBook)
        assertEquals(listOf(book), updated.savedBooks)
        assertEquals(setOf(book.folderUri), updated.pendingSelectionUris)
    }

    @Test
    fun cancelRemoveBookConfirmation_clearsPendingRemoval() {
        val book = LibraryBookItem(
            title = "Book One",
            folderUri = "content://books/one",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/1")),
            isSelected = true,
        )
        val state = LibraryUiState(
            selectedFolderUri = "content://root",
            selectedFolderLabel = "Audiobooks",
            savedBooks = listOf(book),
            pendingSelectionUris = setOf(book.folderUri),
            pendingRemovalBook = book,
        )

        val updated = cancelRemoveBookConfirmation(state)

        assertEquals(null, updated.pendingRemovalBook)
        assertEquals(listOf(book), updated.savedBooks)
        assertEquals(setOf(book.folderUri), updated.pendingSelectionUris)
    }

    @Test
    fun confirmRemoveBookRemoval_removesPendingBookAndClearsDialogState() {
        val book = LibraryBookItem(
            title = "Book One",
            folderUri = "content://books/one",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/1")),
            isSelected = true,
        )
        val state = LibraryUiState(
            selectedFolderUri = "content://root",
            selectedFolderLabel = "Audiobooks",
            savedBooks = listOf(book),
            pendingSelectionUris = setOf(book.folderUri),
            pendingRemovalBook = book,
        )

        val updated = confirmRemoveBookRemoval(state)

        assertTrue(updated.savedBooks.isEmpty())
        assertTrue(updated.pendingSelectionUris.isEmpty())
        assertEquals(null, updated.pendingRemovalBook)
        assertEquals("Removed \"Book One\". Your library is now empty.", updated.statusMessage)
    }

    @Test
    fun removeSavedBook_updatesSavedBooksAndSelectionState() {
        val firstBook = LibraryBookItem(
            title = "Book One",
            folderUri = "content://books/one",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/1")),
            isSelected = true,
        )
        val secondBook = LibraryBookItem(
            title = "Book Two",
            folderUri = "content://books/two",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/2")),
            isSelected = true,
        )
        val state = LibraryUiState(
            selectedFolderUri = "content://root",
            selectedFolderLabel = "Audiobooks",
            savedBooks = listOf(firstBook, secondBook),
            pendingSelectionUris = setOf(firstBook.folderUri, secondBook.folderUri),
            statusMessage = "Loaded 2 saved books.",
        )

        val updated = removeSavedBook(
            state = state,
            folderUri = firstBook.folderUri,
            bookTitle = firstBook.title,
        )

        assertEquals(listOf(secondBook), updated.savedBooks)
        assertEquals(setOf(secondBook.folderUri), updated.pendingSelectionUris)
        assertEquals("Removed \"Book One\" from your library.", updated.statusMessage)
        assertEquals("content://root", updated.selectedFolderUri)
        assertEquals("Audiobooks", updated.selectedFolderLabel)
    }

    @Test
    fun removeSavedBook_whenRemovingLastBook_marksLibraryEmpty() {
        val onlyBook = LibraryBookItem(
            title = "Solo Book",
            folderUri = "content://books/solo",
            tracks = listOf(LibraryTrackItem(title = "track 1", uri = "content://tracks/solo")),
            isSelected = true,
        )
        val state = LibraryUiState(
            selectedFolderUri = "content://root",
            selectedFolderLabel = "Audiobooks",
            savedBooks = listOf(onlyBook),
            pendingSelectionUris = setOf(onlyBook.folderUri),
            statusMessage = "Loaded 1 saved book.",
        )

        val updated = removeSavedBook(
            state = state,
            folderUri = onlyBook.folderUri,
            bookTitle = onlyBook.title,
        )

        assertTrue(updated.savedBooks.isEmpty())
        assertTrue(updated.pendingSelectionUris.isEmpty())
        assertEquals("Removed \"Solo Book\". Your library is now empty.", updated.statusMessage)
    }
}
