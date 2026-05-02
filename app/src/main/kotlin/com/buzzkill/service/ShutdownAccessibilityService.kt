package com.buzzkill.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Skeleton. Phase 6 wires screen-state listening + status writes; phase 8 adds the
 * shutdown sequence.
 */
class ShutdownAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't react to events; we use this service for global actions and our own
        // dynamic broadcasts.
    }

    override fun onInterrupt() {
        // No long-running speech / haptics to interrupt.
    }
}
