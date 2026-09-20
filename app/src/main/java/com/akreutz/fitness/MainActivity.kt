package com.akreutz.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.fitness.ui.home.ActiveTrainingPlanUiState
import com.akreutz.fitness.ui.home.ActiveTrainingPlanView
import com.akreutz.fitness.ui.home.CreateTrainingPlanScreen
import com.akreutz.fitness.ui.home.CreateTrainingPlanViewModel
import com.akreutz.fitness.ui.home.CreateTrainingPlanViewModelFactory
import com.akreutz.fitness.ui.home.HomeViewModel
import com.akreutz.fitness.ui.home.HomeViewModelFactory
import com.akreutz.fitness.ui.theme.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The app is always dark-themed, so the status/navigation bar icons (time, battery,
        // back/home) should always be light (white/light gray) to stay readable, regardless of
        // the system's own light/dark setting.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            FitnessTheme {
                val application = applicationContext as FitnessApplication
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(application.trainingPlanRepository),
                )
                val activeTrainingPlanState by homeViewModel.activeTrainingPlan.collectAsState()

                when (val state = activeTrainingPlanState) {
                    is ActiveTrainingPlanUiState.Loading -> LoadingScreen()
                    is ActiveTrainingPlanUiState.NoPlan -> {
                        val createViewModel: CreateTrainingPlanViewModel = viewModel(
                            factory = CreateTrainingPlanViewModelFactory(application.trainingPlanRepository),
                        )
                        CreateTrainingPlanScreen(
                            onCreate = { name, workoutNames ->
                                createViewModel.createTrainingPlan(name, workoutNames)
                            },
                        )
                    }
                    is ActiveTrainingPlanUiState.Loaded -> FitnessApp(trainingPlan = state)
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** A bottom-navigation destination, per Material Design guidance (3-5 top-level destinations). */
internal data class FitnessDestination(
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    FitnessDestination("Home", Icons.Filled.Home),
    FitnessDestination("Workouts", Icons.AutoMirrored.Filled.DirectionsRun),
    FitnessDestination("Progress", Icons.AutoMirrored.Filled.ShowChart),
    FitnessDestination("Profile", Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessApp(trainingPlan: ActiveTrainingPlanUiState.Loaded) {
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(destinations[selectedDestination].label) },
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = { selectedDestination = index },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedDestination) {
            0 -> ActiveTrainingPlanView(
                trainingPlan = trainingPlan.trainingPlan,
                modifier = Modifier.padding(innerPadding),
            )
            else -> PlaceholderScreen(
                destination = destinations[selectedDestination],
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
internal fun PlaceholderScreen(destination: FitnessDestination, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "${destination.label} coming soon",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateTrainingPlanScreenPreview() {
    FitnessTheme {
        CreateTrainingPlanScreen(onCreate = { _, _ -> })
    }
}
