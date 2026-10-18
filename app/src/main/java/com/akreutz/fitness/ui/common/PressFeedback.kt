package com.akreutz.fitness.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

private const val DEFAULT_PRESSED_SCALE = 0.96f

/**
 * Shrinks this element slightly while it is pressed and springs it back on release. It only
 * observes touches (on the initial pass, without consuming them), so it can sit in front of a
 * button or clickable card without changing how it handles clicks; dragging past the touch slop,
 * e.g. to scroll a list, counts as no longer pressing.
 */
fun Modifier.pressScale(pressedScale: Float = DEFAULT_PRESSED_SCALE): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val moved = event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }
                    if (moved) pressed = false
                } while (event.changes.any { it.pressed })
                pressed = false
            }
        }
}
