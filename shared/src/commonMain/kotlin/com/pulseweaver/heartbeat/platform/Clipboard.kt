package com.pulseweaver.heartbeat.platform

/**
 * Reads plain text from the system clipboard, for a one-tap "Paste" affordance
 * during setup.
 *
 * Availability-gated optional capability: the UI shows the paste control only
 * when [isAvailable] is true. Desktop and Android implement it; iOS is a stub
 * for now ([isAvailable] returns false).
 */
expect object Clipboard {
    fun isAvailable(): Boolean

    /** Returns the clipboard's plain text, or null if empty/unavailable. */
    suspend fun readText(): String?
}
