package com.akreutz.fitness.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

private const val DIALOG_ENTER_MILLIS = 220
private const val DIALOG_START_SCALE = 0.85f

/**
 * Scales a dialog up from [DIALOG_START_SCALE] while fading it in when it first appears.
 * Dialog windows only offer the platform's own enter animation, so this runs on the content.
 */
fun Modifier.dialogEnter(): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DIALOG_ENTER_MILLIS, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        val scale = DIALOG_START_SCALE + (1f - DIALOG_START_SCALE) * progress.value
        scaleX = scale
        scaleY = scale
        alpha = progress.value
    }
}
