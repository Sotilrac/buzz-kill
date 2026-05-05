package ca.asmat.buzzkill.data

import java.time.DayOfWeek

/**
 * Per-weekday kill-zone mode (Hacker mode panel).
 *  - [Off]    : kill zone never engages on this day.
 *  - [Zone]   : the global kill-zone time window applies.
 *  - [AllDay] : kill zone is active 24h on this day (inactivity still required).
 */
enum class DayMode { Off, Zone, AllDay }

/** Default mode per day: weekdays use the kill zone, weekends are all-day. */
val DefaultDayModes: Map<DayOfWeek, DayMode> = DayOfWeek.entries.associateWith { d ->
    when (d) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayMode.AllDay
        else -> DayMode.Zone
    }
}

/**
 * Status the UI shows in the header. Derived from PersistedState + permissions.
 */
sealed class StatusLine {
    /** Permissions incomplete; show checklist. */
    data class NeedsSetup(
        val missing: List<String>,
    ) : StatusLine()

    /** Master toggle off. */
    data object Disabled : StatusLine()

    /** Outside the active window; counting down to next window-open. */
    data class Armed(
        val nextOpenMinutes: Int,
    ) : StatusLine()

    /** Inside the window; foreground service is running. */
    data class Active(
        val minutesRemainingInWindow: Int,
    ) : StatusLine()

    /** Inside the window AND the inactivity countdown is currently running. */
    data class Counting(
        val secondsRemaining: Int,
    ) : StatusLine()
}

data class UiState(
    val persisted: PersistedState,
    val permissions: PermissionStatus,
    val status: StatusLine,
)

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val batteryOptimizationExempt: Boolean,
    val notificationsGranted: Boolean,
    val scheduledPowerOnAcked: Boolean,
    val oemKillerAcked: Boolean,
) {
    val allGranted: Boolean
        get() =
            accessibilityEnabled &&
                batteryOptimizationExempt &&
                notificationsGranted &&
                scheduledPowerOnAcked &&
                oemKillerAcked

    val missingItems: List<String>
        get() =
            buildList {
                if (!accessibilityEnabled) add("Accessibility service")
                if (!batteryOptimizationExempt) add("Battery optimization")
                if (!notificationsGranted) add("Notifications")
                if (!scheduledPowerOnAcked) add("Scheduled power-on")
                if (!oemKillerAcked) add("Battery-killer override")
            }
}
