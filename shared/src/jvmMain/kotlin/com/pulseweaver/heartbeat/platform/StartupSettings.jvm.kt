package com.pulseweaver.heartbeat.platform

// Both targets are URI schemes their OS registers, not executables, so they go
// through the shell handler rather than Desktop.browse(), which only promises
// http(s).
private val startupSettingsCommand: List<String>? =
    when {
        isWindows -> listOf("cmd", "/c", "start", "", "ms-settings:startupapps")
        isMac -> listOf("open", "x-apple.systempreferences:com.apple.LoginItems-Settings.extension")
        else -> null
    }

actual object StartupSettings {
    actual fun isAvailable(): Boolean = startupSettingsCommand != null

    actual fun open() {
        val command = startupSettingsCommand ?: return
        runCatching { ProcessBuilder(command).start() }
            .onFailure { Log.w("AutoStart", "failed to open the OS startup settings — ${it.message}") }
    }
}
