package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElbeitColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianNight,
    primaryContainer = Color(0xFF3A2E10),
    onPrimaryContainer = RoyalGoldLight,
    secondary = WhatsAppGreen,
    onSecondary = ObsidianNight,
    secondaryContainer = Color(0xFF0E3E26),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFF60A5FA),
    onTertiary = ObsidianNight,
    background = ObsidianNight,
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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ElbeitColorScheme,
        typography = Typography,
        content = content
    )
}
