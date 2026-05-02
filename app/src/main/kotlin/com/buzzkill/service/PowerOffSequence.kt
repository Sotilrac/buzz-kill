package com.buzzkill.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.buzzkill.oem.OemDetector
import com.buzzkill.oem.PowerDialogStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Runs the system-power-dialog → tap-power-off sequence.
 *
 * Two paths:
 *  - **Click**: the matched node has a clickable ancestor (Pixel, older Samsung, etc.).
 *    We dispatch ACTION_CLICK.
 *  - **Swipe**: the matched node is just a label with no clickable ancestor (OnePlus
 *    OxygenOS slide-to-power-off, similar drag-style UIs). We dispatch a gesture
 *    swipe from screen-centre to the matched node's centre.
 *
 * dryRun=true stops one step before the final action and reports which path it would
 * have taken. Used by the Test trigger so the user can verify without shutting down.
 */
class PowerOffSequence(private val service: AccessibilityService) {

    sealed interface Result {
        data class Triggered(val matchedText: String, val mode: Mode) : Result
        data class DryRun(val matchedText: String, val mode: Mode) : Result
        data class NotFound(val visited: List<String>) : Result
        data object DialogDidNotOpen : Result
    }

    enum class Mode { Click, Swipe, TwoFingerSwipeDown }

    suspend fun run(dryRun: Boolean): Result {
        Log.i(TAG, "running power-off sequence (dryRun=$dryRun)")
        val opened = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        if (!opened) return Result.DialogDidNotOpen

        delay(POST_DIALOG_DELAY_MS)

        val strings = PowerDialogStrings.stringsFor(OemDetector.current)
        val primaryTargets = (strings.primaryActions + PowerDialogStrings.allKnownPrimary).distinct()

        val visited = mutableListOf<String>()
        val hit = findMatch(service.rootInActiveWindow, primaryTargets, visited)
        if (hit == null) {
            Log.w(TAG, "no text match. visited (${visited.size}): $visited")
            // OEM-specific gesture fallback (e.g. OnePlus two-finger slide UI).
            val fallback = PowerDialogStrings.gestureFallbackFor(OemDetector.current)
            if (fallback == PowerDialogStrings.GestureFallback.TwoFingerSwipeDown) {
                val marker = PowerDialogStrings.twoFingerSwipeDownMarkers
                    .firstOrNull { m -> visited.any { v -> v.contains(m) } }
                if (marker != null) {
                    Log.i(TAG, "two-finger-swipe-down dialog detected via marker: '$marker'")
                    if (dryRun) return Result.DryRun(marker, Mode.TwoFingerSwipeDown)
                    val ok = dispatchTwoFingerSwipeDown()
                    return if (ok) Result.Triggered(marker, Mode.TwoFingerSwipeDown)
                    else Result.NotFound(listOf("two-finger swipe-down dispatch failed (marker '$marker')"))
                }
            }
            Log.w(TAG, "no fallback triggered. looked for: $primaryTargets")
            return Result.NotFound(visited)
        }
        Log.i(TAG, "matched '${hit.matchedText}' bounds=${hit.bounds} clickable=${hit.clickable != null}")

        val mode = if (hit.clickable != null) Mode.Click else Mode.Swipe

        if (dryRun) {
            hit.clickable?.recycleSafely()
            return Result.DryRun(hit.matchedText, mode)
        }

        val acted = when (mode) {
            Mode.Click -> {
                val ok = hit.clickable!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                hit.clickable.recycleSafely()
                ok
            }
            Mode.Swipe -> dispatchSwipe(hit.bounds)
            Mode.TwoFingerSwipeDown -> false // unreachable: only used in the no-match fallback path above
        }
        if (!acted) {
            return Result.NotFound(listOf("matched '${hit.matchedText}' but ${mode.name} failed"))
        }

        // Confirmation step. On click-style dialogs there's often a "Power off" / "OK"
        // button to tap afterward; on swipe-style dialogs the swipe itself is the
        // confirmation, so this best-effort walk usually finds nothing — fine.
        delay(POST_TAP_DELAY_MS)
        val confirmTargets = (strings.confirmActions + PowerDialogStrings.allKnownConfirm).distinct()
        val confirmHit = findMatch(service.rootInActiveWindow, confirmTargets, mutableListOf())
        if (confirmHit?.clickable != null) {
            confirmHit.clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            confirmHit.clickable.recycleSafely()
        }

        return Result.Triggered(hit.matchedText, mode)
    }

    private suspend fun dispatchTwoFingerSwipeDown(): Boolean {
        val metrics = service.resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        val centerX = w / 2f
        // Fingers spread ~80 dp apart; clamp to a reasonable absolute pixel range.
        val spread = (w * 0.10f).coerceIn(60f, 200f)
        val topY = h * 0.30f
        val bottomY = h * 0.80f

        val path1 = Path().apply {
            moveTo(centerX - spread, topY)
            lineTo(centerX - spread, bottomY)
        }
        val path2 = Path().apply {
            moveTo(centerX + spread, topY)
            lineTo(centerX + spread, bottomY)
        }
        Log.i(TAG, "two-finger swipe down: from y=$topY to y=$bottomY, spread=$spread, w=$w h=$h")

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0L, TWO_FINGER_DURATION_MS))
            .addStroke(GestureDescription.StrokeDescription(path2, 0L, TWO_FINGER_DURATION_MS))
            .build()

        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(g: GestureDescription?) {
                    Log.w(TAG, "two-finger swipe cancelled")
                    if (cont.isActive) cont.resume(false)
                }
            }
            val dispatched = service.dispatchGesture(gesture, callback, handler)
            if (!dispatched && cont.isActive) cont.resume(false)
        }
    }

    private suspend fun dispatchSwipe(targetBounds: Rect): Boolean {
        val metrics = service.resources.displayMetrics
        val srcX = metrics.widthPixels / 2f
        val srcY = metrics.heightPixels / 2f
        val dstX = targetBounds.exactCenterX()
        val dstY = targetBounds.exactCenterY()
        Log.i(TAG, "swipe ($srcX,$srcY) → ($dstX,$dstY) over ${SWIPE_DURATION_MS}ms")

        val path = Path().apply {
            moveTo(srcX, srcY)
            lineTo(dstX, dstY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SWIPE_DURATION_MS))
            .build()

        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(g: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }
            val dispatched = service.dispatchGesture(gesture, callback, handler)
            if (!dispatched && cont.isActive) cont.resume(false)
        }
    }

    private data class Hit(
        val matchedText: String,
        val clickable: AccessibilityNodeInfo?,
        val bounds: Rect,
    )

    private fun findMatch(
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
            val rect = Rect().also { node.getBoundsInScreen(it) }
            // Walk up looking for a clickable ancestor (the text often lives on a
            // child label whose parent is the actual button).
            var clickable: AccessibilityNodeInfo? = node
            while (clickable != null && !clickable.isClickable) {
                clickable = clickable.parent
            }
            val target = clickable ?: node.takeIf { it.isClickable }
            return Hit(matched, target, rect)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val h = walk(child, targets, visited)
            if (h != null) return h
        }
        return null
    }

    private fun AccessibilityNodeInfo.recycleSafely() {
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
        private const val SWIPE_DURATION_MS = 400L
        private const val TWO_FINGER_DURATION_MS = 600L
    }
}
