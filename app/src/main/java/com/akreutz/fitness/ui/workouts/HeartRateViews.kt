package com.akreutz.fitness.ui.workouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.HeartRateSamples
import com.akreutz.fitness.data.model.HeartRateStats
import com.akreutz.fitness.data.model.HeartRateWindow
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.model.performanceOn
import com.akreutz.fitness.ui.theme.HeartRateCoral
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** One exercise's heart rate in a session: when it was done, and the average, highest and lowest reading then. */
internal class ExerciseHeartRate(
    val exerciseId: String,
    val name: String,
    val window: HeartRateWindow,
    val stats: HeartRateStats,
    val minBpm: Int,
)

/**
 * A session's heart rate readings with what is shown from them. [samples] has one reading per second since the session
 * began, 0 where the watch had none. [axisLow] and [axisHigh] bound the graphs, rounded out to tens of bpm.
 */
internal class SessionHeartRate(
    val samples: ByteArray,
    val stats: HeartRateStats,
    val minBpm: Int,
    val peakSecond: Int,
    val peakExerciseName: String?,
    val exercises: List<ExerciseHeartRate>,
) {
    val axisLow: Int = (floor((minBpm - 5) / 10.0) * 10).toInt()
    val axisHigh: Int = (ceil((stats.maxBpm + 5) / 10.0) * 10).toInt()
    val durationSeconds: Int get() = max(samples.size - 1, 1)
}

/** The session's heart rate, or `null` if the watch recorded none. Exercises come in the order they were done. */
internal fun WorkoutSessionWithWorkout.sessionHeartRate(): SessionHeartRate? {
    val samples = session.heartRateSamples ?: return null
    val stats = HeartRateSamples.stats(samples) ?: return null
    val minBpm = HeartRateSamples.minBpm(samples) ?: return null
    val exerciseRates = exercises.mapNotNull { exercise ->
        val window = exercise.performanceOn(session.completedAt)?.heartRateWindow ?: return@mapNotNull null
        val exerciseStats = HeartRateSamples.stats(samples, window) ?: return@mapNotNull null
        ExerciseHeartRate(
            exerciseId = exercise.exercise.id,
            name = exercise.exercise.name,
            window = window,
            stats = exerciseStats,
            minBpm = HeartRateSamples.minBpm(samples, window) ?: exerciseStats.avgBpm,
        )
    }.sortedBy { it.window.startSeconds }
    val peakSecond = samples.indexOfFirst { (it.toInt() and 0xFF) == stats.maxBpm }
    return SessionHeartRate(
        samples = samples,
        stats = stats,
        minBpm = minBpm,
        peakSecond = peakSecond,
        peakExerciseName = exerciseRates.firstOrNull { peakSecond in it.window.startSeconds..it.window.endSeconds }?.name,
        exercises = exerciseRates,
    )
}

/** The heart rate block of an expanded session card: average, highest and lowest, and a small graph. Tap to open the detail. */
@Composable
internal fun HeartRateSummary(
    heartRate: SessionHeartRate,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = HeartRateCoral,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Heart rate",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeartRateFigure("Avg", heartRate.stats.avgBpm, Modifier.weight(1f))
                HeartRateFigure("Max", heartRate.stats.maxBpm, Modifier.weight(1f))
                HeartRateFigure("Min", heartRate.minBpm, Modifier.weight(1f))
            }
            HeartRateGraph(heartRate, Modifier.fillMaxWidth().height(96.dp))
        }
    }
}

@Composable
private fun HeartRateFigure(label: String, bpm: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = bpm.toString(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = " bpm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/**
 * The session's heart rate over time as a filled line, with alternate exercises shaded and the average as a dashed line.
 * [detailed] adds the bpm axis with gridlines, the exercise names, the minutes below, and the peak marked; it also
 * needs a taller [modifier] to be readable.
 */
@Composable
internal fun HeartRateGraph(
    heartRate: SessionHeartRate,
    modifier: Modifier = Modifier,
    detailed: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val peakLabelStyle = MaterialTheme.typography.labelSmall.copy(color = HeartRateCoral)
    val bandColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val averageColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerRing = MaterialTheme.colorScheme.surfaceContainerHigh
    val description = "Heart rate over the session, from ${heartRate.minBpm} to ${heartRate.stats.maxBpm} beats per " +
        "minute, averaging ${heartRate.stats.avgBpm}"

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val axisWidth = if (detailed) 32.dp.toPx() else 0f
        val labelHeight = if (detailed) 36.dp.toPx() else 0f
        val plotLeft = axisWidth
        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - labelHeight
        val low = heartRate.axisLow.toFloat()
        val high = heartRate.axisHigh.toFloat()
        val seconds = heartRate.durationSeconds.toFloat()

        fun xAt(second: Float) = plotLeft + second.coerceIn(0f, seconds) / seconds * plotWidth
        fun yAt(bpm: Float) = plotHeight * (high - bpm) / (high - low)

        heartRate.exercises.forEachIndexed { index, exercise ->
            if (index % 2 != 0) return@forEachIndexed
            val left = xAt(exercise.window.startSeconds.toFloat())
            val right = xAt(exercise.window.endSeconds.toFloat())
            drawRect(bandColor, Offset(left, 0f), Size(right - left, plotHeight))
        }

        if (detailed) {
            var gridBpm = (ceil(low / 20f) * 20f).toInt()
            while (gridBpm < high) {
                val y = yAt(gridBpm.toFloat())
                drawLine(gridColor, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(gridBpm.toString(), labelStyle)
                drawText(label, topLeft = Offset(axisWidth - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
                gridBpm += 20
            }
        }

        val averageY = yAt(heartRate.stats.avgBpm.toFloat())
        drawLine(
            color = averageColor,
            start = Offset(plotLeft, averageY),
            end = Offset(size.width, averageY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        if (detailed) {
            val label = textMeasurer.measure("avg ${heartRate.stats.avgBpm}", labelStyle)
            drawText(label, topLeft = Offset(size.width - label.size.width, averageY - label.size.height - 2.dp.toPx()))
        }

        // The readings, averaged into columns about 2 dp wide so the line is smooth; empty seconds are skipped.
        val columns = (plotWidth / 2.dp.toPx()).toInt().coerceIn(1, heartRate.samples.size)
        val points = ArrayList<Offset>(columns)
        var peakPoint: Offset? = null
        for (column in 0 until columns) {
            val from = column * heartRate.samples.size / columns
            val to = (column + 1) * heartRate.samples.size / columns
            var sum = 0
            var count = 0
            for (index in from until to) {
                val bpm = heartRate.samples[index].toInt() and 0xFF
                if (bpm == 0) continue
                sum += bpm
                count++
            }
            if (count == 0) continue
            val point = Offset(xAt((from + to) / 2f), yAt(sum.toFloat() / count))
            points += point
            if (heartRate.peakSecond in from until to) peakPoint = point
        }
        if (points.size >= 2) {
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (point in points.drop(1)) lineTo(point.x, point.y)
            }
            val area = Path().apply {
                addPath(line)
                lineTo(points.last().x, plotHeight)
                lineTo(points.first().x, plotHeight)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(HeartRateCoral.copy(alpha = 0.45f), Color.Transparent),
                    startY = 0f,
                    endY = plotHeight,
                ),
            )
            drawPath(
                path = line,
                color = HeartRateCoral,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        if (!detailed) return@Canvas

        peakPoint?.let { point ->
            drawCircle(markerRing, radius = 7.dp.toPx(), center = point)
            drawCircle(HeartRateCoral, radius = 5.dp.toPx(), center = point)
            val label = textMeasurer.measure(heartRate.stats.maxBpm.toString(), peakLabelStyle)
            drawText(
                label,
                topLeft = Offset(
                    (point.x - label.size.width / 2f).coerceIn(plotLeft, size.width - label.size.width),
                    point.y - label.size.height - 8.dp.toPx(),
                ),
            )
        }

        heartRate.exercises.forEach { exercise ->
            val left = xAt(exercise.window.startSeconds.toFloat())
            val right = xAt(exercise.window.endSeconds.toFloat())
            val label = textMeasurer.measure(
                text = exercise.name,
                style = labelStyle,
                overflow = TextOverflow.Clip,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = max((right - left).toInt() - 4.dp.roundToPx(), 0)),
            )
            drawText(label, topLeft = Offset(left + 2.dp.toPx(), plotHeight + 4.dp.toPx()))
        }

        val totalMinutes = (seconds / 60).toInt()
        val stepMinutes = when {
            totalMinutes >= 40 -> 15
            totalMinutes >= 20 -> 10
            else -> 5
        }
        var minute = 0
        while (minute <= totalMinutes) {
            val text = if (minute + stepMinutes > totalMinutes) "$minute min" else minute.toString()
            val label = textMeasurer.measure(text, labelStyle)
            val x = (xAt(minute * 60f) - label.size.width / 2f).coerceIn(plotLeft, size.width - label.size.width)
            drawText(label, topLeft = Offset(x, plotHeight + 20.dp.toPx()))
            minute += stepMinutes
        }
    }
}
