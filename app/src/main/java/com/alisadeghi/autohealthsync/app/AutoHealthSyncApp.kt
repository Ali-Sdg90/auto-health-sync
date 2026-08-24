package com.alisadeghi.autohealthsync.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoHealthSyncApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (container.stateStore.current().onboardingCompleted) {
                container.backupScheduler.ensureNextBackupScheduled()
            } else {
                container.backupScheduler.cancelAutomaticBackups()
            }
        }
    }
}
