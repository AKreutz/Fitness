package com.akreutz.fitness.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

private const val COUNT_UP_MILLIS = 800

/**
 * A value that counts up from zero to [target] the first time it is shown, and animates from
 * wherever it currently is whenever [target] changes after that.
 */
@Composable
fun rememberCountUp(target: Float): State<Float> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target, tween(COUNT_UP_MILLIS, easing = FastOutSlowInEasing))
    }
    return animatable.asState()
}
