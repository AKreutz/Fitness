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
 * with. `null` means none has been created/selected yet. Backed by DataStore rather than Room
 * since this is a single piece of app-wide UI state, not domain data.
 */
class ActivePlanPreferences(private val context: Context) {

    private val activeTrainingPlanIdKey = longPreferencesKey("active_training_plan_id")

    val activeTrainingPlanId: Flow<Long?> =
        context.activePlanDataStore.data.map { preferences ->
            preferences[activeTrainingPlanIdKey]
        }

    suspend fun setActiveTrainingPlanId(id: Long) {
        context.activePlanDataStore.edit { preferences ->
            preferences[activeTrainingPlanIdKey] = id
        }
    }
}
