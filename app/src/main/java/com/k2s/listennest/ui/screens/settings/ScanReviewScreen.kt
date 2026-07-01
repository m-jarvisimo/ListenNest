package com.k2s.listennest.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryViewModel

@Composable
fun ScanReviewScreen(
    onBackToSettings: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveredBooks = uiState.discoveredBooks
    val selectedCount = uiState.pendingSelectionUris.size

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        viewModel.clearScanResults()
                        onBackToSettings()
                    },
                ) {
                    Text("Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Review scan results",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (discoveredBooks.isEmpty()) {
                            "No books found."
                        } else {
                            "Found ${discoveredBooks.size} book${if (discoveredBooks.size == 1) "" else "s"}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { viewModel.saveSelectedBooks(onSaved = onSaved) },
                    enabled = selectedCount > 0 && discoveredBooks.isNotEmpty(),
                ) {
                    Text("Save selected")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (selectedCount == 0) {
                            "No books selected"
                        } else {
                            "$selectedCount book${if (selectedCount == 1) "" else "s"} selected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = viewModel::selectAllDiscoveredBooks,
                            enabled = discoveredBooks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Select all")
                        }
                        OutlinedButton(
                            onClick = viewModel::clearSelectedDiscoveredBooks,
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Clear all")
                        }
                    }
                }
            }

            HorizontalDivider()

            if (discoveredBooks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No books to review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Go back and scan a folder to discover audiobook folders.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            viewModel.clearScanResults()
                            onBackToSettings()
                        }) {
                            Text("Back to settings")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(discoveredBooks, key = { it.folderUri }) { book ->
                        val isSelected = book.isSelected
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleBookSelection(book.folderUri) },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                        ) {
                            LibrarySelectionRow(
                                book = book,
                                isSelected = isSelected,
                                onToggle = { viewModel.toggleBookSelection(book.folderUri) },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LibrarySelectionRow(
    book: LibraryBookItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isSelected) {
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
        TextButton(onClick = onToggle) {
            Text(if (isSelected) "Unselect" else "Select")
        }
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
