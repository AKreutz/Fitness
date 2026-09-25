package com.akreutz.fitness.ui.workouts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.model.performanceOn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A getter rather than a cached value, so it picks up the default locale fresh each time it's
 * used instead of freezing it at class-init time (the locale can change while the app is
 * running).
 */
private val sessionDateTimeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/**
 * Lists every completed [com.akreutz.fitness.data.model.WorkoutSession], most recent first, each
 * as a card with the workout's name, when it was completed, and how long it took. Tapping a card
 * expands it in place to show that session's per-exercise stats.
 *
 * Deleting the most recent session is offered elsewhere (in the screen's header, since this list
 * can grow long) — see [WorkoutsDeleteLastButton] and [WorkoutsDeleteLastConfirmationDialog].
 */
@Composable
fun WorkoutsScreen(
    uiState: WorkoutHistoryUiState,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is WorkoutHistoryUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is WorkoutHistoryUiState.Empty -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No workouts completed yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is WorkoutHistoryUiState.Loaded -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(uiState.sessions, key = { _, it -> it.session.id }) { _, session ->
                    WorkoutSessionCard(session = session)
                }
            }
        }
    }
}

/**
 * A "Delete last workout" button meant for the screen header, enabled only when there is a
 * completed session to delete.
 */
@Composable
fun WorkoutsDeleteLastButton(
    uiState: WorkoutHistoryUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSessions = uiState is WorkoutHistoryUiState.Loaded && uiState.sessions.isNotEmpty()
    TextButton(onClick = onClick, enabled = hasSessions, modifier = modifier) {
        Text("Delete last workout")
    }
}

/**
 * Confirmation dialog for deleting the most recent completed session — deleting an older one
 * could conflict with rating/weight changes a later session already made (see
 * [com.akreutz.fitness.data.repository.TrainingPlanRepository.deleteWorkoutSession]).
 */
@Composable
fun WorkoutsDeleteLastConfirmationDialog(
    session: WorkoutSessionWithWorkout,
    onConfirm: (WorkoutSession) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete workout") },
        text = {
            Text(
                "Delete the last completed \"${session.workout.name}\" session? This can't be undone.",
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(session.session) }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun WorkoutSessionCard(
    session: WorkoutSessionWithWorkout,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(session.session.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = session.workout.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = session.session.startedAt
                        .atZone(ZoneId.systemDefault())
                        .format(sessionDateTimeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = session.session.durationSeconds.formatDuration(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session.exercises.forEach { exercise ->
                        ExerciseStatsRow(exercise = exercise, sessionCompletedAt = session.session.completedAt)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseStatsRow(
    exercise: ExerciseWithPerformanceHistory,
    sessionCompletedAt: Instant,
    modifier: Modifier = Modifier,
) {
    val entry = exercise.performanceOn(sessionCompletedAt)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = exercise.exercise.name,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (entry != null) {
            Text(
                text = "${entry.weightKg.formatWeight()} kg • ${entry.perceivedEffort.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "No stats recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Display label for a [PerceivedEffort] level. */
private val PerceivedEffort.label: String
    get() = when (this) {
        PerceivedEffort.EASY -> "Easy effort"
        PerceivedEffort.MEDIUM -> "Medium effort"
        PerceivedEffort.HARD -> "Hard effort"
    }

/** Formats a weight in kg, trimming a trailing ".0" (e.g. "42.5" or "40"). */
private fun Double.formatWeight(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/** Formats a duration in whole seconds as e.g. "42 min" or "58 sec" for durations under a minute. */
private fun Long.formatDuration(): String {
    val minutes = this / 60
    return if (minutes > 0) "$minutes min" else "$this sec"
}
