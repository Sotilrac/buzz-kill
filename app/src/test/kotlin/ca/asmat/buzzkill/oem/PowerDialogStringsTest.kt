package ca.asmat.buzzkill.oem

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PowerDialogStringsTest {
    @Test
    fun `OnePlus primaryActions include power off (English) and shutdown variants`() {
        val s = PowerDialogStrings.stringsFor(Oem.OnePlus)
        assertThat(s.primaryActions).contains("power off")
        assertThat(s.primaryActions).contains("shut down")
    }

    @Test
    fun `Pixel primaryActions are Pixel-specific`() {
        val s = PowerDialogStrings.stringsFor(Oem.Pixel)
        assertThat(s.primaryActions).contains("power off")
    }

    @Test
    fun `every OEM has at least one primary and one confirm action`() {
        for (oem in Oem.entries) {
            val s = PowerDialogStrings.stringsFor(oem)
            assertThat(s.primaryActions).isNotEmpty()
            assertThat(s.confirmActions).isNotEmpty()
        }
    }

    @Test
    fun `all strings are lowercase (matcher relies on this)`() {
        for (oem in Oem.entries) {
            val s = PowerDialogStrings.stringsFor(oem)
            for (str in s.primaryActions + s.confirmActions) {
                assertThat(str).isEqualTo(str.lowercase())
            }
        }
    }

    @Test
    fun `allKnownPrimary is a deduplicated union of every OEM`() {
        val expected = Oem.entries.flatMap { PowerDialogStrings.stringsFor(it).primaryActions }.toSet()
        assertThat(PowerDialogStrings.allKnownPrimary.toSet()).isEqualTo(expected)
        // Distinct: list size matches set size.
        assertThat(PowerDialogStrings.allKnownPrimary.size)
            .isEqualTo(PowerDialogStrings.allKnownPrimary.toSet().size)
    }

    @Test
    fun `OnePlus is the only OEM mapped to the SlideDown gesture fallback`() {
        for (oem in Oem.entries) {
            val expected =
                if (oem == Oem.OnePlus) {
                    PowerDialogStrings.GestureFallback.SlideDown
                } else {
                    PowerDialogStrings.GestureFallback.None
                }
            assertThat(PowerDialogStrings.gestureFallbackFor(oem)).isEqualTo(expected)
        }
    }

    @Test
    fun `slide-down markers cover the OnePlus instruction text variants we have observed`() {
        // Real string captured from OxygenOS power dialog.
        val observed =
            "swipe up with two fingers to restart your device. " +
                "swipe down with two fingers to power it off."
        val matched = PowerDialogStrings.slideDownMarkers.any { observed.contains(it) }
        assertThat(matched).isTrue()
    }

    @Test
    fun `slide-down markers are all lowercase substrings`() {
        for (m in PowerDialogStrings.slideDownMarkers) {
            assertThat(m).isEqualTo(m.lowercase())
        }
    }
}
