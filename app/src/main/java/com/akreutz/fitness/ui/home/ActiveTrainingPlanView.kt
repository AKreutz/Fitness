package com.akreutz.fitness.ui.home

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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WorkoutWithExercises

/**
 * Shows the user's active Training Plan: its name and its Workouts (Exercises aren't shown
 * here yet — they're added later from a workout's own screen).
 */
@Composable
fun ActiveTrainingPlanView(trainingPlan: TrainingPlanWithWorkouts, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = trainingPlan.trainingPlan.name, style = MaterialTheme.typography.headlineSmall)
        }

        if (trainingPlan.workouts.isEmpty()) {
            item {
                Text(
                    text = "No workouts yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(
                trainingPlan.workouts.sortedBy { it.workout.position },
                key = { it.workout.id },
            ) { workout ->
                WorkoutCard(workout)
            }
        }
    }
}

@Composable
private fun WorkoutCard(workoutWithExercises: WorkoutWithExercises, modifier: Modifier = Modifier) {
    val exerciseCount = workoutWithExercises.exercises.size
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = workoutWithExercises.workout.name, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = if (exerciseCount == 0) {
                    "No exercises yet"
                } else {
                    "$exerciseCount exercise${if (exerciseCount == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
            )
        }
    }
}
