package com.buzzkill.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.buzzkill.data.SettingsRepository
import com.buzzkill.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShutdownAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: SettingsRepository
    private lateinit var alarms: AlarmScheduler

    companion object {
        private const val TAG = "BuzzKill.svc"

        /**
         * Process-wide reference to the live accessibility service instance.
         * Manifest-registered receivers (e.g. [com.buzzkill.scheduling.InactivityReceiver])
         * use this to invoke shutdown after the system has re-bound the service.
         */
        @Volatile
        var instance: ShutdownAccessibilityService? = null
            private set
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> onScreenOn()
                Broadcasts.WINDOW_OPEN -> onWindowOpen()
                Broadcasts.WINDOW_CLOSE -> onWindowClose()
                // INACTIVITY_FIRED is owned by InactivityReceiver (manifest-registered)
                // so OEM process-killing can't drop the alarm on the floor.
                Broadcasts.TEST_TRIGGER_DRY_RUN -> onTestTrigger(dryRun = true)
                Broadcasts.TEST_TRIGGER_LIVE -> onTestTrigger(dryRun = false)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repo = SettingsRepository(applicationContext)
        alarms = AlarmScheduler(applicationContext)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Broadcasts.WINDOW_OPEN)
            addAction(Broadcasts.WINDOW_CLOSE)
            addAction(Broadcasts.TEST_TRIGGER_DRY_RUN)
            addAction(Broadcasts.TEST_TRIGGER_LIVE)
        }
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        Log.i(TAG, "service connected")
        scope.launch {
            repo.setStatus(isArmed = true)
            // On (re)connect, make sure alarms reflect current settings.
            alarms.rearmDailyWindow()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { unregisterReceiver(receiver) }
        scope.launch { repo.setStatus(isArmed = false, isInWindow = false) }
        scope.cancel()
        instance = null
        return super.onUnbind(intent)
    }

    /**
     * Run the shutdown sequence from outside the service (e.g. from a manifest-
     * registered receiver after the process was woken). Returns immediately;
     * the sequence runs on the service's IO scope.
     */
    fun triggerShutdown(dryRun: Boolean) {
        scope.launch {
            val result = PowerOffSequence(this@ShutdownAccessibilityService).run(dryRun = dryRun)
            Log.i(TAG, "external shutdown trigger result (dryRun=$dryRun): $result")
        }
    }

    private fun onScreenOff() {
        Log.i(TAG, "screen off")
        scope.launch {
            val state = repo.state.first()
            if (state.enabled && state.isInWindow) {
                val now = System.currentTimeMillis()
                repo.setCountdownStartedAt(now)
                alarms.scheduleInactivity(now, state.inactivityTimeoutSeconds)
            }
        }
    }

    private fun onScreenOn() {
        Log.i(TAG, "screen on")
        alarms.cancelInactivity()
        scope.launch { repo.setCountdownStartedAt(null) }
    }

    private fun onWindowOpen() {
        Log.i(TAG, "window open")
        scope.launch { repo.setStatus(isInWindow = true) }
        BuzzKillForegroundService.start(applicationContext)
    }

    private fun onWindowClose() {
        Log.i(TAG, "window close")
        alarms.cancelInactivity()
        scope.launch {
            repo.setStatus(isInWindow = false)
            repo.setCountdownStartedAt(null)
        }
        BuzzKillForegroundService.stop(applicationContext)
    }

    private fun onInactivityFired() {
        Log.i(TAG, "inactivity fired")
        scope.launch {
            val state = repo.state.first()
            // Final guard: still in window, still enabled. The screen-on receiver should have
            // cancelled this alarm already if the user came back, but we double-check.
            if (!state.enabled || !state.isInWindow) {
                Log.i(TAG, "inactivity fired but no longer eligible; aborting")
                return@launch
            }
            repo.recordTriggered(System.currentTimeMillis())

            // Safety: refuse to actually power off on the emulator. The dry-run is enough
            // to verify the dialog matching; we don't want a runaway test session to keep
            // rebooting the emulator.
            val onEmulator = com.buzzkill.oem.OemDetector.isEmulator
            val result = PowerOffSequence(this@ShutdownAccessibilityService).run(dryRun = onEmulator)
            Log.i(TAG, "shutdown sequence result (emulator=$onEmulator): $result")
        }
    }

    private fun onTestTrigger(dryRun: Boolean) {
        Log.i(TAG, "test trigger (dryRun=$dryRun)")
        scope.launch {
            val result = PowerOffSequence(this@ShutdownAccessibilityService).run(dryRun = dryRun)
            val msg = when (result) {
                is PowerOffSequence.Result.DryRun ->
                    "Match: '${result.matchedText}' via ${result.mode}. Strings OK."
                is PowerOffSequence.Result.NotFound ->
                    "No match. Visited (${result.visited.size} nodes): ${result.visited.take(12)}"
                is PowerOffSequence.Result.DialogDidNotOpen -> "Power dialog did not open."
                is PowerOffSequence.Result.Triggered ->
                    "Triggered: '${result.matchedText}' via ${result.mode}"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ShutdownAccessibilityService, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

}
