package com.akreutz.fitness.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.ui.common.AddExerciseDialog
import com.akreutz.fitness.ui.common.DraggableList

/**
 * The mandatory first-run onboarding flow: name the plan and its workouts, then optionally add
 * exercises to them, before the rest of the app becomes available. Nothing is saved until
 * [OnboardingViewModel.finish] is called, so the back gesture on the exercises step can return to
 * the naming step without touching the database. Also reused from the Profile tab to create an
 * additional plan, via [onFinished].
 */
@Composable
fun OnboardingScreen(
    repository: TrainingPlanRepository,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(repository),
    )

    when (viewModel.step) {
        OnboardingStep.NAME_PLAN -> NamePlanStep(
            planName = viewModel.planName,
            onPlanNameChange = viewModel::updatePlanName,
            workouts = viewModel.workouts,
            onWorkoutNameChange = viewModel::updateWorkoutName,
            onAddWorkout = viewModel::addWorkout,
            onRemoveWorkout = viewModel::removeWorkout,
            onNext = viewModel::proceedToAddExercises,
            modifier = modifier,
        )
        OnboardingStep.ADD_EXERCISES -> {
            BackHandler(onBack = viewModel::backToNamePlan)
            AddExercisesStep(
                planName = viewModel.planName,
                workouts = viewModel.workouts,
                onAddExercise = viewModel::addExercise,
                onUpdateExercise = viewModel::updateExercise,
                onMoveExercise = viewModel::moveExercise,
                onRemoveExercise = viewModel::removeExercise,
                onFinish = {
                    viewModel.finish()
                    onFinished()
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun NamePlanStep(
    planName: String,
    onPlanNameChange: (String) -> Unit,
    workouts: List<DraftWorkout>,
    onWorkoutNameChange: (index: Int, name: String) -> Unit,
    onAddWorkout: () -> Unit,
    onRemoveWorkout: (index: Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = planName.isNotBlank() && workouts.any { it.name.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 144.dp),
    ) {
        Text(
            text = "Create your training plan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        Text(
            text = "Give it a name and add the workouts it's made up of. You can add exercises " +
                "to each workout next.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        OutlinedTextField(
            value = planName,
            onValueChange = onPlanNameChange,
            label = { Text("Plan name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        Text(
            text = "Workouts",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(workouts.size) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = workouts[index].name,
                        onValueChange = { onWorkoutNameChange(index, it) },
                        label = { Text("Workout ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (workouts.size > 1) {
                        IconButton(onClick = { onRemoveWorkout(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove workout")
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onAddWorkout) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add workout")
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Next")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddExercisesStep(
    planName: String,
    workouts: List<DraftWorkout>,
    onAddExercise: (
        workoutIndex: Int,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) -> Unit,
    onUpdateExercise: (
        workoutIndex: Int,
        exerciseIndex: Int,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) -> Unit,
    onMoveExercise: (workoutIndex: Int, fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveExercise: (workoutIndex: Int, exerciseIndex: Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var workoutIndexForDialog by remember { mutableStateOf<Int?>(null) }
    var exerciseIndexForDialog by remember { mutableStateOf<Int?>(null) }
    var exerciseForDeleteConfirm by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val namedWorkouts = workouts.withIndex().filter { (_, workout) -> workout.name.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 144.dp),
    ) {
        Text(
            text = "Add your exercises",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        Text(
            text = "Add exercises to \"$planName\"'s workouts, or skip this and add them later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Text(
            text = "Workouts",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(namedWorkouts, key = { indexedWorkout -> indexedWorkout.index }) { (index, workout) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                workoutIndexForDialog = index
                                exerciseIndexForDialog = null
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("Add exercise")
                        }
                    }
                    DraggableList(
                        items = workout.exercises,
                        onMove = { fromIndex, toIndex -> onMoveExercise(index, fromIndex, toIndex) },
                    ) { exerciseIndex, exercise ->
                        Text(
                            text = "${exercise.name} — ${exercise.sets} sets, " +
                                "${RepScheme.format(exercise.reps)} reps, ${exercise.weightKg}kg " +
                                "(+${exercise.weightIncrementKg}kg)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .combinedClickable(
                                    onClick = {
                                        workoutIndexForDialog = index
                                        exerciseIndexForDialog = exerciseIndex
                                    },
                                    onLongClick = {
                                        exerciseForDeleteConfirm = index to exerciseIndex
                                    },
                                ),
                        )
                    }
                }
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Finish setup")
        }
    }

    val workoutIndex = workoutIndexForDialog
    if (workoutIndex != null) {
        val exerciseIndex = exerciseIndexForDialog
        AddExerciseDialog(
            initial = exerciseIndex?.let { workouts[workoutIndex].exercises[it] },
            onDismiss = {
                workoutIndexForDialog = null
                exerciseIndexForDialog = null
            },
            onConfirm = { name, type, sets, reps, weightKg, weightIncrementKg, restSeconds ->
                if (exerciseIndex == null) {
                    onAddExercise(
                        workoutIndex,
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
                        workoutIndex,
                        exerciseIndex,
                        name,
                        type,
                        sets,
                        reps,
                        weightKg,
                        weightIncrementKg,
                        restSeconds,
                    )
                }
                workoutIndexForDialog = null
                exerciseIndexForDialog = null
            },
        )
    }

    val deleteTarget = exerciseForDeleteConfirm
    if (deleteTarget != null) {
        val (deleteWorkoutIndex, deleteExerciseIndex) = deleteTarget
        AlertDialog(
            onDismissRequest = { exerciseForDeleteConfirm = null },
            title = { Text("Delete exercise") },
            text = {
                Text(
                    "Delete \"${workouts[deleteWorkoutIndex].exercises[deleteExerciseIndex].name}\"?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveExercise(deleteWorkoutIndex, deleteExerciseIndex)
                        exerciseForDeleteConfirm = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseForDeleteConfirm = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

