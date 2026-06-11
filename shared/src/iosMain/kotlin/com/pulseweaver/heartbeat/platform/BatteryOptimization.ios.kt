package com.pulseweaver.heartbeat.platform

// iOS background scheduling is a later stage; no battery-optimization concept to surface here.
actual object BatteryOptimization {
    actual fun isExempt(): Boolean = true

    actual fun requestExemption() = Unit
}
