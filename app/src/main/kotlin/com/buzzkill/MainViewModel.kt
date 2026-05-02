package com.buzzkill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzkill.data.PermissionStatus
import com.buzzkill.data.PersistedState
import com.buzzkill.data.SettingsRepository
import com.buzzkill.data.StatusLine
import com.buzzkill.data.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    /**
     * Permission status is recomputed from the system on each onResume by MainActivity,
     * which calls [refreshPermissions].
     */
    private val permissionsFlow = MutableStateFlow(
        PermissionStatus(
            accessibilityEnabled = false,
            batteryOptimizationExempt = false,
            notificationsGranted = false,
            scheduledPowerOnAcked = false,
            oemKillerAcked = false,
        ),
    )

    val uiState: StateFlow<UiState> = combine(repo.state, permissionsFlow) { persisted, perms ->
        // Sync ack flags into the in-memory permission view so the UI reflects DataStore.
        val effectivePerms = perms.copy(
            scheduledPowerOnAcked = persisted.scheduledPowerOnAcked,
            oemKillerAcked = persisted.oemKillerAcked,
        )
        UiState(
            persisted = persisted,
            permissions = effectivePerms,
            status = deriveStatus(persisted, effectivePerms),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(
            persisted = defaultPersisted(),
            permissions = permissionsFlow.value,
            status = StatusLine.Disabled,
        ),
    )

    fun refreshPermissions(
        accessibilityEnabled: Boolean,
        batteryOptimizationExempt: Boolean,
        notificationsGranted: Boolean,
    ) {
        permissionsFlow.value = permissionsFlow.value.copy(
            accessibilityEnabled = accessibilityEnabled,
            batteryOptimizationExempt = batteryOptimizationExempt,
            notificationsGranted = notificationsGranted,
        )
    }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { repo.setEnabled(enabled) }
    fun setWindow(startMin: Int, endMin: Int) = viewModelScope.launch { repo.setWindow(startMin, endMin) }
    fun setInactivitySeconds(s: Int) = viewModelScope.launch { repo.setInactivityTimeoutSeconds(s) }
    fun setScheduledPowerOnAcked(b: Boolean) = viewModelScope.launch { repo.setScheduledPowerOnAcked(b) }
    fun setOemKillerAcked(b: Boolean) = viewModelScope.launch { repo.setOemKillerAcked(b) }

    private fun defaultPersisted() = PersistedState(
        windowStartMinutes = SettingsRepository.DEFAULT_WINDOW_START,
        windowEndMinutes = SettingsRepository.DEFAULT_WINDOW_END,
        inactivityTimeoutSeconds = SettingsRepository.DEFAULT_INACTIVITY,
        enabled = false,
        isInWindow = false,
        isArmed = false,
        lastTriggerTime = null,
        countdownStartedAt = null,
        scheduledPowerOnAcked = false,
        oemKillerAcked = false,
        firstShutdownConfirmed = false,
    )

    private fun deriveStatus(p: PersistedState, perms: PermissionStatus): StatusLine {
        if (!perms.allGranted) return StatusLine.NeedsSetup(perms.missingItems)
        if (!p.enabled) return StatusLine.Disabled
        val nowMinute = currentMinuteOfDay()
        val inWindow = isInWindow(nowMinute, p.windowStartMinutes, p.windowEndMinutes)
        return if (inWindow) {
            val countdown = p.countdownStartedAt
            if (countdown != null) {
                val elapsedSec = ((System.currentTimeMillis() - countdown) / 1000L).toInt()
                StatusLine.Counting((p.inactivityTimeoutSeconds - elapsedSec).coerceAtLeast(0))
            } else {
                StatusLine.Active(minutesUntilWindowClose(nowMinute, p.windowEndMinutes))
            }
        } else {
            StatusLine.Armed(minutesUntilWindowOpen(nowMinute, p.windowStartMinutes))
        }
    }

    companion object {
        fun currentMinuteOfDay(): Int = Calendar.getInstance().run {
            get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
        }

        /** Window may wrap midnight (e.g. 23:00 → 07:00). */
        fun isInWindow(now: Int, start: Int, end: Int): Boolean =
            if (start == end) false
            else if (start < end) now in start until end
            else now >= start || now < end

        fun minutesUntilWindowOpen(now: Int, start: Int): Int =
            if (now <= start) start - now else (24 * 60 - now) + start

        fun minutesUntilWindowClose(now: Int, end: Int): Int =
            if (now < end) end - now else (24 * 60 - now) + end
    }
}
