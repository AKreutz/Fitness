package com.akreutz.fitness

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.akreutz.fitness.data.db.ALL_MIGRATIONS
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.data.sync.LocalChangeTracker
import com.akreutz.fitness.data.sync.SyncManager
import com.akreutz.fitness.data.sync.auth.GoogleAuthManager
import com.akreutz.fitness.data.sync.drive.GoogleDriveDataSource
import com.akreutz.fitness.data.watch.WatchSessionBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether a Google Drive sync is idle, in progress, or has just finished (successfully or not). */
sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class Failed(val message: String?) : SyncState
}

/**
 * Holds this app's singleton dependencies (database, repositories). No DI framework — just
 * lazily-built singletons handed out from here.
 */
class FitnessApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: FitnessDatabase by lazy {
        Room.databaseBuilder(this, FitnessDatabase::class.java, FitnessDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    private val activePlanPreferences: ActivePlanPreferences by lazy {
        ActivePlanPreferences(this)
    }

    /**
     * Passes the open workout session to the watch and the watch's commands back (see
     * `WatchSyncService`, which keeps the connection up while a session is open).
     */
    val watchSessionBridge = WatchSessionBridge()

    /** Tracks whether local data has changed since the last successful sync. */
    val localChangeTracker = LocalChangeTracker()

    val trainingPlanRepository: TrainingPlanRepository by lazy {
        TrainingPlanRepository(database, activePlanPreferences, localChangeTracker)
    }

    val authManager: GoogleAuthManager by lazy { GoogleAuthManager(this) }

    private val syncManager: SyncManager by lazy {
        SyncManager(
            trainingPlanDao = database.trainingPlanDao(),
            workoutDao = database.workoutDao(),
            exerciseDao = database.exerciseDao(),
            workoutSessionDao = database.workoutSessionDao(),
            exercisePerformanceRecordDao = database.exercisePerformanceRecordDao(),
            purgedIdDao = database.purgedIdDao(),
            weightStackDao = database.weightStackDao(),
            remoteDataSource = GoogleDriveDataSource(authManager),
        )
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Triggers a sync in the background, without blocking the caller on its result. */
    fun syncInBackground() {
        applicationScope.launch { runSync() }
    }

    /** Signs in (if not already) and syncs, for the manual sync button. */
    fun syncNow() {
        applicationScope.launch {
            if (authManager.signedInAccount == null) {
                authManager.signIn()
            }
            runSync()
        }
    }

    private suspend fun runSync() {
        _syncState.value = SyncState.Syncing
        _syncState.value = runCatching { syncManager.sync() }
            .fold(
                onSuccess = {
                    localChangeTracker.markSynced()
                    SyncState.Success
                },
                onFailure = {
                    Log.e("FitnessSync", "sync() failed", it)
                    SyncState.Failed(it.message)
                },
            )
    }
}
