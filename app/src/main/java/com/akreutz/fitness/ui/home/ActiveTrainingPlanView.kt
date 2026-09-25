package com.akreutz.fitness.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.model.lastPerformanceAtCurrentWeight
import com.akreutz.fitness.ui.theme.PlateGreen10
import com.akreutz.fitness.ui.theme.PlateRed
import com.akreutz.fitness.ui.theme.PlateYellow15
import java.util.Locale

/**
 * Shows the user's active Training Plan: each workout as a heading followed by its exercises,
 * two per row in cards, with dividers separating one workout from the next. [workouts] is shown
 * in the order given, so callers that want the workout up next shown first should reorder it
 * before passing it in.
 */
@Composable
fun ActiveTrainingPlanView(
    workouts: List<WorkoutWithExercises>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(workouts) { index, workout ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            }
            WorkoutSection(workout = workout)
        }
    }
}

@Composable
private fun WorkoutSection(workout: WorkoutWithExercises) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = workout.workout.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Most recent results",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        workout.exercises.chunked(2).forEach { rowExercises ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowExercises.forEach { exercise ->
                    ExerciseCard(exercise = exercise, modifier = Modifier.weight(1f))
                }
                if (rowExercises.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Card for an exercise: the exercise name is shown above the image area, and that area displays
 * the current weight as a large centered "x.x KG" figure, with a bar below it indicating the
 * perceived effort from the last time the exercise was performed.
 */
@Composable
private fun ExerciseCard(exercise: ExerciseWithPerformanceHistory, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f KG", exercise.exercise.weightKg),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val effortLevel = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
                    PerceivedEffortBar(
                        effortLevel = effortLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** Display label for a [PerceivedEffort] level. */
private val PerceivedEffort.label: String
    get() = when (this) {
        PerceivedEffort.EASY -> "Easy"
        PerceivedEffort.MEDIUM -> "Medium"
        PerceivedEffort.HARD -> "Hard"
    }

/**
 * Displays the perceived effort of the last time an exercise was performed as a horizontal bar
 * with three equal segments: [PerceivedEffort.EASY] colors in the first segment, MEDIUM the first
 * two, and HARD all three. Filled segments use the same color as the corresponding weight plate:
 * green for easy, yellow for medium, red for hard. The effort's label is shown underneath,
 * aligned under the right-most filled segment. When [effortLevel] is null (no performance
 * recorded yet), no segment is filled and a "Nothing on record yet" label spans the full bar
 * width instead.
 */
@Composable
private fun PerceivedEffortBar(effortLevel: PerceivedEffort?, modifier: Modifier = Modifier) {
    val filledSegments = when (effortLevel) {
        PerceivedEffort.EASY -> 1
        PerceivedEffort.MEDIUM -> 2
        PerceivedEffort.HARD -> 3
        null -> 0
    }
    val filledColor = when (effortLevel) {
        PerceivedEffort.EASY -> PlateGreen10
        PerceivedEffort.MEDIUM -> PlateYellow15
        PerceivedEffort.HARD -> PlateRed
        null -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(3) { index ->
                val color = if (index < filledSegments) {
                    filledColor
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(color),
                )
            }
        }
        if (effortLevel == null) {
            Text(
                text = "Nothing on record yet",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) { index ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (index == filledSegments - 1) {
                            Text(
                                text = effortLevel.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = filledColor,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}
