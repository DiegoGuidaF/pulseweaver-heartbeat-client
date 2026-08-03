package com.pulseweaver.heartbeat.platform

actual object AutoStart {
    actual fun isAvailable(): Boolean = false

    actual fun isEnabled(): Boolean = false

    actual fun setEnabled(enabled: Boolean): Boolean = false
}
