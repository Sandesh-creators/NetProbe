package com.netprobe.diagnostics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun NetProbeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NetProbeDarkColorScheme,
        typography = NetProbeTypography,
        content = content
    )
}
