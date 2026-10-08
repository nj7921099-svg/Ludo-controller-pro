package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PurpleGlowPrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleGlowSecondary,
    onPrimaryContainer = PurpleGlowLight,
    secondary = GreenActiveTurn,
    onSecondary = Color.Black,
    secondaryContainer = GreenActiveTurnGlow,
    onSecondaryContainer = GreenActiveTurnLight,
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = VoidBackground,
    onBackground = TextWhite,
    surface = SurfaceCard,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextMuted,
    outline = PurpleBorderSubtle,
    outlineVariant = PurpleBorderBright
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
