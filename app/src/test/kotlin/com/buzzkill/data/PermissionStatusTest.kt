package com.buzzkill.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStatusTest {
    private fun perms(
        accessibility: Boolean = true,
        battery: Boolean = true,
        notifications: Boolean = true,
        scheduledPowerOn: Boolean = true,
        oemKiller: Boolean = true,
    ) = PermissionStatus(
        accessibilityEnabled = accessibility,
        batteryOptimizationExempt = battery,
        notificationsGranted = notifications,
        scheduledPowerOnAcked = scheduledPowerOn,
        oemKillerAcked = oemKiller,
    )

    @Test
    fun `allGranted is true when every flag is true`() {
        assertThat(perms().allGranted).isTrue()
    }

    @Test
    fun `allGranted is false when any single flag is false`() {
        assertThat(perms(accessibility = false).allGranted).isFalse()
        assertThat(perms(battery = false).allGranted).isFalse()
        assertThat(perms(notifications = false).allGranted).isFalse()
        assertThat(perms(scheduledPowerOn = false).allGranted).isFalse()
        assertThat(perms(oemKiller = false).allGranted).isFalse()
    }

    @Test
    fun `missingItems is empty when allGranted`() {
        assertThat(perms().missingItems).isEmpty()
    }

    @Test
    fun `missingItems lists the right human-readable names`() {
        val all =
            perms(
                accessibility = false,
                battery = false,
                notifications = false,
                scheduledPowerOn = false,
                oemKiller = false,
            )
        assertThat(all.missingItems)
            .containsExactly(
                "Accessibility service",
                "Battery optimization",
                "Notifications",
                "Scheduled power-on",
                "Battery-killer override",
            ).inOrder()
    }

    @Test
    fun `missingItems is in the same order as the checklist`() {
        // The UI expects accessibility first, OEM-killer last; the order matters.
        val twoMissing = perms(accessibility = false, oemKiller = false)
        assertThat(twoMissing.missingItems)
            .containsExactly(
                "Accessibility service",
                "Battery-killer override",
            ).inOrder()
    }
}
