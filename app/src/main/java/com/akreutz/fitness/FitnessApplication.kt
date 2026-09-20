package com.akreutz.fitness

import android.app.Application
import androidx.room.Room
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import com.akreutz.fitness.data.repository.TrainingPlanRepository

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
}
