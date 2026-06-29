package com.k2s.listennest.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.k2s.listennest.ui.screens.library.LibraryBookItem
import com.k2s.listennest.ui.screens.library.LibraryScreen
import com.k2s.listennest.ui.screens.player.PlayerScreen
import com.k2s.listennest.ui.screens.settings.SettingsScreen

private enum class AppRoute {
    Library,
    Player,
    Settings,
}

@Composable
fun NavGraph() {
    var route by rememberSaveable { mutableStateOf(AppRoute.Library) }
    var selectedBook by remember { mutableStateOf<LibraryBookItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { route = AppRoute.Library }) { Text("Library") }
            Button(onClick = { route = AppRoute.Player }) { Text("Player") }
            Button(onClick = { route = AppRoute.Settings }) { Text("Settings") }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (route) {
            AppRoute.Library -> LibraryScreen(
                onBookSelected = {
                    selectedBook = it
                    route = AppRoute.Player
                },
            )
            AppRoute.Player -> PlayerScreen(book = selectedBook)
            AppRoute.Settings -> SettingsScreen()
        }
    }
}
