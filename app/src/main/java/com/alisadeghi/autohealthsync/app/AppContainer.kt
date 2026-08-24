package com.alisadeghi.autohealthsync.app

import android.content.Context
import com.alisadeghi.autohealthsync.backup.BackupCoordinator
import com.alisadeghi.autohealthsync.backup.BackupScheduler
import com.alisadeghi.autohealthsync.drive.DriveAuthorizationManager
import com.alisadeghi.autohealthsync.drive.DriveBackupManager
import com.alisadeghi.autohealthsync.health.HealthConnectManager
import com.alisadeghi.autohealthsync.notification.BackupNotificationManager
import com.alisadeghi.autohealthsync.storage.AppStateStore
import com.alisadeghi.autohealthsync.system.BackgroundAccessManager

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
