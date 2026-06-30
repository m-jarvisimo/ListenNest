package com.k2s.listennest.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun ScanResultsScreen(
    viewModel: LibraryViewModel = viewModel(),
    onDone: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveredBooks = uiState.discoveredBooks
    val selectedCount = uiState.pendingSelectionUris.size

    fun exitScan() {
        viewModel.clearScanResults()
        onDone()
    }

    BackHandler { exitScan() }

    Scaffold(
        bottomBar = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = ::exitScan) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            viewModel.saveSelectedBooks {
                                onDone()
                            }
                        },
                        enabled = selectedCount > 0 && !uiState.isScanning,
                    ) {
                        Text("Save selected")
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = ::exitScan) {
                    Text("Back")
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Scan results",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose what to keep",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = uiState.selectedFolderLabel ?: "Selected folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = when {
                            uiState.isScanning -> "Scanning the folder for audiobook libraries…"
                            discoveredBooks.isEmpty() -> "No supported audiobook folders were found in this location."
                            else -> "Found ${discoveredBooks.size} book${if (discoveredBooks.size == 1) "" else "s"}. Select the ones you want to import into ListenNest."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = viewModel::selectAllDiscoveredBooks,
                            enabled = discoveredBooks.isNotEmpty(),
                        ) {
                            Text("Select all")
                        }
                        OutlinedButton(
                            onClick = viewModel::clearSelectedDiscoveredBooks,
                            enabled = selectedCount > 0,
                        ) {
                            Text("Clear all")
                        }
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            if (discoveredBooks.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No books found yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Try a folder that contains audiobook subfolders or supported audio files, then scan again.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = ::exitScan) {
                            Text("Back to library")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
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
                                TextButton(onClick = { viewModel.toggleBookSelection(book.folderUri) }) {
                                    Text(if (isSelected) "Unselect" else "Select")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
