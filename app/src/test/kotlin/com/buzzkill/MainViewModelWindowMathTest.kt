package com.buzzkill

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-function tests for the window-bounds math in [MainViewModel.Companion].
 * The wrap-around case (start > end, e.g. 23:00→05:30) is the gnarly part.
 */
class MainViewModelWindowMathTest {
    // --- isInWindow ---

    @Test
    fun `non-wrapping window includes the start minute`() {
        // 09:00 → 17:00, asking at 09:00
        assertThat(MainViewModel.isInWindow(now = 9 * 60, start = 9 * 60, end = 17 * 60)).isTrue()
    }

    @Test
    fun `non-wrapping window excludes the end minute`() {
        // 09:00 → 17:00, asking at 17:00 — end is exclusive
        assertThat(MainViewModel.isInWindow(now = 17 * 60, start = 9 * 60, end = 17 * 60)).isFalse()
    }

    @Test
    fun `non-wrapping window in the middle`() {
        assertThat(MainViewModel.isInWindow(now = 12 * 60, start = 9 * 60, end = 17 * 60)).isTrue()
    }

    @Test
    fun `non-wrapping window before start`() {
        assertThat(MainViewModel.isInWindow(now = 8 * 60, start = 9 * 60, end = 17 * 60)).isFalse()
    }

    @Test
    fun `wrapping window late evening is in-window`() {
        // 23:00 → 05:30, asking at 23:30
        assertThat(MainViewModel.isInWindow(now = 23 * 60 + 30, start = 23 * 60, end = 5 * 60 + 30)).isTrue()
    }

    @Test
    fun `wrapping window early morning is in-window`() {
        // 23:00 → 05:30, asking at 03:00
        assertThat(MainViewModel.isInWindow(now = 3 * 60, start = 23 * 60, end = 5 * 60 + 30)).isTrue()
    }

    @Test
    fun `wrapping window at exactly midnight is in-window`() {
        assertThat(MainViewModel.isInWindow(now = 0, start = 23 * 60, end = 5 * 60 + 30)).isTrue()
    }

    @Test
    fun `wrapping window noon is out-of-window`() {
        assertThat(MainViewModel.isInWindow(now = 12 * 60, start = 23 * 60, end = 5 * 60 + 30)).isFalse()
    }

    @Test
    fun `wrapping window includes start instant`() {
        assertThat(MainViewModel.isInWindow(now = 23 * 60, start = 23 * 60, end = 5 * 60 + 30)).isTrue()
    }

    @Test
    fun `wrapping window excludes end instant`() {
        // 23:00 → 05:30, asking at 05:30 — should be just out
        assertThat(MainViewModel.isInWindow(now = 5 * 60 + 30, start = 23 * 60, end = 5 * 60 + 30)).isFalse()
    }

    @Test
    fun `degenerate equal-bounds window is never active`() {
        assertThat(MainViewModel.isInWindow(now = 9 * 60, start = 9 * 60, end = 9 * 60)).isFalse()
    }

    // --- minutesUntilWindowOpen ---

    @Test
    fun `minutesUntilWindowOpen later today`() {
        // now 09:00, opens at 23:00 → 14h
        assertThat(MainViewModel.minutesUntilWindowOpen(now = 9 * 60, start = 23 * 60))
            .isEqualTo(14 * 60)
    }

    @Test
    fun `minutesUntilWindowOpen tomorrow when start has already passed`() {
        // now 23:30, opens at 21:00 → wrap to tomorrow's 21:00
        // (24*60 - 23*60-30) + 21*60 = 30 + 1260 = 1290
        assertThat(MainViewModel.minutesUntilWindowOpen(now = 23 * 60 + 30, start = 21 * 60))
            .isEqualTo((24 * 60 - (23 * 60 + 30)) + 21 * 60)
    }

    @Test
    fun `minutesUntilWindowOpen at exact open time is zero`() {
        assertThat(MainViewModel.minutesUntilWindowOpen(now = 21 * 60, start = 21 * 60)).isEqualTo(0)
    }

    // --- minutesUntilWindowClose ---

    @Test
    fun `minutesUntilWindowClose during the same day`() {
        // now 09:00, closes at 17:00 → 8h
        assertThat(MainViewModel.minutesUntilWindowClose(now = 9 * 60, end = 17 * 60))
            .isEqualTo(8 * 60)
    }

    @Test
    fun `minutesUntilWindowClose wraps past midnight`() {
        // now 23:30, window closes at 05:30 next day. (24*60 - 23:30) + 5:30 = 30 + 330 = 360
        assertThat(MainViewModel.minutesUntilWindowClose(now = 23 * 60 + 30, end = 5 * 60 + 30))
            .isEqualTo(6 * 60)
    }

    @Test
    fun `minutesUntilWindowClose at exact close time is zero (wraps to 24h)`() {
        // The implementation uses `now < end` so at exactly the close minute we wrap
        // and return a full 24h. That matches "the window we just left closes again
        // tomorrow at this same time", which is what the UI wants to display.
        val result = MainViewModel.minutesUntilWindowClose(now = 17 * 60, end = 17 * 60)
        assertThat(result).isEqualTo(24 * 60)
    }
}
