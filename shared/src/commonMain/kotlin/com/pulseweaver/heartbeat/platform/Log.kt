package com.pulseweaver.heartbeat.platform

/**
 * Minimal cross-platform logger for diagnosing field issues (missed heartbeats,
 * network failures, scheduler lifecycle).
 *
 * INFO and above are always recorded; DEBUG is emitted only when platform debug
 * logging is enabled (e.g. the `PW_DEBUG` env var on desktop) so day-to-day
 * volume stays low while a bug report can be turned up. Sinks are platform
 * specific (see `writePlatformLog`): a rotating file plus stderr on desktop,
 * logcat on Android, the device console on iOS.
 */
enum class LogLevel(val label: String) {
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
}

object Log {
    private val minLevel: LogLevel = if (isDebugLoggingEnabled()) LogLevel.DEBUG else LogLevel.INFO

    fun d(
        tag: String,
        message: String,
    ) = emit(LogLevel.DEBUG, tag, message, null)

    fun i(
        tag: String,
        message: String,
    ) = emit(LogLevel.INFO, tag, message, null)

    fun w(
        tag: String,
        message: String,
        error: Throwable? = null,
    ) = emit(LogLevel.WARN, tag, message, error)

    fun e(
        tag: String,
        message: String,
        error: Throwable? = null,
    ) = emit(LogLevel.ERROR, tag, message, error)

    private fun emit(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable?,
    ) {
        if (level.ordinal < minLevel.ordinal) return
        writePlatformLog(level, tag, message, error)
    }
}

/** Writes one already-level-filtered record to the platform sink. */
internal expect fun writePlatformLog(
    level: LogLevel,
    tag: String,
    message: String,
    error: Throwable?,
)

/** True when verbose DEBUG logging should be recorded (opt-in per platform). */
internal expect fun isDebugLoggingEnabled(): Boolean
