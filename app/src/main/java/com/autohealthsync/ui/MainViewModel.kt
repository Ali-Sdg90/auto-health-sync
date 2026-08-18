package com.autohealthsync.ui

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autohealthsync.app.AutoHealthSyncApp
import com.autohealthsync.backup.BackupOutcome
import com.autohealthsync.backup.BackupTrigger
import com.autohealthsync.drive.AuthorizationOutcome
import com.autohealthsync.model.ActivitySeverity
import com.autohealthsync.model.AppState
import com.autohealthsync.model.ConnectionState
import com.autohealthsync.util.DateUtils
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as AutoHealthSyncApp).container
    private val healthStatus = MutableStateFlow(ConnectionState.CHECKING)
    private val driveStatus = MutableStateFlow(ConnectionState.CHECKING)
    private val isBackingUp = MutableStateFlow(false)
    private val statusText = MutableStateFlow<String?>(null)
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)

    val healthPermissions: Set<String>
        get() = container.healthManager.requestedPermissions

    val events = eventChannel.receiveAsFlow()

    val uiState = combine(
        container.stateStore.state,
        healthStatus,
        driveStatus,
        isBackingUp,
        statusText,
    ) { state, health, drive, backingUp, text ->
        MainUiState(
            appState = state,
            healthState = health,
            driveState = drive,
            isBackingUp = backingUp,
            operationStatus = text,
            nextBackupEpochMillis = DateUtils.nextBackup().toInstant().toEpochMilli(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        refreshConnections()
        container.backupScheduler.ensureNextBackupScheduled()
    }

    fun refreshConnections() {
        viewModelScope.launch {
            healthStatus.value = ConnectionState.CHECKING
            healthStatus.value = when {
                !container.healthManager.isAvailable -> ConnectionState.UNAVAILABLE
                container.healthManager.hasBackgroundAccess() -> ConnectionState.CONNECTED
                else -> ConnectionState.ACTION_REQUIRED
            }

            driveStatus.value = ConnectionState.CHECKING
            driveStatus.value = try {
                when (container.driveAuthorizationManager.authorize()) {
                    is AuthorizationOutcome.Authorized -> ConnectionState.CONNECTED
                    is AuthorizationOutcome.UserActionRequired -> ConnectionState.ACTION_REQUIRED
                    is AuthorizationOutcome.Unavailable -> ConnectionState.UNAVAILABLE
                }
            } catch (_: Exception) {
                ConnectionState.ACTION_REQUIRED
            }
        }
    }

    fun requestDriveConnection() {
        viewModelScope.launch {
            driveStatus.value = ConnectionState.CHECKING
            try {
                when (val outcome = container.driveAuthorizationManager.authorize()) {
                    is AuthorizationOutcome.Authorized -> onDriveConnected()
                    is AuthorizationOutcome.UserActionRequired -> {
                        driveStatus.value = ConnectionState.ACTION_REQUIRED
                        eventChannel.send(UiEvent.ResolveDriveAuthorization(outcome.pendingIntent))
                    }
                    is AuthorizationOutcome.Unavailable -> {
                        driveStatus.value = ConnectionState.UNAVAILABLE
                        eventChannel.send(UiEvent.Message(outcome.reason))
                    }
                }
            } catch (_: Exception) {
                driveStatus.value = ConnectionState.ACTION_REQUIRED
                eventChannel.send(UiEvent.Message("Google Drive authorization could not be started"))
            }
        }
    }

    fun completeDriveConnection(data: Intent?) {
        if (data == null) {
            driveStatus.value = ConnectionState.ACTION_REQUIRED
            return
        }
        viewModelScope.launch {
            try {
                when (container.driveAuthorizationManager.completeAuthorization(data)) {
                    is AuthorizationOutcome.Authorized -> onDriveConnected()
                    else -> driveStatus.value = ConnectionState.ACTION_REQUIRED
                }
            } catch (_: Exception) {
                driveStatus.value = ConnectionState.ACTION_REQUIRED
                eventChannel.send(UiEvent.Message("Google Drive access was not granted"))
            }
        }
    }

    fun onHealthPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            if (granted.containsAll(healthPermissions)) {
                healthStatus.value = ConnectionState.CONNECTED
                container.stateStore.addActivity(ActivitySeverity.SUCCESS, "Health Connect connected")
            } else {
                healthStatus.value = ConnectionState.ACTION_REQUIRED
                container.stateStore.addActivity(
                    ActivitySeverity.WARNING,
                    "Health permission missing",
                    "Background backup requires all requested read permissions",
                )
                eventChannel.send(UiEvent.Message("Health Connect access is incomplete"))
            }
        }
    }

    fun requestHealthConnection() {
        viewModelScope.launch {
            if (container.healthManager.isAvailable) {
                eventChannel.send(UiEvent.RequestHealthPermissions)
            } else {
                eventChannel.send(UiEvent.OpenHealthConnectStore)
            }
        }
    }

    fun backupNow() {
        if (isBackingUp.value) return
        viewModelScope.launch {
            isBackingUp.value = true
            statusText.value = "Collecting today's health summary…"
            when (val outcome = container.backupCoordinator.run(BackupTrigger.MANUAL)) {
                is BackupOutcome.Success -> {
                    statusText.value = if (outcome.updatedExisting) {
                        "Today's backup was updated"
                    } else {
                        "Today's backup is safe in Drive"
                    }
                    eventChannel.send(UiEvent.Message("Backup completed: ${outcome.fileName}"))
                }
                is BackupOutcome.ActionRequired -> {
                    statusText.value = outcome.message
                    refreshConnections()
                    eventChannel.send(UiEvent.Message(outcome.message))
                }
                is BackupOutcome.RetryableFailure -> {
                    statusText.value = outcome.message
                    eventChannel.send(UiEvent.Message("Backup failed. Please try again."))
                }
                is BackupOutcome.PermanentFailure -> {
                    statusText.value = outcome.message
                    eventChannel.send(UiEvent.Message(outcome.message))
                }
            }
            isBackingUp.value = false
        }
    }

    private suspend fun onDriveConnected() {
        driveStatus.value = ConnectionState.CONNECTED
        container.stateStore.addActivity(ActivitySeverity.SUCCESS, "Google Drive connected")
        eventChannel.send(UiEvent.Message("Google Drive connected"))
    }
}

data class MainUiState(
    val appState: AppState = AppState(),
    val healthState: ConnectionState = ConnectionState.CHECKING,
    val driveState: ConnectionState = ConnectionState.CHECKING,
    val isBackingUp: Boolean = false,
    val operationStatus: String? = null,
    val nextBackupEpochMillis: Long = DateUtils.nextBackup().toInstant().toEpochMilli(),
)

sealed interface UiEvent {
    data class ResolveDriveAuthorization(val pendingIntent: PendingIntent) : UiEvent
    data class Message(val text: String) : UiEvent
    data object RequestHealthPermissions : UiEvent
    data object OpenHealthConnectStore : UiEvent
}
