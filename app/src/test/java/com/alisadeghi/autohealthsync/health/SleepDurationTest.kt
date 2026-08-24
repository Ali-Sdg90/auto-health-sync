package com.alisadeghi.autohealthsync.health

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepDurationTest {
    @Test
    fun `awake time is excluded from total sleep minutes`() {
        assertEquals(429L, netSleepMinutes(sessionMinutes = 448L, awakeMinutes = 19L))
    }

    @Test
    fun `missing awake stages preserve the session duration`() {
        assertEquals(448L, netSleepMinutes(sessionMinutes = 448L, awakeMinutes = null))
    }

    @Test
    fun `invalid awake duration cannot produce negative sleep`() {
        assertEquals(0L, netSleepMinutes(sessionMinutes = 20L, awakeMinutes = 25L))
    }

    @Test
    fun `all distinct sessions contribute to total sleep minutes`() {
        val mainSleep = netSleepMinutes(sessionMinutes = 333L, awakeMinutes = 19L)
        val secondSleep = netSleepMinutes(sessionMinutes = 158L, awakeMinutes = null)

        assertEquals(472L, sumSleepMinutes(listOf(mainSleep, secondSleep)))
    }

    @Test
    fun `sleep stages from every session are summed`() {
        val firstSessionStage = Instant.parse("2026-08-20T08:00:00Z") to
            Instant.parse("2026-08-20T08:32:00Z")
        val secondSessionStage = Instant.parse("2026-08-20T11:10:00Z") to
            Instant.parse("2026-08-20T12:00:00Z")

        assertEquals(82L, sumDurationMinutes(listOf(firstSessionStage, secondSessionStage)))
    }

    @Test
    fun `overlapping duplicate is discarded while separate nap is preserved`() {
        val main = SleepWindow("2026-08-20T07:34:00Z", "2026-08-20T13:07:00Z")
        val duplicate = SleepWindow("2026-08-20T07:41:00Z", "2026-08-20T13:07:00Z")
        val nap = SleepWindow("2026-08-20T01:05:00Z", "2026-08-20T03:43:00Z")

        val distinct = distinctNonOverlappingSleepSessions(
            listOf(duplicate, nap, main),
        ) { it.start to it.end }

        assertEquals(listOf(nap, main), distinct)
    }
}

private data class SleepWindow(
    private val startText: String,
    private val endText: String,
) {
    val start: Instant = Instant.parse(startText)
    val end: Instant = Instant.parse(endText)
}
