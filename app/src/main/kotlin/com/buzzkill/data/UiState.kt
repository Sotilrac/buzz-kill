package com.buzzkill.data

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
