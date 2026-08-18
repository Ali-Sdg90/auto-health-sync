package com.autohealthsync.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.autohealthsync.model.ActivityEntry
import com.autohealthsync.model.ActivitySeverity
import com.autohealthsync.model.AppState
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.appStateDataStore: DataStore<AppState> by dataStore(
    fileName = "app-state.json",
    serializer = AppStateSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        AppState(
            recentActivity = listOf(
                ActivityEntry(
                    id = UUID.randomUUID().toString(),
                    timestampEpochMillis = System.currentTimeMillis(),
                    severity = ActivitySeverity.ERROR,
                    title = "Local state was repaired",
                    detail = "Backup history was unreadable; recent days will be checked again",
                ),
            ),
        )
    },
)

class AppStateStore(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dataStore = context.appStateDataStore

    val state: Flow<AppState> = dataStore.data

    suspend fun current(): AppState = dataStore.data.first()

    suspend fun setDriveFolderId(folderId: String?) {
        dataStore.updateData { it.copy(driveFolderId = folderId) }
    }

    suspend fun markBackedUp(date: LocalDate, fileId: String) {
        val dateKey = date.toString()
        val cutoff = date.minusDays(7).toString()
        dataStore.updateData { state ->
            state.copy(
                lastSuccessfulBackupEpochMillis = Instant.now(clock).toEpochMilli(),
                lastSuccessfulBackupDate = dateKey,
                successfulDates = (state.successfulDates + dateKey).filterTo(mutableSetOf()) { it >= cutoff },
                driveFileIds = (state.driveFileIds + (dateKey to fileId)).filterKeys { it >= cutoff },
            )
        }
    }

    suspend fun clearBackedUp(date: LocalDate) {
        val key = date.toString()
        dataStore.updateData { state ->
            state.copy(
                successfulDates = state.successfulDates - key,
                driveFileIds = state.driveFileIds - key,
            )
        }
    }

    suspend fun addActivity(
        severity: ActivitySeverity,
        title: String,
        detail: String? = null,
    ) {
        val entry = ActivityEntry(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = Instant.now(clock).toEpochMilli(),
            severity = severity,
            title = title,
            detail = detail,
        )
        dataStore.updateData { state ->
            state.copy(recentActivity = (listOf(entry) + state.recentActivity).take(MAX_ACTIVITY_ENTRIES))
        }
    }

    companion object {
        const val MAX_ACTIVITY_ENTRIES = 50
    }
}

private object AppStateSerializer : Serializer<AppState> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: AppState = AppState()

    override suspend fun readFrom(input: InputStream): AppState = try {
        json.decodeFromString(AppState.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Could not read local backup state", error)
    }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(json.encodeToString(AppState.serializer(), t).encodeToByteArray())
    }
}
