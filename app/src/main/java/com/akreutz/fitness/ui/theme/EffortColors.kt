package com.akreutz.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.akreutz.fitness.data.model.PerceivedEffort

/**
 * The colors for [PerceivedEffort] levels (green/yellow/red for easy/medium/hard), with [onEffort]
 * for content drawn on top of any of them. Read via [MaterialTheme.effortColors] so every screen
 * shows effort in the same colors, independent of the barbell-plate palette.
 */
@Immutable
data class EffortColors(
    val easy: Color,
    val medium: Color,
    val hard: Color,
    val onEffort: Color,
) {
    fun forEffort(effort: PerceivedEffort): Color = when (effort) {
        PerceivedEffort.EASY -> easy
        PerceivedEffort.MEDIUM -> medium
        PerceivedEffort.HARD -> hard
    }
}

internal val FitnessEffortColors = EffortColors(
    easy = EffortEasyGreen,
    medium = EffortMediumYellow,
    hard = EffortHardRed,
    onEffort = OnEffort,
)

val LocalEffortColors = staticCompositionLocalOf { FitnessEffortColors }

val MaterialTheme.effortColors: EffortColors
    @Composable
    @ReadOnlyComposable
    get() = LocalEffortColors.current
