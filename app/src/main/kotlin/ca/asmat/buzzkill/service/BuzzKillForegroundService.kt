package ca.asmat.buzzkill.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ca.asmat.buzzkill.MainActivity

/**
 * Persistent foreground service while the active window is open. The work itself is
 * done by [ShutdownAccessibilityService]; this service exists so OxygenOS / Doze
 * doesn't sweep the app from memory mid-window.
 */
class BuzzKillForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startInForeground()
        }
        return START_STICKY
    }

    private fun startInForeground() {
        ensureChannel()
        val notif = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIF_ID, notif)
            }
            Log.i(TAG, "foreground service started")
        } catch (t: Throwable) {
            // ForegroundServiceStartNotAllowedException, MissingForegroundServiceTypeException,
            // SecurityException — all can land here on newer Android / preview builds. Don't
            // crash the process; the timer logic in the accessibility service still works
            // without an FGS, just with worse OS-retention guarantees.
            Log.w(TAG, "FGS start failed; continuing without foreground notification", t)
            stopSelf()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "BuzzKill window active",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while the inactivity-shutdown window is open."
                setShowBadge(false)
            }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("BuzzKill armed")
            .setContentText("Window is active. Phone will power off after inactivity.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_STOP = "ca.asmat.buzzkill.action.FGS_STOP"
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "buzzkill_active"
        private const val TAG = "BuzzKill.fgs"

        fun start(context: android.content.Context) {
            val intent = Intent(context, BuzzKillForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                // ForegroundServiceStartNotAllowedException, SecurityException etc. on
                // newer Android can land here. The timer logic in the accessibility
                // service still works without an FGS — just with worse OS retention.
                Log.w(TAG, "FGS start call failed; continuing without it", t)
            }
        }

        fun stop(context: android.content.Context) {
            val intent =
                Intent(context, BuzzKillForegroundService::class.java)
                    .setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "FGS stop call failed", t)
            }
        }
    }
}
