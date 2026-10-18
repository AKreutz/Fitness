package com.akreutz.fitness.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_MILLIS = 1300
private const val HIGHLIGHT_STRENGTH = 0.1f

/** Paints this element as a placeholder: a muted fill with a highlight sweeping across it. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = lerp(base, MaterialTheme.colorScheme.onSurface, HIGHLIGHT_STRENGTH)
    val phase by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerPhase",
    )
    drawWithCache {
        onDrawBehind {
            // The highlight band travels from one band-width left of the element to one right of it.
            val start = (phase * 2f - 1f) * size.width
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(start, 0f),
                    end = Offset(start + size.width, size.height * 0.3f),
                ),
            )
        }
    }
}

/** A shimmering rounded block standing in for a card or row that hasn't loaded yet. */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
) {
    Box(modifier = modifier.clip(shape).shimmer())
}

/** A screen-sized stack of [ShimmerBlock]s, one per entry of [heights], shown while data loads. */
@Composable
fun ShimmerPlaceholderList(
    heights: List<Dp>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        heights.forEach { height -> ShimmerBlock(Modifier.fillMaxWidth().height(height)) }
    }
}
