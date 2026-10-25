package com.akreutz.fitness.ui.workouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.ui.theme.HeartRateCoral
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A completed session's heart rate in detail: the average, peak and lowest, the full graph with the exercises marked on
 * it, and how each exercise compares. Opened from the heart rate block of a session in the history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateDetailScreen(
    repository: TrainingPlanRepository,
    sessionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions by remember(repository) { repository.observeWorkoutSessions() }.collectAsState(initial = null)
    val session = sessions?.firstOrNull { it.session.id == sessionId }
    val heartRate = remember(session) { session?.sessionHeartRate() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Heart rate")
                        if (session != null) {
                            Text(
                                text = session.subtitle(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (heartRate == null) {
            if (sessions != null) {
                Box(Modifier.padding(innerPadding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No heart rate was recorded for this workout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeartRateTile("Average", heartRate.stats.avgBpm, "bpm", Modifier.weight(1f))
                HeartRateTile(
                    "Peak",
                    heartRate.stats.maxBpm,
                    listOfNotNull("bpm", heartRate.peakExerciseName).joinToString(" · "),
                    Modifier.weight(1f),
                    valueColor = HeartRateCoral,
                )
                HeartRateTile("Lowest", heartRate.minBpm, "bpm", Modifier.weight(1f))
            }
            HeartRateCard {
                Text(text = "Over the session", style = MaterialTheme.typography.titleSmall)
                HeartRateGraph(heartRate, Modifier.fillMaxWidth().height(256.dp), detailed = true)
            }
            if (heartRate.exercises.isNotEmpty()) {
                HeartRateCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "By exercise",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "range · dot is average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    heartRate.exercises.forEach { exercise ->
                        ExerciseRangeRow(exercise, heartRate)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val axisStyle = MaterialTheme.typography.labelSmall
                        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Text("${heartRate.axisLow}", style = axisStyle, color = axisColor)
                        Text("${(heartRate.axisLow + heartRate.axisHigh) / 2}", style = axisStyle, color = axisColor)
                        Text("${heartRate.axisHigh} bpm", style = axisStyle, color = axisColor)
                    }
                }
            }
        }
    }
}

private fun WorkoutSessionWithWorkout.subtitle(): String {
    val startedAt = session.startedAt.atZone(ZoneId.systemDefault())
    return listOf(
        workout.name,
        startedAt.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)),
        "${session.durationSeconds / 60} min",
    ).joinToString(" · ")
}

@Composable
private fun HeartRateCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun HeartRateTile(
    label: String,
    bpm: Int,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color =MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = bpm.toString(), style = MaterialTheme.typography.headlineMedium, color = valueColor)
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** One exercise's lowest-to-highest heart rate as a bar on the session's axis, with a dot at its average. */
@Composable
private fun ExerciseRangeRow(exercise: ExerciseHeartRate, heartRate: SessionHeartRate) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerLow
    val rangeColor = HeartRateCoral.copy(alpha = 0.35f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = "${exercise.minBpm}–${exercise.stats.maxBpm} · avg ${exercise.stats.avgBpm}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val span = (heartRate.axisHigh - heartRate.axisLow).toFloat()
            fun xAt(bpm: Int) = (bpm - heartRate.axisLow) / span * size.width
            val barHeight = 8.dp.toPx()
            val top = (size.height - barHeight) / 2
            val corner = CornerRadius(barHeight / 2)
            drawRoundRect(trackColor, Offset(0f, top), Size(size.width, barHeight), corner)
            drawRoundRect(
                color = rangeColor,
                topLeft = Offset(xAt(exercise.minBpm), top),
                size = Size(xAt(exercise.stats.maxBpm) - xAt(exercise.minBpm), barHeight),
                cornerRadius = corner,
            )
            drawCircle(HeartRateCoral, radius = 5.dp.toPx(), center = Offset(xAt(exercise.stats.avgBpm), size.height / 2))
        }
    }
}
