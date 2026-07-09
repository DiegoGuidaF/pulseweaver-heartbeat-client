package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.platform.channelSuffix
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("com/pulseweaver/heartbeat${channelSuffix()}")

actual class ConfigStore actual constructor() {
    actual suspend fun load(): HeartbeatConfig =
        HeartbeatConfig(
            serverUrl = prefs.get("serverUrl", ""),
            apiKey = prefs.get("apiKey", ""),
            intervalSeconds = normalizeInterval(prefs.getInt("intervalSeconds", MIN_INTERVAL_SECONDS)),
            enabled = prefs.getBoolean("enabled", false),
            biometricEnabled = false, // not applicable on desktop
            themeMode =
                runCatching { ThemeMode.valueOf(prefs.get("themeMode", "AUTO")) }
                    .getOrDefault(ThemeMode.AUTO),
            settingsLocked = prefs.getBoolean("settingsLocked", false),
        )

    actual suspend fun save(config: HeartbeatConfig) {
        prefs.put("serverUrl", config.serverUrl)
        prefs.put("apiKey", config.apiKey)
        prefs.putInt("intervalSeconds", config.intervalSeconds)
        prefs.putBoolean("enabled", config.enabled)
        prefs.put("themeMode", config.themeMode.name)
        prefs.putBoolean("settingsLocked", config.settingsLocked)
        prefs.flush()
    }
}
