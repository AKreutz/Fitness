package com.akreutz.fitness.data.sync

/** A backend-agnostic remote store for a [FitnessSnapshot]. See [com.akreutz.fitness.data.sync.drive.GoogleDriveDataSource] for the Google Drive implementation. */
interface RemoteDataSource {
    /** The remote's current version token, or `null` if nothing has been pushed yet. */
    suspend fun currentVersion(): String?

    /** The remote's current snapshot together with its version, or `null` if nothing exists yet. */
    suspend fun pull(): VersionedSnapshot?

    /**
     * Writes [snapshot] as the remote's new content, succeeding only if the remote's current
     * version still matches [expectedVersion] (optimistic concurrency) — `null` means "expect
     * nothing to exist yet".
     */
    suspend fun push(snapshot: FitnessSnapshot, expectedVersion: String?): PushResult
}

data class VersionedSnapshot(
    val snapshot: FitnessSnapshot,
    val version: String,
)

sealed interface PushResult {
    data class Success(val newVersion: String) : PushResult
    data object Conflict : PushResult
}
