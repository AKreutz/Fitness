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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme

/**
 * Prompts the user for the fields needed to add or edit a
 * [com.akreutz.fitness.data.model.Exercise]. Used from the onboarding flow's add-exercises step
 * and from the plan editor. When [initial] is non-null, the fields are prefilled from it and the
 * dialog behaves as an edit rather than an add.
 */
@Composable
fun AddExerciseDialog(
    initial: DraftExercise? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: ExerciseType.FREE_WEIGHTS) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var setsText by remember { mutableStateOf(initial?.sets?.toString() ?: "") }
    var reps by remember { mutableStateOf(initial?.reps ?: RepScheme.options.first()) }
    var repsMenuExpanded by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var weightIncrementText by remember {
        mutableStateOf(initial?.weightIncrementKg?.toString() ?: "")
    }

    val sets = setsText.toIntOrNull()
    val weightKg = weightText.replace(',', '.').toDoubleOrNull()
    val weightIncrementKg = weightIncrementText.replace(',', '.').toDoubleOrNull()
    val canConfirm = name.isNotBlank() && sets != null && sets > 0 &&
        weightKg != null && weightKg >= 0 && weightIncrementKg != null && weightIncrementKg >= 0

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
                OutlinedTextField(
                    value = weightIncrementText,
                    onValueChange = { weightIncrementText = it },
                    label = { Text("Weight increment (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, type, sets!!, reps, weightKg!!, weightIncrementKg!!) },
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
}

fun ExerciseType.label(): String = when (this) {
    ExerciseType.FREE_WEIGHTS -> "Free weights"
    ExerciseType.CABLE -> "Cable"
}
