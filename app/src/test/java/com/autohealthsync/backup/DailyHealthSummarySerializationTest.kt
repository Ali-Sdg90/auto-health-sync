package com.autohealthsync.backup

import com.autohealthsync.model.DailyHealthSummary
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
}
