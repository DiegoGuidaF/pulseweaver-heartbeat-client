package com.pulseweaver.heartbeat.platform

/**
 * Camera-based QR code scanner for capturing the pairing code during setup.
 *
 * Desktop: unavailable — [isAvailable] returns false, UI hides the scan button.
 * Android: Google Play Services code scanner (no camera permission required).
 * iOS: unavailable for now — [isAvailable] returns false.
 */
expect object QrScanner {
    fun isAvailable(): Boolean

    /** Opens the scanner UI and returns the decoded text, or null on cancellation/failure. */
    suspend fun scan(): String?
}
