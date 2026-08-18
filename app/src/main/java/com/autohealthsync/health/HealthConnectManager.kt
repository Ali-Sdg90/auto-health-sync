package com.autohealthsync.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.DataOrigin
import com.autohealthsync.model.ActivitySummary
import com.autohealthsync.model.DailyHealthSummary
import com.autohealthsync.model.HeartSummary
import com.autohealthsync.model.SleepSummary
import com.autohealthsync.model.SpO2Summary
import com.autohealthsync.model.WorkoutSummary
import com.autohealthsync.util.DateUtils
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToLong
import kotlin.reflect.KClass

class HealthConnectManager(private val context: Context) {
    private val providerPackageName = "com.google.android.apps.healthdata"
    private val preferredOrigin = setOf(DataOrigin(HEALTH_SYNC_PACKAGE))

    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context, providerPackageName)

    val isAvailable: Boolean
        get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient
        get() = HealthConnectClient.getOrCreate(context, providerPackageName)

    val foregroundPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    )

    val backgroundReadAvailable: Boolean
        get() = isAvailable && client.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    val requestedPermissions: Set<String>
        get() = if (backgroundReadAvailable) {
            foregroundPermissions + HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        } else {
            foregroundPermissions
        }

    suspend fun grantedPermissions(): Set<String> =
        if (isAvailable) client.permissionController.getGrantedPermissions() else emptySet()

    suspend fun hasForegroundAccess(): Boolean =
        isAvailable && grantedPermissions().containsAll(foregroundPermissions)

    suspend fun hasBackgroundAccess(): Boolean =
        hasForegroundAccess() && backgroundReadAvailable &&
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in grantedPermissions()

    suspend fun readDailySummary(date: LocalDate): DailyHealthSummary {
        check(isAvailable) { "Health Connect is unavailable" }
        if (!hasForegroundAccess()) throw HealthPermissionRequiredException()

        val (start, end) = DateUtils.dayBounds(date)
        val aggregates = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                dataOriginFilter = preferredOrigin,
            ),
        )
        val workouts = readAll(ExerciseSessionRecord::class, start, end)
        val heartRates = readAll(HeartRateRecord::class, start, end)
        val restingRates = readAll(RestingHeartRateRecord::class, start, end)
        val oxygen = readAll(OxygenSaturationRecord::class, start, end)
        val sleep = readSleepForWakeDate(date, start, end)

        return DailyHealthSummary(
            date = DateUtils.jalaliDate(date),
            dateGregorian = DateUtils.gregorianDate(date),
            steps = aggregates[StepsRecord.COUNT_TOTAL],
            activity = buildActivity(aggregates, workouts, start, end),
            heart = buildHeart(heartRates, restingRates),
            sleep = sleep,
            spo2 = buildSpO2(oxygen),
        )
    }

    private fun buildActivity(
        aggregates: AggregationResult,
        sessions: List<ExerciseSessionRecord>,
        start: Instant,
        end: Instant,
    ): ActivitySummary? {
        val workouts = sessions.mapNotNull { session ->
            val clippedStart = if (session.startTime < start) start else session.startTime
            val clippedEnd = if (session.endTime > end) end else session.endTime
            val minutes = Duration.between(clippedStart, clippedEnd).toMinutes()
            if (minutes <= 0) null else WorkoutSummary(exerciseName(session.exerciseType), minutes)
        }
        val exerciseMinutes = workouts.sumOf { it.durationMinutes }.takeIf { workouts.isNotEmpty() }
        val distance = aggregates[DistanceRecord.DISTANCE_TOTAL]?.inKilometers?.rounded(2)
        val calories = aggregates[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            ?.inKilocalories?.rounded(1)

        if (exerciseMinutes == null && distance == null && calories == null) return null
        return ActivitySummary(exerciseMinutes, distance, calories, workouts)
    }

    private fun buildHeart(
        records: List<HeartRateRecord>,
        restingRecords: List<RestingHeartRateRecord>,
    ): HeartSummary? {
        val samples = records.flatMap { it.samples }.map { it.beatsPerMinute }
        val resting = restingRecords.map { it.beatsPerMinute }.minOrNull()
        if (samples.isEmpty() && resting == null) return null
        return HeartSummary(
            resting = resting,
            average = samples.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            min = samples.minOrNull(),
            max = samples.maxOrNull(),
        )
    }

    private suspend fun readSleepForWakeDate(
        date: LocalDate,
        dayStart: Instant,
        dayEnd: Instant,
    ): SleepSummary? {
        val queryStart = DateUtils.dayBounds(date.minusDays(1)).first
        val sessions = readAll(SleepSessionRecord::class, queryStart, dayEnd)
            .filter { !it.endTime.isBefore(dayStart) && it.endTime.isBefore(dayEnd) }
        val session = sessions.maxByOrNull { Duration.between(it.startTime, it.endTime) } ?: return null

        fun stageMinutes(vararg types: Int): Long? {
            val matching = session.stages.filter { it.stage in types }
            return matching.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                .takeIf { matching.isNotEmpty() }
        }

        return SleepSummary(
            bedTime = DateUtils.formatTime(session.startTime),
            wakeTime = DateUtils.formatTime(session.endTime),
            totalMinutes = Duration.between(session.startTime, session.endTime).toMinutes(),
            deepMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_DEEP),
            lightMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_LIGHT),
            remMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_REM),
            awakeMinutes = stageMinutes(
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            ),
        )
    }

    private fun buildSpO2(records: List<OxygenSaturationRecord>): SpO2Summary? {
        val values = records.map { it.percentage.value }.filter { it in 1.0..100.0 }
        if (values.isEmpty()) return null
        return SpO2Summary(
            average = values.average().rounded(1),
            min = values.min().rounded(1),
        )
    }

    private suspend fun <T : Record> readAll(
        recordType: KClass<T>,
        start: Instant,
        end: Instant,
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response: ReadRecordsResponse<T> = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    dataOriginFilter = preferredOrigin,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private fun exerciseName(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "treadmill_running"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "stationary_cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "pool_swimming"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "open_water_swimming"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "strength_training"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "weightlifting"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "other"
        else -> "exercise_$type"
    }

    companion object {
        const val HEALTH_SYNC_PACKAGE = "nl.appyhapps.healthsync"
    }
}

class HealthPermissionRequiredException : SecurityException("Health Connect permission required")

private fun Double.rounded(decimals: Int): Double {
    val scale = when (decimals) {
        1 -> 10.0
        2 -> 100.0
        else -> 1.0
    }
    return round(this * scale) / scale
}
