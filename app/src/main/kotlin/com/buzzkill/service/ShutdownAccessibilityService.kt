package com.buzzkill.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.buzzkill.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShutdownAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: SettingsRepository

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
                Intent.ACTION_USER_PRESENT -> onScreenOn()
                Broadcasts.WINDOW_OPEN -> onWindowOpen()
                Broadcasts.WINDOW_CLOSE -> onWindowClose()
                Broadcasts.INACTIVITY_FIRED -> onInactivityFired()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = SettingsRepository(applicationContext)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Broadcasts.WINDOW_OPEN)
            addAction(Broadcasts.WINDOW_CLOSE)
            addAction(Broadcasts.INACTIVITY_FIRED)
        }
        // RECEIVER_NOT_EXPORTED on Android 14+; system actions are fine, our private
        // actions go through the not-exported flow.
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        Log.i(TAG, "service connected; screen + window broadcasts registered")
        scope.launch { repo.setStatus(isArmed = true) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't react to events; we use this service for global actions only.
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { unregisterReceiver(receiver) }
        scope.launch { repo.setStatus(isArmed = false, isInWindow = false) }
        scope.cancel()
        return super.onUnbind(intent)
    }

    private fun onScreenOff() {
        Log.i(TAG, "screen off")
        // Phase 7 will schedule the inactivity alarm here, gated on isInWindow.
        scope.launch {
            repo.setCountdownStartedAt(System.currentTimeMillis())
        }
    }

    private fun onScreenOn() {
        Log.i(TAG, "screen on")
        scope.launch {
            repo.setCountdownStartedAt(null)
        }
        // Phase 7 will cancel the inactivity alarm here.
    }

    private fun onWindowOpen() {
        Log.i(TAG, "window open")
        scope.launch { repo.setStatus(isInWindow = true) }
    }

    private fun onWindowClose() {
        Log.i(TAG, "window close")
        scope.launch {
            repo.setStatus(isInWindow = false)
            repo.setCountdownStartedAt(null)
        }
        // Phase 7 will cancel any pending inactivity alarm here.
    }

    private fun onInactivityFired() {
        Log.i(TAG, "inactivity fired (phase 8 will trigger shutdown)")
        scope.launch { repo.recordTriggered(System.currentTimeMillis()) }
    }

    companion object {
        private const val TAG = "BuzzKill.svc"
    }
}
