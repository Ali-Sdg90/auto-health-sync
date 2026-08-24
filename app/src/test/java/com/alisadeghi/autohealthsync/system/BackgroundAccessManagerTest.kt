package com.alisadeghi.autohealthsync.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundAccessManagerTest {
    @Test
    fun `xiaomi family uses the MIUI auto start activity`() {
        val components = autoStartComponents("Xiaomi")

        assertEquals(1, components.size)
        assertEquals("com.miui.securitycenter", components.single().first)
        assertEquals(
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
            components.single().second,
        )
    }

    @Test
    fun `manufacturer matching ignores case and whitespace`() {
        assertEquals(autoStartComponents("oppo"), autoStartComponents("  OPPO "))
    }

    @Test
    fun `stock Android does not require an OEM auto start screen`() {
        assertTrue(autoStartComponents("Google").isEmpty())
    }
}
