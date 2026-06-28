package com.k2s.listennest

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.k2s.listennest.ui.navigation.NavGraph
import com.k2s.listennest.ui.theme.ListenNestTheme

@Composable
fun ListenNestApp() {
    ListenNestTheme {
        Surface {
            NavGraph()
        }
    }
}
