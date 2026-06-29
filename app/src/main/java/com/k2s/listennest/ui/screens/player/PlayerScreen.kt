package com.k2s.listennest.ui.screens.player

import android.view.ViewGroup
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.k2s.listennest.ui.screens.library.LibraryBookItem

@Composable
fun PlayerScreen(
    book: LibraryBookItem? = null,
    viewModel: PlayerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(book?.folderUri) {
        viewModel.loadBook(book)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Player",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = uiState.bookTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = uiState.currentTrackLabel, style = MaterialTheme.typography.bodyMedium)
                Text(text = uiState.statusMessage, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Resume position: ${formatTime(uiState.positionMs)} / ${formatTime(uiState.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Playback speed: ${"%.1f".format(uiState.speed)}x",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::previousTrack) { Text("Prev") }
                    OutlinedButton(onClick = viewModel::rewind30Seconds) { Text("-30s") }
                    Button(onClick = viewModel::togglePlayback) {
                        Text(if (uiState.isPlaying) "Pause" else "Play")
                    }
                    OutlinedButton(onClick = viewModel::forward30Seconds) { Text("+30s") }
                    OutlinedButton(onClick = viewModel::nextTrack) { Text("Next") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { localContext ->
                    PlayerView(localContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        useController = true
                        player = viewModel.playerForView()
                    }
                },
                update = { playerView ->
                    playerView.player = viewModel.playerForView()
                },
            )
        }

        HorizontalDivider()

        Text(
            text = "Track list",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        if (uiState.tracks.isEmpty()) {
            Text(
                text = "No tracks available yet. Pick a scanned book from Library.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.tracks.withIndex().toList()) { indexedTrack ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.seekToTrack(indexedTrack.index) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = indexedTrack.value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (indexedTrack.index == uiState.currentTrackIndex) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Chapter placeholders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        if (uiState.chapterPlaceholders.isEmpty()) {
            Text(text = "No chapter placeholders yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.chapterPlaceholders) { chapter ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = chapter, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
