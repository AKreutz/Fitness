package com.akreutz.fitness.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ENTRANCE_MILLIS = 400
private const val ENTRANCE_STAGGER_MILLIS = 70L
private const val ENTRANCE_MAX_STAGGERED = 6

// Screens whose entrance has already played in this process, so it only runs on first load.
private val playedEntrances = mutableSetOf<String>()

/**
 * Fades [screen]'s item number [index] in while sliding it up, each item starting a little after
 * the previous one. Only plays the first time [screen] is shown in the app's lifetime; items
 * composed after the first one finishes (scrolling, later data changes) appear immediately.
 */
fun Modifier.staggeredEntrance(screen: String, index: Int): Modifier = composed {
    val play = remember { screen !in playedEntrances }
    val progress = remember { Animatable(if (play) 0f else 1f) }
    if (play) {
        LaunchedEffect(Unit) {
            delay(index.coerceAtMost(ENTRANCE_MAX_STAGGERED) * ENTRANCE_STAGGER_MILLIS)
            progress.animateTo(1f, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
            playedEntrances += screen
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/**
 * Animates this item to its new position within its parent whenever a sibling is added, removed
 * or reordered, instead of jumping. Set [enabled] to false while the item is positioned by hand
 * (e.g. being dragged).
 */
fun Modifier.animatePlacement(enabled: Boolean = true): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val anim = animatable
                ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    if (enabled) {
                        anim.animateTo(
                            targetOffset,
                            spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                        )
                    } else {
                        anim.snapTo(targetOffset)
                    }
                }
            }
            // Offset back toward the old position and let the animation close the gap to zero.
            if (enabled) anim.value - targetOffset else IntOffset.Zero
        }
}
