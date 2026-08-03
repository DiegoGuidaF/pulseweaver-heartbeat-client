package com.pulseweaver.heartbeat.platform

/**
 * Opens the OS screen that lists start-at-login entries.
 *
 * The app's own toggle is the primary control, but the OS keeps a competing one:
 * Windows' Startup apps and macOS' Login Items can switch our entry off behind
 * our back. This is the escape hatch to that screen, so a user who doubts what
 * the app reports can go and look.
 *
 * Linux has no dependable equivalent across desktop environments, and mobile has
 * no login items at all — [isAvailable] returns false there and the UI hides the
 * link.
 */
expect object StartupSettings {
    fun isAvailable(): Boolean

    fun open()
}
