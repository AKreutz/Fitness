package com.akreutz.fitness.ui.workouts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.ui.common.pressScale
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val MONTH_TRANSITION_MILLIS = 300
private const val MARKER_POP_STAGGER_MILLIS = 15L
private const val DAY_PRESSED_SCALE = 0.88f

/**
 * A month-grid calendar that marks every day in [workoutDays] (the days on which at least one
 * workout was started) and lets the user pick one of them via [onDaySelected]. The chevrons page
 * between months, sliding the grid in the direction of travel with the workout-day markers
 * popping in; the shown month survives configuration changes.
 *
 * @param selectedDay the currently selected day, if any; tapping it again reports `null`.
 */
@Composable
fun WorkoutCalendar(
    workoutDays: Set<LocalDate>,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now(ZoneId.systemDefault())
    var monthIndex by rememberSaveable { mutableStateOf(today.year * 12L + today.monthValue - 1) }

    val locale = Locale.ENGLISH
    val firstDayOfWeek = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek

    // Forward months come in from the right, earlier ones from the left.
    val monthTransition: AnimatedContentTransitionScope<Long>.() -> ContentTransform = {
        val direction = if (targetState > initialState) 1 else -1
        (slideInHorizontally(tween(MONTH_TRANSITION_MILLIS)) { direction * it / 3 } +
            fadeIn(tween(MONTH_TRANSITION_MILLIS))) togetherWith
            (slideOutHorizontally(tween(MONTH_TRANSITION_MILLIS)) { -direction * it / 3 } +
                fadeOut(tween(MONTH_TRANSITION_MILLIS / 2))) using
            SizeTransform(clip = true)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthIndex -= 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                AnimatedContent(
                    targetState = monthIndex,
                    transitionSpec = monthTransition,
                    label = "calendarTitle",
                ) { shownIndex ->
                    val shownMonth = monthOf(shownIndex)
                    val trainingDays = remember(workoutDays, shownMonth) {
                        workoutDays.count { YearMonth.from(it) == shownMonth }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = shownMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (trainingDays == 1) "1 training day" else "$trainingDays training days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { monthIndex += 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    Text(
                        text = firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedContent(
                targetState = monthIndex,
                transitionSpec = monthTransition,
                label = "calendarGrid",
            ) { shownIndex ->
                val shownMonth = monthOf(shownIndex)
                val leadingBlanks = (shownMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
                val daysInMonth = shownMonth.lengthOfMonth()
                val rows = (leadingBlanks + daysInMonth + 6) / 7
                Column {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val day = row * 7 + col - leadingBlanks + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (day in 1..daysInMonth) {
                                        val date = shownMonth.atDay(day)
                                        val worked = date in workoutDays
                                        DayCell(
                                            day = day,
                                            worked = worked,
                                            isToday = date == today,
                                            isSelected = date == selectedDay,
                                            onClick = if (worked) {
                                                { onDaySelected(if (date == selectedDay) null else date) }
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The month for an index of `year * 12 + (month - 1)`, as kept by [WorkoutCalendar]. */
private fun monthOf(monthIndex: Long): YearMonth =
    YearMonth.of((monthIndex / 12).toInt(), (monthIndex % 12).toInt() + 1)

@Composable
private fun DayCell(
    day: Int,
    worked: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = when {
            isSelected -> scheme.primary
            worked -> scheme.primaryContainer
            else -> Color.Transparent
        },
        label = "dayBackground",
    )
    val contentColor = when {
        isSelected -> scheme.onPrimary
        worked -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    // Workout-day markers pop in, a little later for each day, when their month appears.
    val markerScale = remember { Animatable(if (worked) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (worked) {
            delay(day * MARKER_POP_STAGGER_MILLIS)
            markerScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
    }
    val border = if (isToday && !isSelected) BorderStroke(1.5.dp, scheme.primary) else null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (onClick != null) Modifier.pressScale(DAY_PRESSED_SCALE) else Modifier)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(color = background, radius = size.minDimension / 2f * markerScale.value)
            }
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (worked) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
