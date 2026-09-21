package com.akreutz.fitness.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.akreutz.fitness.ui.theme.OnPlateChrome1_25
import com.akreutz.fitness.ui.theme.OnPlateWhite5
import com.akreutz.fitness.ui.theme.PlateBlack2_5
import com.akreutz.fitness.ui.theme.PlateBlue20
import com.akreutz.fitness.ui.theme.PlateChrome1_25
import com.akreutz.fitness.ui.theme.PlateGreen10
import com.akreutz.fitness.ui.theme.PlateWhite5
import com.akreutz.fitness.ui.theme.PlateYellow15
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
    onRespondToWeightIncreaseOffer: (Double?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cancelConfirmationShown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseDotProgress(
                exerciseNumber = state.exerciseNumber,
                totalExercises = state.totalExercises,
            )
            ExerciseHeaderRow(exercise = state.exercise)
            if (state.currentSet.isWarmUp.not()) {
                SetPillsRow(exercise = state.exercise, setProgress = state.currentSet)
            }
            CurrentExerciseCard(exercise = state.exercise, setProgress = state.currentSet)
            if (state.nextExercise != null) {
                UpNextRow(exercise = state.nextExercise)
            }
            val resting = state.currentSet as? SetProgress.Resting
            if (state.recoverySeconds != null && resting != null) {
                RecoveryRow(
                    seconds = state.recoverySeconds,
                    goalSeconds = state.recoveryGoalSeconds,
                    nextSetLabel = resting.nextSetLabel,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(state.currentSet.nextLabel)
            }
            OutlinedButton(
                onClick = { cancelConfirmationShown = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
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

    if (cancelConfirmationShown) {
        CancelWorkoutDialog(
            onConfirm = {
                cancelConfirmationShown = false
                onCancel()
            },
            onDismiss = { cancelConfirmationShown = false },
        )
    }
}

/**
 * Confirms that the user wants to cancel the in-progress workout, since doing so discards its
 * progress (no partial session or ratings are saved).
 */
@Composable
private fun CancelWorkoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel workout?") },
        text = { Text("Your progress in this workout won't be saved.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Cancel workout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep going")
            }
        },
    )
}

/**
 * How far through the workout's exercises the user is: one dot per exercise, the current one
 * shown as a wider pill, with the exact count alongside for precision dots alone don't give.
 */
@Composable
private fun ExerciseDotProgress(
    exerciseNumber: Int,
    totalExercises: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(totalExercises) { index ->
            val isCurrent = index == exerciseNumber - 1
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isCurrent) 20.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
        Text(
            text = "$exerciseNumber / $totalExercises",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

/**
 * The current exercise's name, with a badge showing how its most recent performance at its
 * current weight felt, if it's been performed since it was last at this weight. A performance
 * recorded at a now-outdated weight (e.g. right before a weight increase) doesn't count, since it
 * no longer reflects how the exercise feels now.
 */
@Composable
private fun ExerciseHeaderRow(exercise: Exercise, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f, fill = false),
        )
        val lastEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
        if (lastEffort != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Last workout:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = lastEffort.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

/** How [PerceivedEffort] is labeled in the "last workout" badge. */
private fun PerceivedEffort.label(): String = when (this) {
    PerceivedEffort.EASY -> "Easy"
    PerceivedEffort.MEDIUM -> "Medium"
    PerceivedEffort.HARD -> "Hard"
}

/**
 * All of [exercise]'s working sets at a glance, each a pill showing its target weight and reps:
 * completed sets dimmed with a checkmark, the current set highlighted, later ones plain. Only
 * meaningful once warm-up is done, since sets aren't numbered until then.
 */
@Composable
private fun SetPillsRow(exercise: Exercise, setProgress: SetProgress, modifier: Modifier = Modifier) {
    val currentSetNumber = setProgress.setNumber ?: return
    val lastPerceivedEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
    val targetReps = RepScheme.targetReps(exercise.reps, lastPerceivedEffort)
    val label = "${formatWeightCompact(exercise.weightKg)}kg×$targetReps"

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        for (setNumber in 1..exercise.sets) {
            val isDone = setNumber < currentSetNumber
            val isCurrent = setNumber == currentSetNumber
            SetPill(
                setNumber = setNumber,
                label = label,
                isDone = isDone,
                isCurrent = isCurrent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SetPill(
    setNumber: Int,
    label: String,
    isDone: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.surfaceContainerHigh
        isDone -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surface
    }
    val contentAlpha = if (isCurrent || isDone) 1f else 0.6f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            )
            .background(containerColor)
            .padding(vertical = 8.dp)
            .alpha(if (isDone) 0.7f else 1f),
    ) {
        Text(
            text = "SET $setNumber",
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }.copy(alpha = contentAlpha),
        )
        Text(
            text = if (isDone) "✓ $label" else label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A preview strip for whichever exercise comes after the current one. */
@Composable
private fun UpNextRow(exercise: Exercise, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "UP NEXT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${exercise.sets} sets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Button(onClick = { onRate(PerceivedEffort.EASY) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Easy")
                }
                Button(onClick = { onRate(PerceivedEffort.MEDIUM) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Medium")
                }
                Button(onClick = { onRate(PerceivedEffort.HARD) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hard")
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * Prompts the user to raise [exercise]'s prescribed weight for next time, shown after they rate
 * it "easy" effort for the second session in a row. Not dismissible without answering, since the
 * session can't move on without one. For a [ExerciseType.CABLE] exercise, whose weight levels
 * aren't evenly spaced, the user types the next weight level directly instead of it being
 * computed from [Exercise.weightIncrementKg]; for others, the increase is prefilled from
 * [Exercise.weightIncrementKg] but editable, in case a different bump makes sense just this once.
 * [onRespond] receives the new weight to persist, or `null` to keep the exercise's current one.
 */
@Composable
private fun WeightIncreaseOfferDialog(exercise: Exercise, onRespond: (Double?) -> Unit) {
    if (exercise.type == ExerciseType.CABLE) {
        var weightText by remember { mutableStateOf("") }
        val newWeightKg = weightText.replace(',', '.').toDoubleOrNull()
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Increase the weight?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${exercise.name} has felt easy twice in a row. " +
                            "What weight level should it use next time?",
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Next weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onRespond(newWeightKg) },
                    enabled = newWeightKg != null && newWeightKg >= 0,
                ) { Text("Increase") }
            },
            dismissButton = {
                OutlinedButton(onClick = { onRespond(null) }) { Text("Keep as is") }
            },
        )
    } else {
        var incrementText by remember { mutableStateOf(exercise.weightIncrementKg.toString()) }
        val incrementKg = incrementText.replace(',', '.').toDoubleOrNull()
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Increase the weight?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        String.format(
                            Locale.US,
                            "%s has felt easy twice in a row. Raise it from %.1f KG for next time?",
                            exercise.name,
                            exercise.weightKg,
                        ),
                    )
                    OutlinedTextField(
                        value = incrementText,
                        onValueChange = { incrementText = it },
                        label = { Text("Weight increase (kg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onRespond(exercise.weightKg + incrementKg!!) },
                    enabled = incrementKg != null && incrementKg >= 0,
                ) { Text("Increase") }
            },
            dismissButton = {
                OutlinedButton(onClick = { onRespond(null) }) { Text("Keep as is") }
            },
        )
    }
}

/**
 * Card for the exercise currently being guided through: during warm-up, just says so; otherwise
 * the plate breakdown (for free-weight exercises) on the left and the prescribed weight/reps on
 * the right.
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        if (setProgress.isWarmUp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Warm-up",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            return@Card
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            if (exercise.type == ExerciseType.FREE_WEIGHTS) {
                PlateBreakdownRow(weightKg = exercise.weightKg)
            }
            val lastPerceivedEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
            val targetReps = RepScheme.targetReps(exercise.reps, lastPerceivedEffort)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.1f KG", exercise.weightKg),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "$targetReps target reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A weight formatted compactly for the set pills, e.g. "45" or "45.5" — no trailing zero. */
private fun formatWeightCompact(weightKg: Double): String =
    if (weightKg == weightKg.toLong().toDouble()) {
        weightKg.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", weightKg)
    }

/**
 * The color a plate of [plateKg] is drawn in, following gym convention (color keyed to size) but
 * desaturated to match the app's palette. Falls back to the theme's primary color for any size
 * not in [PlateBreakdown.PLATE_SIZES_KG].
 */
@Composable
private fun plateColor(plateKg: Double): Color = when (plateKg) {
    20.0 -> PlateBlue20
    15.0 -> PlateYellow15
    10.0 -> PlateGreen10
    5.0 -> PlateWhite5
    2.5 -> PlateBlack2_5
    1.25 -> PlateChrome1_25
    else -> MaterialTheme.colorScheme.primary
}

/** The color a plate of [plateKg]'s weight label is drawn in, readable against [plateColor]. */
@Composable
private fun onPlateColor(plateKg: Double): Color = when (plateKg) {
    5.0 -> OnPlateWhite5
    1.25 -> OnPlateChrome1_25
    else -> Color.White
}

/**
 * A visual breakdown of the barbell plates needed per side to reach [weightKg]: a side-on view
 * of the bar's sleeve and collar with each plate drawn as a color-coded disc seen edge-on, sized
 * by weight, labeled with its weight, and stacked in loading order — largest first, closest to
 * the collar. Shows nothing if [weightKg] can't be made up exactly from
 * [PlateBreakdown.PLATE_SIZES_KG].
 */
@Composable
private fun PlateBreakdownRow(weightKg: Double, modifier: Modifier = Modifier) {
    val plates = PlateBreakdown.forWeight(weightKg)
    if (plates.isEmpty()) return

    BarbellSideView(plates = plates, modifier = modifier)
}

/**
 * Draws the bar's sleeve as a horizontal rod with [plates] slid onto it left to right (largest
 * first, as they'd be loaded closest to the collar), each plate a color-coded disc seen edge-on —
 * taller and thicker the heavier it is — with its weight labeled in the middle.
 */
@Composable
private fun BarbellSideView(plates: List<Double>, modifier: Modifier = Modifier) {
    val plateHeight = 64.dp
    val sleeveHeight = 12.dp
    val sleeveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Every child (sleeve stub, plates) is drawn against the same plateHeight, so all discs sit
    // on a shared centerline regardless of their own height.
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
        modifier = modifier.height(plateHeight),
    ) {
        // Short stub of exposed sleeve before the first plate, as on a real bar.
        Canvas(modifier = Modifier.width(12.dp).height(plateHeight)) {
            drawRect(
                color = sleeveColor,
                topLeft = Offset(0f, (size.height - sleeveHeight.toPx()) / 2f),
                size = Size(size.width, sleeveHeight.toPx()),
            )
        }
        for (plateKg in plates) {
            PlateDisc(
                plateKg = plateKg,
                plateHeight = plateHeight,
                color = plateColor(plateKg),
                labelColor = onPlateColor(plateKg),
            )
        }
    }
}

/**
 * One plate: a color-coded disc seen edge-on, sized by [plateKg] relative to the heaviest
 * available size, centered within [plateHeight], with its weight labeled in the middle of the
 * disc, rotated 90° to the left to fit along the plate's long (vertical) axis. The smallest
 * (1.25kg) plate is drawn unlabeled, at the same height as the 2.5kg plate but half its
 * thickness — too thin to fit a label.
 */
@Composable
private fun PlateDisc(
    plateKg: Double,
    plateHeight: Dp,
    color: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val width = plateWidth(plateKg)
    val heightFraction = plateHeightFraction(plateKg)
    val isSmallestPlate = plateKg == PlateBreakdown.PLATE_SIZES_KG.last()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val label = formatPlateWeight(plateKg)

    Canvas(
        modifier = modifier
            .width(width)
            .height(plateHeight),
    ) {
        val plateSize = Size(width = size.width, height = size.height * heightFraction)
        val topLeft = Offset(x = 0f, y = (size.height - plateSize.height) / 2f)
        val corner = CornerRadius(x = plateSize.width * 0.25f, y = plateSize.width * 0.25f)
        drawRoundRect(color = color, topLeft = topLeft, size = plateSize, cornerRadius = corner)
        // A thin white border so each plate reads distinctly against the dark background, even
        // for darker plate colors that would otherwise blend into it.
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = plateSize,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
        )

        if (isSmallestPlate.not()) {
            val textLayout = textMeasurer.measure(text = label, style = labelStyle)
            val center = Offset(x = plateSize.width / 2f, y = topLeft.y + plateSize.height / 2f)
            rotate(degrees = -90f, pivot = center) {
                translate(
                    left = center.x - textLayout.size.width / 2f,
                    top = center.y - textLayout.size.height / 2f,
                ) {
                    drawText(textLayout)
                }
            }
        }
    }
}

/** The weight label drawn on a plate, e.g. "20" or "2.5" — whole numbers without a decimal. */
private fun formatPlateWeight(plateKg: Double): String =
    if (plateKg == plateKg.toLong().toDouble()) {
        plateKg.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", plateKg).trimEnd('0').trimEnd('.')
    }

/**
 * How wide (thick) a plate is drawn, scaled by [plateKg] relative to the heaviest plate size.
 * The 1.25kg plate is the exception: it's drawn at half the width of the 2.5kg plate rather than
 * scaled from its own weight, since scaling it normally would make it too thin to read.
 */
private fun plateWidth(plateKg: Double): Dp {
    val maxWidth = 22.dp
    val minWidth = 10.dp
    val maxPlateKg = PlateBreakdown.PLATE_SIZES_KG.first()
    if (plateKg == 1.25) return plateWidth(2.5) / 2
    val fraction = (plateKg / maxPlateKg).toFloat().coerceIn(0f, 1f)
    return minWidth + (maxWidth - minWidth) * fraction
}

/**
 * How tall a plate is drawn, as a fraction of the row's full height, scaled by [plateKg]. The
 * 1.25kg plate is the exception: it's drawn at the same height as the 2.5kg plate rather than
 * scaled from its own (smaller) weight.
 */
private fun plateHeightFraction(plateKg: Double): Float {
    val minFraction = 0.55f
    val maxPlateKg = PlateBreakdown.PLATE_SIZES_KG.first()
    val effectiveKg = if (plateKg == 1.25) 2.5 else plateKg
    val fraction = (effectiveKg / maxPlateKg).toFloat().coerceIn(0f, 1f)
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

/**
 * The label for the set coming up once rest ends, e.g. "Set 3 of 3" — one past whichever
 * [SetProgress.Resting.completedSet] was. Only meaningful while resting.
 */
private val SetProgress.Resting.nextSetLabel: String
    get() {
        val completed = completedSet as? SetProgress.Working ?: return label
        return "Set ${completed.setNumber + 1} of ${completed.totalSets}"
    }

/**
 * The 1-based set number the set-pills row should treat as current, or `null` during warm-up
 * (sets aren't numbered yet). While resting, this is the *upcoming* set (one past whichever was
 * just completed), since that's the one the pills should highlight.
 */
private val SetProgress.setNumber: Int?
    get() = when (this) {
        SetProgress.WarmUp -> null
        is SetProgress.Working -> setNumber
        is SetProgress.Resting -> completedSet.setNumber?.plus(1)
    }

/**
 * The rest state between sets: a countdown ring towards [goalSeconds] (filling clockwise, capped
 * once [seconds] reaches or passes it) with the running time inside, alongside what's resting
 * towards and what's coming up next.
 */
@Composable
private fun RecoveryRow(
    seconds: Int,
    goalSeconds: Int,
    nextSetLabel: String,
    modifier: Modifier = Modifier,
) {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val progress = (seconds / goalSeconds.toFloat()).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
    ) {
        RestCountdownRing(progress = progress) {
            Text(
                text = String.format(Locale.US, "%d:%02d", minutes, remainingSeconds),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(
                text = "RESTING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Next: $nextSetLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A ring that fills clockwise from the top as [progress] (0f–1f) increases, with [content] inside. */
@Composable
private fun RestCountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
