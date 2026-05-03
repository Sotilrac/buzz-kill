package com.buzzkill.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.buzzkill.data.SettingsRepository
import com.buzzkill.oem.OemDetector
import com.buzzkill.service.Broadcasts
import com.buzzkill.service.ShutdownAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives [Broadcasts.INACTIVITY_FIRED] from AlarmManager. Manifest-registered (not
 * dynamic) so the alarm can wake our process even after OxygenOS / Doze evicted it.
 *
 * Once the process is awake, the system re-binds the accessibility service. We poll
 * briefly for the bind, then run the shutdown sequence directly on the service.
 */
class InactivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Broadcasts.INACTIVITY_FIRED) return
        Log.i(TAG, "inactivity broadcast received")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context.applicationContext)
                val state = repo.state.first()
                if (!state.enabled || !state.isInWindow) {
                    Log.i(TAG, "ineligible (enabled=${state.enabled} inWindow=${state.isInWindow})")
                    return@launch
                }
                repo.recordTriggered(System.currentTimeMillis())

                val service = waitForService(SERVICE_BIND_TIMEOUT_MS)
                if (service == null) {
                    Log.w(TAG, "accessibility service did not bind within ${SERVICE_BIND_TIMEOUT_MS}ms")
                    return@launch
                }
                val onEmulator = OemDetector.isEmulator
                Log.i(TAG, "running shutdown (emulator=$onEmulator)")
                service.triggerShutdown(dryRun = onEmulator)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun waitForService(timeoutMs: Long): ShutdownAccessibilityService? =
        withTimeoutOrNull(timeoutMs) {
            while (true) {
                val s = ShutdownAccessibilityService.instance
                if (s != null) return@withTimeoutOrNull s
                delay(100)
            }
            @Suppress("UNREACHABLE_CODE") null
        }

    companion object {
        private const val TAG = "BuzzKill.inactivity"
        private const val SERVICE_BIND_TIMEOUT_MS = 8000L
    }
}
