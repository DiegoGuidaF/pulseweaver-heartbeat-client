package com.pulseweaver.heartbeat.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences

private val osName = System.getProperty("os.name").lowercase()
internal val isWindows = "win" in osName
internal val isMac = "mac" in osName

// The dev channel registers under its own names so a dev install and a release
// install manage independent login items, mirroring their separate identities.
private val entryDisplayName =
    "PulseWeaver Companion" +
        channelSuffix().removePrefix("-").replaceFirstChar(Char::uppercase).let { if (it.isEmpty()) "" else " $it" }
private val entryId = "com.pulseweaver.companion" + channelSuffix().replace('-', '.')

private const val WINDOWS_RUN_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""

// Where Windows' own Startup-apps UI records an entry the user switched off. It
// leaves the Run value untouched, so the Run value alone reports entries that
// never actually run.
private const val WINDOWS_STARTUP_APPROVED_KEY =
    """HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run"""

// A .reg file spells the hive out in full; the abbreviation is only understood
// as a `reg` command-line argument.
private val windowsRunKeyPath = WINDOWS_RUN_KEY.replaceFirst("HKCU", "HKEY_CURRENT_USER")

// Marks that the start-at-login question has been answered — by the user, or by
// the default applied to an install that never saw setup. Versioned: bumping the
// suffix re-asks installs an earlier build already marked, which is how a machine
// left unregistered by a broken registration gets another chance. v3 re-asks the
// macOS installs whose agent was written to disk but never handed to launchd.
private const val DECIDED_KEY = "autoStartDecided.v3"

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
        if (launcher == null || !hasEntry()) return false
        return when {
            isWindows -> !disabledByWindows()
            // The file only says the agent was written. launchd has to have it loaded
            // for the next login to honour it, and unloading is also what switching the
            // entry off under System Settings' "Allow in the Background" does.
            isMac -> macAgentLoaded()
            else -> true
        }
    }

    actual fun setEnabled(enabled: Boolean): Boolean {
        if (launcher == null) return false
        // Enabling always rewrites the entry, which repairs a launcher path left
        // stale by a reinstall; removing one that was never registered is already
        // done, and asking the OS to delete it would only log a spurious failure.
        // The test is whether an entry exists at all, not whether the OS honours it:
        // an entry the OS has switched off still has to be cleaned up on disable.
        val ok = if (!enabled && !hasEntry()) true else applyRegistration(enabled)
        if (ok) {
            prefs.putBoolean(DECIDED_KEY, true)
            prefs.flush()
        }
        return ok
    }

    actual fun applyDefaultIfUndecided() {
        if (launcher == null || prefs.getBoolean(DECIDED_KEY, false)) return
        setEnabled(true)
    }

    private fun applyRegistration(enabled: Boolean): Boolean {
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
                            val outcome = bootstrapMacAgent()
                            detail = outcome.output
                            outcome.ok
                        } else {
                            // Removing the plist is the whole of it — see bootstrapMacAgent
                            // on why the loaded agent is left alone. It has already run its
                            // one action for this session and is gone by the next login.
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

    /** Whether an entry exists at all, before asking whether the OS still honours it. */
    private fun hasEntry(): Boolean =
        when {
            isWindows -> reg("query", WINDOWS_RUN_KEY, "/v", entryDisplayName).ok
            isMac -> Files.exists(macPlistPath)
            else -> Files.exists(linuxDesktopPath)
        }

    private fun disabledByWindows(): Boolean {
        val outcome = reg("query", WINDOWS_STARTUP_APPROVED_KEY, "/v", entryDisplayName)
        return outcome.ok && startupApprovedIsDisabled(outcome.output)
    }

    // launchd's name for this user's GUI session. A JVM has no way to read its own uid, so
    // it comes from `id`; a non-numeric answer means the call failed and the domain is
    // unusable, which every caller has to treat as "cannot register".
    private val guiDomain: String? by lazy {
        run("id", "-u")
            .takeIf { it.ok }
            ?.output
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?.let { "gui/$it" }
    }

    /** True when launchd currently holds the agent — the only state that survives to a login. */
    private fun macAgentLoaded(): Boolean {
        val domain = guiDomain ?: return false
        return launchctl("print", "$domain/$entryId").ok
    }

    /**
     * Hands the freshly written agent to launchd. Writing the plist alone only queues it for
     * the next login, which is why a toggle that had only written the file could report a
     * registration the OS knew nothing about.
     *
     * Never paired with a `bootout`, in either direction: booting a service out terminates
     * its running process, and when the app was itself started at login that process is
     * this one — the toggle would quit the app under the hand that flipped it. Nothing
     * needs it. An agent already loaded refuses the bootstrap and stays as it is; a plist
     * replaced on disk (a reinstall moved the launcher) is re-read at the next login, which
     * is the only moment the agent has a job to do anyway.
     */
    private fun bootstrapMacAgent(): CommandOutcome {
        val domain = guiDomain ?: return CommandOutcome(false, "could not resolve the launchd GUI domain")
        val outcome = launchctl("bootstrap", domain, macPlistPath.toString())
        // bootstrap reports failure for agents that are loaded — an already-bootstrapped
        // service answers "Input/output error" — so launchd's own view of the agent is the
        // verdict, not the exit status.
        return if (macAgentLoaded()) CommandOutcome(true, outcome.output) else outcome
    }

    /**
     * Registers the Run value from a temporary `.reg` file rather than passing it
     * to `reg add /d`. The value has to quote the launcher path (it contains
     * spaces) while trailing `--minimized` outside those quotes; Java sees an
     * argument that is unquoted-but-spaced, wraps it in a second pair of quotes,
     * and `reg` then parses the result as several parameters and rejects it. A
     * file carries the value verbatim, past any command-line quoting.
     */
    private fun importRunEntry(launcher: Path): CommandOutcome {
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

    private fun reg(vararg args: String): CommandOutcome = run("reg", *args)

    private fun launchctl(vararg args: String): CommandOutcome = run("launchctl", *args)

    private fun run(vararg args: String): CommandOutcome =
        runCatching {
            val process = ProcessBuilder(*args).redirectErrorStream(true).start()
            val output = process.inputStream.readAllBytes().decodeToString()
            CommandOutcome(process.waitFor() == 0, output.trim())
        }.getOrElse { CommandOutcome(false, "${it::class.simpleName}: ${it.message}") }
}

/** Exit status of a helper command plus whatever it printed, so failures can say why. */
private class CommandOutcome(
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
 * Reads the enabled bit out of a `reg query` dump of a StartupApproved value.
 * The value is binary and its first byte carries the state: even while the entry
 * is enabled, odd once the user switches it off in Settings or Task Manager.
 * An unreadable dump counts as enabled — the Run value stays the primary answer.
 */
internal fun startupApprovedIsDisabled(regQueryOutput: String): Boolean {
    val hex =
        regQueryOutput
            .substringAfter("REG_BINARY", "")
            .trim()
            .takeWhile { !it.isWhitespace() }
    return hex.take(2).toIntOrNull(radix = 16)?.let { it % 2 != 0 } ?: false
}

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
