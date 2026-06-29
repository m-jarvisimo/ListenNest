package com.k2s.listennest.domain.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryTrackItem
import java.util.Locale

object LibraryFolderScanner {
    private val supportedExtensions = setOf("mp3", "m4b", "flac")

    fun scanLibraryTree(context: Context, rootUri: android.net.Uri): List<LibraryBookItem> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        val childFolders = root.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }

        val candidateFolders = if (childFolders.isNotEmpty()) childFolders else listOf(root)

        return candidateFolders.mapNotNull { folder ->
            val audioFiles = folder.listFiles()
                .filter { it.isFile && it.isSupportedAudio() }
                .sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }

            if (audioFiles.isEmpty()) {
                null
            } else {
                LibraryBookItem(
                    title = folder.name?.takeIf { it.isNotBlank() } ?: "Untitled book",
                    folderUri = folder.uri.toString(),
                    tracks = audioFiles.mapIndexed { index, file ->
                        LibraryTrackItem(
                            title = file.name?.takeIf { it.isNotBlank() } ?: "Track ${index + 1}",
                            uri = file.uri.toString(),
                        )
                    },
                )
            }
        }
    }

    private fun DocumentFile.isSupportedAudio(): Boolean {
        val extension = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.getDefault())
            .orEmpty()
        return extension in supportedExtensions
    }
}
