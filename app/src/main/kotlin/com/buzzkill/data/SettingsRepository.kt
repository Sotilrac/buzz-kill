package com.buzzkill.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "buzzkill_prefs")

/**
 * Single point of access to persisted settings and live status.
 *
 * Settings are user-edited via the UI. Status fields are written by the service and
 * observed by the UI; the service never reads anything mutable from the UI's flows.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val WindowStartMinutes = intPreferencesKey("window_start_minutes")
        val WindowEndMinutes = intPreferencesKey("window_end_minutes")
        val InactivityTimeoutSeconds = intPreferencesKey("inactivity_timeout_seconds")
        val Enabled = booleanPreferencesKey("enabled")

        // Status (service-written)
        val IsInWindow = booleanPreferencesKey("is_in_window")
        val IsArmed = booleanPreferencesKey("is_armed")
        val LastTriggerTime = longPreferencesKey("last_trigger_time")
        val CountdownStartedAt = longPreferencesKey("countdown_started_at")

        // Acknowledgements
        val ScheduledPowerOnAcked = booleanPreferencesKey("ack_scheduled_power_on")
        val OemKillerAcked = booleanPreferencesKey("ack_oem_killer")
        val FirstShutdownConfirmed = booleanPreferencesKey("first_shutdown_confirmed")
    }

    val state: Flow<PersistedState> = context.dataStore.data.map { prefs ->
        PersistedState(
            windowStartMinutes = prefs[Keys.WindowStartMinutes] ?: DEFAULT_WINDOW_START,
            windowEndMinutes = prefs[Keys.WindowEndMinutes] ?: DEFAULT_WINDOW_END,
            inactivityTimeoutSeconds = prefs[Keys.InactivityTimeoutSeconds] ?: DEFAULT_INACTIVITY,
            enabled = prefs[Keys.Enabled] ?: false,
            isInWindow = prefs[Keys.IsInWindow] ?: false,
            isArmed = prefs[Keys.IsArmed] ?: false,
            lastTriggerTime = prefs[Keys.LastTriggerTime],
            countdownStartedAt = prefs[Keys.CountdownStartedAt],
            scheduledPowerOnAcked = prefs[Keys.ScheduledPowerOnAcked] ?: false,
            oemKillerAcked = prefs[Keys.OemKillerAcked] ?: false,
            firstShutdownConfirmed = prefs[Keys.FirstShutdownConfirmed] ?: false,
        )
    }

    suspend fun setWindow(startMinutes: Int, endMinutes: Int) {
        context.dataStore.edit {
            it[Keys.WindowStartMinutes] = startMinutes
            it[Keys.WindowEndMinutes] = endMinutes
        }
    }

    suspend fun setInactivityTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.InactivityTimeoutSeconds] = seconds }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.Enabled] = enabled }
    }

    suspend fun setStatus(isInWindow: Boolean? = null, isArmed: Boolean? = null) {
        context.dataStore.edit { prefs ->
            isInWindow?.let { prefs[Keys.IsInWindow] = it }
            isArmed?.let { prefs[Keys.IsArmed] = it }
        }
    }

    suspend fun setCountdownStartedAt(epochMillis: Long?) {
        context.dataStore.edit { prefs ->
            if (epochMillis == null) prefs.remove(Keys.CountdownStartedAt)
            else prefs[Keys.CountdownStartedAt] = epochMillis
        }
    }

    suspend fun recordTriggered(epochMillis: Long) {
        context.dataStore.edit { it[Keys.LastTriggerTime] = epochMillis }
    }

    suspend fun setScheduledPowerOnAcked(acked: Boolean) {
        context.dataStore.edit { it[Keys.ScheduledPowerOnAcked] = acked }
    }

    suspend fun setOemKillerAcked(acked: Boolean) {
        context.dataStore.edit { it[Keys.OemKillerAcked] = acked }
    }

    suspend fun setFirstShutdownConfirmed(confirmed: Boolean) {
        context.dataStore.edit { it[Keys.FirstShutdownConfirmed] = confirmed }
    }

    companion object {
        const val DEFAULT_WINDOW_START = 23 * 60         // 23:00
        const val DEFAULT_WINDOW_END = 7 * 60            // 07:00
        const val DEFAULT_INACTIVITY = 5 * 60            // 5 min
        const val MIN_INACTIVITY_MINUTES = 1
        const val MAX_INACTIVITY_MINUTES = 15
    }
}

data class PersistedState(
    val windowStartMinutes: Int,
    val windowEndMinutes: Int,
    val inactivityTimeoutSeconds: Int,
    val enabled: Boolean,
    val isInWindow: Boolean,
    val isArmed: Boolean,
    val lastTriggerTime: Long?,
    val countdownStartedAt: Long?,
    val scheduledPowerOnAcked: Boolean,
    val oemKillerAcked: Boolean,
    val firstShutdownConfirmed: Boolean,
)
