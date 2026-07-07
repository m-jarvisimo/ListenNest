package com.k2s.listennest.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k2s.listennest.ui.screens.library.LibraryUiState
import com.k2s.listennest.ui.screens.library.LibraryViewModel
import com.k2s.listennest.ui.theme.ListenNestTheme

private const val PHONE_STATE_PERMISSION = Manifest.permission.READ_PHONE_STATE

@Composable
fun SettingsScreen(
    onScanComplete: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val phoneStatePermissionGranted =
        ContextCompat.checkSelfPermission(context, PHONE_STATE_PERMISSION) == PackageManager.PERMISSION_GRANTED
    var waitingToEnablePhoneCallPause by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

            val folderLabel = DocumentFile.fromTreeUri(context, uri)?.name
                ?.takeIf { it.isNotBlank() }
                ?: "Selected folder"

            libraryViewModel.onFolderSelected(uri.toString(), folderLabel)
        }
    }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (waitingToEnablePhoneCallPause) {
            settingsViewModel.setPhoneCallPauseEnabled(granted)
        }
        waitingToEnablePhoneCallPause = false
    }

    SettingsScreenContent(
        libraryUiState = libraryUiState,
        phoneCallPauseEnabled = settingsUiState.phoneCallPauseEnabled,
        phoneStatePermissionGranted = phoneStatePermissionGranted,
        onChooseFolder = { folderPickerLauncher.launch(null) },
        onScanFolder = { libraryViewModel.scanLibrary(onScanComplete = onScanComplete) },
        onPhoneCallPauseChanged = { enabled ->
            if (!enabled) {
                settingsViewModel.setPhoneCallPauseEnabled(false)
            } else if (phoneStatePermissionGranted) {
                settingsViewModel.setPhoneCallPauseEnabled(true)
            } else {
                waitingToEnablePhoneCallPause = true
                phoneStatePermissionLauncher.launch(PHONE_STATE_PERMISSION)
            }
        },
    )
}

@Composable
internal fun SettingsScreenContent(
    libraryUiState: LibraryUiState,
    phoneCallPauseEnabled: Boolean,
    phoneStatePermissionGranted: Boolean,
    onChooseFolder: () -> Unit,
    onScanFolder: () -> Unit,
    onPhoneCallPauseChanged: (Boolean) -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Choose the library folder, scan it, and control phone-call pause behavior.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "1. Choose your library folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = libraryUiState.selectedFolderLabel ?: "No folder selected",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = libraryUiState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onChooseFolder,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Choose folder")
                        }
                        OutlinedButton(
                            onClick = onScanFolder,
                            enabled = !libraryUiState.isScanning && libraryUiState.hasFolderSelected,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Scan folder")
                        }
                        if (libraryUiState.isScanning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 4.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "Scanning…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Phone controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pause playback when a phone call starts and resume after it ends if playback was already active.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (phoneCallPauseEnabled) "Phone-call pause is on" else "Phone-call pause is off",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = if (phoneStatePermissionGranted) {
                                    "Phone permission granted."
                                } else {
                                    "Grant phone permission to use call-state pause."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = phoneCallPauseEnabled,
                            onCheckedChange = onPhoneCallPauseChanged,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingsScreenPreview() {
    ListenNestTheme {
        SettingsScreenContent(
            libraryUiState = LibraryUiState(
                selectedFolderLabel = "Audiobooks",
                statusMessage = "Folder selected. Tap Scan folder to discover books.",
            ),
            phoneCallPauseEnabled = true,
            phoneStatePermissionGranted = true,
            onChooseFolder = {},
            onScanFolder = {},
            onPhoneCallPauseChanged = {},
        )
    }
}
