package com.akreutz.fitness.ui.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.HeartRateStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val NORMAL_CONFETTI_COUNT = 36
private const val RECORD_CONFETTI_COUNT = 120
private const val NORMAL_CONFETTI_MILLIS = 1600
private const val RECORD_CONFETTI_MILLIS = 2600
private const val NORMAL_SHOW_MILLIS = 2000L
private const val RECORD_SHOW_MILLIS = 3200L

/** One piece of confetti, flung out from the badge with its own direction, speed and spin. */
private class ConfettiPiece(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
    val round: Boolean,
)

/**
 * The end-of-workout celebration: a badge that pops in and ticks itself off, with a burst of
 * confetti and an expanding ring. When [personalRecords] is above zero it goes bigger, with far
 * more confetti, a second ring, a trophy and a "new personal record" line. Calls [onDone] after a
 * few seconds, or as soon as the user taps.
 */
@Composable
internal fun WorkoutCompleteContent(
    personalRecords: Int,
    heartRate: HeartRateStats?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecord = personalRecords > 0
    val haptics = LocalHapticFeedback.current
    val currentOnDone by rememberUpdatedState(onDone)
    var finished by remember { mutableStateOf(false) }

    val badgeScale = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val textProgress = remember { Animatable(0f) }
    val confettiProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        launch {
            badgeScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        launch {
            delay(250)
            checkProgress.animateTo(1f, tween(400))
        }
        launch {
            delay(350)
            textProgress.animateTo(1f, tween(400))
        }
        launch {
            delay(200)
            confettiProgress.animateTo(
                1f,
                tween(if (isRecord) RECORD_CONFETTI_MILLIS else NORMAL_CONFETTI_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            if (isRecord) {
                delay(500)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        delay(if (isRecord) RECORD_SHOW_MILLIS else NORMAL_SHOW_MILLIS)
        if (!finished) {
            finished = true
            currentOnDone()
        }
    }

    val scheme = MaterialTheme.colorScheme
    val confettiColors = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primaryContainer, scheme.tertiaryContainer)
    val pieces = remember(isRecord) {
        val random = Random(seed = 7)
        List(if (isRecord) RECORD_CONFETTI_COUNT else NORMAL_CONFETTI_COUNT) {
            ConfettiPiece(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.35f + random.nextFloat() * if (isRecord) 1.1f else 0.7f,
                size = 0.012f + random.nextFloat() * 0.012f,
                spin = (random.nextFloat() - 0.5f) * 1080f,
                colorIndex = random.nextInt(confettiColors.size),
                round = random.nextBoolean(),
            )
        }
    }
    val badgeColor = if (isRecord) scheme.tertiary else scheme.primary
    val onBadgeColor = if (isRecord) scheme.onTertiary else scheme.onPrimary
    val durationSeconds = (if (isRecord) RECORD_CONFETTI_MILLIS else NORMAL_CONFETTI_MILLIS) / 1000f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!finished) {
                    finished = true
                    currentOnDone()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * BADGE_CENTER_FRACTION)
            val unit = size.minDimension
            val t = confettiProgress.value * durationSeconds
            val fade = ((1f - confettiProgress.value) / 0.3f).coerceIn(0f, 1f)

            // Rings expanding from the badge; records get a second, later one.
            val ringCount = if (isRecord) 2 else 1
            repeat(ringCount) { ring ->
                val ringProgress = ((confettiProgress.value * 1.6f) - ring * 0.25f).coerceIn(0f, 1f)
                if (ringProgress > 0f && ringProgress < 1f) {
                    drawCircle(
                        color = badgeColor.copy(alpha = (1f - ringProgress) * 0.5f),
                        radius = 60.dp.toPx() + unit * 0.5f * ringProgress,
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * (1f - ringProgress) + 1.dp.toPx()),
                    )
                }
            }

            pieces.forEach { piece ->
                // Flung outwards with drag, then pulled down by gravity.
                val reach = piece.speed * unit * (1f - exp(-3f * t)) / 3f * 1.6f
                val x = center.x + cos(piece.angle) * reach
                val y = center.y + sin(piece.angle) * reach + 0.5f * GRAVITY * unit * t * t
                val pieceSize = piece.size * unit
                val color = confettiColors[piece.colorIndex].copy(alpha = fade)
                if (piece.round) {
                    drawCircle(color = color, radius = pieceSize / 2f, center = Offset(x, y))
                } else {
                    rotate(degrees = piece.spin * confettiProgress.value, pivot = Offset(x, y)) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x - pieceSize / 2f, y - pieceSize),
                            size = Size(pieceSize, pieceSize * 2f),
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                    }
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedCheckmark(progress = checkProgress.value, color = onBadgeColor, checkSize = 56.dp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = textProgress.value
                    translationY = (1f - textProgress.value) * 16.dp.toPx()
                },
            ) {
                Text(
                    text = "Workout complete",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                if (isRecord) {
                    PersonalRecordLine(personalRecords)
                }
                if (heartRate != null) {
                    HeartRateLine(heartRate)
                }
            }
        }
    }
}

/** The "new personal record" line shown under the title when a workout set one or more. */
@Composable
private fun PersonalRecordLine(personalRecords: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = if (personalRecords == 1) "New personal record!" else "$personalRecords new personal records!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/** The session's heart rate as the watch measured it, shown under the title. */
@Composable
private fun HeartRateLine(heartRate: HeartRateStats) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${heartRate.avgBpm} bpm avg · ${heartRate.maxBpm} bpm max",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val BADGE_CENTER_FRACTION = 0.45f
private const val GRAVITY = 0.9f
