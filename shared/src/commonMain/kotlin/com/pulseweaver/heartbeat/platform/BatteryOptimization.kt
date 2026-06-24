package com.pulseweaver.heartbeat.platform

/**
 * Battery-optimization exemption gate.
 *
 * Android pauses background apps under Doze and App Standby, which can defer the
 * heartbeat for hours. Exempting the app from battery optimization lets the
 * existing WorkManager schedule run roughly on time. [requestExemption] opens
 * the system battery-optimization settings list rather than the per-app
 * exemption dialog, which would need the Play-restricted
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission.
 *
 * Desktop / iOS: no such restriction — [isExempt] reports true and
 * [requestExemption] is a no-op, so the reliability UI never appears.
 */
expect object BatteryOptimization {
    fun isExempt(): Boolean

    /** Opens the system battery-optimization settings list so the user can exempt the app. */
    fun requestExemption()
}
