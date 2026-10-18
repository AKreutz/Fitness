package com.akreutz.fitness.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.model.WeightStack
import java.util.Locale

/** How much the weight and rest steppers move per tap. */
private const val WEIGHT_STEP_KG = 2.5
private const val INCREMENT_STEP_KG = 1.25
private const val REST_STEP_SECONDS = 15

/**
 * Full-screen form for the fields needed to add or edit a
 * [com.akreutz.fitness.data.model.Exercise]. Used from the onboarding flow's add-exercises step
 * and from the plan editor. When [initial] is non-null, the fields are prefilled from it and the
 * form behaves as an edit rather than an add. A full screen (instead of a dialog) keeps the
 * confirm button visible above the keyboard, and sets, weight and rest are steppers and chips so
 * only the name and, optionally, exact weights need typing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var sets by remember { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by remember { mutableStateOf(initial?.reps ?: RepScheme.options.first()) }
    var weightText by remember { mutableStateOf(initial?.weightKg?.let(::formatKg) ?: "") }
    var weightIncrementText by remember {
        mutableStateOf(initial?.weightIncrementKg?.let(::formatKg) ?: "")
    }
    var weightStackId by remember { mutableStateOf(initial?.weightStackId) }
    var weightStackMenuExpanded by remember { mutableStateOf(false) }
    var showCreateWeightStack by remember { mutableStateOf(false) }
    var restSeconds by remember {
        mutableIntStateOf(initial?.restSeconds ?: Exercise.DEFAULT_REST_SECONDS)
    }

    val weightKg = weightText.replace(',', '.').toDoubleOrNull()
    val weightIncrementKg = if (type == ExerciseType.CABLE) {
        0.0
    } else {
        weightIncrementText.replace(',', '.').toDoubleOrNull()
    }
    val canConfirm = name.isNotBlank() && sets > 0 &&
        weightKg != null && weightKg >= 0 && weightIncrementKg != null && weightIncrementKg >= 0 &&
        restSeconds > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.dialogEnter().fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    title = { Text(if (initial == null) "Add exercise" else "Edit exercise") },
                    actions = {
                        Button(
                            onClick = {
                                onConfirm(
                                    name.trim(),
                                    type,
                                    sets,
                                    reps,
                                    weightKg!!,
                                    weightIncrementKg!!,
                                    weightStackId,
                                    restSeconds,
                                )
                            },
                            enabled = canConfirm,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(if (initial == null) "Add" else "Save")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                FormSection("Equipment") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ExerciseType.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = type == option,
                                onClick = { type = option },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ExerciseType.entries.size,
                                ),
                            ) {
                                Text(option.label())
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormSection("Sets") {
                            StepperRow(
                                onMinus = { if (sets > 1) sets-- },
                                onPlus = { sets++ },
                                minusDescription = "Fewer sets",
                                plusDescription = "More sets",
                            ) {
                                Text(
                                    text = sets.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FormSection("Reps per set") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(52.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                RepScheme.options.forEach { option ->
                                    FilterChip(
                                        selected = reps == option,
                                        onClick = { reps = option },
                                        label = { Text(RepScheme.format(option)) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Free weights also get a second stepper for how much to add after two easy
                // sessions; cable weights follow their stack's levels instead.
                val showIncrement = type == ExerciseType.FREE_WEIGHTS
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormSection("Starting weight") {
                            StepperRow(
                                onMinus = {
                                    weightText = formatKg(
                                        ((weightKg ?: 0.0) - WEIGHT_STEP_KG).coerceAtLeast(0.0),
                                    )
                                },
                                onPlus = { weightText = formatKg((weightKg ?: 0.0) + WEIGHT_STEP_KG) },
                                minusDescription = "Lighter by ${formatKg(WEIGHT_STEP_KG)} kg",
                                plusDescription = "Heavier by ${formatKg(WEIGHT_STEP_KG)} kg",
                            ) {
                                KgField(
                                    text = weightText,
                                    onTextChange = { weightText = it },
                                    compact = true,
                                )
                            }
                        }
                    }
                    if (showIncrement) {
                        Box(modifier = Modifier.weight(1f)) {
                            FormSection("Increment") {
                                StepperRow(
                                    onMinus = {
                                        weightIncrementText = formatKg(
                                            ((weightIncrementKg ?: 0.0) - INCREMENT_STEP_KG)
                                                .coerceAtLeast(0.0),
                                        )
                                    },
                                    onPlus = {
                                        weightIncrementText =
                                            formatKg((weightIncrementKg ?: 0.0) + INCREMENT_STEP_KG)
                                    },
                                    minusDescription = "Smaller increase",
                                    plusDescription = "Bigger increase",
                                ) {
                                    KgField(
                                        text = weightIncrementText,
                                        onTextChange = { weightIncrementText = it },
                                        compact = true,
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            FormSection("Weight stack") {
                                val selectedStackName = weightStacks
                                    .firstOrNull { it.id == weightStackId }
                                    ?.name
                                    ?: "None"
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                            .clickable { weightStackMenuExpanded = true }
                                            .padding(start = 16.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = selectedStackName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(
                                            Icons.Filled.ArrowDropDown,
                                            contentDescription = "Choose weight stack",
                                        )
                                    }
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
                        }
                    }
                }

                FormSection("Rest between sets") {
                    StepperRow(
                        onMinus = {
                            restSeconds = (restSeconds - REST_STEP_SECONDS).coerceAtLeast(REST_STEP_SECONDS)
                        },
                        onPlus = { restSeconds += REST_STEP_SECONDS },
                        minusDescription = "Shorter rest",
                        plusDescription = "Longer rest",
                    ) {
                        Text(
                            text = "$restSeconds s",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (showCreateWeightStack) {
        WeightStackDialog(
            title = "New weight stack",
            confirmLabel = "Create",
            onConfirm = { stackName, levelsKg ->
                val stack = onCreateWeightStack(stackName, levelsKg)
                weightStackId = stack.id
                showCreateWeightStack = false
            },
            onDismiss = { showCreateWeightStack = false },
        )
    }
}

/** A labeled group of controls in [AddExerciseDialog]. */
@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A minus button, a centered [content] value, and a plus button in one rounded row. */
@Composable
private fun StepperRow(
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusDescription: String,
    plusDescription: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onMinus) {
            Icon(Icons.Filled.Remove, contentDescription = minusDescription)
        }
        content()
        FilledTonalIconButton(onClick = onPlus) {
            Icon(Icons.Filled.Add, contentDescription = plusDescription)
        }
    }
}

/**
 * An editable weight shown as a large number followed by "kg". The field is sized to its text so
 * the number and unit read as one centered unit.
 */
@Composable
private fun KgField(text: String, onTextChange: (String) -> Unit, compact: Boolean = false) {
    val style = (if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall).copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // An empty field still gets room for the placeholder "0" so it stays tappable.
    val fieldWidth = with(density) {
        textMeasurer.measure(text.ifEmpty { "0" }, style).size.width.toDp()
    } + 4.dp
    Row(verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.width(fieldWidth),
        )
        Text(
            text = " kg",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** A weight with up to two decimals and no trailing zeros, e.g. "60", "62.5" or "1.25". */
private fun formatKg(kg: Double): String =
    String.format(Locale.US, "%.2f", kg).trimEnd('0').trimEnd('.')

fun ExerciseType.label(): String = when (this) {
    ExerciseType.FREE_WEIGHTS -> "Free weights"
    ExerciseType.CABLE -> "Cable"
}
