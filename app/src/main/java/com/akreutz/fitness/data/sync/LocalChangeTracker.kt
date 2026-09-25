package com.akreutz.fitness.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether local data has changed since the last successful sync, purely in memory (not
 * persisted — a fresh app process starts assuming everything is synced). [com.akreutz.fitness.
 * data.repository.TrainingPlanRepository] calls [markDirty] after every local write; [markSynced]
 * is called once a sync completes successfully. Used to show an "unsynced changes" indicator.
 */
class LocalChangeTracker {
    private val _hasUnsyncedChanges = MutableStateFlow(false)
    val hasUnsyncedChanges: StateFlow<Boolean> = _hasUnsyncedChanges.asStateFlow()

    fun markDirty() {
        _hasUnsyncedChanges.value = true
    }

    fun markSynced() {
        _hasUnsyncedChanges.value = false
    }
}
