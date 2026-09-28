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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.launch

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
                    stack = stack,
                    onClick = { stackDialogTarget = stack },
                    onDeleteRequest = { stackPendingDelete = stack },
                )
            }
            item {
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Create new weight stack")
                }
            }
        }
    }

    if (showCreateDialog) {
        WeightStackNameLevelsDialog(
            title = "New weight stack",
            initialName = "",
            initialLevelsKg = emptyList(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, levelsKg ->
                viewModel.createWeightStack(name, levelsKg)
                showCreateDialog = false
            },
        )
    }

    val editTarget = stackDialogTarget
    if (editTarget != null) {
        WeightStackNameLevelsDialog(
            title = "Edit weight stack",
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
        AlertDialog(
            onDismissRequest = { stackPendingDelete = null },
            title = { Text("Delete weight stack") },
            text = {
                Text(
                    "Delete \"${deleteTarget.name}\"? Any exercise using it will fall back to " +
                        "typing its next weight manually.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWeightStack(deleteTarget)
                        stackPendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stackPendingDelete = null }) {
                    Text("Cancel")
                }
            },
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

/** Prompts for a [WeightStack]'s name and its levels, typed as comma-separated kg values. */
@Composable
private fun WeightStackNameLevelsDialog(
    title: String,
    initialName: String,
    initialLevelsKg: List<Double>,
    onDismiss: () -> Unit,
    onConfirm: suspend (name: String, levelsKg: List<Double>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var levelsText by remember {
        mutableStateOf(initialLevelsKg.joinToString(", ") { it.toString() })
    }

    val levelTokens = levelsText.split(",")
        .map { it.trim().replace(',', '.') }
        .filter { it.isNotEmpty() }
        .map { it.toDoubleOrNull() }
    val parsedLevels = levelTokens.filterNotNull()
    val canConfirm = name.isNotBlank() && levelTokens.isNotEmpty() &&
        levelTokens.size == parsedLevels.size && parsedLevels.all { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stack name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = levelsText,
                    onValueChange = { levelsText = it },
                    label = { Text("Weight levels (kg), comma-separated") },
                    placeholder = { Text("5, 10, 15, 20") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { coroutineScope.launch { onConfirm(name, parsedLevels) } },
                enabled = canConfirm,
            ) {
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
