package com.autohealthsync.backup

import com.autohealthsync.model.DailyHealthSummary
import com.autohealthsync.model.BackupMetric
import com.autohealthsync.model.HeartSummary
import com.autohealthsync.model.SleepSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyHealthSummarySerializationTest {
    private val json = Json {
        encodeDefaults = false
        explicitNulls = true
    }

    @Test
    fun `missing metrics are omitted rather than fabricated`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-27",
                dateGregorian = "2026-08-18",
                heart = HeartSummary(average = 72, min = 48, max = 137),
                weight = null,
            ),
        )

        assertFalse(encoded.contains("steps"))
        assertFalse(encoded.contains("resting"))
        assertFalse(encoded.contains("spo2"))
        assertTrue(encoded.contains("\"weight\":null"))
        assertFalse(encoded.contains("activeCalories"))
        assertTrue(encoded.contains("\"average\":72"))
    }

    @Test
    fun `weight is stored as one top-level value`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                weight = 78.4,
            ),
        )

        assertTrue(encoded.contains("\"weight\":78.4"))
        assertFalse(encoded.contains("weightKg"))
    }

    @Test
    fun `weight is serialized immediately after steps`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                steps = 5816,
                weight = 79.3,
            ),
        )

        assertTrue(encoded.contains("\"steps\":5816,\"weight\":79.3"))
    }

    @Test
    fun `nap is stored separately and included in total sleep`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-29",
                dateGregorian = "2026-08-20",
                weight = null,
                sleep = SleepSummary(
                    bedTime = "11:04",
                    wakeTime = "16:37",
                    totalMinutes = 465,
                    napMinutes = 158,
                    lightMinutes = 367,
                    awakeMinutes = 26,
                ),
            ),
        )

        assertTrue(encoded.contains("\"totalMinutes\":465,\"napMinutes\":158"))
    }

    @Test
    fun `nap field is omitted when no separate sleep session exists`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                weight = null,
                sleep = SleepSummary(
                    bedTime = "03:12",
                    wakeTime = "13:08",
                    totalMinutes = 552,
                ),
            ),
        )

        assertFalse(encoded.contains("napMinutes"))
    }

    @Test
    fun `disabled metric groups are completely omitted from backup JSON`() {
        val encoded = json.encodeSelectedSummary(
            summary = DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                steps = 5816,
                weight = null,
                heart = HeartSummary(average = 72),
                sleep = SleepSummary(
                    bedTime = "03:12",
                    wakeTime = "13:08",
                    totalMinutes = 552,
                ),
            ),
            includedMetrics = setOf(BackupMetric.STEPS, BackupMetric.SLEEP),
        )

        assertTrue(encoded.contains("\"dateGregorian\":\"2026-08-19\""))
        assertTrue(encoded.contains("\"steps\":5816"))
        assertTrue(encoded.contains("\"sleep\""))
        assertFalse(encoded.contains("weight"))
        assertFalse(encoded.contains("heart"))
        assertFalse(encoded.contains("activity"))
        assertFalse(encoded.contains("spo2"))
    }

    @Test
    fun `enabled missing weight keeps the stable null field`() {
        val encoded = json.encodeSelectedSummary(
            summary = DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                weight = null,
            ),
            includedMetrics = setOf(BackupMetric.WEIGHT),
        )

        assertTrue(encoded.contains("\"weight\":null"))
    }

    @Test
    fun `default metric selection preserves the existing field order`() {
        val encoded = json.encodeSelectedSummary(
            summary = DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                steps = 5816,
                weight = 79.3,
            ),
            includedMetrics = BackupMetric.entries.toSet(),
        )

        assertTrue(encoded.contains("\"steps\":5816,\"weight\":79.3"))
    }

    @Test
    fun `empty metric selection keeps only date identifiers`() {
        val encoded = json.encodeSelectedSummary(
            summary = DailyHealthSummary(
                date = "1405-05-28",
                dateGregorian = "2026-08-19",
                steps = 5816,
                weight = 79.3,
            ),
            includedMetrics = emptySet(),
        )

        assertTrue(encoded.contains("\"date\":\"1405-05-28\""))
        assertTrue(encoded.contains("\"dateGregorian\":\"2026-08-19\""))
        assertFalse(encoded.contains("steps"))
        assertFalse(encoded.contains("weight"))
    }
}
