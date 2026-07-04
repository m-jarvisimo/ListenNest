package com.k2s.listennest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import com.k2s.listennest.ui.navigation.NavGraph
import com.k2s.listennest.ui.theme.ListenNestTheme

@Composable
fun ListenNestApp() {
    val context = LocalContext.current
    val requestNotificationPermission = rememberLauncherForActivityResult(RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ListenNestTheme {
        Surface {
            NavGraph()
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun ListenNestAppPreview() {
    ListenNestApp()
}
