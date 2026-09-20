package com.akreutz.fitness.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The mandatory first-run screen: the user names their Training Plan and adds one or more
 * named Workouts before they can use the rest of the app. Exercises are added later, from the
 * plan/workout's own screen.
 */
@Composable
fun CreateTrainingPlanScreen(
    onCreate: (name: String, workoutNames: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var planName by remember { mutableStateOf("") }
    val workoutNames = remember { mutableStateListOf("") }

    val canCreate = planName.isNotBlank() && workoutNames.any { it.isNotBlank() }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Create your training plan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        Text(
            text = "Give it a name and add the workouts it's made up of. You can add exercises " +
                "to each workout later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        OutlinedTextField(
            value = planName,
            onValueChange = { planName = it },
            label = { Text("Plan name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        Text(
            text = "Workouts",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(workoutNames.size) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = workoutNames[index],
                        onValueChange = { workoutNames[index] = it },
                        label = { Text("Workout ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (workoutNames.size > 1) {
                        IconButton(onClick = { workoutNames.removeAt(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove workout")
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { workoutNames.add("") }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add workout")
                }
            }
        }

        Button(
            onClick = { onCreate(planName, workoutNames.toList()) },
            enabled = canCreate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Create plan")
        }
    }
}
