package com.autohealthsync.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autohealthsync.app.AutoHealthSyncApp
import com.autohealthsync.model.ActivitySeverity
import java.time.LocalDate

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AutoHealthSyncApp
        if (!app.container.stateStore.current().onboardingCompleted) {
            return Result.success()
        }
        val date = inputData.getString(KEY_DATE)?.let(LocalDate::parse) ?: LocalDate.now()
        val attempt = inputData.getInt(KEY_ATTEMPT, 0)

        // A one-time request schedules its successor, which avoids periodic-work drift across DST changes.
        app.container.backupScheduler.ensureNextBackupScheduled()
        if (attempt > 0) {
            app.container.stateStore.addActivity(
                ActivitySeverity.INFO,
                "Retrying backup",
                "Attempt $attempt of ${BackupScheduler.MAX_RETRIES}",
            )
        }

        return when (val outcome = app.container.backupCoordinator.run(BackupTrigger.SCHEDULED, date)) {
            is BackupOutcome.Success -> Result.success()
            is BackupOutcome.ActionRequired,
            is BackupOutcome.PermanentFailure,
            -> Result.failure()
            is BackupOutcome.RetryableFailure -> {
                if (attempt < BackupScheduler.MAX_RETRIES) {
                    app.container.backupScheduler.scheduleRetry(date, attempt + 1)
                    app.container.stateStore.addActivity(
                        ActivitySeverity.WARNING,
                        "Retry scheduled",
                        "Attempt ${attempt + 1} of ${BackupScheduler.MAX_RETRIES} in about 3 minutes",
                    )
                } else {
                    app.container.stateStore.addActivity(
                        ActivitySeverity.ERROR,
                        "Backup failed",
                        "Google Drive upload failed after 5 retries",
                    )
                    app.container.notifications.notifyFinalFailure()
                }
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_DATE = "backup_date"
        const val KEY_ATTEMPT = "retry_attempt"
    }
}
