package com.pulseweaver.heartbeat.config

enum class ThemeMode { AUTO, LIGHT, DARK }

// Heartbeat interval bounds, in seconds. The 15-minute floor matches Android's
// PeriodicWorkRequest minimum and keeps the configured interval honest on every
// platform; the 1-day ceiling keeps a dozing device refreshing often enough to
// stay authorized. Enforced in the config load path via [normalizeInterval] so
// every consumer (worker, boot receiver, scheduler, UI) sees a valid value.
const val MIN_INTERVAL_SECONDS = 900
const val MAX_INTERVAL_SECONDS = 86_400

/** Coerces a stored or requested interval into the supported range. */
fun normalizeInterval(seconds: Int): Int = seconds.coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS)

data class HeartbeatConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val intervalSeconds: Int = MIN_INTERVAL_SECONDS,
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val settingsLocked: Boolean = false,
)
