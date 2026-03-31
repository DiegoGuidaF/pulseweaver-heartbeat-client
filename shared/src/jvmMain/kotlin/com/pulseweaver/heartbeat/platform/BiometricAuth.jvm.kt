package com.pulseweaver.heartbeat.platform

actual object BiometricAuth {
    actual fun isAvailable(): Boolean = false

    actual suspend fun authenticate(title: String): Boolean = true
}
