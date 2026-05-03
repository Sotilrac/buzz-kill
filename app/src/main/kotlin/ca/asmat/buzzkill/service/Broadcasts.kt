package ca.asmat.buzzkill.service

/**
 * Internal broadcast actions used between AlarmScheduler, BootReceiver, and the
 * accessibility service. Kept in one place so we don't have action-string drift.
 */
object Broadcasts {
    const val WINDOW_OPEN = "ca.asmat.buzzkill.action.WINDOW_OPEN"
    const val WINDOW_CLOSE = "ca.asmat.buzzkill.action.WINDOW_CLOSE"
    const val INACTIVITY_FIRED = "ca.asmat.buzzkill.action.INACTIVITY_FIRED"
    const val TEST_TRIGGER_DRY_RUN = "ca.asmat.buzzkill.action.TEST_TRIGGER_DRY_RUN"
    const val TEST_TRIGGER_LIVE = "ca.asmat.buzzkill.action.TEST_TRIGGER_LIVE"
}
