package com.k2s.listennest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ListenNestDarkColorScheme = darkColorScheme(
    primary = ListenNestPurple,
    secondary = ListenNestPurpleDark,
    background = ListenNestBackground,
    surface = ListenNestSurface,
)

@Composable
fun ListenNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ListenNestDarkColorScheme,
        content = content,
    )
}
