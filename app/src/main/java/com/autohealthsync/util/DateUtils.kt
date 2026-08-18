package com.autohealthsync.util

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

object DateUtils {
    val HEALTH_ZONE: ZoneId = ZoneId.of("Asia/Tehran")
    val BACKUP_TIME: LocalTime = LocalTime.of(23, 0)
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(clock: Clock = Clock.systemUTC()): LocalDate =
        LocalDate.now(clock.withZone(HEALTH_ZONE))

    fun dayBounds(date: LocalDate): Pair<Instant, Instant> =
        date.atStartOfDay(HEALTH_ZONE).toInstant() to
            date.plusDays(1).atStartOfDay(HEALTH_ZONE).toInstant()

    fun nextBackup(clock: Clock = Clock.systemUTC()): ZonedDateTime {
        val now = ZonedDateTime.now(clock.withZone(HEALTH_ZONE))
        val todayAtBackup = now.toLocalDate().atTime(BACKUP_TIME).atZone(HEALTH_ZONE)
        return if (now.isBefore(todayAtBackup)) todayAtBackup else todayAtBackup.plusDays(1)
    }

    fun delayUntilNextBackup(clock: Clock = Clock.systemUTC()): Duration =
        Duration.between(Instant.now(clock), nextBackup(clock).toInstant()).coerceAtLeast(Duration.ZERO)

    fun gregorianDate(date: LocalDate): String = isoDate.format(date)

    fun jalaliDate(date: LocalDate): String {
        val jalali = JalaliDate.fromGregorian(date.year, date.monthValue, date.dayOfMonth)
        return "%04d-%02d-%02d".format(jalali.year, jalali.month, jalali.day)
    }

    fun fileName(date: LocalDate): String = "health-data-${jalaliDate(date)}.json"

    fun recoveryDates(today: LocalDate): List<LocalDate> =
        listOf(today.minusDays(2), today.minusDays(1))

    fun formatTime(instant: Instant): String =
        instant.atZone(HEALTH_ZONE).format(DateTimeFormatter.ofPattern("HH:mm"))
}

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    companion object {
        private val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
        )

        fun fromGregorian(year: Int, month: Int, day: Int): JalaliDate {
            val jdn = gregorianToJulianDay(year, month, day)
            val gregorian = julianDayToGregorian(jdn)
            var jy = gregorian.first - 621
            val march = jalaliCalendar(jy).second
            val firstFarvardin = gregorianToJulianDay(gregorian.first, 3, march)
            var offset = jdn - firstFarvardin
            if (offset >= 0) {
                if (offset <= 185) {
                    return JalaliDate(jy, 1 + offset / 31, 1 + offset % 31)
                }
                offset -= 186
            } else {
                jy -= 1
                offset += 179
                if (jalaliCalendar(jy).first == 1) offset += 1
            }
            return JalaliDate(jy, 7 + offset / 30, 1 + offset % 30)
        }

        private fun jalaliCalendar(jy: Int): Pair<Int, Int> {
            require(jy >= breaks.first() && jy < breaks.last()) { "Jalali year out of range: $jy" }
            val gy = jy + 621
            var leapJ = -14
            var previousBreak = breaks[0]
            var jump = 0
            for (index in 1 until breaks.size) {
                val currentBreak = breaks[index]
                jump = currentBreak - previousBreak
                if (jy < currentBreak) break
                leapJ += (jump / 33) * 8 + (jump % 33) / 4
                previousBreak = currentBreak
            }
            var yearsSinceBreak = jy - previousBreak
            leapJ += (yearsSinceBreak / 33) * 8 + ((yearsSinceBreak % 33) + 3) / 4
            if (jump % 33 == 4 && jump - yearsSinceBreak == 4) leapJ += 1
            val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
            val march = 20 + leapJ - leapG
            if (jump - yearsSinceBreak < 6) {
                yearsSinceBreak = yearsSinceBreak - jump + ((jump + 4) / 33) * 33
            }
            var leap = ((yearsSinceBreak + 1) % 33 - 1) % 4
            if (leap == -1) leap = 4
            return leap to march
        }

        private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
            var result = (year + (month - 8) / 6 + 100100) * 1461 / 4
            result += (153 * ((month + 9) % 12) + 2) / 5
            result += day - 34840408
            result -= ((year + 100100 + (month - 8) / 6) / 100 * 3) / 4
            return result + 752
        }

        private fun julianDayToGregorian(julianDay: Int): Triple<Int, Int, Int> {
            var j = 4 * julianDay + 139361631
            j += (((4 * julianDay + 183187720) / 146097) * 3 / 4) * 4 - 3908
            val i = (j % 1461) / 4 * 5 + 308
            val day = (i % 153) / 5 + 1
            val month = (i / 153 % 12) + 1
            val year = j / 1461 - 100100 + (8 - month) / 6
            return Triple(year, month, day)
        }
    }
}

private fun Duration.coerceAtLeast(minimum: Duration): Duration =
    if (this < minimum) minimum else this
