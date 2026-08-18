package com.autohealthsync.backup

import com.autohealthsync.model.DailyHealthSummary
import com.autohealthsync.model.HeartSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyHealthSummarySerializationTest {
    private val json = Json {
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun `missing metrics are omitted rather than fabricated`() {
        val encoded = json.encodeToString(
            DailyHealthSummary(
                date = "1405-05-27",
                dateGregorian = "2026-08-18",
                heart = HeartSummary(average = 72, min = 48, max = 137),
            ),
        )

        assertFalse(encoded.contains("steps"))
        assertFalse(encoded.contains("resting"))
        assertFalse(encoded.contains("spo2"))
        assertTrue(encoded.contains("\"average\":72"))
    }
}
