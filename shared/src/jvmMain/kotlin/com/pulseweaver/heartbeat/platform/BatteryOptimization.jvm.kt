package com.pulseweaver.heartbeat.platform

// Desktop has no Doze/App-Standby; always exempt and nothing to request.
actual object BatteryOptimization {
    actual fun isExempt(): Boolean = true

    actual fun requestExemption() = Unit
}
