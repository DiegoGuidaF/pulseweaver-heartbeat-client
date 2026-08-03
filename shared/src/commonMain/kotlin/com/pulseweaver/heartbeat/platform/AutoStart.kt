package com.pulseweaver.heartbeat.platform

/**
 * Start-at-login registration for installed desktop builds.
 *
 * The Companion only keeps a device authorized while it runs, so installed
 * desktop builds enrol themselves to start at login (hidden to the tray via
 * `--minimized`) and expose a toggle to opt out. Registration is per-user and
 * lives with the OS: the `HKCU\…\Run` registry key (Windows), a LaunchAgent
 * plist (macOS), or an XDG autostart entry (Linux).
 *
 * Android/iOS: not applicable — [isAvailable] returns false, UI hides the
 * toggle. Desktop dev runs (`gradlew shared:run`) are unavailable too: only a
 * packaged launcher can meaningfully re-launch itself.
 */
expect object AutoStart {
    fun isAvailable(): Boolean

    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)
}
