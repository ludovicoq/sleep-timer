package com.lquerin.sleeptimer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Midnight palette: deep navy sky, soft lavender and moonlight blue.
 * Text is never pure white, so the screen stays gentle on the eyes at night.
 */
object Midnight {
    val SkyTop = Color(0xFF181C42)
    val SkyMid = Color(0xFF0E1130)
    val SkyBottom = Color(0xFF06071A)

    val Lavender = Color(0xFFB4ACFF)
    val MoonBlue = Color(0xFF7FA6F0)
    val MoonGlow = Color(0xFFF4E3B5)
    val StarWhite = Color(0xFFE8E6FF)
}

private val MidnightColors = darkColorScheme(
    primary = Color(0xFFB4ACFF),
    onPrimary = Color(0xFF1E1A4D),
    primaryContainer = Color(0xFF332E6E),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFF8FB3F2),
    onSecondary = Color(0xFF0F2350),
    secondaryContainer = Color(0xFF283263),
    onSecondaryContainer = Color(0xFFD7E2FF),
    tertiary = Color(0xFFF4E3B5),
    onTertiary = Color(0xFF3A2F0B),
    background = Color(0xFF06071A),
    onBackground = Color(0xFFD9DAF0),
    surface = Color(0xFF14173A),
    onSurface = Color(0xFFD9DAF0),
    surfaceVariant = Color(0xFF242857),
    onSurfaceVariant = Color(0xFF9497C2),
    outline = Color(0xFF454A85),
    outlineVariant = Color(0xFF2C3063),
)

/** Always dark, regardless of the system setting. */
@Composable
fun SleepTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MidnightColors, content = content)
}
