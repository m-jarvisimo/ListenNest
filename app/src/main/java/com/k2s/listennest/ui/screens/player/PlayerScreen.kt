package com.k2s.listennest.ui.screens.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k2s.listennest.R
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryTrackItem
import com.k2s.listennest.ui.theme.ListenNestTheme

@Composable
fun PlayerScreen(
    book: LibraryBookItem? = null,
    viewModel: PlayerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(book?.folderUri) {
        viewModel.loadBook(book)
    }

    PlayerScreenContent(
        uiState = uiState,
        onTogglePlayback = viewModel::togglePlayback,
        onRewindOneMinute = viewModel::rewindOneMinute,
        onRewindTenSeconds = viewModel::rewindTenSeconds,
        onForwardTenSeconds = viewModel::forwardTenSeconds,
        onForwardOneMinute = viewModel::forwardOneMinute,
    )
}

@Composable
internal fun PlayerScreenContent(
    uiState: PlayerUiState,
    onTogglePlayback: () -> Unit,
    onRewindOneMinute: () -> Unit,
    onRewindTenSeconds: () -> Unit,
    onForwardTenSeconds: () -> Unit,
    onForwardOneMinute: () -> Unit,
) {
    val progress = if (uiState.durationMs > 0L) {
        (uiState.positionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = uiState.bookTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = uiState.currentTrackLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val hasCoverArt = !uiState.coverArtUri.isNullOrBlank()
                    if (hasCoverArt) {
                        AsyncImage(
                            model = uiState.coverArtUri,
                            contentDescription = "${uiState.bookTitle} cover art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.cover_art_fallback),
                            error = painterResource(id = R.drawable.cover_art_fallback),
                            fallback = painterResource(id = R.drawable.cover_art_fallback),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                    ),
                                ),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cover_art_fallback),
                                contentDescription = "${uiState.bookTitle} cover art fallback",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.20f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = fallbackCoverBadge(uiState.bookTitle),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "No cover art",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Add folder art to personalize this book",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.00f),
                                        0.55f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.05f),
                                        1.00f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.76f),
                                    ),
                                ),
                            ),
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Now playing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = uiState.currentTrackLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.10f))
                            .padding(4.dp),
                    ) {
                        FloatingActionButton(
                            onClick = onTogglePlayback,
                            modifier = Modifier.size(52.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = if (uiState.isPlaying) "❚❚" else "▶",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = formatTime(uiState.positionMs))
                Text(text = formatTime((uiState.durationMs - uiState.positionMs).coerceAtLeast(0L)))
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRewindOneMinute,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("<<", style = MaterialTheme.typography.titleMedium)
                        Text("1m", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = onRewindTenSeconds,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("<", style = MaterialTheme.typography.titleMedium)
                        Text("10s", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = onForwardTenSeconds,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(">", style = MaterialTheme.typography.titleMedium)
                        Text("10s", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = onForwardOneMinute,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(">>", style = MaterialTheme.typography.titleMedium)
                        Text("1m", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PlayerScreenPreview() {
    ListenNestTheme {
        PlayerScreenContent(
            uiState = PlayerUiState(
                bookTitle = "Dune",
                folderUri = "content://books/dune",
                coverArtUri = null,
                tracks = listOf(
                    "01 - Dune",
                    "02 - Muad'Dib",
                    "03 - A Lesson",
                ),
                currentTrackIndex = 1,
                positionMs = 8 * 60 * 1000L + 42 * 1000L,
                durationMs = 19 * 60 * 1000L,
                isPlaying = true,
            ),
            onTogglePlayback = {},
            onRewindOneMinute = {},
            onRewindTenSeconds = {},
            onForwardTenSeconds = {},
            onForwardOneMinute = {},
        )
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun fallbackCoverBadge(bookTitle: String): String {
    val words = bookTitle
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    val letters = when {
        words.isEmpty() -> "LN"
        words.size >= 2 -> words.take(2).mapNotNull { it.firstOrNull()?.takeIf(Char::isLetterOrDigit) }.joinToString("")
        else -> words.first().filter { it.isLetterOrDigit() }.take(2)
    }

    return letters.ifBlank { "LN" }.uppercase()
}
