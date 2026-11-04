package com.sector6080.app.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Console di comando: acciaio scuro, ambra per ciò che conta, blu e rosso come sulle carte NATO
val Void = Color(0xFF090D0C)
val Steel = Color(0xFF121917)
val SteelHigh = Color(0xFF1B2421)
val Line = Color(0xFF2B3934)
val LineBright = Color(0xFF4C6157)
val Ink = Color(0xFFE9F0EA)
val InkDim = Color(0xFF93A59B)
val Signal = Color(0xFFFFB627)
val OnSignal = Color(0xFF171002)
val Friend = Color(0xFF63C9F2)
val Hostile = Color(0xFFFF4F45)
val Success = Color(0xFF6FE08E)
val Warning = Signal
val Danger = Hostile
val Fog = Color(0xFF040706)

private val Scheme = darkColorScheme(
    primary = Signal,
    onPrimary = OnSignal,
    primaryContainer = Signal,
    onPrimaryContainer = OnSignal,
    secondary = Friend,
    onSecondary = Void,
    secondaryContainer = SteelHigh,
    onSecondaryContainer = Ink,
    background = Void,
    onBackground = Ink,
    surface = Steel,
    onSurface = Ink,
    surfaceVariant = SteelHigh,
    onSurfaceVariant = InkDim,
    surfaceContainerLowest = Void,
    surfaceContainerLow = Steel,
    surfaceContainer = Steel,
    surfaceContainerHigh = SteelHigh,
    surfaceContainerHighest = SteelHigh,
    outline = LineBright,
    outlineVariant = Line,
    error = Danger,
    onError = Void,
)

private fun condensed(weight: FontWeight) = Font(DeviceFontFamilyName("sans-serif-condensed"), weight)

/** Titoli e pulsanti. I caratteri sono quelli di sistema, così non serve scaricare nulla. */
val Condensed = FontFamily(condensed(FontWeight.Normal), condensed(FontWeight.Bold))

/** Dati, coordinate, etichette. */
val Mono = FontFamily.Monospace

private fun title(size: Int, spacing: Double) = TextStyle(
    fontFamily = Condensed,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    letterSpacing = spacing.sp,
)

private val Type = Typography(
    headlineLarge = title(36, 3.0),
    headlineMedium = title(28, 2.0),
    headlineSmall = title(22, 1.5),
    titleLarge = title(20, 1.0),
    titleMedium = title(17, 1.0),
    titleSmall = title(14, 1.0),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = InkDim),
    labelLarge = title(15, 1.2),
    labelMedium = TextStyle(fontFamily = Mono, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp),
)

private val Cuts = Shapes(
    extraSmall = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp),
    small = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp),
    medium = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
    large = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
    extraLarge = CutCornerShape(topEnd = 20.dp, bottomStart = 20.dp),
)

@Composable
fun Sector6080Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = Cuts, content = content)
}
