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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.ui.home.ActiveTrainingPlanUiState
import com.akreutz.fitness.ui.home.ActiveTrainingPlanView
import com.akreutz.fitness.ui.home.HomeViewModel
import com.akreutz.fitness.ui.home.HomeViewModelFactory
import com.akreutz.fitness.ui.home.OnboardingScreen
import com.akreutz.fitness.ui.session.WorkoutSessionScreen
import com.akreutz.fitness.ui.theme.FitnessTheme
import com.akreutz.fitness.ui.workouts.WorkoutsScreen
import com.akreutz.fitness.ui.workouts.WorkoutsViewModel
import com.akreutz.fitness.ui.workouts.WorkoutsViewModelFactory

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
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            OnboardingScreen(repository = application.trainingPlanRepository)
                        }
                    }
                    is ActiveTrainingPlanUiState.Loaded -> FitnessApp(
                        trainingPlan = state,
                        homeViewModel = homeViewModel,
                        trainingPlanRepository = application.trainingPlanRepository,
                    )
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

private const val ROUTE_HOME = "home"
private const val ROUTE_SESSION = "session/{workoutId}"
private const val ARG_WORKOUT_ID = "workoutId"

@Composable
fun FitnessApp(
    trainingPlan: ActiveTrainingPlanUiState.Loaded,
    homeViewModel: HomeViewModel,
    trainingPlanRepository: TrainingPlanRepository,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            LaunchedEffect(Unit) {
                homeViewModel.startWorkoutEvents.collect { workoutId ->
                    navController.navigate("session/$workoutId")
                }
            }
            HomeScreen(
                trainingPlan = trainingPlan,
                homeViewModel = homeViewModel,
                trainingPlanRepository = trainingPlanRepository,
            )
        }
        composable(
            route = ROUTE_SESSION,
            arguments = listOf(navArgument(ARG_WORKOUT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong(ARG_WORKOUT_ID) ?: return@composable
            WorkoutSessionScreen(
                repository = trainingPlanRepository,
                workoutId = workoutId,
                onFinish = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    trainingPlan: ActiveTrainingPlanUiState.Loaded,
    homeViewModel: HomeViewModel,
    trainingPlanRepository: TrainingPlanRepository,
) {
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
    val workoutsViewModel: WorkoutsViewModel = viewModel(
        factory = WorkoutsViewModelFactory(trainingPlanRepository),
    )

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
        floatingActionButton = {
            if (selectedDestination == 0) {
                ExtendedFloatingActionButton(
                    text = { Text("Start workout") },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = { homeViewModel.startNextWorkout() },
                )
            }
        },
    ) { innerPadding ->
        when (selectedDestination) {
            0 -> ActiveTrainingPlanView(
                workouts = trainingPlan.workoutsNextFirst,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> {
                val workoutHistory by workoutsViewModel.workoutHistory.collectAsState()
                WorkoutsScreen(
                    uiState = workoutHistory,
                    modifier = Modifier.padding(innerPadding),
                )
            }
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
