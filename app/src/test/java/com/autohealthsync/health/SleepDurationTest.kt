package com.autohealthsync.health

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
}
