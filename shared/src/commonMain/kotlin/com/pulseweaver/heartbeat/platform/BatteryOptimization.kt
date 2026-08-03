package com.pulseweaver.heartbeat.platform

/**
 * Battery-optimization exemption gate.
 *
 * Android pauses background apps under Doze and App Standby, which can defer the
 * heartbeat for hours. The exemption is not what makes the schedule fire — that is
 * the allow-while-idle alarm chain in [BackgroundScheduler], since the allowlist
 * does not release `WorkManager`/`JobScheduler` work from Doze deferral. What it
 * does buy is lifting App Standby throttling and granting the network access an
 * alarm needs when it wakes a dozing device, so it is still required for a
 * heartbeat that lands on time.
 *
 * [requestExemption] opens the app's own App info page rather than the per-app
 * exemption dialog, which would need the Play-restricted
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission.
 *
 * Desktop / iOS: no such restriction — [isExempt] reports true and
 * [requestExemption] is a no-op, so the reliability UI never appears.
 */
expect object BatteryOptimization {
    fun isExempt(): Boolean

    /** Opens this app's App info page, where the user sets Battery to Unrestricted. */
    fun requestExemption()
}
