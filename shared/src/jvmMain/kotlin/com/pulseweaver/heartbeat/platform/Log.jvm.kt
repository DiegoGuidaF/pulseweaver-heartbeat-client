package com.pulseweaver.heartbeat.platform

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

private const val MAX_LOG_BYTES = 1_000_000L

private val writeLock = Any()

/** OS-standard per-user log file, or null if its directory can't be created. */
private val logFile: File? by lazy { resolveLogFile() }

internal actual fun writePlatformLog(
    level: LogLevel,
    tag: String,
    message: String,
    error: Throwable?,
) {
    val line =
        buildString {
            append(timestampFormat.format(Instant.now()))
            append(' ').append(level.label)
            append(" [").append(tag).append("] ")
            append(message)
            if (error != null) {
                append(" | ").append(error::class.simpleName).append(": ").append(error.message)
            }
        }
    // Mirror to stderr so terminal / `gradlew run` launches show logs live.
    System.err.println(line)
    val file = logFile ?: return
    synchronized(writeLock) {
        runCatching {
            if (file.length() > MAX_LOG_BYTES) rotate(file)
            file.appendText(line + System.lineSeparator())
            if (error != null) file.appendText(error.stackTraceToString() + System.lineSeparator())
        }
    }
}

internal actual fun isDebugLoggingEnabled(): Boolean = System.getenv("PW_DEBUG") != null || System.getProperty("pw.debug") != null

/** Renames the current log to `<name>.1` (replacing any prior backup) so the live file starts fresh. */
private fun rotate(file: File) {
    val backup = File(file.parentFile, file.name + ".1")
    if (backup.exists()) backup.delete()
    file.renameTo(backup)
}

private fun resolveLogFile(): File? {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val home = System.getProperty("user.home").orEmpty()
    val dir =
        when {
            os.contains("mac") -> File("$home/Library/Logs/PulseWeaver")
            os.contains("win") -> {
                val base = System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local"
                File("$base\\PulseWeaver\\logs")
            }
            else -> {
                val base = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/state"
                File("$base/pulseweaver")
            }
        }
    return runCatching {
        dir.mkdirs()
        File(dir, "companion.log")
    }.getOrNull()
}
