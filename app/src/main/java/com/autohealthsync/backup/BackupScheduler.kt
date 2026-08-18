package com.autohealthsync.backup

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.autohealthsync.util.DateUtils
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class BackupScheduler(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val workManager = WorkManager.getInstance(context)

    fun ensureNextBackupScheduled() {
        val next = DateUtils.nextBackup(clock)
        val delay = Duration.between(java.time.Instant.now(clock), next.toInstant())
            .coerceAtLeast(Duration.ZERO)
        val date = next.toLocalDate()
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(BackupWorker.KEY_DATE, date.toString())
                    .putInt(BackupWorker.KEY_ATTEMPT, 0)
                    .build(),
            )
            .addTag(TAG_SCHEDULED)
            .build()
        workManager.enqueueUniqueWork(
            "scheduled-backup-$date",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleRetry(date: LocalDate, attempt: Int) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putString(BackupWorker.KEY_DATE, date.toString())
                    .putInt(BackupWorker.KEY_ATTEMPT, attempt)
                    .build(),
            )
            .addTag(TAG_RETRY)
            .build()
        workManager.enqueueUniqueWork(
            "backup-retry-$date-$attempt",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val RETRY_DELAY_MINUTES = 3L
        const val MAX_RETRIES = 5
        private const val TAG_SCHEDULED = "scheduled-health-backup"
        private const val TAG_RETRY = "health-backup-retry"
    }
}

private fun Duration.coerceAtLeast(minimum: Duration): Duration =
    if (this < minimum) minimum else this

