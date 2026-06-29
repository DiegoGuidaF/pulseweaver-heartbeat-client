package com.pulseweaver.heartbeat.platform

// Stage later: implement with UIPasteboard.
actual object Clipboard {
    actual fun isAvailable(): Boolean = false

    actual suspend fun readText(): String? = null
}
