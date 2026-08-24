package com.alisadeghi.autohealthsync.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyHealthSummary(
    val date: String,
    val dateGregorian: String,
    val steps: Long? = null,
    val weight: Double?,
    val activity: ActivitySummary? = null,
    val heart: HeartSummary? = null,
    val sleep: SleepSummary? = null,
    val spo2: SpO2Summary? = null,
)

@Serializable
data class ActivitySummary(
    val exerciseMinutes: Long? = null,
    val distanceKm: Double? = null,
    val workouts: List<WorkoutSummary> = emptyList(),
)

@Serializable
data class WorkoutSummary(
    val type: String,
    val durationMinutes: Long,
)

@Serializable
data class HeartSummary(
    val resting: Long? = null,
    val average: Long? = null,
    val min: Long? = null,
    val max: Long? = null,
)

@Serializable
data class SleepSummary(
    val bedTime: String,
    val wakeTime: String,
    val totalMinutes: Long,
    val napMinutes: Long? = null,
    val deepMinutes: Long? = null,
    val lightMinutes: Long? = null,
    val remMinutes: Long? = null,
    val awakeMinutes: Long? = null,
)

@Serializable
data class SpO2Summary(
    val average: Double,
    val min: Double,
)
