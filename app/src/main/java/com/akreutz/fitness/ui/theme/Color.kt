package com.akreutz.fitness.ui.theme

import androidx.compose.ui.graphics.Color

/** The app's single dark theme: a desaturated dark blue background with white content on top. */

// Backgrounds
val DarkBlueBackground = Color(0xFF1B2430)
val DarkBlueSurface = Color(0xFF232E3D)
val DarkBlueSurfaceVariant = Color(0xFF2C3A4C)

// Surface-container tones (cards, top bar, bottom bar, dialogs, sheets) at increasing elevation.
// Set explicitly so every Material 3 default container color stays on the same dark blue instead
// of falling back to the stock (purple-tinted) dark scheme's tones. surfaceContainer is pinned to
// the same value as surface so components that default to either one (e.g. TopAppBar uses
// surface, NavigationBar uses surfaceContainer) still end up visually identical.
val DarkBlueSurfaceContainerLowest = Color(0xFF141B24)
val DarkBlueSurfaceContainerLow = Color(0xFF1E2733)
val DarkBlueSurfaceContainer = DarkBlueSurface
val DarkBlueSurfaceContainerHigh = Color(0xFF2C3A4C)
val DarkBlueSurfaceContainerHighest = Color(0xFF37475C)

// Content on the backgrounds above
val OnDarkBlue = Color.White
val OnDarkBlueMuted = Color(0xFFC7CCD4)

// Accent, used for primary actions/highlights (e.g. the FAB, selected nav item)
val AccentBlue = Color(0xFF7AA2F7)
val OnAccentBlue = Color(0xFF102040)

// Secondary/tertiary accents, kept as dark-blue-family tones instead of Material's default
// purple, so every color role in the scheme stays visually consistent with the app's theme.
val SlateBlueContainer = Color(0xFF3A4A5F)
val OnSlateBlueContainer = Color(0xFFD7E1EE)
val MutedTealContainer = Color(0xFF344B4A)
val OnMutedTealContainer = Color(0xFFCFE8E5)

// Error, kept desaturated to match the app's low-saturation dark palette
val ErrorRed = Color(0xFFCF6679)
val OnErrorRed = Color(0xFF3A0A12)
val ErrorRedContainer = Color(0xFF5C1A26)
val OnErrorRedContainer = Color(0xFFF2C6CE)

// Outline tones for borders/dividers
val OutlineBlue = Color(0xFF8D99A8)
val OutlineVariantBlue = Color(0xFF3F4C5C)

// Inverse tones (e.g. Snackbar background) and the scrim
val InverseSurfaceBlue = Color(0xFFD6DEE8)
val InverseOnSurfaceBlue = Color(0xFF1B2430)
val InversePrimaryBlue = Color(0xFF2F5CB8)
val ScrimBlack = Color(0xFF000000)
