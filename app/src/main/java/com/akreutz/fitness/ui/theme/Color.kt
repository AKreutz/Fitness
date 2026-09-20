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
