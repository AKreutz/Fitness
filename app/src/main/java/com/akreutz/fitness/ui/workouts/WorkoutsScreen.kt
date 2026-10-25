package com.akreutz.fitness.ui.workouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.ui.common.ShimmerPlaceholderList
import com.akreutz.fitness.ui.common.pressScale
import com.akreutz.fitness.ui.common.rememberCountUp
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.model.heartRate
import com.akreutz.fitness.data.model.performanceOn
import com.akreutz.fitness.ui.common.ConfirmDestructiveDialog
import com.akreutz.fitness.ui.theme.effortColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A getter rather than a cached value, so it picks up the default locale fresh each time it's
 * used instead of freezing it at class-init time (the locale can change while the app is
 * running).
 */
private val sessionTimeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.ENGLISH)

/**
 * The workout history: a summary of recent activity, a month calendar marking training days (tap
 * one to show only that day's sessions), and every completed
 * [com.akreutz.fitness.data.model.WorkoutSession] grouped by month, most recent first. Tapping a
 * card expands it in place to show that session's per-exercise stats.
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
            ShimmerPlaceholderList(heights = listOf(72.dp, 330.dp, 84.dp, 84.dp), modifier = modifier)
        }
        is WorkoutHistoryUiState.Empty -> {
            Column(
                modifier = modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Text(text = "No workouts yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Complete a workout and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        is WorkoutHistoryUiState.Loaded -> WorkoutHistory(uiState.sessions, modifier)
    }
}

@Composable
private fun WorkoutHistory(
    sessions: List<WorkoutSessionWithWorkout>,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDay = selectedEpochDay?.let(LocalDate::ofEpochDay)

    val datedSessions = remember(sessions) {
        sessions.map { it to it.session.startedAt.atZone(zone).toLocalDate() }
    }
    val workoutDays = remember(datedSessions) { datedSessions.map { it.second }.toSet() }
    val groups = remember(datedSessions, selectedDay) {
        datedSessions
            .filter { selectedDay == null || it.second == selectedDay }
            .groupBy { YearMonth.from(it.second) }
            .toList()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "stats") {
            WorkoutStatsRow(
                workoutDays = workoutDays,
                total = sessions.size,
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "calendar") {
            WorkoutCalendar(
                workoutDays = workoutDays,
                selectedDay = selectedDay,
                onDaySelected = { selectedEpochDay = it?.toEpochDay() },
                modifier = Modifier.animateItem(),
            )
        }
        if (selectedDay != null) {
            item(key = "filter") {
                InputChip(
                    modifier = Modifier.animateItem(),
                    selected = true,
                    onClick = { selectedEpochDay = null },
                    label = { Text(selectedDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH))) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear date filter",
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
        groups.forEach { (month, entries) ->
            item(key = "header-$month") {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.animateItem().padding(top = 8.dp, start = 4.dp),
                )
            }
            items(entries, key = { it.first.session.id }) { (session, _) ->
                WorkoutSessionCard(session = session, modifier = Modifier.animateItem())
            }
        }
    }
}

/** Three at-a-glance tiles: training days this week and this month, and sessions overall. */
@Composable
private fun WorkoutStatsRow(
    workoutDays: Set<LocalDate>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(LocalConfiguration.current.locales[0])
    val thisWeek = workoutDays.count {
        it.get(weekFields.weekBasedYear()) == today.get(weekFields.weekBasedYear()) &&
            it.get(weekFields.weekOfWeekBasedYear()) == today.get(weekFields.weekOfWeekBasedYear())
    }
    val thisMonth = workoutDays.count { YearMonth.from(it) == YearMonth.from(today) }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("This week", thisWeek, Modifier.weight(1f))
        StatTile("This month", thisMonth, Modifier.weight(1f))
        StatTile("All time", total, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    val shown by rememberCountUp(value.toFloat())
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = shown.roundToInt().toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    ConfirmDestructiveDialog(
        title = "Delete \"${session.workout.name}\" session?",
        message = "This permanently removes your most recent completed session of this " +
            "workout. This can't be undone.",
        confirmLabel = "Delete session",
        dismissLabel = "Keep session",
        onConfirm = { onConfirm(session.session) },
        onDismiss = onDismiss,
    )
}

@Composable
private fun WorkoutSessionCard(
    session: WorkoutSessionWithWorkout,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(session.session.id) { mutableStateOf(false) }
    val startedAt = session.session.startedAt.atZone(ZoneId.systemDefault())
    val exerciseCount = session.exercises.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Column(
                        modifier = Modifier.width(52.dp).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = startedAt.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = startedAt.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = session.workout.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = listOf(
                            startedAt.format(sessionTimeFormatter),
                            session.session.durationSeconds.formatDuration(),
                            session.session.heartRate?.let { "${it.avgBpm} bpm avg" },
                            if (exerciseCount == 1) "1 exercise" else "$exerciseCount exercises",
                        ).filterNotNull().joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        session.exercises.forEach { exercise ->
                            ExerciseStatsRow(exercise = exercise, sessionCompletedAt = session.session.completedAt)
                        }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = exercise.exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (entry != null) {
            Text(
                text = "${entry.weightKg.formatWeight()} kg",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            EffortBadge(entry.perceivedEffort)
        } else {
            Text(
                text = "No stats recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A small pill showing how hard an exercise felt, tinted by level. */
@Composable
private fun EffortBadge(effort: PerceivedEffort) {
    val effortColors = MaterialTheme.effortColors
    Surface(
        shape = CircleShape,
        color = effortColors.forEffort(effort),
        contentColor = effortColors.onEffort,
    ) {
        Text(
            text = effort.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/** Display label for a [PerceivedEffort] level. */
private val PerceivedEffort.label: String
    get() = when (this) {
        PerceivedEffort.EASY -> "Easy"
        PerceivedEffort.MEDIUM -> "Medium"
        PerceivedEffort.HARD -> "Hard"
    }

/** Formats a weight in kg, trimming a trailing ".0" (e.g. "42.5" or "40"). */
private fun Double.formatWeight(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/** Formats a duration in whole seconds as e.g. "42 min" or "58 sec" for durations under a minute. */
private fun Long.formatDuration(): String {
    val minutes = this / 60
    return if (minutes > 0) "$minutes min" else "$this sec"
}
