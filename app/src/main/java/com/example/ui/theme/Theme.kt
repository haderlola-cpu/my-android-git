package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = CreamContainer,
    onPrimaryContainer = EmeraldDeep,
    secondary = RoyalGold,
    onSecondary = EmeraldDeep,
    secondaryContainer = RoyalGoldPale,
    onSecondaryContainer = EmeraldDeep,
    background = IvoryBgLight,
    onBackground = EmeraldGreen,
    surface = CreamContainer,
    onSurface = EmeraldGreen,
    surfaceVariant = SandCard,
    onSurfaceVariant = EmeraldMuted,
    outline = MidnightOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldMedium,
    onPrimaryContainer = RoyalGoldPale,
    secondary = RoyalGold,
    onSecondary = EmeraldDeep,
    secondaryContainer = Color(0xFF3A2E10),
    onSecondaryContainer = RoyalGoldLight,
    background = EmeraldDeep,
    onBackground = IvoryText,
    surface = MidnightSurface,
    onSurface = IvoryText,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = MutedSilver,
    outline = MidnightOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
