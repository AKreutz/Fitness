package com.akreutz.fitness.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activePlanDataStore by preferencesDataStore(name = "active_plan")

/**
 * Tracks which [com.akreutz.fitness.data.model.TrainingPlan] the user is currently associated
 * with, and which [com.akreutz.fitness.data.model.Workout] they most recently finished, so the
 * next one in the plan's rotation can be offered. `null` active plan id means none has been
 * created/selected yet. Backed by DataStore rather than Room since this is app-wide UI state,
 * not domain data.
 */
class ActivePlanPreferences(private val context: Context) {

    private val activeTrainingPlanIdKey = longPreferencesKey("active_training_plan_id")
    private val lastFinishedWorkoutIdKey = longPreferencesKey("last_finished_workout_id")

    val activeTrainingPlanId: Flow<Long?> =
        context.activePlanDataStore.data.map { preferences ->
            preferences[activeTrainingPlanIdKey]
        }

    /** The id of the [com.akreutz.fitness.data.model.Workout] last finished, or `null` if none yet. */
    val lastFinishedWorkoutId: Flow<Long?> =
        context.activePlanDataStore.data.map { preferences ->
            preferences[lastFinishedWorkoutIdKey]
        }

    suspend fun setActiveTrainingPlanId(id: Long) {
        context.activePlanDataStore.edit { preferences ->
            preferences[activeTrainingPlanIdKey] = id
        }
    }

    suspend fun setLastFinishedWorkoutId(id: Long) {
        context.activePlanDataStore.edit { preferences ->
            preferences[lastFinishedWorkoutIdKey] = id
        }
    }
}
