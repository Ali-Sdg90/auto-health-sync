package com.autohealthsync.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSettingsTest {
    @Test
    fun `defaults preserve the existing backup behavior`() {
        val settings = BackupSettings()

        assertEquals(23, settings.backupHour)
        assertEquals(0, settings.backupMinute)
        assertEquals("Auto: Health Data", settings.driveFolderName)
        assertEquals(FileDateSystem.JALALI, settings.fileDateSystem)
    }

    @Test
    fun `normalization trims folder and protects time ranges`() {
        val settings = BackupSettings(
            backupHour = 25,
            backupMinute = -2,
            driveFolderName = "  My Health  ",
        ).normalized()

        assertEquals(23, settings.backupHour)
        assertEquals(0, settings.backupMinute)
        assertEquals("My Health", settings.driveFolderName)
    }
}
