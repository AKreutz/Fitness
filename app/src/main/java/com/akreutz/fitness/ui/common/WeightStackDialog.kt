package com.akreutz.fitness.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.lbToKg
import com.akreutz.fitness.data.model.roundToTenthKg
import kotlinx.coroutines.launch

/**
 * Prompts for a [com.akreutz.fitness.data.model.WeightStack]'s name and its levels. Levels are
 * entered one at a time (or several at once, separated by commas or spaces) and shown as removable
 * chips, always sorted, so a bad value is flagged as it is typed instead of silently disabling the
 * confirm button. Used both to create a stack (empty [initialLevelsKg]) and to edit one.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeightStackDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    initialLevelsKg: List<Double> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: suspend (name: String, levelsKg: List<Double>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var levels by remember { mutableStateOf(initialLevelsKg.distinct().sorted()) }
    var levelInput by remember { mutableStateOf("") }

    // Typing in lb is input only: levels are converted to kg (to 0.1) as they are added.
    var lbMode by remember { mutableStateOf(false) }

    val tokens = levelInput.split(',', ' ').filter { it.isNotBlank() }
    val parsed = tokens.map { token ->
        token.toDoubleOrNull()?.let { if (lbMode) roundToTenthKg(lbToKg(it)) else it }
    }
    val badToken = tokens.zip(parsed).firstOrNull { (_, value) -> value == null || value <= 0 }?.first
    val inputError = badToken?.let { token ->
        val fixed = token.replace('O', '0').replace('o', '0')
        if (fixed != token && fixed.toDoubleOrNull() != null) {
            "\"$token\" isn't a number. Did you mean $fixed?"
        } else {
            "\"$token\" isn't a valid weight."
        }
    }
    val canAdd = tokens.isNotEmpty() && badToken == null

    fun addInput() {
        if (!canAdd) return
        levels = (levels + parsed.filterNotNull()).distinct().sorted()
        levelInput = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogEnter(),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stack name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Levels (kg) · ${levels.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (levels.isEmpty()) {
                        Text(
                            text = "No levels yet. Add the weights your stack offers, e.g. 5, 10, 15.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // The chips are 40 dp tall (not the default 48 dp touch-target floor) so
                        // wrapped rows sit close together.
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                levels.forEach { level ->
                                    InputChip(
                                        selected = false,
                                        onClick = { levels = levels - level },
                                        label = {
                                            Text(
                                                text = formatStackLevel(level),
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove ${formatStackLevel(level)} kg",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = levelInput,
                        onValueChange = { levelInput = it },
                        label = { Text(if (lbMode) "Add level (lb)" else "Add level (kg)") },
                        placeholder = { Text("20") },
                        trailingIcon = {
                            UnitToggle(
                                lbMode = lbMode,
                                onLbModeChange = { lbMode = it },
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        supportingText = if (lbMode && canAdd) {
                            { Text("= ${parsed.filterNotNull().joinToString(", ") { formatStackLevel(it) }} kg") }
                        } else {
                            null
                        },
                        isError = inputError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { addInput() }),
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = ::addInput,
                        enabled = canAdd,
                        modifier = Modifier
                            .offset(y = 4.dp)
                            .size(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add level")
                    }
                }
                if (inputError != null) {
                    Text(
                        text = inputError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = (-8).dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { coroutineScope.launch { onConfirm(name.trim(), levels) } },
                enabled = name.isNotBlank() && levels.isNotEmpty() && inputError == null,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/** A weight level formatted compactly, e.g. "5" or "5.5": no trailing zero. */
private fun formatStackLevel(kg: Double): String =
    if (kg == kg.toLong().toDouble()) kg.toLong().toString() else kg.toString()
