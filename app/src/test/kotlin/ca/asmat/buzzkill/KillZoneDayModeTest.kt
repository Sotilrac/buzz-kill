package ca.asmat.buzzkill

import ca.asmat.buzzkill.data.DayMode
import ca.asmat.buzzkill.data.DefaultDayModes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek

/**
 * Tests for [MainViewModel.isKillZoneActive] — the per-weekday "advanced day"
 * (Hacker-mode) override that gates whether the kill zone engages on a given day.
 *
 * The regression that motivated these: per-day modes were stored and shown in the
 * UI but never consulted, so a day set to [DayMode.Off] still killed the phone
 * inside the time window (e.g. weekend pause failed at Saturday 01:00).
 *
 * Day attribution is by the CURRENT calendar day at the instant of evaluation: the
 * early-morning tail of a wrapping window (e.g. 21:00→05:30) belongs to the day it
 * is currently, not the day the window opened the previous evening.
 */
class KillZoneDayModeTest {
    private val START = 21 * 60 // 21:00
    private val END = 5 * 60 + 30 // 05:30 (wraps midnight)

    private fun modes(vararg overrides: Pair<DayOfWeek, DayMode>): Map<DayOfWeek, DayMode> =
        DefaultDayModes + overrides.toMap()

    private fun active(
        day: DayOfWeek,
        now: Int,
        dayModes: Map<DayOfWeek, DayMode>,
        start: Int = START,
        end: Int = END,
    ) = MainViewModel.isKillZoneActive(day, now, start, end, dayModes)

    // --- DayMode.Off: never engages, regardless of time ---

    @Test
    fun `Off day does not kill in the early morning window (the reported bug)`() {
        // Saturday 01:00, Saturday set to Off. Must NOT be active.
        val m = modes(DayOfWeek.SATURDAY to DayMode.Off)
        assertThat(active(DayOfWeek.SATURDAY, 1 * 60, m)).isFalse()
    }

    @Test
    fun `Off day does not kill in the late evening window`() {
        val m = modes(DayOfWeek.SATURDAY to DayMode.Off)
        assertThat(active(DayOfWeek.SATURDAY, 23 * 60, m)).isFalse()
    }

    @Test
    fun `Off day does not kill at midday`() {
        val m = modes(DayOfWeek.SATURDAY to DayMode.Off)
        assertThat(active(DayOfWeek.SATURDAY, 12 * 60, m)).isFalse()
    }

    // --- DayMode.Zone: the configured time window applies ---

    @Test
    fun `Zone day kills inside the late-evening window`() {
        val m = modes(DayOfWeek.MONDAY to DayMode.Zone)
        assertThat(active(DayOfWeek.MONDAY, 22 * 60, m)).isTrue()
    }

    @Test
    fun `Zone day kills inside the early-morning wrap`() {
        // Monday 01:00 is within the wrapping window → active.
        val m = modes(DayOfWeek.MONDAY to DayMode.Zone)
        assertThat(active(DayOfWeek.MONDAY, 1 * 60, m)).isTrue()
    }

    @Test
    fun `Zone day does not kill outside the window`() {
        val m = modes(DayOfWeek.MONDAY to DayMode.Zone)
        assertThat(active(DayOfWeek.MONDAY, 12 * 60, m)).isFalse()
    }

    @Test
    fun `Zone day respects a non-wrapping window`() {
        val m = modes(DayOfWeek.MONDAY to DayMode.Zone)
        assertThat(active(DayOfWeek.MONDAY, 10 * 60, m, start = 9 * 60, end = 17 * 60)).isTrue()
        assertThat(active(DayOfWeek.MONDAY, 18 * 60, m, start = 9 * 60, end = 17 * 60)).isFalse()
    }

    // --- DayMode.AllDay: active 24h ---

    @Test
    fun `AllDay day kills at any time including outside the window`() {
        val m = modes(DayOfWeek.SATURDAY to DayMode.AllDay)
        assertThat(active(DayOfWeek.SATURDAY, 1 * 60, m)).isTrue()
        assertThat(active(DayOfWeek.SATURDAY, 12 * 60, m)).isTrue()
        assertThat(active(DayOfWeek.SATURDAY, 23 * 60, m)).isTrue()
        assertThat(active(DayOfWeek.SATURDAY, 0, m)).isTrue()
    }

    // --- Day attribution: current calendar day, not the day the window opened ---

    @Test
    fun `weekend pause stops the kill at the Friday-to-Saturday midnight boundary`() {
        // Weekdays Zone, weekend Off. Friday 23:00 kills; Saturday 01:00 does not,
        // even though both fall in one continuous 21:00→05:30 window.
        val m = modes(
            DayOfWeek.FRIDAY to DayMode.Zone,
            DayOfWeek.SATURDAY to DayMode.Off,
        )
        assertThat(active(DayOfWeek.FRIDAY, 23 * 60, m)).isTrue()
        assertThat(active(DayOfWeek.SATURDAY, 1 * 60, m)).isFalse()
    }

    @Test
    fun `weekday kill resumes at the Sunday-to-Monday midnight boundary`() {
        // Sunday Off, Monday Zone. Sunday 23:00 does not kill; Monday 01:00 does.
        val m = modes(
            DayOfWeek.SUNDAY to DayMode.Off,
            DayOfWeek.MONDAY to DayMode.Zone,
        )
        assertThat(active(DayOfWeek.SUNDAY, 23 * 60, m)).isFalse()
        assertThat(active(DayOfWeek.MONDAY, 1 * 60, m)).isTrue()
    }

    // --- Defaults: weekdays Zone, weekends AllDay ---

    @Test
    fun `default weekday uses the time window`() {
        assertThat(active(DayOfWeek.WEDNESDAY, 22 * 60, DefaultDayModes)).isTrue()
        assertThat(active(DayOfWeek.WEDNESDAY, 12 * 60, DefaultDayModes)).isFalse()
    }

    @Test
    fun `default weekend is all-day`() {
        assertThat(active(DayOfWeek.SATURDAY, 12 * 60, DefaultDayModes)).isTrue()
        assertThat(active(DayOfWeek.SUNDAY, 12 * 60, DefaultDayModes)).isTrue()
    }

    @Test
    fun `missing day entry falls back to the default for that day`() {
        // Empty map → defaults apply. Weekday Wednesday → Zone semantics.
        val empty = emptyMap<DayOfWeek, DayMode>()
        assertThat(active(DayOfWeek.WEDNESDAY, 22 * 60, empty)).isTrue()
        assertThat(active(DayOfWeek.WEDNESDAY, 12 * 60, empty)).isFalse()
        // Weekend Saturday → AllDay semantics.
        assertThat(active(DayOfWeek.SATURDAY, 12 * 60, empty)).isTrue()
    }

    // --- Exhaustive matrix: every mode × in/out of window ---

    @Test
    fun `every mode behaves correctly inside and outside the window`() {
        val inWindow = 23 * 60 // within 21:00→05:30
        val outWindow = 12 * 60 // outside
        val day = DayOfWeek.TUESDAY

        // Off: never active.
        assertThat(active(day, inWindow, modes(day to DayMode.Off))).isFalse()
        assertThat(active(day, outWindow, modes(day to DayMode.Off))).isFalse()

        // Zone: active only inside window.
        assertThat(active(day, inWindow, modes(day to DayMode.Zone))).isTrue()
        assertThat(active(day, outWindow, modes(day to DayMode.Zone))).isFalse()

        // AllDay: always active.
        assertThat(active(day, inWindow, modes(day to DayMode.AllDay))).isTrue()
        assertThat(active(day, outWindow, modes(day to DayMode.AllDay))).isTrue()
    }
}
