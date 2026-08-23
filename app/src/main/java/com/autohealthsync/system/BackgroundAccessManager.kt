package com.autohealthsync.system

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class BackgroundAccessManager(private val context: Context) {
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    val status: BackgroundAccessStatus
        get() = BackgroundAccessStatus(
            batteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(
                context.packageName,
            ),
            backgroundRestricted = activityManager.isBackgroundRestricted,
            autoStartSettingsAvailable = resolveAutoStartIntent() != null,
            manufacturerName = Build.MANUFACTURER.ifBlank { "this device" },
        )

    fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun autoStartSettingsIntent(): Intent? = resolveAutoStartIntent()

    fun appDetailsIntent(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri(),
    )

    private fun resolveAutoStartIntent(): Intent? =
        autoStartComponents(Build.MANUFACTURER)
            .asSequence()
            .map { (packageName, className) ->
                Intent().setComponent(ComponentName(packageName, className))
            }
            .firstOrNull { intent ->
                intent.resolveActivity(context.packageManager) != null
            }
}

data class BackgroundAccessStatus(
    val batteryOptimizationDisabled: Boolean = false,
    val backgroundRestricted: Boolean = false,
    val autoStartSettingsAvailable: Boolean = false,
    val manufacturerName: String = "this device",
) {
    val batteryAccessGranted: Boolean
        get() = batteryOptimizationDisabled && !backgroundRestricted
}

internal fun autoStartComponents(manufacturer: String): List<Pair<String, String>> =
    when (manufacturer.trim().lowercase()) {
        "xiaomi", "redmi", "poco" -> listOf(
            "com.miui.securitycenter" to
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
        "huawei", "honor" -> listOf(
            "com.huawei.systemmanager" to
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        )
        "oppo", "realme", "oneplus" -> listOf(
            "com.oplus.safecenter" to
                "com.oplus.safecenter.startupapp.StartupAppListActivity",
            "com.coloros.safecenter" to
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
        )
        "vivo", "iqoo" -> listOf(
            "com.vivo.permissionmanager" to
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        )
        "asus" -> listOf(
            "com.asus.mobilemanager" to "com.asus.mobilemanager.MainActivity",
        )
        else -> emptyList()
    }

private fun String.toUri() = android.net.Uri.parse(this)
