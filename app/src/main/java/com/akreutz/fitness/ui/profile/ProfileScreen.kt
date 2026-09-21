package com.akreutz.fitness.ui.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts

/**
 * Lists every [TrainingPlan] the user has, each with how many workouts it has and how many
 * sessions have been completed under it. Tapping an inactive plan switches to it; the trailing
 * delete icon removes a plan (with its workouts and history) after confirmation. The last item
 * starts the flow to create a new plan.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSelectPlan: (id: Long) -> Unit,
    onEditPlan: (id: Long) -> Unit,
    onDeletePlan: (plan: TrainingPlan) -> Unit,
    onCreatePlan: () -> Unit,
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

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.plans, key = { it.trainingPlan.id }) { plan ->
                    TrainingPlanCard(
                        plan = plan,
                        isActive = plan.trainingPlan.id == uiState.activePlanId,
                        completedSessionCount = uiState.completedSessionCounts[plan.trainingPlan.id]
                            ?: 0,
                        onSelect = { onSelectPlan(plan.trainingPlan.id) },
                        onEdit = { onEditPlan(plan.trainingPlan.id) },
                        onDeleteRequest = { planPendingDelete = plan },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onCreatePlan,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Create new plan")
                    }
                }
            }

            val deleteTarget = planPendingDelete
            if (deleteTarget != null) {
                AlertDialog(
                    onDismissRequest = { planPendingDelete = null },
                    title = { Text("Delete plan") },
                    text = {
                        Text(
                            "Delete \"${deleteTarget.trainingPlan.name}\"? This removes its " +
                                "workouts and completed-session history.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeletePlan(deleteTarget.trainingPlan)
                                planPendingDelete = null
                            },
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { planPendingDelete = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TrainingPlanCard(
    plan: TrainingPlanWithWorkouts,
    isActive: Boolean,
    completedSessionCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive, onClick = onSelect),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.trainingPlan.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Active plan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(18.dp),
                        )
                    }
                }
                Text(
                    text = "${plan.workouts.size} workouts • $completedSessionCount completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit \"${plan.trainingPlan.name}\"",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete \"${plan.trainingPlan.name}\"",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
