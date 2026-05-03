package com.buzzkill

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.buzzkill.service.ShutdownAccessibilityService

object PermissionsHelper {
    fun isAccessibilityEnabled(context: Context): Boolean {
        // Reading Settings.Secure is the canonical, version-stable check. The
        // AccessibilityManager.getEnabledAccessibilityServiceList() API returns
        // empty on some Android builds (saw it on the API 37 emulator) until the
        // service has actually been bound, which doesn't happen until the user
        // returns to a different activity. The Secure setting flips the moment
        // the toggle is on, which is what we want for the checklist UI.
        val expectedId = "${context.packageName}/${ShutdownAccessibilityService::class.java.name}"
        val enabled =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        for (id in splitter) {
            if (id.equals(expectedId, ignoreCase = true)) return true
        }
        return false
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAccessibilitySettings(activity: Activity) {
        activity.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestBatteryOptimizationExempt(activity: Activity) {
        val intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            // Some OEMs hide the direct intent; fall back to the generic settings page.
            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
