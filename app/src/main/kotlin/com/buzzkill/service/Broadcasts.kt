package com.buzzkill.service

/**
 * Internal broadcast actions used between AlarmScheduler, BootReceiver, and the
 * accessibility service. Kept in one place so we don't have action-string drift.
 */
object Broadcasts {
    const val WINDOW_OPEN = "com.buzzkill.action.WINDOW_OPEN"
    const val WINDOW_CLOSE = "com.buzzkill.action.WINDOW_CLOSE"
    const val INACTIVITY_FIRED = "com.buzzkill.action.INACTIVITY_FIRED"
    const val TEST_TRIGGER_DRY_RUN = "com.buzzkill.action.TEST_TRIGGER_DRY_RUN"
}
