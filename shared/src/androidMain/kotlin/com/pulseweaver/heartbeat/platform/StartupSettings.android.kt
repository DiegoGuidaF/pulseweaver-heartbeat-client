package com.pulseweaver.heartbeat.platform

// Android has no login items; the equivalent surface is battery optimization,
// which the reliability card already handles.
actual object StartupSettings {
    actual fun isAvailable(): Boolean = false

    actual fun open() = Unit
}
