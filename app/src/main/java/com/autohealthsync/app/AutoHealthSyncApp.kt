package com.autohealthsync.app

import android.app.Application

class AutoHealthSyncApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.backupScheduler.ensureNextBackupScheduled()
    }
}

