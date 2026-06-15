package com.pulseweaver.heartbeat.platform

actual object QrScanner {
    actual fun isAvailable(): Boolean = false

    actual suspend fun scan(): String? = null
}
