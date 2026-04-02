package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.ThemeMode

/**
 * Pure helper functions extracted from UI composables so they can be
 * unit-tested without a Compose runtime.
 */
object HeartbeatUtils {

    /**
     * Human-readable elapsed time since a past epoch timestamp.
     *
     *     formatElapsed(epochMs, now)  →  "<1m ago" | "5m ago" | "2h ago" | "3d ago"
     */
    fun formatElapsed(epochMs: Long, nowMs: Long): String {
        val seconds = (nowMs - epochMs) / 1000
        return when {
            seconds < 60 -> "<1m ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86400}d ago"
        }
    }

    /**
     * Human-readable duration string.
     *
     *     formatDuration(0)    → "0s"
     *     formatDuration(65)   → "1m 05s"
     *     formatDuration(3661) → "61m 01s"
     */
    fun formatDuration(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
    }

    /**
     * A heartbeat config is valid when the URL starts with http(s)://
     * and the API key is non-empty.
     */
    fun isConfigValid(serverUrl: String, apiKey: String): Boolean =
        (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) &&
            apiKey.isNotEmpty()

    /**
     * Determines whether a dark color scheme should be used based on the
     * persisted [ThemeMode] and the current system preference.
     */
    fun shouldUseDarkTheme(mode: ThemeMode, systemIsDark: Boolean): Boolean =
        when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.AUTO -> systemIsDark
        }
}
