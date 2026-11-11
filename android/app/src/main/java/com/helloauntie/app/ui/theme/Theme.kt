package com.helloauntie.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F8B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBF2),
    onPrimaryContainer = Color(0xFF0C2A36),
    secondary = Color(0xFF2E7D5A),
    onSecondary = Color.White,
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF16233A),
    surface = Color.White,
    onSurface = Color(0xFF16233A),
    surfaceVariant = Color(0xFFEAF3F7),
    onSurfaceVariant = Color(0xFF3D4C5C),
    outline = Color(0xFFB9C7D3),
)

@Composable
fun HelloAuntieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content,
    )
}
