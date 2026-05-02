package com.buzzkill.oem

/**
 * Strings to match against the system power dialog. Keep these LOWERCASED here; the
 * matcher does a case-insensitive `equals` against trimmed node text.
 *
 * Two phases:
 *  - [primaryActions]: the first dialog. We tap one of these.
 *  - [confirmActions]: a confirmation dialog that may follow.
 *
 * Add OEMs by adding rows to [stringsFor]. The matcher uses the union of all strings
 * unless a manufacturer-specific row is requested, so unknown OEMs get reasonable
 * coverage out of the box.
 */
object PowerDialogStrings {

    data class StringSet(
        val primaryActions: List<String>,
        val confirmActions: List<String>,
    )

    fun stringsFor(oem: Oem): StringSet = when (oem) {
        Oem.OnePlus, Oem.Oppo, Oem.Realme -> StringSet(
            primaryActions = listOf("power off", "shut down", "shutdown", "apagar"),
            confirmActions = listOf("power off", "ok", "shut down", "tap to power off"),
        )
        Oem.Pixel -> StringSet(
            primaryActions = listOf("power off"),
            confirmActions = listOf("power off", "ok"),
        )
        Oem.Samsung -> StringSet(
            primaryActions = listOf("power off"),
            confirmActions = listOf("power off", "ok"),
        )
        Oem.Xiaomi -> StringSet(
            primaryActions = listOf("power off", "shutdown"),
            confirmActions = listOf("power off", "ok"),
        )
        Oem.Huawei -> StringSet(
            primaryActions = listOf("power off"),
            confirmActions = listOf("power off", "ok"),
        )
        Oem.Vivo, Oem.Generic -> StringSet(
            primaryActions = listOf("power off", "shut down", "shutdown"),
            confirmActions = listOf("power off", "ok", "tap to power off"),
        )
    }

    /** Union of all known strings, used as a fallback. */
    val allKnownPrimary: List<String> = Oem.entries.flatMap { stringsFor(it).primaryActions }.distinct()
    val allKnownConfirm: List<String> = Oem.entries.flatMap { stringsFor(it).confirmActions }.distinct()

    /**
     * For dialogs with no clickable "Power off" target — the action is a fixed gesture.
     * The matcher uses substring/contains against visited node text to detect the dialog.
     */
    enum class GestureFallback { None, TwoFingerSwipeDown }

    fun gestureFallbackFor(oem: Oem): GestureFallback = when (oem) {
        // OxygenOS 13+ on OnePlus 11 etc.: instruction caption + two-finger swipe.
        Oem.OnePlus -> GestureFallback.TwoFingerSwipeDown
        else -> GestureFallback.None
    }

    /** Marker substrings (lowercase) that identify a two-finger-swipe-style dialog. */
    val twoFingerSwipeDownMarkers: List<String> = listOf(
        "two fingers to power it off",
        "two fingers to power off",
        "swipe down with two fingers",
    )
}
