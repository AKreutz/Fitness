package com.akreutz.fitness

import android.app.Application
import androidx.room.Room
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.RepScheme
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Holds this app's singleton dependencies (database, repositories). No DI framework — just
 * lazily-built singletons handed out from here.
 */
class FitnessApplication : Application() {

    private val database: FitnessDatabase by lazy {
        Room.databaseBuilder(this, FitnessDatabase::class.java, FitnessDatabase.DATABASE_NAME)
            // No migrations exist yet; the app is early enough in development that resetting
            // local data on a schema change is acceptable.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val activePlanPreferences: ActivePlanPreferences by lazy {
        ActivePlanPreferences(this)
    }

    val trainingPlanRepository: TrainingPlanRepository by lazy {
        TrainingPlanRepository(database, activePlanPreferences)
    }

    override fun onCreate() {
        super.onCreate()
        // TODO: hardcoded for now, until there's a real onboarding-skip/import path. Runs
        // synchronously so the UI never observes the pre-seed empty state on a clean install.
        runBlocking {
            if (activePlanPreferences.activeTrainingPlanId.first() == null) {
                trainingPlanRepository.createTrainingPlan(
                    name = "Push Pull Legs",
                    workouts = listOf(
                        DraftWorkout(
                            name = "Push",
                            exercises = listOf(
                                DraftExercise(
                                    name = "Bench Press",
                                    type = ExerciseType.FREE_WEIGHTS,
                                    sets = 3,
                                    reps = RepScheme.options.first(),
                                    weightKg = 40.0,
                                    weightIncrementKg = 2.5,
                                    restSeconds = 90,
                                ),
                            ),
                        ),
                        DraftWorkout(
                            name = "Pull",
                            exercises = listOf(
                                DraftExercise(
                                    name = "Lat Pulldown",
                                    type = ExerciseType.CABLE,
                                    sets = 3,
                                    reps = RepScheme.options.first(),
                                    weightKg = 35.0,
                                    weightIncrementKg = 2.5,
                                    restSeconds = 90,
                                ),
                            ),
                        ),
                        DraftWorkout(
                            name = "Legs",
                            exercises = listOf(
                                DraftExercise(
                                    name = "Squat",
                                    type = ExerciseType.FREE_WEIGHTS,
                                    sets = 3,
                                    reps = RepScheme.options.first(),
                                    weightKg = 50.0,
                                    weightIncrementKg = 5.0,
                                    restSeconds = 90,
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
