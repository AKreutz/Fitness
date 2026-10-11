package com.akreutz.fitness.ui.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * A month-grid calendar that marks every day in [workoutDays] (the days on which at least one
 * workout was started) and lets the user pick one of them via [onDaySelected]. The chevrons page
 * between months; the shown month survives configuration changes.
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
    val month = YearMonth.of((monthIndex / 12).toInt(), (monthIndex % 12).toInt() + 1)

    val locale = Locale.ENGLISH
    val firstDayOfWeek = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek
    val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val rows = (leadingBlanks + daysInMonth + 6) / 7
    val trainingDays = remember(workoutDays, month) { workoutDays.count { YearMonth.from(it) == month } }

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (trainingDays == 1) "1 training day" else "$trainingDays training days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                                val date = month.atDay(day)
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

@Composable
private fun DayCell(
    day: Int,
    worked: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val border = when {
        isSelected -> BorderStroke(2.dp, scheme.onSurface)
        isToday -> BorderStroke(1.5.dp, scheme.primary)
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (worked) Modifier.background(scheme.primary) else Modifier)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (worked) FontWeight.SemiBold else FontWeight.Normal,
            color = if (worked) scheme.onPrimary else scheme.onSurface,
        )
    }
}
