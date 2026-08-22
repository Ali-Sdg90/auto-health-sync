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
import com.autohealthsync.model.BackupSettings
import com.autohealthsync.model.ConnectionState
import com.autohealthsync.model.localTime
import com.autohealthsync.model.normalized
import com.autohealthsync.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    private val selectedBackupDate = MutableStateFlow(DateUtils.today())
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)

    val healthPermissions: Set<String>
        get() = container.healthManager.requestedPermissions

    val events = eventChannel.receiveAsFlow()

    private val backupActionState = combine(
        isBackingUp,
        statusText,
        selectedBackupDate,
    ) { backingUp, text, date -> BackupActionState(backingUp, text, date) }

    val uiState = combine(
        container.stateStore.state,
        healthStatus,
        driveStatus,
        backupActionState,
    ) { state, health, drive, backupAction ->
        MainUiState(
            appState = state,
            healthState = health,
            driveState = drive,
            isBackingUp = backupAction.isBackingUp,
            operationStatus = backupAction.status,
            selectedBackupDate = backupAction.date,
            nextBackupEpochMillis = DateUtils.nextBackup(state.backupSettings.localTime())
                .toInstant()
                .toEpochMilli(),
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
                container.healthManager.hasCompleteAccess() -> ConnectionState.CONNECTED
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
                    "Backups require all requested read permissions",
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

    fun openHealthConnect() {
        viewModelScope.launch { eventChannel.send(UiEvent.OpenHealthConnect) }
    }

    fun openGoogleDrive() {
        viewModelScope.launch {
            eventChannel.send(UiEvent.OpenGoogleDrive(container.stateStore.current().driveFolderId))
        }
    }

    fun saveSettings(settings: BackupSettings) {
        viewModelScope.launch {
            val normalized = settings.normalized()
            container.stateStore.setBackupSettings(normalized)
            container.backupScheduler.rescheduleNextBackup(normalized)
            eventChannel.send(UiEvent.Message("Settings saved"))
        }
    }

    fun backupNow() {
        if (isBackingUp.value) return
        viewModelScope.launch {
            val date = selectedBackupDate.value
            val dateLabel = backupDateLabel(date)
            isBackingUp.value = true
            statusText.value = "Collecting the health summary for $dateLabel…"
            when (val outcome = container.backupCoordinator.run(BackupTrigger.MANUAL, date)) {
                is BackupOutcome.Success -> {
                    statusText.value = if (outcome.updatedExisting) {
                        "Backup for $dateLabel was updated"
                    } else {
                        "Backup for $dateLabel is safe in Drive"
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

    fun selectBackupDate(date: LocalDate) {
        if (!date.isAfter(DateUtils.today())) {
            selectedBackupDate.value = date
            statusText.value = null
        }
    }

    private suspend fun onDriveConnected() {
        driveStatus.value = ConnectionState.CONNECTED
        container.stateStore.addActivity(ActivitySeverity.SUCCESS, "Google Drive connected")
        eventChannel.send(UiEvent.Message("Google Drive connected"))
    }
}

private data class BackupActionState(
    val isBackingUp: Boolean,
    val status: String?,
    val date: LocalDate,
)

private fun backupDateLabel(date: LocalDate): String {
    val today = DateUtils.today()
    return when (date) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

data class MainUiState(
    val appState: AppState = AppState(),
    val healthState: ConnectionState = ConnectionState.CHECKING,
    val driveState: ConnectionState = ConnectionState.CHECKING,
    val isBackingUp: Boolean = false,
    val operationStatus: String? = null,
    val selectedBackupDate: LocalDate = DateUtils.today(),
    val nextBackupEpochMillis: Long = DateUtils.nextBackup().toInstant().toEpochMilli(),
)

sealed interface UiEvent {
    data class ResolveDriveAuthorization(val pendingIntent: PendingIntent) : UiEvent
    data class Message(val text: String) : UiEvent
    data object RequestHealthPermissions : UiEvent
    data object OpenHealthConnectStore : UiEvent
    data object OpenHealthConnect : UiEvent
    data class OpenGoogleDrive(val folderId: String?) : UiEvent
}
