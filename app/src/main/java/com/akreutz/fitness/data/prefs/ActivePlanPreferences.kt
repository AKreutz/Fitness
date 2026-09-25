package com.akreutz.fitness.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activePlanDataStore by preferencesDataStore(name = "active_plan")

/**
 * Tracks which [com.akreutz.fitness.data.model.TrainingPlan] the user is currently associated
 * with. `null` means none has been created/selected yet. Backed by DataStore rather than Room
 * since this is app-wide UI state, not domain data. (Which workout was most recently finished,
 * for the plan's rotation, is derived straight from logged [com.akreutz.fitness.data.model.
 * WorkoutSession]s instead of tracked here, so deleting one is reflected immediately — see
 * [com.akreutz.fitness.data.repository.TrainingPlanRepository.nextWorkout].)
 */
class ActivePlanPreferences(private val context: Context) {

    // Renamed (not just retyped) from the pre-UUID-migration "active_training_plan_id": that key
    // held a Long, and DataStore throws a ClassCastException if a stringPreferencesKey of the
    // same name reads a value still stored under the old type. A fresh key name means an
    // existing installation's old Long value is simply never read, rather than crashing.
    private val activeTrainingPlanIdKey = stringPreferencesKey("active_training_plan_id_v2")

    val activeTrainingPlanId: Flow<String?> =
        context.activePlanDataStore.data.map { preferences ->
            preferences[activeTrainingPlanIdKey]
        }

    suspend fun setActiveTrainingPlanId(id: String) {
        context.activePlanDataStore.edit { preferences ->
            preferences[activeTrainingPlanIdKey] = id
        }
    }

    /** Clears the active plan, e.g. after it's been deleted, so the app falls back to no plan. */
    suspend fun clearActiveTrainingPlanId() {
        context.activePlanDataStore.edit { preferences ->
            preferences.remove(activeTrainingPlanIdKey)
        }
    }
}
