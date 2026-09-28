package com.akreutz.fitness.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.WeightStack
import kotlinx.coroutines.launch

/**
 * Prompts the user for the fields needed to add or edit a
 * [com.akreutz.fitness.data.model.Exercise]. Used from the onboarding flow's add-exercises step
 * and from the plan editor. When [initial] is non-null, the fields are prefilled from it and the
 * dialog behaves as an edit rather than an add.
 */
@Composable
fun AddExerciseDialog(
    initial: DraftExercise? = null,
    weightStacks: List<WeightStack> = emptyList(),
    onCreateWeightStack: suspend (name: String, levelsKg: List<Double>) -> WeightStack = { _, _ ->
        error("onCreateWeightStack not provided")
    },
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        weightStackId: String?,
        restSeconds: Int,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: ExerciseType.FREE_WEIGHTS) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var setsText by remember { mutableStateOf((initial?.sets ?: 3).toString()) }
    var reps by remember { mutableStateOf(initial?.reps ?: RepScheme.options.first()) }
    var repsMenuExpanded by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var weightIncrementText by remember {
        mutableStateOf(initial?.weightIncrementKg?.toString() ?: "")
    }
    var weightStackId by remember { mutableStateOf(initial?.weightStackId) }
    var weightStackMenuExpanded by remember { mutableStateOf(false) }
    var showCreateWeightStack by remember { mutableStateOf(false) }
    var restSecondsText by remember {
        mutableStateOf((initial?.restSeconds ?: Exercise.DEFAULT_REST_SECONDS).toString())
    }

    val sets = setsText.toIntOrNull()
    val weightKg = weightText.replace(',', '.').toDoubleOrNull()
    val weightIncrementKg = if (type == ExerciseType.CABLE) {
        0.0
    } else {
        weightIncrementText.replace(',', '.').toDoubleOrNull()
    }
    val restSeconds = restSecondsText.toIntOrNull()
    val canConfirm = name.isNotBlank() && sets != null && sets > 0 &&
        weightKg != null && weightKg >= 0 && weightIncrementKg != null && weightIncrementKg >= 0 &&
        restSeconds != null && restSeconds > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add exercise" else "Edit exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment type") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .clickable { typeMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        ExerciseType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label()) },
                                onClick = {
                                    type = option
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it },
                        label = { Text("Sets") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = RepScheme.format(reps),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reps") },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { repsMenuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = repsMenuExpanded,
                            onDismissRequest = { repsMenuExpanded = false },
                        ) {
                            RepScheme.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(RepScheme.format(option)) },
                                    onClick = {
                                        reps = option
                                        repsMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Starting weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == ExerciseType.CABLE) {
                    val selectedStackName = weightStacks
                        .firstOrNull { it.id == weightStackId }
                        ?.name
                        ?: "None"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStackName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Weight stack") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matchParentSize()
                                .clickable { weightStackMenuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = weightStackMenuExpanded,
                            onDismissRequest = { weightStackMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    weightStackId = null
                                    weightStackMenuExpanded = false
                                },
                            )
                            weightStacks.forEach { stack ->
                                DropdownMenuItem(
                                    text = { Text(stack.name) },
                                    onClick = {
                                        weightStackId = stack.id
                                        weightStackMenuExpanded = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create new stack…") },
                                onClick = {
                                    weightStackMenuExpanded = false
                                    showCreateWeightStack = true
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = restSecondsText,
                    onValueChange = { restSecondsText = it },
                    label = { Text("Rest between sets (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        type,
                        sets!!,
                        reps,
                        weightKg!!,
                        weightIncrementKg!!,
                        weightStackId,
                        restSeconds!!,
                    )
                },
                enabled = canConfirm,
            ) {
                Text(if (initial == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showCreateWeightStack) {
        CreateWeightStackDialog(
            onCreate = { stackName, levelsKg ->
                val stack = onCreateWeightStack(stackName, levelsKg)
                weightStackId = stack.id
                showCreateWeightStack = false
            },
            onDismiss = { showCreateWeightStack = false },
        )
    }
}

/**
 * Prompts for a new [WeightStack]'s name and its levels, typed as comma-separated kg values (e.g.
 * "5, 10, 15, 20"). [onCreate] persists it via
 * [com.akreutz.fitness.data.repository.TrainingPlanRepository.createWeightStack].
 */
@Composable
private fun CreateWeightStackDialog(
    onCreate: suspend (name: String, levelsKg: List<Double>) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var levelsText by remember { mutableStateOf("") }

    val levelsKg = levelsText.split(",")
        .map { it.trim().replace(',', '.') }
        .filter { it.isNotEmpty() }
        .map { it.toDoubleOrNull() }
    val parsedLevels = levelsKg.filterNotNull()
    val canCreate = name.isNotBlank() && levelsKg.isNotEmpty() &&
        levelsKg.size == parsedLevels.size && parsedLevels.all { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New weight stack") },
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
                onClick = { coroutineScope.launch { onCreate(name, parsedLevels) } },
                enabled = canCreate,
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

fun ExerciseType.label(): String = when (this) {
    ExerciseType.FREE_WEIGHTS -> "Free weights"
    ExerciseType.CABLE -> "Cable"
}
