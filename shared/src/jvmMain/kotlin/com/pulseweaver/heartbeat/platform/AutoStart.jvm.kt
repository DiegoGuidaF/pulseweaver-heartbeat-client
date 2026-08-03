package com.pulseweaver.heartbeat.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences

private val osName = System.getProperty("os.name").lowercase()
private val isWindows = "win" in osName
private val isMac = "mac" in osName

// The dev channel registers under its own names so a dev install and a release
// install manage independent login items, mirroring their separate identities.
private val entryDisplayName =
    "PulseWeaver Companion" +
        channelSuffix().removePrefix("-").replaceFirstChar(Char::uppercase).let { if (it.isEmpty()) "" else " $it" }
private val entryId = "com.pulseweaver.companion" + channelSuffix().replace('-', '.')

private const val WINDOWS_RUN_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""

actual object AutoStart {
    private val prefs = Preferences.userRoot().node("com/pulseweaver/heartbeat${channelSuffix()}")

    // The jpackage launcher binary, or null when running via a plain `java`
    // process (dev runs, tests) — nothing sensible can be registered then.
    private val launcher: Path? =
        ProcessHandle.current().info().command().orElse(null)?.let { command ->
            val path = Paths.get(command)
            val name = path.fileName.toString().lowercase().removeSuffix(".exe")
            path.takeUnless { name == "java" || name == "javaw" }
        }

    private val macPlistPath: Path
        get() = Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents", "$entryId.plist")

    private val linuxDesktopPath: Path
        get() = Paths.get(System.getProperty("user.home"), ".config", "autostart", "$entryId.desktop")

    actual fun isAvailable(): Boolean = launcher != null

    actual fun isEnabled(): Boolean {
        if (launcher == null) return false
        return when {
            isWindows -> reg("query", WINDOWS_RUN_KEY, "/v", entryDisplayName)
            isMac -> Files.exists(macPlistPath)
            else -> Files.exists(linuxDesktopPath)
        }
    }

    actual fun setEnabled(enabled: Boolean) {
        val launcher = launcher ?: return
        val ok =
            runCatching {
                when {
                    isWindows ->
                        if (enabled) {
                            reg("add", WINDOWS_RUN_KEY, "/v", entryDisplayName, "/t", "REG_SZ", "/d", "\"$launcher\" --minimized", "/f")
                        } else {
                            reg("delete", WINDOWS_RUN_KEY, "/v", entryDisplayName, "/f")
                        }
                    isMac ->
                        if (enabled) {
                            writeEntry(macPlistPath, launchAgentPlist(entryId, launcher.toString()))
                        } else {
                            Files.deleteIfExists(macPlistPath)
                            true
                        }
                    else ->
                        if (enabled) {
                            writeEntry(linuxDesktopPath, autostartDesktopEntry(entryDisplayName, launcher.toString()))
                        } else {
                            Files.deleteIfExists(linuxDesktopPath)
                            true
                        }
                }
            }.getOrDefault(false)
        if (ok) {
            Log.i("AutoStart", "start-at-login ${if (enabled) "enabled" else "disabled"}")
        } else {
            Log.w("AutoStart", "failed to ${if (enabled) "enable" else "disable"} start-at-login")
        }
    }

    /**
     * Applies the enabled-by-default policy exactly once per install. After the
     * marker is set, the OS-level registration is the single source of truth,
     * so a user who opts out is never re-enrolled.
     */
    fun ensureDefaultEnabled() {
        if (launcher == null || prefs.getBoolean("autoStartConfigured", false)) return
        setEnabled(true)
        prefs.putBoolean("autoStartConfigured", true)
        prefs.flush()
    }

    private fun writeEntry(path: Path, content: String): Boolean {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
        return true
    }

    private fun reg(vararg args: String): Boolean =
        runCatching {
            val process = ProcessBuilder("reg", *args).redirectErrorStream(true).start()
            process.inputStream.readAllBytes()
            process.waitFor() == 0
        }.getOrDefault(false)
}

internal fun launchAgentPlist(
    label: String,
    launcherPath: String,
): String =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>Label</key>
        <string>$label</string>
        <key>ProgramArguments</key>
        <array>
            <string>${xmlEscape(launcherPath)}</string>
            <string>--minimized</string>
        </array>
        <key>RunAtLoad</key>
        <true/>
    </dict>
    </plist>
    """.trimIndent() + "\n"

internal fun autostartDesktopEntry(
    name: String,
    launcherPath: String,
): String =
    """
    [Desktop Entry]
    Type=Application
    Name=$name
    Comment=Keeps this device authorized on your PulseWeaver server
    Exec="$launcherPath" --minimized
    X-GNOME-Autostart-enabled=true
    """.trimIndent() + "\n"

private fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
