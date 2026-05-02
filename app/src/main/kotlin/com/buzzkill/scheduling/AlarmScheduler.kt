package com.buzzkill.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.buzzkill.MainViewModel
import com.buzzkill.data.PersistedState
import com.buzzkill.data.SettingsRepository
import com.buzzkill.service.Broadcasts
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Owns the three AlarmManager pending intents:
 *  - WINDOW_OPEN  daily, at the configured start time
 *  - WINDOW_CLOSE daily, at the configured end time
 *  - INACTIVITY   one-shot, scheduled when the screen turns off inside the window
 *
 * All three use [AlarmManager.setAlarmClock]: it survives Doze, fires on time, and does
 * not require SCHEDULE_EXACT_ALARM. The downside is the alarm shows up in the system
 * "Next alarm" UI, which is an acceptable trade-off for an app whose whole purpose is
 * to power the device off on schedule.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rearmDailyWindow() {
        val s = SettingsRepository(context).state.first()
        if (!s.enabled) {
            cancelDailyWindow()
            cancelInactivity()
            // Make sure the service tears down state and the FGS stops.
            context.sendBroadcast(internalIntent(Broadcasts.WINDOW_CLOSE))
            return
        }
        scheduleDaily(REQ_OPEN, s.windowStartMinutes, Broadcasts.WINDOW_OPEN)
        scheduleDaily(REQ_CLOSE, s.windowEndMinutes, Broadcasts.WINDOW_CLOSE)

        // If we're already inside the window at re-arm time, fire WINDOW_OPEN now.
        val now = MainViewModel.currentMinuteOfDay()
        if (MainViewModel.isInWindow(now, s.windowStartMinutes, s.windowEndMinutes)) {
            context.sendBroadcast(internalIntent(Broadcasts.WINDOW_OPEN))
        } else {
            context.sendBroadcast(internalIntent(Broadcasts.WINDOW_CLOSE))
        }
        Log.i(TAG, "daily window armed: ${s.windowStartMinutes} → ${s.windowEndMinutes}")
    }

    fun cancelDailyWindow() {
        alarmManager.cancel(broadcastPending(REQ_OPEN, Broadcasts.WINDOW_OPEN))
        alarmManager.cancel(broadcastPending(REQ_CLOSE, Broadcasts.WINDOW_CLOSE))
    }

    /** One-shot, fired inactivityTimeoutSeconds after [referenceMillis]. */
    fun scheduleInactivity(referenceMillis: Long, inactivityTimeoutSeconds: Int) {
        val triggerAt = referenceMillis + inactivityTimeoutSeconds * 1000L
        val pi = broadcastPending(REQ_INACTIVITY, Broadcasts.INACTIVITY_FIRED)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, openMainActivityPendingIntent()),
            pi,
        )
        Log.i(TAG, "inactivity scheduled for $triggerAt (in ${inactivityTimeoutSeconds}s)")
    }

    fun cancelInactivity() {
        alarmManager.cancel(broadcastPending(REQ_INACTIVITY, Broadcasts.INACTIVITY_FIRED))
    }

    private fun scheduleDaily(requestCode: Int, minuteOfDay: Int, action: String) {
        val triggerAt = nextOccurrenceMillis(minuteOfDay)
        val pi = broadcastPending(requestCode, action)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, openMainActivityPendingIntent()),
            pi,
        )
    }

    private fun nextOccurrenceMillis(minuteOfDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun broadcastPending(requestCode: Int, action: String): PendingIntent {
        val intent = internalIntent(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun internalIntent(action: String): Intent =
        Intent(action).setPackage(context.packageName)

    /** Used as the "show me the alarm" intent on the alarm-clock indicator. */
    private fun openMainActivityPendingIntent(): PendingIntent {
        val intent = Intent().apply {
            setClassName(context, "com.buzzkill.MainActivity")
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQ_SHOW, intent, flags)
    }

    companion object {
        private const val TAG = "BuzzKill.alarm"
        private const val REQ_OPEN = 1001
        private const val REQ_CLOSE = 1002
        private const val REQ_INACTIVITY = 1003
        private const val REQ_SHOW = 1099

        suspend fun rearm(context: Context) = AlarmScheduler(context).rearmDailyWindow()

        /** Helper for callers that only have a snapshot of state. */
        fun isInWindowOf(state: PersistedState): Boolean {
            val now = MainViewModel.currentMinuteOfDay()
            return MainViewModel.isInWindow(now, state.windowStartMinutes, state.windowEndMinutes)
        }
    }
}
