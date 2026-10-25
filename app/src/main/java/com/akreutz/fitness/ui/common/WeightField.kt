package com.akreutz.fitness.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akreutz.fitness.data.model.formatWeight
import com.akreutz.fitness.data.model.kgToLb
import com.akreutz.fitness.data.model.lbToKg

/**
 * An editable weight shown as a large number followed by a tappable "kg" / "lb" unit. The field is
 * sized to its text so the number and unit read as one centered unit.
 *
 * [kgText] and [onKgTextChange] are always in kg. While [lbMode] is on, the field shows what was
 * typed in lb and reports each edit converted to kg and passed through [roundKg] (e.g. snapped to
 * the weights the plates can make), so the caller only ever sees reachable kg values. Changes to
 * [kgText] from elsewhere, like a stepper, are mirrored back into the lb text.
 */
@Composable
fun WeightField(
    kgText: String,
    onKgTextChange: (String) -> Unit,
    lbMode: Boolean,
    onLbModeChange: (Boolean) -> Unit,
    roundKg: (Double) -> Double,
    style: TextStyle,
    unitBottomPadding: Dp,
    placeholder: String = "0",
) {
    var lbText by remember { mutableStateOf("") }
    val centeredStyle = style.copy(textAlign = TextAlign.Center)

    // Keep the lb text in step with kg values set by the steppers, chips or the mode switch.
    LaunchedEffect(kgText, lbMode) {
        val kg = kgText.replace(',', '.').toDoubleOrNull()
        if (lbMode && kg != null && lbTextToKg(lbText, roundKg) != kg) {
            lbText = formatWeight(kgToLb(kg).round1())
        }
    }

    val shown = if (lbMode) lbText else kgText
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // An empty field still gets room for the placeholder "0" so it stays tappable.
    val fieldWidth = with(density) {
        textMeasurer.measure(shown.ifEmpty { placeholder }, centeredStyle).size.width.toDp()
    } + 4.dp
    Row(verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = shown,
            onValueChange = { text ->
                if (lbMode) {
                    lbText = text
                    // Unparseable text goes through as is so the caller sees it as invalid.
                    onKgTextChange(lbTextToKg(text, roundKg)?.let(::formatWeight) ?: text)
                } else {
                    onKgTextChange(text)
                }
            },
            textStyle = centeredStyle,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (shown.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = centeredStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.width(fieldWidth),
        )
        UnitToggle(
            lbMode = lbMode,
            onLbModeChange = onLbModeChange,
            modifier = Modifier.padding(start = 4.dp, bottom = unitBottomPadding),
        )
    }
}

/** A small tappable "kg" / "lb" label that flips the entry unit. */
@Composable
fun UnitToggle(lbMode: Boolean, onLbModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (lbMode) "lb" else "kg",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onLbModeChange(!lbMode) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** [text] read as lb, converted to kg and passed through [roundKg], or null if it isn't a number. */
fun lbTextToKg(text: String, roundKg: (Double) -> Double): Double? =
    text.replace(',', '.').toDoubleOrNull()?.let { roundKg(lbToKg(it)) }

private fun Double.round1(): Double = Math.round(this * 10) / 10.0
