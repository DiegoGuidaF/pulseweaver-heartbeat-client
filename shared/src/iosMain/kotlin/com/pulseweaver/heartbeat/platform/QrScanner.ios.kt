package com.pulseweaver.heartbeat.platform

// Stage later: implement with AVFoundation / VisionKit.
actual object QrScanner {
    actual fun isAvailable(): Boolean = false

    actual suspend fun scan(): String? = null
}
