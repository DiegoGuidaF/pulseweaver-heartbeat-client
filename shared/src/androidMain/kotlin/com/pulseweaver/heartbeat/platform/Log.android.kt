package com.pulseweaver.heartbeat.platform

import android.util.Log as AndroidLog

internal actual fun writePlatformLog(
    level: LogLevel,
    tag: String,
    message: String,
    error: Throwable?,
) {
    val prefixed = "PulseWeaver/$tag"
    when (level) {
        LogLevel.DEBUG -> AndroidLog.d(prefixed, message, error)
        LogLevel.INFO -> AndroidLog.i(prefixed, message, error)
        LogLevel.WARN -> AndroidLog.w(prefixed, message, error)
        LogLevel.ERROR -> AndroidLog.e(prefixed, message, error)
    }
}

// Android logs go to logcat, which is already filterable; verbose DEBUG stays off by default.
internal actual fun isDebugLoggingEnabled(): Boolean = false
