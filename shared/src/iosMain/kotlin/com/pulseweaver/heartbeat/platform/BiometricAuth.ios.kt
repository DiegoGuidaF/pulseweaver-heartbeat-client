package com.pulseweaver.heartbeat.platform

// Stage later: implement with LAContext.
actual object BiometricAuth {
    actual fun isAvailable(): Boolean = false
    actual suspend fun authenticate(title: String): Boolean = false
}
