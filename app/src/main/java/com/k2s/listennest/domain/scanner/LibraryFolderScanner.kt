package com.k2s.listennest.domain.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryTrackItem
import java.util.Locale

object LibraryFolderScanner {
    private val supportedExtensions = setOf("mp3", "m4b", "flac")
    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val preferredCoverNameHints = listOf("cover", "folder", "front", "art", "album")

    fun scanLibraryTree(context: Context, rootUri: android.net.Uri): List<LibraryBookItem> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        val childFolders = root.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }

        val candidateFolders = if (childFolders.isNotEmpty()) childFolders else listOf(root)

        return candidateFolders.mapNotNull { folder ->
            val childFiles = folder.listFiles()
            val audioFiles = childFiles
                .filter { it.isFile && it.isSupportedAudio() }
                .sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }

            if (audioFiles.isEmpty()) {
                null
            } else {
                LibraryBookItem(
                    title = folder.name?.takeIf { it.isNotBlank() } ?: "Untitled book",
                    folderUri = folder.uri.toString(),
                    coverArtUri = folder.findCoverArtUri(childFiles),
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

    private fun DocumentFile.findCoverArtUri(childFiles: Array<DocumentFile>): String? {
        val imageFiles = childFiles.filter { it.isFile && it.isSupportedImage() }
        if (imageFiles.isEmpty()) return null

        val preferredImage = imageFiles.firstOrNull { file ->
            val fileName = file.name.orEmpty().lowercase(Locale.getDefault())
            preferredCoverNameHints.any { hint -> fileName.contains(hint) }
        }
        return (preferredImage ?: imageFiles.first()).uri.toString()
    }

    private fun DocumentFile.isSupportedAudio(): Boolean {
        val extension = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.getDefault())
            .orEmpty()
        return extension in supportedExtensions
    }

    private fun DocumentFile.isSupportedImage(): Boolean {
        val mimeType = type.orEmpty().lowercase(Locale.getDefault())
        if (mimeType.startsWith("image/")) return true

        val extension = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.getDefault())
            .orEmpty()
        return extension in supportedImageExtensions
    }
}
