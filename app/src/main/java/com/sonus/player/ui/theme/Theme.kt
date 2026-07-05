package com.sonus.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Sonus Dark Theme — Primary experience (Ink Black + Cyber Lime)
private val SonusDarkColorScheme = darkColorScheme(
    primary = CyberLime,
    onPrimary = InkBlack,
    primaryContainer = CyberLimeDark,
    onPrimaryContainer = InkBlack,

    secondary = CyberLime,
    onSecondary = InkBlack,

    background = InkBlack,
    onBackground = PaperWhite,

    surface = InkBlack,
    onSurface = PaperWhite,

    surfaceVariant = DeepGray,
    onSurfaceVariant = SoftGray,

    outline = MediumGray,
    outlineVariant = GhostWhite,

    inverseSurface = PaperWhite,
    inverseOnSurface = InkBlack
)

// Sonus Light Theme — Alternative (kept minimal)
private val SonusLightColorScheme = lightColorScheme(
    primary = CyberLimeDark,
    onPrimary = InkBlack,

    background = PureWhite,
    onBackground = InkBlack,

    surface = PureWhite,
    onSurface = InkBlack,

    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = SoftGray,

    outline = MediumGray
)

@Composable
fun SonusTheme(
    darkTheme: Boolean = true, // Always dark — Sonus brand identity
    content: @Composable () -> Unit
) {
    // Sonus is always dark. No light mode.
    MaterialTheme(
        colorScheme = SonusDarkColorScheme,
        typography = Typography,
        shapes = SonusShapes,
        content = content
    )
}
