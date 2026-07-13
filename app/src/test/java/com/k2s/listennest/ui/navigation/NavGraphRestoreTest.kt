package com.k2s.listennest.ui.navigation

import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryTrackItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavGraphRestoreTest {
    @Test
    fun resolveBookToOpen_returnsMatchingBookWhenUriExists() {
        val dune = LibraryBookItem(
            title = "Dune",
            folderUri = "content://books/dune",
            tracks = listOf(LibraryTrackItem(title = "01 - Dune", uri = "content://tracks/dune/01")),
        )
        val hailMary = LibraryBookItem(
            title = "Project Hail Mary",
            folderUri = "content://books/hail-mary",
            tracks = listOf(LibraryTrackItem(title = "01 - Rocky", uri = "content://tracks/hail-mary/01")),
        )

        val restored = resolveBookToOpen(
            books = listOf(dune, hailMary),
            lastPlayedBookUri = "content://books/hail-mary",
        )

        assertEquals(hailMary, restored)
    }

    @Test
    fun resolveBookToOpen_returnsNullForMissingOrBlankUri() {
        val dune = LibraryBookItem(
            title = "Dune",
            folderUri = "content://books/dune",
            tracks = listOf(LibraryTrackItem(title = "01 - Dune", uri = "content://tracks/dune/01")),
        )

        assertNull(resolveBookToOpen(listOf(dune), null))
        assertNull(resolveBookToOpen(listOf(dune), ""))
        assertNull(resolveBookToOpen(listOf(dune), "content://books/unknown"))
    }
}
