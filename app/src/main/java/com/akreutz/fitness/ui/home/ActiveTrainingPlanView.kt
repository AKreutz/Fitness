package com.akreutz.fitness.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.model.lastPerformanceAtCurrentWeight
import com.akreutz.fitness.ui.common.AddExerciseDialog
import com.akreutz.fitness.ui.common.staggeredEntrance
import com.akreutz.fitness.ui.profile.toDraft
import com.akreutz.fitness.ui.progress.WeightDataPoint
import com.akreutz.fitness.ui.progress.WeightLineChart
import com.akreutz.fitness.ui.theme.effortColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    weightStacks: List<WeightStack>,
    onCreateWeightStack: suspend (name: String, levelsKg: List<Double>) -> WeightStack,
    onUpdateExercise: (
        exercise: Exercise,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        weightStackId: String?,
        restSeconds: Int,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept as ids and looked up per recomposition, so the sheet reflects edits as they land.
    var detailsExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val allExercises = workouts.flatMap { it.exercises }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(workouts, key = { _, workout -> workout.workout.id }) { index, workout ->
            Column(modifier = Modifier.animateItem().staggeredEntrance("home", index)) {
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                }
                WorkoutSection(
                    workout = workout,
                    onExerciseClick = { detailsExerciseId = it.exercise.id },
                )
            }
        }
    }

    allExercises.firstOrNull { it.exercise.id == detailsExerciseId }?.let { exercise ->
        ExerciseDetailsSheet(
            exercise = exercise,
            workoutName = workouts.first { it.workout.id == exercise.exercise.workoutId }.workout.name,
            weightStacks = weightStacks,
            onEdit = {
                editingExerciseId = exercise.exercise.id
                detailsExerciseId = null
            },
            onDismiss = { detailsExerciseId = null },
        )
    }

    allExercises.firstOrNull { it.exercise.id == editingExerciseId }?.let { target ->
        AddExerciseDialog(
            initial = target.exercise.toDraft(),
            weightStacks = weightStacks,
            onCreateWeightStack = onCreateWeightStack,
            onDismiss = { editingExerciseId = null },
            onConfirm = { name, type, sets, reps, weightKg, weightIncrementKg, weightStackId, restSeconds ->
                onUpdateExercise(
                    target.exercise,
                    name,
                    type,
                    sets,
                    reps,
                    weightKg,
                    weightIncrementKg,
                    weightStackId,
                    restSeconds,
                )
                editingExerciseId = null
            },
        )
    }
}

/**
 * Bottom sheet with an exercise's details. From the top: the name and workout with an Edit button,
 * the current weight with the same effort bar as the card, the prescription as three tiles, a
 * chart of the weight used over time with its overall change and an effort legend, and the three
 * most recent sessions. While there are fewer than two sessions to chart, a hint replaces the chart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailsSheet(
    exercise: ExerciseWithPerformanceHistory,
    workoutName: String,
    weightStacks: List<WeightStack>,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val details = exercise.exercise
    val history = exercise.performanceHistory.entries.sortedBy { it.key }
    val dateFormat = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val typeLabel = when (details.type) {
        ExerciseType.FREE_WEIGHTS -> "Free weights"
        ExerciseType.CABLE -> "Cable"
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = details.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "$workoutName · $typeLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit exercise")
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", details.weightKg),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = " kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    PerceivedEffortBar(
                        effortLevel = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailTile(
                    label = "Sets × reps",
                    value = "${details.sets} × ${details.reps.joinToString("/")}",
                    modifier = Modifier.weight(1f),
                )
                if (details.type == ExerciseType.CABLE) {
                    DetailTile(
                        label = "Weight stack",
                        value = weightStacks.firstOrNull { it.id == details.weightStackId }?.name
                            ?: "None",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    DetailTile(
                        label = "Increment",
                        value = String.format(Locale.US, "+%.2f kg", details.weightIncrementKg),
                        modifier = Modifier.weight(1f),
                    )
                }
                DetailTile(
                    label = "Rest",
                    value = "${details.restSeconds} s",
                    modifier = Modifier.weight(1f),
                )
            }

            if (history.size < 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (history.isEmpty()) {
                            "Your progress chart appears after the first finished workout with this exercise."
                        } else {
                            "Your progress chart appears after one more finished workout with this exercise."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                val change = history.last().value.weightKg - history.first().value.weightKg
                val percent = if (history.first().value.weightKg > 0.0) {
                    change / history.first().value.weightKg * 100.0
                } else {
                    0.0
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(text = "Weight over time", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = String.format(Locale.US, "%+.1f kg · %+.1f%%", change, percent),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                change > 0 -> MaterialTheme.effortColors.easy
                                change < 0 -> MaterialTheme.effortColors.hard
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    WeightLineChart(
                        dataPoints = history.map {
                            WeightDataPoint(it.key, it.value.weightKg, it.value.perceivedEffort)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PerceivedEffort.entries.forEach { effort ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                EffortDot(effort)
                                Text(
                                    text = effort.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Recent sessions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    history.takeLast(3).reversed().forEach { (completedAt, entry) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            EffortDot(entry.perceivedEffort)
                            Text(
                                text = dateFormat.format(completedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f kg", entry.weightKg),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = entry.perceivedEffort.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(64.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small outlined tile with a muted [label] above a [value], for one prescription field. */
@Composable
private fun DetailTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** A dot in the color of [effort], paired with a text label wherever effort is shown. */
@Composable
private fun EffortDot(effort: PerceivedEffort) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(MaterialTheme.effortColors.forEffort(effort), CircleShape),
    )
}

@Composable
private fun WorkoutSection(
    workout: WorkoutWithExercises,
    onExerciseClick: (ExerciseWithPerformanceHistory) -> Unit,
) {
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
                    ExerciseCard(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise) },
                        modifier = Modifier.weight(1f),
                    )
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
 * perceived effort from the last time the exercise was performed. Tapping it calls [onClick]
 * (which opens the details sheet).
 */
@Composable
private fun ExerciseCard(
    exercise: ExerciseWithPerformanceHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
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
    val filledColor = effortLevel
        ?.let { MaterialTheme.effortColors.forEffort(it) }
        ?: MaterialTheme.colorScheme.surfaceContainerHighest
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
