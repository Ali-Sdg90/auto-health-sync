package com.autohealthsync.backup

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.autohealthsync.util.DateUtils
import com.autohealthsync.model.BackupSettings
import com.autohealthsync.model.localTime
import com.autohealthsync.storage.AppStateStore
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BackupScheduler(
    context: Context,
    private val stateStore: AppStateStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val workManager = WorkManager.getInstance(context)
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureNextBackupScheduled() {
        scheduleNextBackup(stateStore = stateStore, policy = ExistingWorkPolicy.KEEP)
    }

    fun rescheduleNextBackup(settings: BackupSettings) {
        schedulerScope.launch {
            // The selected time can move the next run to another date, so remove the old tagged
            // request before enqueueing its replacement rather than relying only on its work name.
            workManager.cancelAllWorkByTag(TAG_SCHEDULED)
            enqueueNextBackup(settings, ExistingWorkPolicy.REPLACE)
        }
    }

    fun cancelAutomaticBackups() {
        workManager.cancelAllWorkByTag(TAG_SCHEDULED)
        workManager.cancelAllWorkByTag(TAG_RETRY)
    }

    private fun scheduleNextBackup(stateStore: AppStateStore, policy: ExistingWorkPolicy) {
        schedulerScope.launch {
            enqueueNextBackup(stateStore.current().backupSettings, policy)
        }
    }

    private fun enqueueNextBackup(settings: BackupSettings, policy: ExistingWorkPolicy) {
        val next = DateUtils.nextBackup(settings.localTime(), clock)
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
            policy,
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
