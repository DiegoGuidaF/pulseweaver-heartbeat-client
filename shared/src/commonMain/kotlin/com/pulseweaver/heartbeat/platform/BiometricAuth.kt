package com.pulseweaver.heartbeat.platform

/**
 * Biometric / device-credential authentication gate.
 *
 * Desktop: always unavailable — [isAvailable] returns false, UI hides the toggle.
 * Android: androidx.biometric (Stage 4).
 * iOS: LAContext (Stage later).
 */
expect object BiometricAuth {
    fun isAvailable(): Boolean

    /** Returns true on success, false on failure/cancellation. */
    suspend fun authenticate(title: String): Boolean
}
