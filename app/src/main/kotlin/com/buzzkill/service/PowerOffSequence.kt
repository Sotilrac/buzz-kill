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

    enum class Mode { Click, Swipe, SlideDown }

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
            // OEM-specific gesture fallback (e.g. OnePlus single-finger slide UI).
            val fallback = PowerDialogStrings.gestureFallbackFor(OemDetector.current)
            if (fallback == PowerDialogStrings.GestureFallback.SlideDown) {
                val marker = PowerDialogStrings.slideDownMarkers
                    .firstOrNull { m -> visited.any { v -> v.contains(m) } }
                if (marker != null) {
                    Log.i(TAG, "slide-down dialog detected via marker: '$marker'")
                    if (dryRun) return Result.DryRun(marker, Mode.SlideDown)
                    // Retry up to 3 times: when invoked from a background trigger
                    // (alarm-driven), the dialog can render over the lockscreen and
                    // not consume the first gesture.
                    repeat(SLIDE_RETRIES) { attempt ->
                        Log.i(TAG, "slide attempt ${attempt + 1}/$SLIDE_RETRIES")
                        val ok = dispatchSlideDownGesture()
                        if (!ok) return Result.NotFound(listOf("slide-down dispatch failed (marker '$marker')"))
                        delay(POST_GESTURE_VERIFY_MS)
                        // If the dialog text is gone, the slider engaged.
                        val stillVisible = findMatch(service.rootInActiveWindow, primaryTargets, mutableListOf()) != null ||
                            slideDownDialogStillVisible(service)
                        if (!stillVisible) {
                            Log.i(TAG, "dialog dismissed after attempt ${attempt + 1} — assuming triggered")
                            return Result.Triggered(marker, Mode.SlideDown)
                        }
                        Log.w(TAG, "dialog still visible after attempt ${attempt + 1}; retrying")
                    }
                    return Result.NotFound(listOf("slide-down dispatched ${SLIDE_RETRIES}× but dialog still visible (marker '$marker')"))
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
            Mode.SlideDown -> false // unreachable: only used in the no-match fallback path above
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

    private suspend fun dispatchSlideDownGesture(): Boolean {
        val metrics = service.resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        // OnePlus / OxygenOS dialog: slider widget at screen centre, target 25%
        // below. Despite the on-screen "two fingers" instruction, a single-finger
        // slide on the slider engages the power-off action.
        val startX = w / 2f
        val startY = h * 0.50f
        val endY = h * 0.75f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        Log.i(TAG, "slide down: ($startX,$startY)→($startX,$endY), duration=${SLIDE_DOWN_DURATION_MS}ms, w=$w h=$h")

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SLIDE_DOWN_DURATION_MS))
            .build()

        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) {
                    Log.i(TAG, "slide-down completed")
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(g: GestureDescription?) {
                    Log.w(TAG, "slide-down cancelled by system")
                    if (cont.isActive) cont.resume(false)
                }
            }
            val dispatched = service.dispatchGesture(gesture, callback, handler)
            if (!dispatched) {
                Log.w(TAG, "dispatchGesture returned false (refused)")
                if (cont.isActive) cont.resume(false)
            }
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
        private const val POST_DIALOG_DELAY_MS = 1200L
        private const val POST_TAP_DELAY_MS = 400L
        private const val SWIPE_DURATION_MS = 400L
        private const val SLIDE_DOWN_DURATION_MS = 1300L
        private const val POST_GESTURE_VERIFY_MS = 700L
        private const val SLIDE_RETRIES = 3
    }

    private fun slideDownDialogStillVisible(service: AccessibilityService): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val visited = mutableListOf<String>()
        walk(root, PowerDialogStrings.slideDownMarkers.toSet(), visited)
        return PowerDialogStrings.slideDownMarkers.any { m -> visited.any { v -> v.contains(m) } }
    }
}
