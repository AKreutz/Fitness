package com.akreutz.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** The app's one and only theme: a deliberately-styled dark blue design, always applied. */
private val FitnessColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccentBlue,
    secondary = AccentBlue,
    onSecondary = OnAccentBlue,
    tertiary = AccentBlue,
    onTertiary = OnAccentBlue,
    background = DarkBlueBackground,
    onBackground = OnDarkBlue,
    surface = DarkBlueSurface,
    onSurface = OnDarkBlue,
    surfaceVariant = DarkBlueSurfaceVariant,
    onSurfaceVariant = OnDarkBlueMuted,
    surfaceContainerLowest = DarkBlueSurfaceContainerLowest,
    surfaceContainerLow = DarkBlueSurfaceContainerLow,
    surfaceContainer = DarkBlueSurfaceContainer,
    surfaceContainerHigh = DarkBlueSurfaceContainerHigh,
    surfaceContainerHighest = DarkBlueSurfaceContainerHighest,
)

@Composable
fun FitnessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitnessColorScheme,
        typography = Typography,
        content = content,
    )
}
