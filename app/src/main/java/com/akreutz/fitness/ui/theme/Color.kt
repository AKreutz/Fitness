package com.akreutz.fitness.ui.theme

import androidx.compose.ui.graphics.Color

/** The app's single dark theme: a desaturated dark blue background with white content on top. */

// Backgrounds
val DarkBlueBackground = Color(0xFF1B2430)
val DarkBlueSurface = Color(0xFF273343)
val DarkBlueSurfaceVariant = Color(0xFF2F3C4E)

// Surface-container tones (cards, top bar, bottom bar, dialogs, sheets) at increasing elevation.
// Set explicitly so every Material 3 default container color stays on the same dark blue instead
// of falling back to the stock (purple-tinted) dark scheme's tones. surfaceContainer is pinned to
// the same value as surface so components that default to either one (e.g. TopAppBar uses
// surface, NavigationBar uses surfaceContainer) still end up visually identical.
val DarkBlueSurfaceContainerLowest = Color(0xFF141B24)
val DarkBlueSurfaceContainerLow = Color(0xFF212C3A)
val DarkBlueSurfaceContainer = DarkBlueSurface
val DarkBlueSurfaceContainerHigh = Color(0xFF2F3C4E)
val DarkBlueSurfaceContainerHighest = Color(0xFF384659)

// Content on the backgrounds above
val OnDarkBlue = Color(0xFFE6EAF0)
val OnDarkBlueMuted = Color(0xFFC7CCD4)

// Accent, used for primary actions/highlights (e.g. the FAB, selected nav item)
val AccentBlue = Color(0xFF7AA2F7)
val OnAccentBlue = Color(0xFF102040)

// Primary container: a muted tint of the accent, for filled-but-quiet elements (e.g. calendar days
// with a workout), so the full AccentBlue stays reserved for the one thing that should stand out.
val PrimaryContainerBlue = Color(0xFF2F4A7A)
val OnPrimaryContainerBlue = Color(0xFFD6E3FF)

// Secondary (soft slate) and tertiary (teal) accents, distinct from the primary blue.
val SecondarySlate = Color(0xFFA8B8D0)
val OnSecondarySlate = Color(0xFF14202F)
val TertiaryTeal = Color(0xFF7CC4BC)
val OnTertiaryTeal = Color(0xFF0E2B28)

// Secondary/tertiary containers, kept as dark-blue-family tones instead of Material's default
// purple, so every color role in the scheme stays visually consistent with the app's theme.
val SlateBlueContainer = Color(0xFF3A4A5F)
val OnSlateBlueContainer = Color(0xFFD7E1EE)
val MutedTealContainer = Color(0xFF344B4A)
val OnMutedTealContainer = Color(0xFFCFE8E5)

// Error, kept desaturated to match the app's low-saturation dark palette
val ErrorRed = Color(0xFFD06A6A)
val OnErrorRed = Color(0xFF3A0A12)
val ErrorRedContainer = Color(0xFF5C1A26)
val OnErrorRedContainer = Color(0xFFF2C6CE)

// Outline tones for borders/dividers
val OutlineBlue = Color(0xFF8D99A8)
val OutlineVariantBlue = Color(0xFF435368)

// Inverse tones (e.g. Snackbar background) and the scrim
val InverseSurfaceBlue = Color(0xFFD6DEE8)
val InverseOnSurfaceBlue = Color(0xFF1B2430)
val InversePrimaryBlue = Color(0xFF2F5CB8)
val ScrimBlack = Color(0xFF000000)

// Perceived-effort colors (easy/medium/hard), exposed through the theme as
// MaterialTheme.effortColors. Hard shares the error red.
val EffortEasyGreen = Color(0xFF62B083)
val EffortMediumYellow = Color(0xFFD1B24F)
val EffortHardRed = ErrorRed
val OnEffort = Color(0xFF1B2430)

// Heart rate readings and graphs, a soft coral kept apart from the effort red.
val HeartRateCoral = Color(0xFFE8907F)

// Barbell plate colors, one per size in PlateBreakdown.PLATE_SIZES_KG, desaturated to match the
// app's low-saturation palette instead of using saturated competition-plate colors outright.
val PlateBlue20 = Color(0xFF5B7FB8)
val PlateRed = Color(0xFFB85C5C)
val PlateYellow15 = Color(0xFFC7A94A)
val PlateGreen10 = Color(0xFF5C9E77)
val PlateWhite5 = Color(0xFFD8DCE3)
val OnPlateWhite5 = Color(0xFF1B2430)
val PlateBlack2_5 = Color(0xFF3A4352)
val PlateChrome1_25 = Color(0xFFA9B2BF)
val OnPlateChrome1_25 = Color(0xFF1B2430)
