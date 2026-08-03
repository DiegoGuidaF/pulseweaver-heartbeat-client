package com.pulseweaver.heartbeat.platform

actual object StartupSettings {
    actual fun isAvailable(): Boolean = false

    actual fun open() = Unit
}
