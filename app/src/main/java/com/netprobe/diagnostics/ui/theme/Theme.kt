package com.netprobe.diagnostics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NetProbeDarkColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = SurfaceDark,
    primaryContainer = TerminalGreen.copy(alpha = 0.15f),
    onPrimaryContainer = TerminalGreen,

    secondary = TerminalCyan,
    onSecondary = SurfaceDark,
    secondaryContainer = TerminalCyan.copy(alpha = 0.12f),
    onSecondaryContainer = TerminalCyan,

    tertiary = TerminalAmber,
    onTertiary = SurfaceDark,
    tertiaryContainer = TerminalAmber.copy(alpha = 0.12f),
    onTertiaryContainer = TerminalAmber,

    error = TerminalRed,
    onError = SurfaceDark,
    errorContainer = TerminalRed.copy(alpha = 0.15f),
    onErrorContainer = TerminalRed,

    background = SurfaceDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondary,

    surfaceTint = TerminalGreen,
    outline = SurfaceOverlay,
    outlineVariant = TextDisabled
)

private val LightSurface = Color(0xFFF8F8F8)
private val LightSurfaceCard = Color(0xFFFFFFFF)
private val LightSurfaceElevated = Color(0xFFFAFAFA)
private val LightSurfaceOverlay = Color(0xFFDCDCDC)

private val LightTextPrimary = Color(0xFF1A1A1A)
private val LightTextSecondary = Color(0xFF5A5A5A)
private val LightTextDisabled = Color(0xFFAAAAAA)

private val NetProbeLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A28),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2F5B0),
    onPrimaryContainer = Color(0xFF002206),

    secondary = Color(0xFF00697A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0E8F5),
    onSecondaryContainer = Color(0xFF001F26),

    tertiary = Color(0xFF895A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA1),
    onTertiaryContainer = Color(0xFF2C1800),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = LightSurface,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceCard,
    onSurfaceVariant = LightTextSecondary,

    surfaceTint = Color(0xFF007A28),
    outline = LightSurfaceOverlay,
    outlineVariant = LightTextDisabled
)

@Composable
fun NetProbeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NetProbeDarkColorScheme,
        typography = NetProbeTypography,
        content = content
    )
}
