package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.ThemeMode

/** Which battery-reliability surface the screen should render, if any. */
enum class ReliabilityPrompt {
    NONE,
    DIALOG,
    CARD,
}

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
    fun formatElapsed(
        epochMs: Long,
        nowMs: Long,
    ): String {
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
    fun isConfigValid(
        serverUrl: String,
        apiKey: String,
    ): Boolean =
        (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) &&
            apiKey.isNotEmpty()

    /**
     * Picks the battery-reliability surface for the current state.
     *
     * The modal gets exactly one appearance per install: it is the only surface prominent
     * enough to make someone act, but a device that cannot report the exemption — some OEM
     * builds never flip `isIgnoringBatteryOptimizations`, however the user answers — would
     * otherwise show it on every single launch. Afterwards the inline card carries the same
     * request, staying on screen (and in any screenshot a user sends) until the exemption
     * actually lands, without blocking the app.
     *
     * [isLoaded] is load-bearing rather than belt-and-braces: the caller assigns the loaded
     * config before it reads [promptSeen], so between those two writes a recomposition sees a
     * real `enabled` beside a still-default `promptSeen`. Without the gate the modal reopens
     * on an install that already retired it.
     */
    fun reliabilityPrompt(
        isLoaded: Boolean,
        heartbeatEnabled: Boolean,
        isExempt: Boolean,
        promptSeen: Boolean,
    ): ReliabilityPrompt =
        when {
            !isLoaded || !heartbeatEnabled || isExempt -> ReliabilityPrompt.NONE
            promptSeen -> ReliabilityPrompt.CARD
            else -> ReliabilityPrompt.DIALOG
        }

    /**
     * Determines whether a dark color scheme should be used based on the
     * persisted [ThemeMode] and the current system preference.
     */
    fun shouldUseDarkTheme(
        mode: ThemeMode,
        systemIsDark: Boolean,
    ): Boolean =
        when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.AUTO -> systemIsDark
        }
}
