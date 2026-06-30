package com.k2s.listennest.domain.settings

import android.content.Context

private const val LIBRARY_PREFS_NAME = "library_settings"
private const val KEY_SOURCE_FOLDER_URI = "source_folder_uri"
private const val KEY_SOURCE_FOLDER_LABEL = "source_folder_label"
private const val KEY_SELECTED_BOOK_URIS = "selected_book_uris"

data class LibrarySelectionSnapshot(
    val sourceFolderUri: String? = null,
    val sourceFolderLabel: String? = null,
    val selectedBookUris: Set<String> = emptySet(),
)

class LibrarySettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        LIBRARY_PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun loadSelection(): LibrarySelectionSnapshot {
        return LibrarySelectionSnapshot(
            sourceFolderUri = prefs.getString(KEY_SOURCE_FOLDER_URI, null),
            sourceFolderLabel = prefs.getString(KEY_SOURCE_FOLDER_LABEL, null),
            selectedBookUris = prefs.getStringSet(KEY_SELECTED_BOOK_URIS, emptySet())
                .orEmpty()
                .toSet(),
        )
    }

    fun saveSelection(
        sourceFolderUri: String,
        sourceFolderLabel: String,
        selectedBookUris: Set<String>,
    ) {
        prefs.edit()
            .putString(KEY_SOURCE_FOLDER_URI, sourceFolderUri)
            .putString(KEY_SOURCE_FOLDER_LABEL, sourceFolderLabel)
            .putStringSet(KEY_SELECTED_BOOK_URIS, selectedBookUris)
            .apply()
    }

    fun clearSelection() {
        prefs.edit()
            .remove(KEY_SOURCE_FOLDER_URI)
            .remove(KEY_SOURCE_FOLDER_LABEL)
            .remove(KEY_SELECTED_BOOK_URIS)
            .apply()
    }
}
