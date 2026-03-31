package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.config.HeartbeatConfig
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("com/pulseweaver/heartbeat")

actual class ConfigStore actual constructor() {

    actual suspend fun load(): HeartbeatConfig = HeartbeatConfig(
        serverUrl = prefs.get("serverUrl", ""),
        apiKey = prefs.get("apiKey", ""),
        intervalSeconds = prefs.getInt("intervalSeconds", 900),
        enabled = prefs.getBoolean("enabled", false),
        biometricEnabled = false, // not applicable on desktop
        themeMode = runCatching { ThemeMode.valueOf(prefs.get("themeMode", "AUTO")) }
            .getOrDefault(ThemeMode.AUTO),
    )

    actual suspend fun save(config: HeartbeatConfig) {
        prefs.put("serverUrl", config.serverUrl)
        prefs.put("apiKey", config.apiKey)
        prefs.putInt("intervalSeconds", config.intervalSeconds)
        prefs.putBoolean("enabled", config.enabled)
        prefs.put("themeMode", config.themeMode.name)
        prefs.flush()
    }
}
