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

// A .reg file spells the hive out in full; the abbreviation is only understood
// as a `reg` command-line argument.
private val windowsRunKeyPath = WINDOWS_RUN_KEY.replaceFirst("HKCU", "HKEY_CURRENT_USER")

// Versioned marker: bumping the suffix re-runs the one-time enrolment on installs
// an earlier build already marked, which is how a machine left unregistered by a
// broken registration gets another chance.
private const val ENROLLED_KEY = "autoStartEnrolled.v2"

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
            isWindows -> reg("query", WINDOWS_RUN_KEY, "/v", entryDisplayName).ok
            isMac -> Files.exists(macPlistPath)
            else -> Files.exists(linuxDesktopPath)
        }
    }

    actual fun setEnabled(enabled: Boolean): Boolean {
        val launcher = launcher ?: return false
        var detail = ""
        val ok =
            runCatching {
                when {
                    isWindows -> {
                        val outcome =
                            if (enabled) {
                                importRunEntry(launcher)
                            } else {
                                reg("delete", WINDOWS_RUN_KEY, "/v", entryDisplayName, "/f")
                            }
                        detail = outcome.output
                        outcome.ok
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
            }.getOrElse { error ->
                detail = "${error::class.simpleName}: ${error.message}"
                false
            }
        if (ok) {
            Log.i("AutoStart", "start-at-login ${if (enabled) "enabled" else "disabled"}")
        } else {
            val suffix = if (detail.isBlank()) "" else " — $detail"
            Log.w("AutoStart", "failed to ${if (enabled) "enable" else "disable"} start-at-login$suffix")
        }
        return ok
    }

    /**
     * Applies the enabled-by-default policy once per install, retrying on later
     * launches until the OS accepts it: only a registration that succeeded marks
     * the install enrolled, so a rejected one never silently costs the default.
     * Once marked, the OS-level registration is the single source of truth, so a
     * user who opts out is never re-enrolled.
     */
    fun ensureDefaultEnabled() {
        if (launcher == null || prefs.getBoolean(ENROLLED_KEY, false)) return
        if (!setEnabled(true)) return
        prefs.putBoolean(ENROLLED_KEY, true)
        prefs.flush()
    }

    /**
     * Registers the Run value from a temporary `.reg` file rather than passing it
     * to `reg add /d`. The value has to quote the launcher path (it contains
     * spaces) while trailing `--minimized` outside those quotes; Java sees an
     * argument that is unquoted-but-spaced, wraps it in a second pair of quotes,
     * and `reg` then parses the result as several parameters and rejects it. A
     * file carries the value verbatim, past any command-line quoting.
     */
    private fun importRunEntry(launcher: Path): RegOutcome {
        val file = Files.createTempFile("pulseweaver-autostart", ".reg")
        return try {
            // reg only reads a Unicode .reg as UTF-16LE with a byte-order mark.
            val content = "\uFEFF" + runKeyRegistryFile(entryDisplayName, launcher.toString())
            Files.write(file, content.toByteArray(Charsets.UTF_16LE))
            reg("import", file.toString())
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun writeEntry(path: Path, content: String): Boolean {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
        return true
    }

    private fun reg(vararg args: String): RegOutcome =
        runCatching {
            val process = ProcessBuilder("reg", *args).redirectErrorStream(true).start()
            val output = process.inputStream.readAllBytes().decodeToString()
            RegOutcome(process.waitFor() == 0, output.trim())
        }.getOrElse { RegOutcome(false, "${it::class.simpleName}: ${it.message}") }
}

/** Exit status of a `reg` call plus whatever it printed, so failures can say why. */
private class RegOutcome(
    val ok: Boolean,
    val output: String,
)

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

/**
 * A `.reg` file adding one `REG_SZ` under the Run key. Registry files are CRLF
 * delimited, and string values escape both backslashes and quotes — so a Windows
 * path plus the quoted launcher survive intact.
 */
internal fun runKeyRegistryFile(
    valueName: String,
    launcherPath: String,
): String =
    listOf(
        "Windows Registry Editor Version 5.00",
        "",
        "[$windowsRunKeyPath]",
        "\"${registryEscape(valueName)}\"=\"${registryEscape("\"$launcherPath\" --minimized")}\"",
        "",
    ).joinToString("\r\n")

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

private fun registryEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
