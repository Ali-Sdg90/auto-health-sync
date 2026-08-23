package com.autohealthsync.app

import android.content.Context
import com.autohealthsync.backup.BackupCoordinator
import com.autohealthsync.backup.BackupScheduler
import com.autohealthsync.drive.DriveAuthorizationManager
import com.autohealthsync.drive.DriveBackupManager
import com.autohealthsync.health.HealthConnectManager
import com.autohealthsync.notification.BackupNotificationManager
import com.autohealthsync.storage.AppStateStore
import com.autohealthsync.system.BackgroundAccessManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val stateStore = AppStateStore(appContext)
    val backgroundAccessManager = BackgroundAccessManager(appContext)
    val healthManager = HealthConnectManager(appContext)
    val driveAuthorizationManager = DriveAuthorizationManager(appContext)
    val driveBackupManager = DriveBackupManager(driveAuthorizationManager, stateStore)
    val notifications = BackupNotificationManager(appContext)
    val backupScheduler = BackupScheduler(appContext, stateStore)
    val backupCoordinator = BackupCoordinator(
        healthManager,
        driveBackupManager,
        stateStore,
        notifications,
    )
}
