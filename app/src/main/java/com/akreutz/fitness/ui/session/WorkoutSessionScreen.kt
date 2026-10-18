package com.akreutz.fitness.ui.session

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.FitnessApplication
import com.akreutz.fitness.ui.common.ConfirmDestructiveDialog
import com.akreutz.fitness.ui.common.pressScale
import com.akreutz.fitness.ui.common.dialogEnter
import com.akreutz.fitness.ui.common.label
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.PlateBreakdown
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.WeightStack
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
import com.akreutz.fitness.ui.theme.SlateBlueContainer
import com.akreutz.fitness.ui.theme.effortColors
import java.util.Locale
import kotlin.math.pow

/**
 * The screen shown while a workout is in progress: guides the user through [workoutId]'s
 * exercises one at a time, in their predefined order unless the user switches to a later one
 * during an exercise's warm-up (see [SwitchExerciseDialog]). The top bar matches Home's, titled with
 * the workout's name. Each exercise starts with a warm-up set, then steps through its prescribed
 * working sets, resting (with a running timer) between all but the last. Finishing an exercise's
 * last set prompts the user to rate their perceived effort for it before moving on; once every
 * exercise is rated, the ratings are saved under today's date and the session ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    repository: TrainingPlanRepository,
    workoutId: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as FitnessApplication
    val viewModel: WorkoutSessionViewModel = viewModel(
        factory = WorkoutSessionViewModelFactory(repository, workoutId, application.restTimerAlerter),
    )
    val uiState by viewModel.uiState.collectAsState()
    val weightStacks by repository.observeWeightStacks().collectAsState(initial = emptyList())

    // So the rest timer's notification (see RestTimerAlerter) can actually be shown; harmless to
    // ask every time the screen opens, since the system no-ops once already granted or denied.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
            is WorkoutSessionUiState.Finished -> WorkoutCompleteContent(
                personalRecords = state.personalRecords,
                onDone = onFinish,
                modifier = Modifier.padding(innerPadding),
            )
            is WorkoutSessionUiState.InProgress -> InProgressContent(
                state = state,
                weightStacks = weightStacks,
                onNext = viewModel::advance,
                onSwitchTo = viewModel::switchTo,
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
        Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp).pressScale()) {
            Text("Back")
        }
    }
}

@Composable
private fun InProgressContent(
    state: WorkoutSessionUiState.InProgress,
    weightStacks: List<WeightStack>,
    onNext: () -> Unit,
    onSwitchTo: (String) -> Unit,
    onRate: (PerceivedEffort) -> Unit,
    onRespondToWeightIncreaseOffer: (Double?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cancelConfirmationShown by remember { mutableStateOf(false) }
    var switchPickerShown by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val buttonScale = remember { Animatable(1f) }
    val checkProgress = remember { Animatable(0f) }
    val checkAlpha = remember { Animatable(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            ExerciseDotProgress(
                exerciseNumber = state.exerciseNumber,
                totalExercises = state.totalExercises,
            )
            // Everything about the exercise slides out and the next one in when the exercise
            // changes; set-to-set updates within one exercise (same key) just recompose in place.
            AnimatedContent(
                targetState = state,
                contentKey = { it.exercise.exercise.id },
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it / 3 } + fadeOut(tween(200))) using
                        SizeTransform(clip = false)
                },
                label = "exercise",
            ) { exerciseState ->
                ExerciseContent(state = exerciseState, weightStacks = weightStacks)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val finishingSet = state.currentSet is SetProgress.Working
                        haptics.performHapticFeedback(
                            if (finishingSet) HapticFeedbackType.Confirm else HapticFeedbackType.TextHandleMove,
                        )
                        scope.launch {
                            buttonScale.snapTo(0.94f)
                            buttonScale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            )
                        }
                        if (finishingSet) {
                            scope.launch {
                                checkProgress.snapTo(0f)
                                checkAlpha.snapTo(1f)
                                checkProgress.animateTo(1f, tween(350))
                                delay(450)
                                checkAlpha.animateTo(0f, tween(200))
                            }
                        }
                        onNext()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = buttonScale.value
                            scaleY = buttonScale.value
                        },
                ) {
                    AnimatedContent(
                        targetState = state.currentSet.nextLabel,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "nextLabel",
                    ) { label ->
                        Text(label)
                    }
                }
                if (checkAlpha.value > 0f) {
                    AnimatedCheckmark(
                        progress = checkProgress.value,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .alpha(checkAlpha.value),
                    )
                }
            }
            if (state.currentSet.isWarmUp && state.remainingExercises.isNotEmpty()) {
                OutlinedButton(
                    onClick = { switchPickerShown = true },
                    modifier = Modifier.fillMaxWidth().pressScale(),
                ) {
                    Text("Switch exercise")
                }
            }
            OutlinedButton(
                onClick = { cancelConfirmationShown = true },
                modifier = Modifier.fillMaxWidth().pressScale(),
            ) {
                Text("Cancel")
            }
        }
    }

    val exerciseToRate = state.exerciseToRate
    if (exerciseToRate != null) {
        RateEffortDialog(exerciseName = exerciseToRate.exercise.name, onRate = onRate)
    }

    val exerciseToOfferWeightIncrease = state.exerciseToOfferWeightIncrease
    if (exerciseToOfferWeightIncrease != null) {
        WeightIncreaseOfferDialog(
            exercise = exerciseToOfferWeightIncrease,
            weightStacks = weightStacks,
            onRespond = onRespondToWeightIncreaseOffer,
        )
    }

    if (switchPickerShown) {
        SwitchExerciseDialog(
            exercises = state.remainingExercises,
            onPick = { exerciseId ->
                switchPickerShown = false
                onSwitchTo(exerciseId)
            },
            onDismiss = { switchPickerShown = false },
        )
    }

    if (cancelConfirmationShown) {
        CancelWorkoutDialog(
            exercisesFinished = state.exerciseNumber - 1,
            totalExercises = state.totalExercises,
            onConfirm = {
                cancelConfirmationShown = false
                onCancel()
            },
            onDismiss = { cancelConfirmationShown = false },
        )
    }
}

/** What [RecoveryRow] shows, as one value so it can key an [AnimatedContent] (`null` = not resting). */
private data class RecoveryInfo(val seconds: Int, val goalSeconds: Int, val nextSetLabel: String)

/**
 * Everything on the session screen that belongs to the current exercise: its header, set pills,
 * card, up-next strip and rest timer. The set pills, the warm-up/working card and the rest timer
 * each animate in and out as the user moves between warm-up, working sets and resting.
 */
@Composable
private fun ExerciseContent(
    state: WorkoutSessionUiState.InProgress,
    weightStacks: List<WeightStack>,
) {
    val setProgress = state.currentSet
    val resting = setProgress as? SetProgress.Resting
    val recovery = if (state.recoverySeconds != null && resting != null) {
        RecoveryInfo(state.recoverySeconds, state.recoveryGoalSeconds, resting.nextSetLabel)
    } else {
        null
    }

    Column {
        ExerciseHeaderRow(exercise = state.exercise, modifier = Modifier.padding(top = 12.dp))
        AnimatedVisibility(
            visible = !setProgress.isWarmUp,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SetPillsRow(
                exercise = state.exercise,
                setProgress = setProgress,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        AnimatedContent(
            targetState = setProgress.isWarmUp,
            transitionSpec = {
                (fadeIn(tween(250, delayMillis = 60)) + slideInVertically(tween(300)) { it / 6 }) togetherWith
                    fadeOut(tween(150))
            },
            label = "exerciseCard",
        ) { isWarmUp ->
            CurrentExerciseCard(
                exercise = state.exercise,
                isWarmUp = isWarmUp,
                weightStacks = weightStacks,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (state.nextExercise != null) {
            UpNextRow(exercise = state.nextExercise, modifier = Modifier.padding(top = 12.dp))
        }
        AnimatedContent(
            targetState = recovery,
            contentKey = { it != null },
            transitionSpec = {
                (fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 4 }) togetherWith
                    fadeOut(tween(150))
            },
            label = "recovery",
        ) { info ->
            if (info != null) {
                RecoveryRow(
                    seconds = info.seconds,
                    goalSeconds = info.goalSeconds,
                    nextSetLabel = info.nextSetLabel,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Box(Modifier.fillMaxWidth())
            }
        }
    }
}

private const val CHECKMARK_STROKE_FRACTION = 0.125f

/** A checkmark that draws itself in as [progress] goes from 0f to 1f. */
@Composable
internal fun AnimatedCheckmark(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    checkSize: Dp = 20.dp,
) {
    Canvas(modifier = modifier.size(checkSize)) {
        val start = Offset(size.width * 0.15f, size.height * 0.55f)
        val corner = Offset(size.width * 0.4f, size.height * 0.8f)
        val end = Offset(size.width * 0.85f, size.height * 0.25f)
        val firstLength = (corner - start).getDistance()
        val secondLength = (end - corner).getDistance()
        val drawn = (firstLength + secondLength) * progress
        val path = Path().apply {
            moveTo(start.x, start.y)
            if (drawn <= firstLength) {
                val t = drawn / firstLength
                lineTo(start.x + (corner.x - start.x) * t, start.y + (corner.y - start.y) * t)
            } else {
                lineTo(corner.x, corner.y)
                val t = (drawn - firstLength) / secondLength
                lineTo(corner.x + (end.x - corner.x) * t, corner.y + (end.y - corner.y) * t)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = this.size.width * CHECKMARK_STROKE_FRACTION, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Text for a number that rolls up when [value] increases and down when it decreases, instead of
 * just swapping. [format] turns the value into the text shown.
 */
@Composable
private fun RollingNumberText(
    value: Double,
    format: (Double) -> String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(tween(250)) { if (up) it else -it } + fadeIn(tween(250))) togetherWith
                (slideOutVertically(tween(250)) { if (up) -it else it } + fadeOut(tween(150)))
        },
        modifier = modifier,
        label = "rollingNumber",
    ) { shown ->
        Text(text = format(shown), style = style, color = color)
    }
}

/**
 * Lets the user pick which of the [exercises] still to come to do now, e.g. because the equipment
 * for the current one is taken. The current exercise moves back to just after the picked one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchExerciseDialog(
    exercises: List<ExerciseWithPerformanceHistory>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text("Do which exercise now?", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "The current exercise moves back to just after your pick.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            exercises.forEachIndexed { index, item ->
                val exercise = item.exercise
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clickable { onPick(exercise.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (exercise.type) {
                                ExerciseType.FREE_WEIGHTS -> Icons.Filled.FitnessCenter
                                ExerciseType.CABLE -> Icons.Filled.Cable
                            },
                            contentDescription = exercise.type.label(),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${exercise.sets} sets · ${RepScheme.format(exercise.reps)} reps · " +
                                "${formatWeightCompact(exercise.weightKg)} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (index == 0) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Confirms that the user wants to cancel the in-progress workout, since doing so discards its
 * progress (no partial session or ratings are saved).
 */
@Composable
private fun CancelWorkoutDialog(
    exercisesFinished: Int,
    totalExercises: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val progress = if (exercisesFinished > 0) {
        "You've finished $exercisesFinished of $totalExercises exercises. "
    } else {
        ""
    }
    ConfirmDestructiveDialog(
        title = "Discard this workout?",
        message = "${progress}Nothing from this session will be saved.",
        confirmLabel = "Discard",
        dismissLabel = "Keep going",
        icon = null,
        keepIsPrimary = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
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
            val dotWidth by animateDpAsState(if (isCurrent) 20.dp else 8.dp, tween(300), label = "dotWidth")
            val dotColor by animateColorAsState(
                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                tween(300),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(dotColor),
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
private fun ExerciseHeaderRow(exercise: ExerciseWithPerformanceHistory, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = exercise.exercise.name,
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
private fun SetPillsRow(
    exercise: ExerciseWithPerformanceHistory,
    setProgress: SetProgress,
    modifier: Modifier = Modifier,
) {
    val currentSetNumber = setProgress.setNumber ?: return
    val lastPerceivedEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
    val targetReps = RepScheme.targetReps(exercise.exercise.reps, lastPerceivedEffort)
    val label = "${formatWeightCompact(exercise.exercise.weightKg)}kg×$targetReps"

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        for (setNumber in 1..exercise.exercise.sets) {
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
    val containerColor by animateColorAsState(
        when {
            isCurrent -> MaterialTheme.colorScheme.surfaceContainerHigh
            isDone -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surface
        },
        tween(300),
        label = "pillContainer",
    )
    val borderColor by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(300),
        label = "pillBorder",
    )
    val contentAlpha by animateFloatAsState(
        if (isCurrent || isDone) 1f else 0.6f,
        tween(300),
        label = "pillContentAlpha",
    )
    val pillAlpha by animateFloatAsState(if (isDone) 0.7f else 1f, tween(300), label = "pillAlpha")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(vertical = 8.dp)
            .alpha(pillAlpha),
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
private fun UpNextRow(exercise: ExerciseWithPerformanceHistory, modifier: Modifier = Modifier) {
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
            text = exercise.exercise.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${exercise.exercise.sets} sets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Prompts the user to rate how hard [exerciseName] felt, right after finishing its last set.
 * Not dismissible without picking a rating, since the session can't move on without one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateEffortDialog(exerciseName: String, onRate: (PerceivedEffort) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden },
        ),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("How hard was that?", style = MaterialTheme.typography.titleLarge)
            }
            EffortOption(PerceivedEffort.EASY, "Easy", "Could have done 3+ more reps", onRate)
            EffortOption(PerceivedEffort.MEDIUM, "Medium", "1–2 reps left in the tank", onRate)
            EffortOption(PerceivedEffort.HARD, "Hard", "At or near failure", onRate)
        }
    }
}

/** One tappable rating row in [RateEffortDialog], filled with the effort's theme color. */
@Composable
private fun EffortOption(
    effort: PerceivedEffort,
    label: String,
    hint: String,
    onRate: (PerceivedEffort) -> Unit,
) {
    val onEffort = MaterialTheme.effortColors.onEffort
    Surface(
        onClick = { onRate(effort) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.effortColors.forEffort(effort),
        contentColor = onEffort,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Three bars, filled up to the effort's level, as a color-independent cue.
            Row(
                modifier = Modifier.size(28.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val barHeights = listOf(10.dp, 18.dp, 28.dp)
                barHeights.forEachIndexed { index, barHeight ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(onEffort.copy(alpha = if (index <= effort.ordinal) 1f else 0.25f)),
                    )
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Offers to raise [exercise]'s prescribed weight for next time, shown after they rate it "easy"
 * for the second session in a row. Not dismissible without answering, since the session can't move
 * on without one. The old and new weight are shown side by side, and the new one can be changed
 * with preset chips and a stepper. The chips are the exercise's increment (free weights), or the
 * levels of its [WeightStack] above the current weight (cable). A cable exercise without a stack
 * only gets the stepper, since its levels aren't evenly spaced and [Exercise.weightIncrementKg]
 * doesn't apply. [onRespond] receives the new weight to persist, or `null` to keep the current one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeightIncreaseOfferDialog(
    exercise: ExerciseWithPerformanceHistory,
    weightStacks: List<WeightStack>,
    onRespond: (Double?) -> Unit,
) {
    val current = exercise.exercise.weightKg
    val isCable = exercise.exercise.type == ExerciseType.CABLE
    val stack = weightStacks.firstOrNull { it.id == exercise.exercise.weightStackId }
    val stackLevels = if (isCable && stack != null) {
        stack.levelsKg.filter { it > current }.sorted().take(5)
    } else {
        emptyList()
    }
    val increment = exercise.exercise.weightIncrementKg.takeIf { it > 0 } ?: 2.5
    val stepKg = if (isCable) 2.5 else increment / 2
    // Offsets from the current weight for the free-weight chips, e.g. +1.25, +2.5, +5.
    val offsets = listOf(increment / 2, increment, increment * 2)
    val initial = when {
        stackLevels.isNotEmpty() -> stackLevels.first()
        isCable -> current + 2.5
        else -> current + increment
    }
    var newWeight by remember { mutableStateOf(initial) }
    var weightText by remember { mutableStateOf(formatKg(initial)) }
    // Typed text that doesn't parse leaves newWeight at its last valid value, so check it too.
    val textValid = weightText.replace(',', '.').toDoubleOrNull() != null
    fun setWeight(weight: Double) {
        newWeight = weight
        weightText = formatKg(weight)
    }
    val canUse = textValid && newWeight > current

    // The field is sized to its text, so the number and the "kg" after it read as one centered unit.
    val numberStyle = MaterialTheme.typography.displaySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val numberWidth = with(density) {
        textMeasurer.measure(weightText.ifEmpty { "0" }, numberStyle).size.width.toDp()
    } + 4.dp

    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.dialogEnter(),
        title = { Text("Time to go heavier?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${exercise.exercise.name} has felt easy twice in a row.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (stackLevels.isEmpty()) {
                        FilledTonalIconButton(onClick = { setWeight((newWeight - stepKg).coerceAtLeast(0.0)) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease by ${formatKg(stepKg)} kg")
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            BasicTextField(
                                value = weightText,
                                onValueChange = { text ->
                                    weightText = text
                                    text.replace(',', '.').toDoubleOrNull()?.let { newWeight = it }
                                },
                                textStyle = numberStyle,
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(numberWidth),
                            )
                            Text(
                                " kg",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                        Text(
                            text = if (canUse) "next time (+${formatKg(newWeight - current)} kg)" else "next time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (stackLevels.isEmpty()) {
                        FilledTonalIconButton(onClick = { setWeight(newWeight + stepKg) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase by ${formatKg(stepKg)} kg")
                        }
                    }
                }
                val options = if (stackLevels.isNotEmpty()) {
                    stackLevels.map { it to "${formatKg(it)} kg" }
                } else if (isCable) {
                    emptyList()
                } else {
                    offsets.map { current + it to "+${formatKg(it)} kg" }
                }
                if (options.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        options.forEach { (weight, label) ->
                            FilterChip(
                                selected = newWeight == weight,
                                onClick = { setWeight(weight) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                },
                                modifier = Modifier.width(85.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRespond(newWeight) }, enabled = canUse) {
                Text("Use ${formatKg(newWeight)} kg")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onRespond(null) }) { Text("Keep ${formatKg(current)} kg") }
        },
    )
}

/** A weight with up to two decimals and no trailing zeros, e.g. "60", "62.5" or "1.25". */
private fun formatKg(kg: Double): String =
    String.format(Locale.US, "%.2f", kg).trimEnd('0').trimEnd('.')

/**
 * Card for the exercise currently being guided through: during warm-up, just says so; otherwise
 * the plate breakdown (for free-weight exercises) on the left and the prescribed weight/reps on
 * the right.
 */
@Composable
private fun CurrentExerciseCard(
    exercise: ExerciseWithPerformanceHistory,
    isWarmUp: Boolean,
    weightStacks: List<WeightStack>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        if (isWarmUp) {
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
            if (exercise.exercise.type == ExerciseType.FREE_WEIGHTS) {
                PlateBreakdownRow(weightKg = exercise.exercise.weightKg)
            } else {
                val stack = weightStacks.firstOrNull { it.id == exercise.exercise.weightStackId }
                StackBreakdownRow(
                    weightKg = exercise.exercise.weightKg,
                    incrementKg = exercise.exercise.weightIncrementKg,
                    levelsKg = stack?.levelsKg,
                )
            }
            val lastPerceivedEffort = exercise.lastPerformanceAtCurrentWeight?.perceivedEffort
            val targetReps = RepScheme.targetReps(exercise.exercise.reps, lastPerceivedEffort)
            Column(horizontalAlignment = Alignment.End) {
                RollingNumberText(
                    value = exercise.exercise.weightKg,
                    format = { String.format(Locale.US, "%.1f KG", it) },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row {
                    RollingNumberText(
                        value = targetReps.toDouble(),
                        format = { it.toInt().toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " target reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
 * the collar. Shows the empty bar (no plates) at exactly [PlateBreakdown.BAR_WEIGHT_KG], and
 * nothing if [weightKg] can't be made up exactly from [PlateBreakdown.PLATE_SIZES_KG].
 */
@Composable
private fun PlateBreakdownRow(weightKg: Double, modifier: Modifier = Modifier) {
    val plates = PlateBreakdown.forWeight(weightKg)
    if (plates.isEmpty() && weightKg != PlateBreakdown.BAR_WEIGHT_KG) return

    BarbellSideView(plates = plates, modifier = modifier)
}

/** How many rungs [StackBreakdownRow] draws around the current weight, current one included. */
private const val STACK_RUNG_COUNT = 5

/**
 * A visual stand-in for a cable machine's weight stack: [STACK_RUNG_COUNT] plates centered on
 * [weightKg], drawn lightest at the top and heaviest at the bottom as on a real stack. A pin marks
 * the current weight; that plate and the lighter ones stacked above it (the part of the stack
 * actually lifted together) are shaded as engaged, while the heavier plates left resting below
 * stay outlined. When [levelsKg] is given (the exercise has a real [WeightStack] assigned), the
 * rungs are a window of its actual levels containing [weightKg], clamped to [STACK_RUNG_COUNT] —
 * an accurate picture of the real machine, so every rung is labeled with its weight, not just the
 * pinned one. If [weightKg] is near the top or bottom of the stack, the window (and the pin's
 * position within it) shifts toward whichever end has fewer levels rather than padding the
 * shortfall with fictional ones, so every rung shown is always a real level — the pin just isn't
 * centered in that case. Otherwise (no stack assigned) the rungs are a generic ladder spaced by
 * [incrementKg], always centered on the pin — enough to place it, not to claim it's the real
 * machine's numbers, so only the pinned rung is labeled. If [weightKg] itself isn't one of
 * [levelsKg] (e.g. it was set before the stack was assigned, or the stack was edited since), the
 * pin sits on the next smaller level instead — the highest one the machine could actually be set
 * to without exceeding [weightKg] — or the lowest level if [weightKg] is below all of them; in
 * that case the pin is also annotated with "+X KG", the gap still owed on top of that level to
 * reach [weightKg], so the mismatch is visible rather than silently rounded away.
 */
@Composable
private fun StackBreakdownRow(
    weightKg: Double,
    incrementKg: Double,
    levelsKg: List<Double>?,
    modifier: Modifier = Modifier,
) {
    val centeredPinIndex = STACK_RUNG_COUNT / 2 // 0 = top/lightest rung
    val isRealStack = !levelsKg.isNullOrEmpty()
    val weights: List<Double>
    val pinIndex: Int
    var pinMismatchKg = 0.0
    if (levelsKg.isNullOrEmpty()) {
        val step = if (incrementKg > 0.0) incrementKg else 1.0
        weights = (0 until STACK_RUNG_COUNT).map { index -> weightKg + (index - centeredPinIndex) * step }
        pinIndex = centeredPinIndex
    } else {
        val sorted = levelsKg.sorted()
        val currentIndex = sorted.indexOfLast { it <= weightKg }.takeIf { it >= 0 } ?: 0
        // Center the window on currentIndex, then slide it back on-list if it would run past
        // either end — this can only shrink the shortfall on one side by growing the other, never
        // invent a level, since windowStart..windowEnd stays clamped to sorted.indices throughout.
        val windowSize = minOf(STACK_RUNG_COUNT, sorted.size)
        var windowStart = currentIndex - centeredPinIndex
        windowStart = windowStart.coerceIn(0, sorted.size - windowSize)
        weights = sorted.subList(windowStart, windowStart + windowSize)
        pinIndex = currentIndex - windowStart
        pinMismatchKg = weightKg - sorted[currentIndex]
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier,
    ) {
        weights.forEachIndexed { index, rungWeightKg ->
            StackPlate(
                weightKg = rungWeightKg,
                isPin = index == pinIndex,
                isEngaged = index <= pinIndex,
                showLabel = isRealStack,
                mismatchKg = if (index == pinIndex) pinMismatchKg else 0.0,
            )
        }
    }
}

/**
 * One rung of [StackBreakdownRow]'s stack: a rounded bar, filled solid when [isEngaged] (lifted
 * along with the pinned plate) or just outlined otherwise. The pinned rung — the current weight —
 * is always labeled, with a sideways-T pin (a short rod capped by a rounded crossbar) drawn at its
 * right edge. The other rungs are labeled too when [showLabel] is set (a real [WeightStack] is
 * assigned, so every number is one of the machine's actual levels); otherwise they stay unlabeled,
 * since they're a generic ladder rather than real stack numbers (see [StackBreakdownRow]). When
 * [mismatchKg] is positive (the pinned rung isn't exactly the exercise's current weight), a small
 * "+X KG" label is drawn after the pin, the gap still owed on top of this rung's weight.
 */
@Composable
private fun StackPlate(
    weightKg: Double,
    isPin: Boolean,
    isEngaged: Boolean,
    showLabel: Boolean,
    mismatchKg: Double = 0.0,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isPin -> MaterialTheme.colorScheme.primary
        isEngaged -> SlateBlueContainer
        else -> Color.Transparent
    }
    val borderModifier = if (isEngaged) {
        Modifier
    } else {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
    }
    val labelColor = when {
        isPin -> Color.White
        isEngaged -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(if (isPin) 64.dp else 60.dp)
                .height(if (isPin) 20.dp else 17.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(containerColor)
                .then(borderModifier),
        ) {
            if (isPin || showLabel) {
                Text(
                    text = formatWeightCompact(weightKg),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isPin) FontWeight.Bold else FontWeight.Normal,
                    color = labelColor,
                )
            }
        }
        if (isPin) {
            StackPin()
            if (mismatchKg > 0.0) {
                Text(
                    text = "+${formatWeightCompact(mismatchKg)} KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

/**
 * The sideways-T pin marking the current weight on [StackBreakdownRow]: a short rod, colored the
 * same as the barbell's bar (see [BarbellSideView]'s sleeve), starting right at the selected
 * plate's trailing edge (not overlapping its face) and capped by a rounded vertical crossbar
 * colored [PlateYellow15] as on a real selector pin.
 */
@Composable
private fun StackPin(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PlateYellow15),
        )
    }
}

/**
 * Draws the bar's sleeve as a horizontal rod with [plates] slid onto it left to right (largest
 * first, as they'd be loaded closest to the collar), each plate a color-coded disc seen edge-on.
 * The 20/15/10kg plates share one height and are distinguished only by thickness, proportional
 * to weight relative to the 10kg plate; 5kg keeps 10kg's thickness at a shorter height, and
 * 2.5kg/1.25kg share a shorter height still, with 1.25kg half as thick as 2.5kg. Each plate's
 * weight is labeled in the middle, rotated to fit along its height.
 */
@Composable
private fun BarbellSideView(plates: List<Double>, modifier: Modifier = Modifier) {
    val tallPlateHeight = 92.dp
    val basePlateWidth = 14.dp // thickness of the 10kg plate; 20/15kg scale up from this
    val sleeveHeight = 12.dp
    val rodHeight = 8.dp // the bare bar is thinner than the sleeve it slides into, as on a real barbell
    val sleeveColor = MaterialTheme.colorScheme.onSurfaceVariant

    // The bar runs continuously behind the stub and plates, up to the collar, so there's no gap
    // where a plate hasn't slid on yet (see SlidingPlate); the plates are layered on top of it.
    Box(modifier = modifier.height(tallPlateHeight)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                color = sleeveColor,
                topLeft = Offset(0f, (size.height - rodHeight.toPx()) / 2f),
                size = Size(size.width, rodHeight.toPx()),
            )
        }
        // Every child (sleeve stub, plates) is drawn against the same tallPlateHeight, so all discs
        // sit on a shared centerline regardless of their own height.
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.height(tallPlateHeight),
        ) {
            // Short stub of exposed sleeve before the first plate, as on a real bar.
            BarRodSegment(
                width = 12.dp,
                rodHeight = sleeveHeight,
                color = sleeveColor,
                tallPlateHeight = tallPlateHeight,
            )
            if (plates.isEmpty()) {
                // No plates loaded (bar weight only): extend a bare length of rod past the sleeve
                // stub instead of leaving the bar looking cut off.
                BarRodSegment(
                    width = 64.dp,
                    rodHeight = rodHeight,
                    color = sleeveColor,
                    tallPlateHeight = tallPlateHeight,
                )
            }
            // Plates slide onto the bar one after another; when the weight changes, only the plates
            // that differ from before (by position and size) animate in.
            plates.forEachIndexed { index, plateKg ->
                key(index, plateKg) {
                    SlidingPlate(index = index) {
                        PlateDisc(
                            plateKg = plateKg,
                            tallPlateHeight = tallPlateHeight,
                            basePlateWidth = basePlateWidth,
                            color = plateColor(plateKg),
                            labelColor = onPlateColor(plateKg),
                        )
                    }
                }
            }
            if (plates.isNotEmpty()) {
                // Short length of bare rod past the outermost plate, where the collar clamps it in
                // place on a real bar.
                BarRodSegment(
                    width = 16.dp,
                    rodHeight = rodHeight,
                    color = sleeveColor,
                    tallPlateHeight = tallPlateHeight,
                )
            }
        }
    }
}

/**
 * Slides [content] (a plate) onto the bar from well off its outer end, with a bounce, once its turn
 * comes ([index] staggers them, after the card has settled). The plate always takes up its final
 * space and is only moved with a graphics-layer transform, so the bar itself never shifts as plates
 * arrive. Drawn above the rod, since a real plate sits over the bar it's loaded on.
 */
@Composable
private fun SlidingPlate(index: Int, content: @Composable () -> Unit) {
    val slide = remember { Animatable(1f) } // 1 = off the end of the bar, 0 = seated
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150L + index * 110L)
        launch { fade.animateTo(1f, tween(150)) }
        slide.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier = Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationX = slide.value * size.width * 2
                alpha = fade.value
            },
    ) {
        content()
    }
}

/** A horizontal segment of the bar rod, [rodHeight] thick, centered within [tallPlateHeight]. */
@Composable
private fun BarRodSegment(width: Dp, rodHeight: Dp, color: Color, tallPlateHeight: Dp) {
    Canvas(modifier = Modifier.width(width).height(tallPlateHeight)) {
        drawRect(
            color = color,
            topLeft = Offset(0f, (size.height - rodHeight.toPx()) / 2f),
            size = Size(size.width, rodHeight.toPx()),
        )
    }
}

/**
 * One plate: a color-coded disc seen edge-on, sized by [plateKg]'s stepped height/thickness tier
 * (see [plateDiscSize]), vertically centered within [tallPlateHeight] so every plate shares the
 * bar's centerline, with its weight labeled in the middle of the disc, rotated 90° to the left to
 * fit along the plate's long (vertical) axis. The smallest (1.25kg) plate is drawn unlabeled —
 * too thin to fit a label.
 */
@Composable
private fun PlateDisc(
    plateKg: Double,
    tallPlateHeight: Dp,
    basePlateWidth: Dp,
    color: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val (width, discHeight) = plateDiscSize(plateKg, tallPlateHeight, basePlateWidth)
    val isSmallestPlate = plateKg == PlateBreakdown.PLATE_SIZES_KG.last()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val label = formatPlateWeight(plateKg)

    Canvas(
        modifier = modifier
            .width(width)
            .height(tallPlateHeight),
    ) {
        val plateSize = Size(width = size.width, height = discHeight.toPx())
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
 * Blends a real height ratio (e.g. 0.5, 0.25 relative to [tallPlateHeight]) toward 1 by
 * [HEIGHT_COMPRESSION], so shorter tiers read as closer in size to the tall tier rather than
 * their full real-world height difference, which would otherwise make the smallest plates look
 * disproportionately tiny next to a 92.dp-tall 20kg plate.
 */
private const val HEIGHT_COMPRESSION = 0.65

private fun compressedHeightRatio(realRatio: Double): Double =
    realRatio.pow(HEIGHT_COMPRESSION)

/**
 * The (width, height) a plate of [plateKg] is drawn at. The 20/15/10kg plates share
 * [tallPlateHeight] and differ only in thickness, scaled linearly by weight relative to the
 * 10kg plate's [basePlateWidth]. 5kg keeps 10kg's thickness at a shorter, compressed height;
 * 2.5kg and 1.25kg share a shorter height still, with 1.25kg half as thick as 2.5kg.
 */
private fun plateDiscSize(plateKg: Double, tallPlateHeight: Dp, basePlateWidth: Dp): Pair<Dp, Dp> =
    when {
        plateKg >= 10.0 -> basePlateWidth * (plateKg / 10.0).toFloat() to tallPlateHeight
        plateKg == 5.0 -> basePlateWidth to tallPlateHeight * compressedHeightRatio(0.5).toFloat()
        plateKg == 2.5 -> basePlateWidth to tallPlateHeight * compressedHeightRatio(0.25).toFloat()
        else -> basePlateWidth / 2 to tallPlateHeight * compressedHeightRatio(0.25).toFloat() // 1.25kg
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
    val target = (seconds / goalSeconds.toFloat()).coerceIn(0f, 1f)
    // The timer ticks once a second; a linear tween of the same length makes the ring sweep
    // continuously instead of stepping.
    val progress by animateFloatAsState(target, tween(1000, easing = LinearEasing), label = "restProgress")

    // Once the goal is reached the ring changes colour and gives a single pulse.
    val goalReached = seconds >= goalSeconds
    val ringColor by animateColorAsState(
        targetValue = if (goalReached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "restRingColor",
    )
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(goalReached) {
        if (goalReached) {
            pulse.animateTo(1.2f, tween(150))
            pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
    ) {
        RestCountdownRing(
            progress = progress,
            color = ringColor,
            modifier = Modifier.graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
        ) {
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
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = color
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
