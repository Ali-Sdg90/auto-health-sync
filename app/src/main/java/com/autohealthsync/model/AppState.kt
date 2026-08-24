package com.autohealthsync.model

import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val onboardingCompleted: Boolean = false,
    val autoStartConfirmed: Boolean = false,
    val driveFolderId: String? = null,
    val lastSuccessfulBackupEpochMillis: Long? = null,
    val lastSuccessfulBackupDate: String? = null,
    val successfulDates: Set<String> = emptySet(),
    val driveFileIds: Map<String, String> = emptyMap(),
    val recentActivity: List<ActivityEntry> = emptyList(),
    val backupSettings: BackupSettings = BackupSettings(),
)

@Serializable
data class BackupSettings(
    val backupHour: Int = 23,
    val backupMinute: Int = 0,
    val driveFolderName: String = DEFAULT_DRIVE_FOLDER_NAME,
    val fileDateSystem: FileDateSystem = FileDateSystem.JALALI,
    val includedMetrics: Set<BackupMetric> = BackupMetric.entries.toSet(),
)

@Serializable
enum class BackupMetric {
    STEPS,
    WEIGHT,
    ACTIVITY,
    HEART,
    SLEEP,
    SPO2,
}

@Serializable
enum class FileDateSystem {
    JALALI,
    GREGORIAN,
}

const val DEFAULT_DRIVE_FOLDER_NAME = "Auto: Health Data"
const val MAX_DRIVE_FOLDER_NAME_LENGTH = 100

fun BackupSettings.normalized(): BackupSettings = copy(
    backupHour = backupHour.coerceIn(0, 23),
    backupMinute = backupMinute.coerceIn(0, 59),
    driveFolderName = driveFolderName.trim()
        .take(MAX_DRIVE_FOLDER_NAME_LENGTH)
        .ifBlank { DEFAULT_DRIVE_FOLDER_NAME },
)

fun BackupSettings.localTime(): LocalTime =
    LocalTime.of(backupHour.coerceIn(0, 23), backupMinute.coerceIn(0, 59))

@Serializable
data class ActivityEntry(
    val id: String,
    val timestampEpochMillis: Long,
    val severity: ActivitySeverity,
    val title: String,
    val detail: String? = null,
)

@Serializable
enum class ActivitySeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

enum class ConnectionState {
    CHECKING,
    CONNECTED,
    ACTION_REQUIRED,
    UNAVAILABLE,
}
