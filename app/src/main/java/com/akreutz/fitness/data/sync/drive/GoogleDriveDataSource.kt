package com.akreutz.fitness.data.sync.drive

import android.util.Log
import com.akreutz.fitness.data.sync.FitnessSnapshot
import com.akreutz.fitness.data.sync.PushResult
import com.akreutz.fitness.data.sync.RemoteDataSource
import com.akreutz.fitness.data.sync.VersionedSnapshot
import com.akreutz.fitness.data.sync.auth.GoogleAuthManager
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

private const val SNAPSHOT_FILE_NAME = "fitness-snapshot.json"
private const val TAG = "FitnessSync"

/**
 * Stores a single JSON snapshot file in this app's hidden, per-app Drive `appDataFolder` — never
 * the user's visible Drive files. See [GoogleAuthManager] for how the access token it authorizes
 * with is obtained.
 */
class GoogleDriveDataSource(private val authManager: GoogleAuthManager) : RemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun driveClient(): Drive = withContext(Dispatchers.IO) {
        val token = authManager.getDriveAccessToken()
        if (token == null) {
            Log.w(TAG, "getDriveAccessToken() returned null - not signed in or consent not granted")
            error("Not signed in to Google - cannot reach Drive")
        }
        val requestInitializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $token"
        }
        Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("Fitness")
            .build()
    }

    private suspend fun findSnapshotFile(drive: Drive): File? = withContext(Dispatchers.IO) {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$SNAPSHOT_FILE_NAME' and trashed = false")
            .setFields("files(id, md5Checksum)")
            .execute()
        val file = result.files.firstOrNull()
        Log.d(TAG, if (file == null) "no snapshot file found in appDataFolder" else "found snapshot file id=${file.id}")
        file
    }

    override suspend fun currentVersion(): String? = withContext(Dispatchers.IO) {
        val drive = driveClient()
        findSnapshotFile(drive)?.md5Checksum
    }

    override suspend fun pull(): VersionedSnapshot? = withContext(Dispatchers.IO) {
        val drive = driveClient()
        val file = findSnapshotFile(drive) ?: return@withContext null
        val output = ByteArrayOutputStream()
        drive.files().get(file.id).executeMediaAndDownloadTo(output)
        val dto = json.decodeFromString<FitnessSnapshotDto>(output.toString(Charsets.UTF_8.name()))
        VersionedSnapshot(snapshot = dto.toSnapshot(), version = file.md5Checksum.orEmpty())
    }

    override suspend fun push(snapshot: FitnessSnapshot, expectedVersion: String?): PushResult = withContext(Dispatchers.IO) {
        val drive = driveClient()
        val existing = findSnapshotFile(drive)

        if (existing?.md5Checksum != expectedVersion) return@withContext PushResult.Conflict

        val body = json.encodeToString(FitnessSnapshotDto.serializer(), FitnessSnapshotDto.fromSnapshot(snapshot))
        val content = ByteArrayContent("application/json", body.toByteArray(Charsets.UTF_8))

        val updated = if (existing == null) {
            val metadata = File().setName(SNAPSHOT_FILE_NAME).setParents(listOf("appDataFolder"))
            drive.files().create(metadata, content).setFields("md5Checksum").execute()
        } else {
            drive.files().update(existing.id, null, content).setFields("md5Checksum").execute()
        }
        PushResult.Success(newVersion = updated.md5Checksum.orEmpty())
    }
}
