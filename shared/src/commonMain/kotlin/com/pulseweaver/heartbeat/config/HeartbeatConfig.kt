package com.pulseweaver.heartbeat.config

enum class ThemeMode { AUTO, LIGHT, DARK }

data class HeartbeatConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val intervalSeconds: Int = 900,
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
)
