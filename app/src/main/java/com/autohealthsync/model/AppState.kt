package com.autohealthsync.model

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val driveFolderId: String? = null,
    val lastSuccessfulBackupEpochMillis: Long? = null,
    val lastSuccessfulBackupDate: String? = null,
    val successfulDates: Set<String> = emptySet(),
    val driveFileIds: Map<String, String> = emptyMap(),
    val recentActivity: List<ActivityEntry> = emptyList(),
)

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

