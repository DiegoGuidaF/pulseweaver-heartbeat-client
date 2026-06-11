package com.pulseweaver.heartbeat.platform

/**
 * Battery-optimization exemption gate.
 *
 * Android pauses background apps under Doze and App Standby, which can defer the
 * heartbeat for hours. Exempting the app from battery optimization lets the
 * existing WorkManager schedule run roughly on time. The request must be
 * user-initiated from explanatory UI — never auto-fired — to stay Play-compliant.
 *
 * Desktop / iOS: no such restriction — [isExempt] reports true and
 * [requestExemption] is a no-op, so the reliability UI never appears.
 */
expect object BatteryOptimization {
    fun isExempt(): Boolean

    /** Opens the system battery-optimization exemption dialog. */
    fun requestExemption()
}
