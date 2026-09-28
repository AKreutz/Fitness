package com.akreutz.fitness.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.ui.theme.AccentBlue
import com.akreutz.fitness.ui.theme.OutlineVariantBlue
import com.akreutz.fitness.ui.theme.PlateGreen10
import com.akreutz.fitness.ui.theme.PlateRed
import com.akreutz.fitness.ui.theme.PlateYellow15
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * A getter rather than a cached value, so it picks up the default locale fresh each time it's
 * used instead of freezing it at class-init time.
 */
private val axisDateFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

/**
 * Shows a separate weight-over-time chart for each exercise in the user's active training plan,
 * grouped under its workout, in plan order. Exercises with fewer than two logged data points
 * (nothing to draw a line between) show a placeholder message instead of an empty chart.
 */
@Composable
fun ProgressScreen(uiState: ProgressUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        is ProgressUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProgressUiState.NoPlan -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active training plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is ProgressUiState.Loaded -> {
            if (uiState.workouts.all { it.exercises.isEmpty() }) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No exercises to show yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return
            }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    SummaryProgressSection(summary = uiState.summary)
                }
                items(uiState.workouts, key = { it.workoutId }) { workout ->
                    WorkoutProgressSection(workout = workout)
                }
            }
        }
    }
}

@Composable
private fun WorkoutProgressSection(workout: WorkoutProgress, modifier: Modifier = Modifier) {
    if (workout.exercises.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = workout.workoutName, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        workout.exercises.forEach { exercise ->
            ExerciseChartCard(exercise = exercise)
        }
    }
}

@Composable
private fun ExerciseChartCard(exercise: ExerciseProgress, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
            if (exercise.dataPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (exercise.dataPoints.isEmpty()) {
                            "No stats recorded"
                        } else {
                            "Not enough data yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                WeightLineChart(
                    dataPoints = exercise.dataPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            }
        }
    }
}

/**
 * A summary card, shown above every workout's exercise charts, plotting the training plan's
 * overall progress: for each session, the sum of every exercise's percentage weight increase
 * compared to its own starting weight (see [SummaryDataPoint]), colored by how hard that session
 * felt on average. Shows a placeholder message instead of an empty chart if there's fewer than
 * two sessions to plot.
 */
@Composable
private fun SummaryProgressSection(summary: List<SummaryDataPoint>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Overall progress", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            elevation = CardDefaults.elevatedCardElevation(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Summed % weight increase per session",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (summary.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Not enough data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    SummaryLineChart(
                        dataPoints = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            }
        }
    }
}

/** Fill color for the area under a chart segment, by the [PerceivedEffort] it ended on. */
private val PerceivedEffort.areaColor: Color
    get() = when (this) {
        PerceivedEffort.EASY -> PlateGreen10
        PerceivedEffort.MEDIUM -> PlateYellow15
        PerceivedEffort.HARD -> PlateRed
    }

/**
 * A single-series line chart of weight (kg) over time: a thin accent-colored line connecting each
 * [WeightDataPoint], with the area under each segment filled in the color of the [PerceivedEffort]
 * recorded at that segment's right-hand (later) point, and each point's own marker dot colored the
 * same way (green/yellow/red for easy/medium/hard, the same colors used for the effort bar
 * elsewhere in the app). Recessive horizontal gridlines mark the min/max weight (direct-labeled on
 * the left), and the first/last dates are labeled on the x-axis.
 */
@Composable
private fun WeightLineChart(dataPoints: List<WeightDataPoint>, modifier: Modifier = Modifier) {
    LineChart(
        points = dataPoints.map {
            LineChartPoint(
                completedAt = it.completedAt,
                value = it.weightKg,
                pointColor = it.perceivedEffort.areaColor,
                fillColor = it.perceivedEffort.areaColor,
            )
        },
        formatValueLabel = { it.formatDecimal() },
        modifier = modifier,
    )
}

/**
 * A single-series line chart of a training plan's summed percentage weight increase (see
 * [SummaryDataPoint]) over time: a thin accent-colored line connecting each session, with the
 * area under each segment filled in the color of that session's [SummaryDataPoint.averageEffort]
 * (same treatment as [WeightLineChart], so effort is visible without changing where a session
 * sits on the line). Recessive horizontal gridlines mark the min/max value (direct-labeled on the
 * left), and the first/last dates are labeled on the x-axis.
 */
@Composable
private fun SummaryLineChart(dataPoints: List<SummaryDataPoint>, modifier: Modifier = Modifier) {
    LineChart(
        points = dataPoints.map {
            LineChartPoint(
                completedAt = it.completedAt,
                value = it.summedPercentIncrease,
                pointColor = it.averageEffort.areaColor,
                fillColor = it.averageEffort.areaColor,
            )
        },
        formatValueLabel = { "${it.formatDecimal()}%" },
        modifier = modifier,
    )
}

/** One point on a [LineChart]: [value] plotted at [completedAt], with its own dot/fill colors. */
private data class LineChartPoint(
    val completedAt: Instant,
    val value: Double,
    val pointColor: Color,
    val fillColor: Color,
)

/**
 * The line-chart primitive shared by [WeightLineChart] and [SummaryLineChart]: an accent-colored
 * line connecting each [LineChartPoint] in order, the area under each segment filled (at reduced
 * alpha) in that segment's later point's [LineChartPoint.fillColor], and a marker dot per point in
 * its own [LineChartPoint.pointColor]. Recessive horizontal gridlines mark the min/max value
 * (direct-labeled on the left via [formatValueLabel]), and the first/last dates are labeled on the
 * x-axis. Requires at least two [points].
 */
@Composable
private fun LineChart(
    points: List<LineChartPoint>,
    formatValueLabel: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val lineColor = AccentBlue
    val gridColor = OutlineVariantBlue
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 11.sp)

    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val valueRange = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    val firstDateLabel = points.first().completedAt
        .atZone(ZoneId.systemDefault())
        .format(axisDateFormatter)
    val lastDateLabel = points.last().completedAt
        .atZone(ZoneId.systemDefault())
        .format(axisDateFormatter)
    val minValueLabel = formatValueLabel(minValue)
    val maxValueLabel = formatValueLabel(maxValue)

    Canvas(modifier = modifier) {
        val leftInset = 36.dp.toPx()
        val bottomInset = 18.dp.toPx()
        val topInset = 8.dp.toPx()
        val chartWidth = size.width - leftInset
        val chartHeight = size.height - bottomInset - topInset

        // Recessive gridlines at min and max value, direct-labeled to the left.
        listOf(0f to maxValueLabel, chartHeight to minValueLabel).forEach { (y, label) ->
            drawLine(
                color = gridColor,
                start = Offset(leftInset, topInset + y),
                end = Offset(size.width, topInset + y),
                strokeWidth = 1.dp.toPx(),
            )
            val textLayout = textMeasurer.measure(label, style = labelStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(0f, topInset + y - textLayout.size.height / 2f),
            )
        }

        val offsets = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                leftInset
            } else {
                leftInset + chartWidth * index / (points.size - 1)
            }
            val normalized = (point.value - minValue) / valueRange
            val y = topInset + chartHeight * (1f - normalized.toFloat())
            Offset(x, y)
        }
        val baselineY = topInset + chartHeight

        // The area under each segment, filled in the color of the later (right-hand) point,
        // drawn before the line/dots so they sit on top.
        for (i in 0 until offsets.size - 1) {
            val segmentPath = Path().apply {
                moveTo(offsets[i].x, baselineY)
                lineTo(offsets[i].x, offsets[i].y)
                lineTo(offsets[i + 1].x, offsets[i + 1].y)
                lineTo(offsets[i + 1].x, baselineY)
                close()
            }
            drawPath(path = segmentPath, color = points[i + 1].fillColor.copy(alpha = 0.5f))
        }

        // The line itself: thin, rounded ends, with a marker dot per point.
        for (i in 0 until offsets.size - 1) {
            drawLine(
                color = lineColor,
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // Marker dots, colored by their own point rather than the line's accent color, so a
        // point's status is visible even where the preceding segment's fill doesn't reach.
        offsets.forEachIndexed { index, offset ->
            drawCircle(color = points[index].pointColor, radius = 3.dp.toPx(), center = offset)
        }

        // First/last dates on the x-axis.
        val firstLabelLayout = textMeasurer.measure(firstDateLabel, style = labelStyle)
        drawText(
            textLayoutResult = firstLabelLayout,
            topLeft = Offset(leftInset, size.height - firstLabelLayout.size.height),
        )
        val lastLabelLayout = textMeasurer.measure(lastDateLabel, style = labelStyle)
        drawText(
            textLayoutResult = lastLabelLayout,
            topLeft = Offset(
                size.width - lastLabelLayout.size.width,
                size.height - lastLabelLayout.size.height,
            ),
        )
    }
}

/** Formats a decimal value, trimming a trailing ".0" (e.g. "42.5" or "40"). */
private fun Double.formatDecimal(): String =
    if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
