package com.k2s.listennest.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryScreen
import com.k2s.listennest.ui.screens.player.PlayerScreen
import com.k2s.listennest.ui.screens.settings.ScanReviewScreen
import com.k2s.listennest.ui.screens.settings.SettingsScreen

private enum class AppRoute {
    Library,
    Player,
    Settings,
    Review,
}

@Composable
private fun TopNavPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pillShape = RoundedCornerShape(999.dp)
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "top_nav_container_color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "top_nav_content_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "top_nav_border_color",
    )
    val tonalElevation by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        label = "top_nav_elevation",
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 96.dp)
            .heightIn(min = 40.dp),
        shape = pillShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tonalElevation,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun NavGraph() {
    var route by rememberSaveable { mutableStateOf(AppRoute.Player) }
    var selectedBook by remember { mutableStateOf<LibraryBookItem?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopNavPill(
                    label = "Library",
                    selected = route == AppRoute.Library,
                    onClick = { route = AppRoute.Library },
                )
                TopNavPill(
                    label = "Player",
                    selected = route == AppRoute.Player,
                    onClick = { route = AppRoute.Player },
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("☰")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            menuExpanded = false
                            route = AppRoute.Settings
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (route) {
                AppRoute.Library -> LibraryScreen(
                    onBookSelected = {
                        selectedBook = it
                        route = AppRoute.Player
                    },
                )
                AppRoute.Player -> PlayerScreen(book = selectedBook)
                AppRoute.Settings -> SettingsScreen(
                    onScanComplete = { route = AppRoute.Review },
                )
                AppRoute.Review -> ScanReviewScreen(
                    onBackToSettings = { route = AppRoute.Settings },
                    onSaved = { route = AppRoute.Settings },
                )
            }
        }
    }
}
