package com.akreutz.fitness.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.PlateBreakdown
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.lastPerformanceAtCurrentWeight
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import java.util.Locale

/**
 * The screen shown while a workout is in progress: guides the user through [workoutId]'s
 * exercises one at a time, in their predefined order. The top bar matches Home's, titled with
 * the workout's name. Each exercise starts with a warm-up set, then steps through its prescribed
 * working sets, resting (with a running timer) between all but the last. Finishing an exercise's
 * last set prompts the user to rate their perceived effort for it before moving on; once every
 * exercise is rated, the ratings are saved under today's date and the session ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    repository: TrainingPlanRepository,
    workoutId: Long,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkoutSessionViewModel = viewModel(
        factory = WorkoutSessionViewModelFactory(repository, workoutId),
    )
    val uiState by viewModel.uiState.collectAsState()

    // Matches Home's top bar: same TopAppBar, titled with the workout's name once known.
    val title = (uiState as? WorkoutSessionUiState.InProgress)?.workoutName.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(title) })
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is WorkoutSessionUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is WorkoutSessionUiState.Unavailable ->
                UnavailableContent(onCancel, Modifier.padding(innerPadding))
            is WorkoutSessionUiState.Finished -> {
                LoadingContent(Modifier.padding(innerPadding))
                LaunchedEffect(Unit) { onFinish() }
            }
            is WorkoutSessionUiState.InProgress -> InProgressContent(
                state = state,
                onNext = viewModel::advance,
                onRate = viewModel::rateExercise,
                onRespondToWeightIncreaseOffer = viewModel::respondToWeightIncreaseOffer,
                onCancel = onCancel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UnavailableContent(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "This workout has no exercises to guide you through.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back")
        }
    }
}

@Composable
private fun InProgressContent(
    state: WorkoutSessionUiState.InProgress,
    onNext: () -> Unit,
    onRate: (PerceivedEffort) -> Unit,
    onRespondToWeightIncreaseOffer: (Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Exercise ${state.exerciseNumber} of ${state.totalExercises}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { state.exerciseNumber / state.totalExercises.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CurrentExerciseCard(exercise = state.exercise, setProgress = state.currentSet)
            if (state.recoverySeconds != null) {
                RecoveryTimer(seconds = state.recoverySeconds)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(state.currentSet.nextLabel)
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }

    val exerciseToRate = state.exerciseToRate
    if (exerciseToRate != null) {
        RateEffortDialog(exerciseName = exerciseToRate.name, onRate = onRate)
    }

    val exerciseToOfferWeightIncrease = state.exerciseToOfferWeightIncrease
    if (exerciseToOfferWeightIncrease != null) {
        WeightIncreaseOfferDialog(
            exercise = exerciseToOfferWeightIncrease,
            onRespond = onRespondToWeightIncreaseOffer,
        )
    }
}

/**
 * Prompts the user to rate how hard [exerciseName] felt, right after finishing its last set.
 * Not dismissible without picking a rating, since the session can't move on without one.
 */
@Composable
private fun RateEffortDialog(exerciseName: String, onRate: (PerceivedEffort) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("How did $exerciseName feel?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onRate(PerceivedEffort.LOW) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Low")
                }
                Button(onClick = { onRate(PerceivedEffort.MEDIUM) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Medium")
                }
                Button(onClick = { onRate(PerceivedEffort.HIGH) }, modifier = Modifier.fillMaxWidth()) {
                    Text("High")
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * Prompts the user to raise [exercise]'s prescribed weight by its increment for next time, shown
 * after they rate it "low" effort for the second session in a row. Not dismissible without
 * answering, since the session can't move on without one.
 */
@Composable
private fun WeightIncreaseOfferDialog(exercise: Exercise, onRespond: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Increase the weight?") },
        text = {
            Text(
                String.format(
                    Locale.US,
                    "%s has felt easy twice in a row. Raise it from %.1f to %.1f KG for next time?",
                    exercise.name,
                    exercise.weightKg,
                    exercise.weightKg + exercise.weightIncrementKg,
                ),
            )
        },
        confirmButton = {
            Button(onClick = { onRespond(true) }) { Text("Increase") }
        },
        dismissButton = {
            OutlinedButton(onClick = { onRespond(false) }) { Text("Keep as is") }
        },
    )
}

/**
 * Card for the exercise currently being guided through: its name, and either "Warm-up" or the
 * prescribed sets/reps/weight, depending on [setProgress].
 */
@Composable
private fun CurrentExerciseCard(
    exercise: Exercise,
    setProgress: SetProgress,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = setProgress.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (setProgress.isWarmUp.not()) {
                val lastPerceivedEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
                val targetReps = RepScheme.targetReps(exercise.reps, lastPerceivedEffort)
                Text(
                    text = "$targetReps reps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = String.format(Locale.US, "%.1f KG", exercise.weightKg),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (exercise.type == ExerciseType.FREE_WEIGHTS) {
                    PlateBreakdownRow(
                        weightKg = exercise.weightKg,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * A visual breakdown of the barbell plates needed per side to reach [weightKg]: a side-on view
 * of the bar's sleeve with each plate drawn as a thin vertical bar (a disc seen edge-on), sized
 * by weight and stacked in loading order — largest first, closest to where the bar's collar
 * would sit. Shows nothing if [weightKg] can't be made up exactly from
 * [PlateBreakdown.PLATE_SIZES_KG].
 */
@Composable
private fun PlateBreakdownRow(weightKg: Double, modifier: Modifier = Modifier) {
    val plates = PlateBreakdown.forWeight(weightKg)
    if (plates.isEmpty()) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "Plates per side",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BarbellSideView(
            plates = plates,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

/**
 * Draws the bar's sleeve as a horizontal rod with [plates] slid onto it left to right (largest
 * first, as they'd be loaded closest to the collar), each one a thin vertical bar taller and
 * thicker the heavier it is — a side-on view of a loaded barbell end.
 */
@Composable
private fun BarbellSideView(plates: List<Double>, modifier: Modifier = Modifier) {
    val plateHeight = 64.dp
    val sleeveHeight = 14.dp
    val sleeveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val plateColor = MaterialTheme.colorScheme.primary

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(plateHeight),
    ) {
        // Short stub of exposed sleeve before the first plate, as on a real bar.
        Canvas(modifier = Modifier.width(12.dp).height(sleeveHeight)) {
            drawRect(color = sleeveColor, size = Size(size.width, size.height))
        }
        for (plateKg in plates) {
            val width = plateWidth(plateKg)
            val heightFraction = plateHeightFraction(plateKg)
            Canvas(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight(),
            ) {
                val plateSize = Size(width = size.width, height = size.height * heightFraction)
                drawRoundRect(
                    color = plateColor,
                    topLeft = Offset(x = 0f, y = (size.height - plateSize.height) / 2f),
                    size = plateSize,
                    cornerRadius = CornerRadius(x = size.width * 0.2f, y = size.width * 0.2f),
                )
            }
        }
    }
}

/** How wide (thick) a plate is drawn, scaled by [plateKg] relative to the heaviest plate size. */
@Composable
private fun plateWidth(plateKg: Double): Dp {
    val maxWidth = 22.dp
    val minWidth = 8.dp
    val maxPlateKg = PlateBreakdown.PLATE_SIZES_KG.first()
    val fraction = (plateKg / maxPlateKg).toFloat().coerceIn(0f, 1f)
    return minWidth + (maxWidth - minWidth) * fraction
}

/** How tall a plate is drawn, as a fraction of the row's full height, scaled by [plateKg]. */
private fun plateHeightFraction(plateKg: Double): Float {
    val minFraction = 0.55f
    val maxPlateKg = PlateBreakdown.PLATE_SIZES_KG.first()
    val fraction = (plateKg / maxPlateKg).toFloat().coerceIn(0f, 1f)
    return minFraction + (1f - minFraction) * fraction
}

/** The label shown for a [SetProgress] on the exercise card, e.g. "Warm-up" or "Set 2 of 3". */
private val SetProgress.label: String
    get() = when (this) {
        SetProgress.WarmUp -> "Warm-up"
        is SetProgress.Working -> "Set $setNumber of $totalSets"
        is SetProgress.Resting -> completedSet.label
    }

private val SetProgress.isWarmUp: Boolean
    get() = when (this) {
        SetProgress.WarmUp -> true
        is SetProgress.Working -> false
        is SetProgress.Resting -> completedSet.isWarmUp
    }

/** Shows the running recovery time between sets, counting up past the rest goal once reached. */
@Composable
private fun RecoveryTimer(seconds: Int, modifier: Modifier = Modifier) {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    Text(
        text = String.format(Locale.US, "Resting: %d:%02d", minutes, remainingSeconds),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
