package com.k2s.listennest.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k2s.listennest.ui.theme.ListenNestTheme

private const val GITHUB_URL = "https://github.com/m-jarvisimo/ListenNest"

@Composable
fun AboutScreen(onBackToSettings: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    @Suppress("DEPRECATION")
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "ListenNest",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "A simple local-first audiobook app for personal library folders.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "GitHub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "View the source code and project updates.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { uriHandler.openUri(GITHUB_URL) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open GitHub")
                    }
                }
            }

            OutlinedButton(
                onClick = onBackToSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back to settings")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AboutScreenPreview() {
    ListenNestTheme {
        AboutScreen(onBackToSettings = {})
    }
}
