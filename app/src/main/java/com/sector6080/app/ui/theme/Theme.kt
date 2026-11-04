package com.sector6080.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1A365D)
val NavyLight = Color(0xFF2D4A80)
val Accent = Color(0xFF4299E1)
val AccentLight = Color(0xFF63B3ED)
val Success = Color(0xFF38A169)
val Warning = Color(0xFFD69E2E)
val Danger = Color(0xFFE53E3E)
val Background = Color(0xFF0F1B2D)
val Fog = Color(0xFF070D17)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentLight,
    background = Background,
    surface = Background,
    surfaceVariant = NavyLight,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Navy,
    surfaceContainer = Navy,
    surfaceContainerHigh = Navy,
    surfaceContainerHighest = Navy,
    secondaryContainer = NavyLight,
    onSecondaryContainer = Color(0xFFF7FAFC),
    onSurface = Color(0xFFF7FAFC),
    onBackground = Color(0xFFF7FAFC),
    error = Danger,
)

@Composable
fun Sector6080Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
