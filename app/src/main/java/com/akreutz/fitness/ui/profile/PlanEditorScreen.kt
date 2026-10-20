package com.akreutz.fitness.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.ui.common.ShimmerPlaceholderList
import com.akreutz.fitness.ui.common.pressScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.ui.common.ConfirmDestructiveDialog
import com.akreutz.fitness.ui.common.dialogEnter
import com.akreutz.fitness.ui.common.AddExerciseDialog
import com.akreutz.fitness.ui.common.DraggableListWithHandle
import java.util.Locale
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight

/**
 * Lets the user edit an existing [com.akreutz.fitness.data.model.TrainingPlan]: add, rename,
 * remove, and reorder its workouts, and — within each workout — add, edit (including weight and
 * weight increment), remove, and reorder its exercises. Unlike onboarding, every action here is
 * saved immediately, since the plan already exists and other screens observe it live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    repository: TrainingPlanRepository,
    trainingPlanId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlanEditorViewModel = viewModel(
        factory = PlanEditorViewModelFactory(repository, trainingPlanId),
    )
    val uiState by viewModel.uiState.collectAsState()
    val weightStacks by viewModel.weightStacks.collectAsState()
    var planRenameShown by rememberSaveable { mutableStateOf(false) }

    val planToRename = (uiState as? PlanEditorUiState.Loaded)?.plan?.trainingPlan
    if (planRenameShown && planToRename != null) {
        WorkoutNameDialog(
            title = "Rename plan",
            label = "Plan name",
            initialName = planToRename.name,
            onDismiss = { planRenameShown = false },
            onConfirm = { name ->
                viewModel.renamePlan(planToRename, name)
                planRenameShown = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            val loadedPlan = (uiState as? PlanEditorUiState.Loaded)?.plan
            val title = loadedPlan?.trainingPlan?.name ?: "Edit plan"
            TopAppBar(
                title = {
                    Column {
                        Text(title)
                        if (loadedPlan != null) {
                            val exerciseCount = loadedPlan.workouts.sumOf { it.exercises.size }
                            val workoutCount = loadedPlan.workouts.size
                            Text(
                                text = "$workoutCount ${if (workoutCount == 1) "workout" else "workouts"}" +
                                    " · $exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}" +
                                    " · saved automatically",
                                style = MaterialTheme.typography.bodySmall,
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
                actions = {
                    if (loadedPlan != null) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Plan actions")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename plan") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        planRenameShown = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PlanEditorUiState.Loading -> {
                ShimmerPlaceholderList(
                    heights = listOf(96.dp, 96.dp, 96.dp),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            is PlanEditorUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This plan no longer exists",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is PlanEditorUiState.Loaded -> {
                PlanEditorContent(
                    plan = state.plan,
                    weightStacks = weightStacks,
                    onCreateWeightStack = viewModel::createWeightStack,
                    onRenameWorkout = viewModel::renameWorkout,
                    onAddWorkout = viewModel::addWorkout,
                    onDeleteWorkout = viewModel::deleteWorkout,
                    onMoveWorkout = viewModel::moveWorkout,
                    onAddExercise = viewModel::addExercise,
                    onUpdateExercise = viewModel::updateExercise,
                    onDeleteExercise = viewModel::deleteExercise,
                    onMoveExercise = viewModel::moveExercise,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun PlanEditorContent(
    plan: TrainingPlanWithWorkouts,
    weightStacks: List<WeightStack>,
    onCreateWeightStack: suspend (name: String, levelsKg: List<Double>) -> WeightStack,
    onRenameWorkout: (workout: Workout, name: String) -> Unit,
    onAddWorkout: (name: String) -> Unit,
    onDeleteWorkout: (workout: Workout) -> Unit,
    onMoveWorkout: (workouts: List<Workout>, fromIndex: Int, toIndex: Int) -> Unit,
    onAddExercise: (
        workoutId: String,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        weightStackId: String?,
        restSeconds: Int,
    ) -> Unit,
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
    onDeleteExercise: (exercise: Exercise) -> Unit,
    onMoveExercise: (exercises: List<Exercise>, fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var workoutPendingDelete by remember { mutableStateOf<Workout?>(null) }
    var exercisePendingDelete by remember { mutableStateOf<Exercise?>(null) }
    var addWorkoutDialogShown by remember { mutableStateOf(false) }
    var workoutPendingRename by remember { mutableStateOf<Workout?>(null) }
    // The workout whose "add exercise" dialog is open (null exercise = add, non-null = edit).
    var exerciseDialogTarget by remember { mutableStateOf<Pair<Workout, Exercise?>?>(null) }
    val workouts = plan.workouts.map { it.workout }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DraggableListWithHandle(
                items = plan.workouts,
                onMove = { fromIndex, toIndex -> onMoveWorkout(workouts, fromIndex, toIndex) },
                rowSpacing = 12.dp,
                itemKey = { it.workout.id },
            ) { _, workoutWithExercises, handle ->
                WorkoutEditorCard(
                    workout = workoutWithExercises.workout,
                    exercises = workoutWithExercises.exercises.map { it.exercise },
                    dragHandle = handle,
                    onRename = { workoutPendingRename = workoutWithExercises.workout },
                    onDeleteRequest = { workoutPendingDelete = workoutWithExercises.workout },
                    onAddExercise = { exerciseDialogTarget = workoutWithExercises.workout to null },
                    onExerciseClick = { exercise ->
                        exerciseDialogTarget = workoutWithExercises.workout to exercise
                    },
                    onExerciseDeleteRequest = { exercisePendingDelete = it },
                    onMoveExercise = onMoveExercise,
                )
            }
        }
        item {
            val dashColor = MaterialTheme.colorScheme.outline
            TextButton(
                onClick = { addWorkoutDialogShown = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10.dp.toPx(), 8.dp.toPx()),
                                ),
                            ),
                        )
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add workout", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (addWorkoutDialogShown) {
        WorkoutNameDialog(
            title = "Add workout",
            initialName = "",
            onDismiss = { addWorkoutDialogShown = false },
            onConfirm = { name ->
                onAddWorkout(name)
                addWorkoutDialogShown = false
            },
        )
    }

    val renameTarget = workoutPendingRename
    if (renameTarget != null) {
        WorkoutNameDialog(
            title = "Rename workout",
            initialName = renameTarget.name,
            onDismiss = { workoutPendingRename = null },
            onConfirm = { name ->
                onRenameWorkout(renameTarget, name)
                workoutPendingRename = null
            },
        )
    }

    val deleteWorkoutTarget = workoutPendingDelete
    if (deleteWorkoutTarget != null) {
        val exerciseCount = plan.workouts
            .firstOrNull { it.workout.id == deleteWorkoutTarget.id }?.exercises?.size ?: 0
        ConfirmDestructiveDialog(
            title = "Delete \"${deleteWorkoutTarget.name}\"?",
            message = "This permanently removes its $exerciseCount " +
                "${if (exerciseCount == 1) "exercise" else "exercises"} and its " +
                "completed-session history. This can't be undone.",
            confirmLabel = "Delete workout",
            dismissLabel = "Keep workout",
            onConfirm = {
                onDeleteWorkout(deleteWorkoutTarget)
                workoutPendingDelete = null
            },
            onDismiss = { workoutPendingDelete = null },
        )
    }

    val deleteExerciseTarget = exercisePendingDelete
    if (deleteExerciseTarget != null) {
        ConfirmDestructiveDialog(
            title = "Delete \"${deleteExerciseTarget.name}\"?",
            message = "This removes the exercise from the workout. This can't be undone.",
            confirmLabel = "Delete exercise",
            dismissLabel = "Keep exercise",
            onConfirm = {
                onDeleteExercise(deleteExerciseTarget)
                exercisePendingDelete = null
            },
            onDismiss = { exercisePendingDelete = null },
        )
    }

    val dialogTarget = exerciseDialogTarget
    if (dialogTarget != null) {
        val (workout, exercise) = dialogTarget
        AddExerciseDialog(
            initial = exercise?.toDraft(),
            weightStacks = weightStacks,
            onCreateWeightStack = onCreateWeightStack,
            onDismiss = { exerciseDialogTarget = null },
            onConfirm = { name, type, sets, reps, weightKg, weightIncrementKg, weightStackId, restSeconds ->
                if (exercise == null) {
                    onAddExercise(
                        workout.id,
                        name,
                        type,
                        sets,
                        reps,
                        weightKg,
                        weightIncrementKg,
                        weightStackId,
                        restSeconds,
                    )
                } else {
                    onUpdateExercise(
                        exercise,
                        name,
                        type,
                        sets,
                        reps,
                        weightKg,
                        weightIncrementKg,
                        weightStackId,
                        restSeconds,
                    )
                }
                exerciseDialogTarget = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutEditorCard(
    workout: Workout,
    exercises: List<Exercise>,
    dragHandle: @Composable (Modifier) -> Unit,
    onRename: () -> Unit,
    onDeleteRequest: () -> Unit,
    onAddExercise: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onExerciseDeleteRequest: (Exercise) -> Unit,
    onMoveExercise: (exercises: List<Exercise>, fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(workout.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 4.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dragHandle(Modifier.padding(horizontal = 6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val setCount = exercises.sumOf { it.sets }
                    Text(
                        text = "${exercises.size} ${if (exercises.size == 1) "exercise" else "exercises"}" +
                            " · $setCount ${if (setCount == 1) "set" else "sets"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    AnimatedVisibility(visible = !expanded && exercises.isNotEmpty()) {
                        Text(
                            text = exercises.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                EditorMenuButton(
                    contentDescription = "Actions for \"${workout.name}\"",
                    editLabel = "Rename",
                    deleteLabel = "Delete workout",
                    onEdit = onRename,
                    onDeleteRequest = onDeleteRequest,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    )
                    DraggableListWithHandle(
                        items = exercises,
                        onMove = { fromIndex, toIndex -> onMoveExercise(exercises, fromIndex, toIndex) },
                        itemKey = { it.id },
                    ) { _, exercise, handle ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExerciseClick(exercise) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                handle(Modifier.padding(horizontal = 6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 6.dp),
                                    ) {
                                        StatChip("${exercise.sets} sets · ${RepScheme.format(exercise.reps)} reps")
                                        StatChip("${formatKg(exercise.weightKg)} kg")
                                        StatChip(
                                            "+${formatKg(exercise.weightIncrementKg)} kg / session",
                                            accent = true,
                                        )
                                        StatChip(formatRest(exercise.restSeconds) + " rest")
                                    }
                                }
                                EditorMenuButton(
                                    contentDescription = "Actions for \"${exercise.name}\"",
                                    editLabel = "Edit",
                                    deleteLabel = "Delete exercise",
                                    onEdit = { onExerciseClick(exercise) },
                                    onDeleteRequest = { onExerciseDeleteRequest(exercise) },
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onAddExercise,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 8.dp)
                            .pressScale(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Add exercise", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, accent: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (accent) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (accent) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun formatKg(kg: Double): String =
    String.format(Locale.US, "%.2f", kg).trimEnd('0').trimEnd('.')

private fun formatRest(seconds: Int): String = when {
    seconds < 60 -> "$seconds s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60}:${"%02d".format(seconds % 60)} min"
}

/** Overflow button with an edit-style action and a destructive delete action. */
@Composable
private fun EditorMenuButton(
    contentDescription: String,
    editLabel: String,
    deleteLabel: String,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(editLabel) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onDeleteRequest()
                },
            )
        }
    }
}

@Composable
private fun WorkoutNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
    label: String = "Workout name",
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogEnter(),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

internal fun Exercise.toDraft(): DraftExercise = DraftExercise(
    name = name,
    type = type,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    weightIncrementKg = weightIncrementKg,
    weightStackId = weightStackId,
    restSeconds = restSeconds,
)
