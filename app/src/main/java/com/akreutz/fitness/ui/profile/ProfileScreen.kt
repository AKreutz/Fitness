package com.akreutz.fitness.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akreutz.fitness.ui.common.ConfirmDestructiveDialog
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Shows the active [TrainingPlan] as a prominent card, followed by the other plans, each with how
 * many workouts it has and how many sessions have been completed under it. Tapping an inactive
 * plan switches to it; each plan's overflow menu offers edit, export and delete (the latter after
 * confirmation, removing the plan's workouts and history). Create, import and the weight stacks
 * editor sit below.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSelectPlan: (id: String) -> Unit,
    onEditPlan: (id: String) -> Unit,
    onDeletePlan: (plan: TrainingPlan) -> Unit,
    onCreatePlan: () -> Unit,
    onImportPlan: () -> Unit,
    onExportPlan: (plan: TrainingPlan) -> Unit,
    onEditWeightStacks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is ProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProfileUiState.Loaded -> {
            var planPendingDelete by remember { mutableStateOf<TrainingPlanWithWorkouts?>(null) }

            val activePlan = uiState.plans.firstOrNull { it.trainingPlan.id == uiState.activePlanId }
            val otherPlans = uiState.plans.filter { it !== activePlan }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activePlan != null) {
                    item(key = "active-${activePlan.trainingPlan.id}") {
                        ActivePlanCard(
                            plan = activePlan,
                            completedSessionCount =
                                uiState.completedSessionCounts[activePlan.trainingPlan.id] ?: 0,
                            lastTrainedAt = uiState.lastTrainedAt[activePlan.trainingPlan.id],
                            onEdit = { onEditPlan(activePlan.trainingPlan.id) },
                            onExport = { onExportPlan(activePlan.trainingPlan) },
                            onDeleteRequest = { planPendingDelete = activePlan },
                        )
                    }
                }
                item(key = "plans") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(
                            when {
                                otherPlans.isNotEmpty() && activePlan != null -> "Other plans"
                                otherPlans.isNotEmpty() -> "Plans"
                                else -> "Add a plan"
                            },
                        )
                        SectionCard {
                            otherPlans.forEach { plan ->
                                PlanRow(
                                    plan = plan,
                                    completedSessionCount =
                                        uiState.completedSessionCounts[plan.trainingPlan.id] ?: 0,
                                    onSelect = { onSelectPlan(plan.trainingPlan.id) },
                                    onEdit = { onEditPlan(plan.trainingPlan.id) },
                                    onExport = { onExportPlan(plan.trainingPlan) },
                                    onDeleteRequest = { planPendingDelete = plan },
                                )
                                HorizontalDivider()
                            }
                            ActionRow(
                                icon = Icons.Filled.Add,
                                label = "Create new plan",
                                onClick = onCreatePlan,
                            )
                            HorizontalDivider()
                            ActionRow(
                                icon = Icons.Filled.FileDownload,
                                label = "Import plan from file",
                                onClick = onImportPlan,
                            )
                        }
                    }
                }
                item(key = "equipment") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Equipment")
                        SectionCard {
                            ActionRow(
                                icon = Icons.Filled.FitnessCenter,
                                label = "Weight stacks",
                                supporting = "Machines and plate sizes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = onEditWeightStacks,
                            )
                        }
                    }
                }
            }

            val deleteTarget = planPendingDelete
            if (deleteTarget != null) {
                val workoutCount = deleteTarget.workouts.size
                val sessionCount = uiState.completedSessionCounts[deleteTarget.trainingPlan.id] ?: 0
                ConfirmDestructiveDialog(
                    title = "Delete \"${deleteTarget.trainingPlan.name}\"?",
                    message = "This permanently removes " +
                        "$workoutCount ${if (workoutCount == 1) "workout" else "workouts"} and " +
                        "$sessionCount completed ${if (sessionCount == 1) "session" else "sessions"}. " +
                        "This can't be undone.",
                    confirmLabel = "Delete plan",
                    dismissLabel = "Keep plan",
                    onConfirm = {
                        onDeletePlan(deleteTarget.trainingPlan)
                        planPendingDelete = null
                    },
                    onDismiss = { planPendingDelete = null },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        content = content,
    )
}

/** The plan in use, shown as a prominent card with Edit and Export; Delete lives in its menu. */
@Composable
private fun ActivePlanCard(
    plan: TrainingPlanWithWorkouts,
    completedSessionCount: Int,
    lastTrainedAt: Instant?,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
        ) {
            // No spacing below the header: its 48dp menu button already leaves a gap under the pill.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = "ACTIVE PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                PlanMenuButton(
                    planName = plan.trainingPlan.name,
                    onSelect = null,
                    onEdit = onEdit,
                    onExport = onExport,
                    onDeleteRequest = onDeleteRequest,
                )
            }
            Column(
                modifier = Modifier.padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = plan.trainingPlan.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val exerciseCount = plan.workouts.sumOf { it.exercises.size }
                Text(
                    text = "${pluralize(plan.workouts.size, "workout")} • " +
                        "${pluralize(exerciseCount, "exercise")} • " +
                        "${pluralize(completedSessionCount, "session")} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = lastTrainedLabel(lastTrainedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Edit plan")
                }
                OutlinedButton(
                    onClick = onExport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                    ),
                ) {
                    Text("Export")
                }
            }
        }
    }
}

/** An inactive plan. Tapping the row switches to it; the overflow menu holds the other actions. */
@Composable
private fun PlanRow(
    plan: TrainingPlanWithWorkouts,
    completedSessionCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = plan.trainingPlan.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = planSummary(plan, completedSessionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PlanMenuButton(
            planName = plan.trainingPlan.name,
            onSelect = onSelect,
            onEdit = onEdit,
            onExport = onExport,
            onDeleteRequest = onDeleteRequest,
        )
    }
}

/** Overflow button with the per-plan actions. [onSelect] is null for the already-active plan. */
@Composable
private fun PlanMenuButton(
    planName: String,
    onSelect: (() -> Unit)?,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Actions for \"$planName\"")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onSelect != null) {
                PlanMenuItem("Switch to this plan", Icons.Filled.SwapHoriz) {
                    expanded = false
                    onSelect()
                }
            }
            PlanMenuItem("Edit plan", Icons.Filled.Edit) {
                expanded = false
                onEdit()
            }
            PlanMenuItem("Export to file", Icons.Filled.FileUpload) {
                expanded = false
                onExport()
            }
            PlanMenuItem(
                label = "Delete plan",
                icon = Icons.Filled.Delete,
                tint = MaterialTheme.colorScheme.error,
            ) {
                expanded = false
                onDeleteRequest()
            }
        }
    }
}

@Composable
private fun PlanMenuItem(
    label: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (tint == MaterialTheme.colorScheme.error) tint else Color.Unspecified,
            )
        },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint) },
        onClick = onClick,
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (supporting == null) tint else MaterialTheme.colorScheme.onSurface,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun planSummary(plan: TrainingPlanWithWorkouts, completedSessionCount: Int): String =
    "${pluralize(plan.workouts.size, "workout")} • $completedSessionCount completed"

private fun pluralize(count: Int, noun: String): String =
    "$count ${if (count == 1) noun else "${noun}s"}"

/** "Last trained today / yesterday / 3 days ago / 2 weeks ago", by calendar day, not by 24 hours. */
private fun lastTrainedLabel(lastTrainedAt: Instant?): String {
    if (lastTrainedAt == null) return "Not trained yet"
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(
        lastTrainedAt.atZone(zone).toLocalDate(),
        LocalDate.now(zone),
    ).toInt().coerceAtLeast(0)
    val whenText = when {
        days == 0 -> "today"
        days == 1 -> "yesterday"
        days < 14 -> "$days days ago"
        days < 60 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
    return "Last trained $whenText"
}
