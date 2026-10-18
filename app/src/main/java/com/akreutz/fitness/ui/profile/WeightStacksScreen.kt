package com.akreutz.fitness.ui.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.ui.common.WeightStackDialog
import com.akreutz.fitness.ui.common.pressScale
import com.akreutz.fitness.ui.common.ConfirmDestructiveDialog
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.repository.TrainingPlanRepository

/**
 * Lists every saved [WeightStack] (reusable, named sets of a cable machine's fixed weight levels,
 * assignable to a [com.akreutz.fitness.data.model.ExerciseType.CABLE] exercise). Tapping one edits
 * its name/levels; the trailing delete icon removes it after confirmation, which also clears it
 * from any exercise currently assigned to it (see
 * [TrainingPlanRepository.deleteWeightStack]). The last item starts the flow to create a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightStacksScreen(
    repository: TrainingPlanRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: WeightStacksViewModel = viewModel(
        factory = WeightStacksViewModelFactory(repository),
    )
    val weightStacks by viewModel.weightStacks.collectAsState()
    var stackDialogTarget by remember { mutableStateOf<WeightStack?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var stackPendingDelete by remember { mutableStateOf<WeightStack?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weight stacks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(weightStacks, key = { it.id }) { stack ->
                WeightStackCard(
                    modifier = Modifier.animateItem(),
                    stack = stack,
                    onClick = { stackDialogTarget = stack },
                    onDeleteRequest = { stackPendingDelete = stack },
                )
            }
            item {
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth().pressScale(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Create new weight stack")
                }
            }
        }
    }

    if (showCreateDialog) {
        WeightStackDialog(
            title = "New weight stack",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, levelsKg ->
                viewModel.createWeightStack(name, levelsKg)
                showCreateDialog = false
            },
        )
    }

    val editTarget = stackDialogTarget
    if (editTarget != null) {
        WeightStackDialog(
            title = "Edit weight stack",
            confirmLabel = "Save",
            initialName = editTarget.name,
            initialLevelsKg = editTarget.levelsKg,
            onDismiss = { stackDialogTarget = null },
            onConfirm = { name, levelsKg ->
                viewModel.updateWeightStack(editTarget, name, levelsKg)
                stackDialogTarget = null
            },
        )
    }

    val deleteTarget = stackPendingDelete
    if (deleteTarget != null) {
        ConfirmDestructiveDialog(
            title = "Delete \"${deleteTarget.name}\"?",
            message = "Any exercise using it will fall back to typing its next weight manually.",
            confirmLabel = "Delete stack",
            dismissLabel = "Keep stack",
            onConfirm = {
                viewModel.deleteWeightStack(deleteTarget)
                stackPendingDelete = null
            },
            onDismiss = { stackPendingDelete = null },
        )
    }
}

@Composable
private fun WeightStackCard(
    stack: WeightStack,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onClick),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stack.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${stack.levelsKg.size} levels: " +
                        stack.levelsKg.sorted().joinToString("-") { formatLevel(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete \"${stack.name}\"",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A weight level formatted compactly, e.g. "5" or "5.5" — no trailing zero. */
private fun formatLevel(kg: Double): String =
    if (kg == kg.toLong().toDouble()) kg.toLong().toString() else kg.toString()
