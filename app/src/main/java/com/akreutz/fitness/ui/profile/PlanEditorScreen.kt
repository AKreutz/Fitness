package com.akreutz.fitness.ui.profile

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.ui.common.AddExerciseDialog
import com.akreutz.fitness.ui.common.DraggableList

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
    trainingPlanId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlanEditorViewModel = viewModel(
        factory = PlanEditorViewModelFactory(repository, trainingPlanId),
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            val title = (uiState as? PlanEditorUiState.Loaded)?.plan?.trainingPlan?.name
                ?: "Edit plan"
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PlanEditorUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
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
    onRenameWorkout: (workout: Workout, name: String) -> Unit,
    onAddWorkout: (name: String) -> Unit,
    onDeleteWorkout: (workout: Workout) -> Unit,
    onMoveWorkout: (workouts: List<Workout>, fromIndex: Int, toIndex: Int) -> Unit,
    onAddExercise: (
        workoutId: Long,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
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
            DraggableList(
                items = plan.workouts,
                onMove = { fromIndex, toIndex -> onMoveWorkout(workouts, fromIndex, toIndex) },
            ) { _, workoutWithExercises ->
                WorkoutEditorCard(
                    workout = workoutWithExercises.workout,
                    exercises = workoutWithExercises.exercises,
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
            OutlinedButton(
                onClick = { addWorkoutDialogShown = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add workout")
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
        AlertDialog(
            onDismissRequest = { workoutPendingDelete = null },
            title = { Text("Delete workout") },
            text = {
                Text(
                    "Delete \"${deleteWorkoutTarget.name}\"? This removes its exercises and " +
                        "completed-session history.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorkout(deleteWorkoutTarget)
                        workoutPendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    val deleteExerciseTarget = exercisePendingDelete
    if (deleteExerciseTarget != null) {
        AlertDialog(
            onDismissRequest = { exercisePendingDelete = null },
            title = { Text("Delete exercise") },
            text = { Text("Delete \"${deleteExerciseTarget.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExercise(deleteExerciseTarget)
                        exercisePendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { exercisePendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    val dialogTarget = exerciseDialogTarget
    if (dialogTarget != null) {
        val (workout, exercise) = dialogTarget
        AddExerciseDialog(
            initial = exercise?.toDraft(),
            onDismiss = { exerciseDialogTarget = null },
            onConfirm = { name, type, sets, reps, weightKg, weightIncrementKg, restSeconds ->
                if (exercise == null) {
                    onAddExercise(
                        workout.id,
                        name,
                        type,
                        sets,
                        reps,
                        weightKg,
                        weightIncrementKg,
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
                        restSeconds,
                    )
                }
                exerciseDialogTarget = null
            },
        )
    }
}

@Composable
private fun WorkoutEditorCard(
    workout: Workout,
    exercises: List<Exercise>,
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
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename \"${workout.name}\"",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete \"${workout.name}\"",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                DraggableList(
                    items = exercises,
                    onMove = { fromIndex, toIndex -> onMoveExercise(exercises, fromIndex, toIndex) },
                ) { _, exercise ->
                    Text(
                        text = "${exercise.name} — ${exercise.sets} sets, " +
                            "${RepScheme.format(exercise.reps)} reps, ${exercise.weightKg}kg " +
                            "(+${exercise.weightIncrementKg}kg)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .combinedClickable(
                                onClick = { onExerciseClick(exercise) },
                                onLongClick = { onExerciseDeleteRequest(exercise) },
                            ),
                    )
                }
                TextButton(onClick = onAddExercise) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add exercise")
                }
            }
        }
    }
}

@Composable
private fun WorkoutNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Workout name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
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

private fun Exercise.toDraft(): DraftExercise = DraftExercise(
    name = name,
    type = type,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    weightIncrementKg = weightIncrementKg,
    restSeconds = restSeconds,
)
