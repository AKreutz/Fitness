package com.akreutz.fitness.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Confirms an action that can't be undone. The irreversible choice ([confirmLabel]) is tinted with
 * the error colors and the reversible one ([dismissLabel]) stays a quiet text button, so the two
 * can't be mistaken for each other. Use verbs that differ from the screen's own "Cancel" button.
 *
 * When [keepIsPrimary] is set, the safe choice is the filled button and the destructive one a
 * red text button, for cases where leaving by accident is the likelier mistake (e.g. discarding
 * an in-progress workout).
 */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = Icons.Filled.Delete,
    keepIsPrimary: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (keepIsPrimary) {
                Button(onClick = onDismiss) { Text(dismissLabel) }
            } else {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            if (keepIsPrimary) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(confirmLabel)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(dismissLabel) }
            }
        },
    )
}
