package com.buzzkill.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.buzzkill.oem.OemDetector
import com.buzzkill.oem.PowerDialogStrings
import kotlinx.coroutines.delay

/**
 * Runs the system-power-dialog → tap-power-off sequence.
 *
 * dryRun=true stops one step before the final tap and returns the matched node text
 * via [DryRunResult]. Used by the Test trigger so the user can verify the match
 * without actually shutting the phone down.
 */
class PowerOffSequence(private val service: AccessibilityService) {

    sealed interface Result {
        data class Triggered(val matchedText: String) : Result
        data class DryRun(val matchedText: String) : Result
        data class NotFound(val visited: List<String>) : Result
        data object DialogDidNotOpen : Result
    }

    suspend fun run(dryRun: Boolean): Result {
        Log.i(TAG, "running power-off sequence (dryRun=$dryRun)")
        val opened = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        if (!opened) return Result.DialogDidNotOpen

        // Wait for the dialog to render. 500 ms is plenty on OxygenOS; bump if needed.
        delay(POST_DIALOG_DELAY_MS)

        val strings = PowerDialogStrings.stringsFor(OemDetector.current)
        val primaryTargets = (strings.primaryActions + PowerDialogStrings.allKnownPrimary).distinct()

        val visited = mutableListOf<String>()
        val hit = findClickableMatching(service.rootInActiveWindow, primaryTargets, visited)
            ?: return Result.NotFound(visited)

        if (dryRun) {
            hit.clickable.recycleSafely()
            return Result.DryRun(hit.matchedText)
        }

        val clicked = hit.clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        hit.clickable.recycleSafely()
        if (!clicked) return Result.NotFound(listOf("primary node found but ACTION_CLICK failed: '${hit.matchedText}'"))

        // Confirmation dialog (if any). Best-effort: walk again, tap any matching button.
        delay(POST_TAP_DELAY_MS)
        val confirmTargets = (strings.confirmActions + PowerDialogStrings.allKnownConfirm).distinct()
        val confirmHit = findClickableMatching(service.rootInActiveWindow, confirmTargets, mutableListOf())
        confirmHit?.clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        confirmHit?.clickable?.recycleSafely()

        return Result.Triggered(hit.matchedText)
    }

    private data class Hit(val clickable: AccessibilityNodeInfo, val matchedText: String)

    private fun findClickableMatching(
        root: AccessibilityNodeInfo?,
        targets: List<String>,
        visited: MutableList<String>,
    ): Hit? {
        if (root == null) return null
        val targetSet = targets.map { it.lowercase().trim() }.toSet()
        return walk(root, targetSet, visited)
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        targets: Set<String>,
        visited: MutableList<String>,
    ): Hit? {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val textLc = text?.lowercase()
        val descLc = desc?.lowercase()
        if (!textLc.isNullOrEmpty()) visited += textLc
        if (!descLc.isNullOrEmpty() && descLc != textLc) visited += descLc

        val matched: String? = when {
            !textLc.isNullOrEmpty() && textLc in targets -> text
            !descLc.isNullOrEmpty() && descLc in targets -> desc
            else -> null
        }

        if (matched != null) {
            // The matched text often lives on a label whose parent is the clickable
            // button. Walk up to find the clickable, but keep the child's text as
            // the "matched" display string.
            var clickable: AccessibilityNodeInfo? = node
            while (clickable != null && !clickable.isClickable) {
                clickable = clickable.parent
            }
            val target = clickable ?: node.takeIf { it.isClickable }
            if (target != null) return Hit(target, matched)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val h = walk(child, targets, visited)
            if (h != null) return h
        }
        return null
    }

    private fun AccessibilityNodeInfo.recycleSafely() {
        // recycle() is a no-op since API 33 but harmless to keep for older targets.
        @Suppress("DEPRECATION")
        try {
            recycle()
        } catch (_: Throwable) {
            // no-op
        }
    }

    companion object {
        private const val TAG = "BuzzKill.poweroff"
        private const val POST_DIALOG_DELAY_MS = 500L
        private const val POST_TAP_DELAY_MS = 400L
    }
}
