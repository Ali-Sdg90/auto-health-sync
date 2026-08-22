package com.autohealthsync.backup

import android.util.Log
import com.autohealthsync.drive.DriveAuthorizationRequiredException
import com.autohealthsync.drive.DriveBackupManager
import com.autohealthsync.health.HealthConnectManager
import com.autohealthsync.health.HealthPermissionRequiredException
import com.autohealthsync.model.ActivitySeverity
import com.autohealthsync.model.BackupSettings
import com.autohealthsync.notification.BackupNotificationManager
import com.autohealthsync.storage.AppStateStore
import com.autohealthsync.util.DateUtils
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupCoordinator(
    private val health: HealthConnectManager,
    private val drive: DriveBackupManager,
    private val stateStore: AppStateStore,
    private val notifications: BackupNotificationManager,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = true
    }

    suspend fun run(trigger: BackupTrigger, date: LocalDate = DateUtils.today(clock)): BackupOutcome {
        stateStore.addActivity(
            ActivitySeverity.INFO,
            if (trigger == BackupTrigger.MANUAL) "Manual backup started" else "Scheduled backup started",
            date.toString(),
        )
        return try {
            validateAccess(trigger)
            val settings = stateStore.current().backupSettings
            if (date == DateUtils.today(clock)) {
                recoverMissingDays(date, settings)
            }
            val result = backupDate(date, settings)
            stateStore.addActivity(
                ActivitySeverity.SUCCESS,
                "Backup completed",
                result.fileName,
            )
            BackupOutcome.Success(result.fileName, result.updatedExisting)
        } catch (error: HealthPermissionRequiredException) {
            actionRequired(trigger, "Health Connect permission required")
        } catch (error: DriveAuthorizationRequiredException) {
            actionRequired(trigger, "Google Drive authorization required")
        } catch (error: SecurityException) {
            actionRequired(trigger, "Required access was revoked")
        } catch (error: SerializationException) {
            Log.e(TAG, "Could not serialize health summary", error)
            stateStore.addActivity(ActivitySeverity.ERROR, "Backup failed", "Could not create the daily JSON")
            if (trigger == BackupTrigger.SCHEDULED) {
                notifications.notifyFinalFailure("The daily backup file could not be created.")
            }
            BackupOutcome.PermanentFailure("Could not create the daily JSON")
        } catch (error: Exception) {
            Log.e(TAG, "Backup failed", error)
            val message = friendlyFailure(error)
            stateStore.addActivity(ActivitySeverity.WARNING, "Backup attempt failed", message)
            BackupOutcome.RetryableFailure(message)
        }
    }

    private suspend fun validateAccess(trigger: BackupTrigger) {
        if (!health.isAvailable) throw HealthPermissionRequiredException()
        val healthReady = if (trigger == BackupTrigger.SCHEDULED) {
            health.hasBackgroundAccess()
        } else {
            health.hasForegroundAccess()
        }
        if (!healthReady) throw HealthPermissionRequiredException()
        if (!drive.isAuthorized()) throw DriveAuthorizationRequiredException()
    }

    private suspend fun recoverMissingDays(today: LocalDate, settings: BackupSettings) {
        for (date in DateUtils.recoveryDates(today)) {
            val fileName = DateUtils.fileName(date, settings.fileDateSystem)
            val localSuccess = date.toString() in stateStore.current().successfulDates
            val remoteExists = localSuccess && drive.hasBackup(date, fileName)
            if (localSuccess && remoteExists) continue

            stateStore.addActivity(ActivitySeverity.INFO, "Missing backup found", fileName)
            val result = backupDate(date, settings)
            stateStore.addActivity(ActivitySeverity.SUCCESS, "Missing backup recovered", result.fileName)
            notifications.notifyRecovered(result.fileName)
        }
    }

    private suspend fun backupDate(date: LocalDate, settings: BackupSettings): DateBackupResult {
        stateStore.addActivity(ActivitySeverity.INFO, "Reading Health Connect", date.toString())
        val summary = health.readDailySummary(date)
        val contents = json.encodeToString(summary)
        val fileName = DateUtils.fileName(date, settings.fileDateSystem)
        stateStore.addActivity(ActivitySeverity.INFO, "Uploading to Google Drive", fileName)
        val upload = drive.upload(date, fileName, contents, settings.driveFolderName)
        stateStore.markBackedUp(date, upload.fileId)
        return DateBackupResult(fileName, upload.updatedExisting)
    }

    private suspend fun actionRequired(trigger: BackupTrigger, detail: String): BackupOutcome {
        stateStore.addActivity(ActivitySeverity.ERROR, "Backup needs attention", detail)
        if (trigger == BackupTrigger.SCHEDULED) notifications.notifyActionRequired()
        return BackupOutcome.ActionRequired(detail)
    }

    private fun friendlyFailure(error: Exception): String = when (error) {
        is IOException -> "Network or Google Drive request failed"
        else -> "Health backup could not be completed"
    }

    companion object {
        private const val TAG = "BackupCoordinator"
    }
}

enum class BackupTrigger { MANUAL, SCHEDULED }

sealed interface BackupOutcome {
    data class Success(val fileName: String, val updatedExisting: Boolean) : BackupOutcome
    data class RetryableFailure(val message: String) : BackupOutcome
    data class ActionRequired(val message: String) : BackupOutcome
    data class PermanentFailure(val message: String) : BackupOutcome
}

private data class DateBackupResult(val fileName: String, val updatedExisting: Boolean)
