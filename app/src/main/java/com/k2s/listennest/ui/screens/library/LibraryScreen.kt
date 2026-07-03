package com.k2s.listennest.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k2s.listennest.ui.theme.ListenNestTheme

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onBookSelected: (LibraryBookItem) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreenContent(
        uiState = uiState,
        onBookSelected = onBookSelected,
        onBookLongPressed = viewModel::requestBookActionsMenu,
        onBookMenuDelete = { viewModel.chooseDeleteFromBookActionsMenu() },
        onBookMenuCancel = viewModel::cancelBookActionsMenu,
        onConfirmRemoveBook = viewModel::confirmRemoveBook,
        onCancelRemoveBook = viewModel::cancelRemoveBook,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryScreenContent(
    uiState: LibraryUiState,
    onBookSelected: (LibraryBookItem) -> Unit,
    onBookLongPressed: (LibraryBookItem) -> Unit = {},
    onBookMenuDelete: (LibraryBookItem) -> Unit = {},
    onBookMenuCancel: () -> Unit = {},
    onConfirmRemoveBook: () -> Unit = {},
    onCancelRemoveBook: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Tap a book to open it in the player. Tap and hold for actions.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!uiState.selectedFolderLabel.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Library source",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = uiState.selectedFolderLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        HorizontalDivider()

        if (uiState.hasSavedBooks) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Your books",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.savedBooks, key = { it.folderUri }) { book ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .combinedClickable(
                                                onClick = { onBookSelected(book) },
                                                onLongClick = { onBookLongPressed(book) },
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${book.trackCount} audio file${if (book.trackCount == 1) "" else "s"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = if (book.resumePositionMs > 0) {
                                                val trackName = book.tracks.getOrNull(book.resumeTrackIndex)?.title ?: "track ${book.resumeTrackIndex + 1}"
                                                "Resume saved at ${formatTime(book.resumePositionMs)} in $trackName"
                                            } else {
                                                "No resume position yet"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = uiState.pendingBookMenuBook?.folderUri == book.folderUri,
                                onDismissRequest = onBookMenuCancel,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete from library") },
                                    onClick = { onBookMenuDelete(book) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    onClick = onBookMenuCancel,
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No books in your library yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Open Settings to choose a folder, scan it, and pick the books you want to keep.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    uiState.pendingRemovalBook?.let { book ->
        AlertDialog(
            onDismissRequest = onCancelRemoveBook,
            title = {
                Text("Delete from library?")
            },
            text = {
                Text("Delete \"${book.title}\" from your library? This only removes it from the app.")
            },
            confirmButton = {
                TextButton(onClick = onConfirmRemoveBook) {
                    Text("Delete from library")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRemoveBook) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LibraryScreenPreview() {
    ListenNestTheme {
        LibraryScreenContent(
            uiState = LibraryUiState(
                selectedFolderLabel = "Audiobooks",
                savedBooks = listOf(
                    LibraryBookItem(
                        title = "Dune",
                        folderUri = "content://books/dune",
                        tracks = listOf(
                            LibraryTrackItem("01 - Dune", "content://books/dune/01"),
                            LibraryTrackItem("02 - Muad'Dib", "content://books/dune/02"),
                        ),
                        resumeTrackIndex = 1,
                        resumePositionMs = 8 * 60 * 1000L + 42 * 1000L,
                    ),
                    LibraryBookItem(
                        title = "Project Hail Mary",
                        folderUri = "content://books/hail-mary",
                        tracks = listOf(
                            LibraryTrackItem("01 - Rock", "content://books/hail-mary/01"),
                        ),
                    ),
                ),
            ),
            onBookSelected = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LibraryEmptyPreview() {
    ListenNestTheme {
        LibraryScreenContent(
            uiState = LibraryUiState(),
            onBookSelected = {},
        )
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
