package com.buzzkill.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings

/**
 * "Scheduled power on/off" lives in different settings surfaces per OEM.
 * Each OEM row tries a list of intents, falling back through them. If none resolve, the
 * caller shows an instructional dialog and opens the closest sensible Settings page.
 *
 * To add a new OEM: extend [intentsFor]. No changes required elsewhere.
 */
object ScheduledPowerOnIntents {

    /**
     * Try the OEM-specific intents in order. Returns true if one was launched, false if all
     * resolution attempts failed.
     */
    fun launch(context: Context): Boolean {
        for (intent in intentsFor(OemDetector.current)) {
            if (resolveAndStart(context, intent)) return true
        }
        // Last-ditch fallback: open the Clock app, where most OEMs hide the toggle.
        if (resolveAndStart(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))) return true
        // Otherwise generic Settings root.
        if (resolveAndStart(context, Intent(Settings.ACTION_SETTINGS))) return true
        return false
    }

    private fun resolveAndStart(context: Context, intent: Intent): Boolean {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val withFlags = Intent(intent).addFlags(flags)
        return try {
            if (withFlags.resolveActivity(context.packageManager) != null) {
                context.startActivity(withFlags)
                true
            } else false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun intentsFor(oem: Oem): List<Intent> = when (oem) {
        Oem.OnePlus -> listOf(
            // Known component on OxygenOS / ColorOS-merged builds.
            Intent().setComponent(
                ComponentName(
                    "com.oneplus.timerpoweronoff",
                    "com.oneplus.timerpoweronoff.TimerPowerOnOffActivity",
                ),
            ),
            Intent().setComponent(
                ComponentName(
                    "com.oneplus.timerpoweronoff",
                    "com.coloros.alarmclock.TimerPowerOnOffActivity",
                ),
            ),
            Intent("oneplus.intent.action.SCHEDULED_POWER_ON_OFF"),
            Intent("oppo.intent.action.SCHEDULED_POWER_ON_OFF"),
        )
        Oem.Oppo, Oem.Realme -> listOf(
            Intent().setComponent(
                ComponentName(
                    "com.coloros.alarmclock",
                    "com.coloros.alarmclock.TimerPowerOnOffActivity",
                ),
            ),
            Intent("oppo.intent.action.SCHEDULED_POWER_ON_OFF"),
        )
        Oem.Xiaomi -> listOf(
            Intent().setComponent(
                ComponentName(
                    "com.android.deskclock",
                    "com.android.deskclock.PowerOnOffSettingActivity",
                ),
            ),
            Intent("miui.intent.action.POWER_ONOFF_TIMER"),
        )
        Oem.Samsung -> listOf(
            // Samsung exposes auto-restart, not full scheduled power on.
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity",
                ),
            ),
        )
        Oem.Huawei -> listOf(
            Intent().setComponent(
                ComponentName(
                    "com.huawei.deskclock",
                    "com.huawei.deskclock.smartcover.PowerOnOffActivity",
                ),
            ),
        )
        Oem.Vivo, Oem.Pixel, Oem.Generic -> emptyList()
    }
}
