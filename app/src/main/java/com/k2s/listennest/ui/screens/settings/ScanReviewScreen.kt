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
                    Text(
                        text = if (selectedCount == 0) {
                            "No books selected"
                        } else {
                            "$selectedCount selected"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { viewModel.saveSelectedBooks(onSaved = onSaved) },
                        enabled = selectedCount > 0 && discoveredBooks.isNotEmpty(),
                    ) {
                        Text("Save")
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
                Text(
                    text = if (discoveredBooks.isEmpty()) {
                        "No books found"
                    } else {
                        "Found ${discoveredBooks.size}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

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
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Nothing to review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
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
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${book.trackCount} audio file${if (book.trackCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedButton(onClick = onToggle) {
            Text(if (isSelected) "Unselect" else "Select")
        }
    }
}
