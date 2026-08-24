package com.alisadeghi.autohealthsync.util

import com.alisadeghi.autohealthsync.model.FileDateSystem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DateUtilsTest {
    @Test
    fun `vision example converts to expected Jalali date`() {
        assertEquals("1405-05-27", DateUtils.jalaliDate(LocalDate.of(2026, 8, 18)))
        assertEquals("health-data-1405-05-27.json", DateUtils.fileName(LocalDate.of(2026, 8, 18)))
    }

    @Test
    fun `Persian new year converts correctly`() {
        assertEquals("1403-01-01", DateUtils.jalaliDate(LocalDate.of(2024, 3, 20)))
    }

    @Test
    fun `file name can use Gregorian date`() {
        assertEquals(
            "health-data-2026-08-18.json",
            DateUtils.fileName(LocalDate.of(2026, 8, 18), FileDateSystem.GREGORIAN),
        )
    }

    @Test
    fun `next backup uses Tehran and rolls after 23`() {
        val before = Clock.fixed(Instant.parse("2026-08-18T18:00:00Z"), ZoneOffset.UTC)
        val after = Clock.fixed(Instant.parse("2026-08-18T20:00:00Z"), ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 8, 18), DateUtils.nextBackup(before).toLocalDate())
        assertEquals(23, DateUtils.nextBackup(before).hour)
        assertEquals(LocalDate.of(2026, 8, 19), DateUtils.nextBackup(after).toLocalDate())
    }

    @Test
    fun `next backup respects a custom time`() {
        val clock = Clock.fixed(Instant.parse("2026-08-18T08:00:00Z"), ZoneOffset.UTC)
        val next = DateUtils.nextBackup(java.time.LocalTime.of(14, 30), clock)

        assertEquals(LocalDate.of(2026, 8, 18), next.toLocalDate())
        assertEquals(14, next.hour)
        assertEquals(30, next.minute)
    }

    @Test
    fun `recovery window contains exactly the prior two days`() {
        val dates = DateUtils.recoveryDates(LocalDate.of(2026, 8, 18))
        assertEquals(listOf(LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 17)), dates)
        assertFalse(LocalDate.of(2026, 8, 15) in dates)
    }
}
