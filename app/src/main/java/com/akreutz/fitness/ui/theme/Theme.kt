package com.akreutz.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app's one and only theme: a deliberately-styled dark blue design, always applied. Every
 * color role is set explicitly (rather than left to [darkColorScheme]'s defaults) so nothing
 * falls back to Material's stock purple-tinted dark scheme.
 */
private val FitnessColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccentBlue,
    primaryContainer = AccentBlue,
    onPrimaryContainer = OnAccentBlue,
    inversePrimary = InversePrimaryBlue,
    secondary = AccentBlue,
    onSecondary = OnAccentBlue,
    secondaryContainer = SlateBlueContainer,
    onSecondaryContainer = OnSlateBlueContainer,
    tertiary = AccentBlue,
    onTertiary = OnAccentBlue,
    tertiaryContainer = MutedTealContainer,
    onTertiaryContainer = OnMutedTealContainer,
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
    surfaceTint = AccentBlue,
    inverseSurface = InverseSurfaceBlue,
    inverseOnSurface = InverseOnSurfaceBlue,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    outline = OutlineBlue,
    outlineVariant = OutlineVariantBlue,
    scrim = ScrimBlack,
)

@Composable
fun FitnessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitnessColorScheme,
        typography = Typography,
        content = content,
    )
}
