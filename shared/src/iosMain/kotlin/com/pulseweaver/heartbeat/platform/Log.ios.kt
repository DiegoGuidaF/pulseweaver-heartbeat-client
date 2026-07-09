package com.pulseweaver.heartbeat.platform

import platform.Foundation.NSLog

internal actual fun writePlatformLog(
    level: LogLevel,
    tag: String,
    message: String,
    error: Throwable?,
) {
    val suffix = error?.let { " | ${it::class.simpleName}: ${it.message}" } ?: ""
    // "%@" format avoids interpreting any '%' in the message as a format specifier.
    NSLog("%@", "PulseWeaver [${level.label}] $tag: $message$suffix")
}

internal actual fun isDebugLoggingEnabled(): Boolean = false
