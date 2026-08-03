package com.pulseweaver.heartbeat.platform

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AutoStartTest {
    @Test
    fun launchAgentPlist_registersLauncherMinimized() {
        val plist =
            launchAgentPlist(
                label = "com.pulseweaver.companion",
                launcherPath = "/Applications/PulseWeaver Companion.app/Contents/MacOS/PulseWeaver Companion",
            )
        assertContains(plist, "<string>com.pulseweaver.companion</string>")
        assertContains(plist, "<string>/Applications/PulseWeaver Companion.app/Contents/MacOS/PulseWeaver Companion</string>")
        assertContains(plist, "<string>--minimized</string>")
        assertContains(plist, "<key>RunAtLoad</key>")
    }

    @Test
    fun launchAgentPlist_escapesXmlInPath() {
        val plist = launchAgentPlist(label = "l", launcherPath = "/Apps/A&B<C/bin")
        assertContains(plist, "/Apps/A&amp;B&lt;C/bin")
    }

    @Test
    fun autostartDesktopEntry_quotesExecAndNamesEntry() {
        val entry =
            autostartDesktopEntry(
                name = "PulseWeaver Companion",
                launcherPath = "/opt/pulseweaver-companion/bin/PulseWeaver Companion",
            )
        assertContains(entry, "Name=PulseWeaver Companion")
        assertContains(entry, "Exec=\"/opt/pulseweaver-companion/bin/PulseWeaver Companion\" --minimized")
        assertContains(entry, "Type=Application")
    }

    @Test
    fun runKeyRegistryFile_escapesPathAndKeepsLauncherQuoted() {
        val file =
            runKeyRegistryFile(
                valueName = "PulseWeaver Companion Dev",
                launcherPath = """C:\Program Files\PulseWeaver Companion Dev\PulseWeaver Companion Dev.exe""",
            )
        assertContains(file, "Windows Registry Editor Version 5.00")
        assertContains(file, """[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run]""")
        // Quotes around the path and every backslash are escaped, so reg stores
        // the value the Run key needs: "<launcher>" --minimized
        assertContains(file, """"PulseWeaver Companion Dev"=""")
        assertContains(file, """\"C:\\Program Files\\PulseWeaver Companion Dev\\PulseWeaver Companion Dev.exe\" --minimized""")
    }

    @Test
    fun runKeyRegistryFile_usesCrlfLineEndings() {
        // reg import rejects a file whose lines end with a bare LF.
        val file = runKeyRegistryFile(valueName = "PulseWeaver Companion", launcherPath = """C:\app\pw.exe""")
        assertContains(file, "\r\n")
        assertFalse(Regex("(?<!\\r)\\n").containsMatchIn(file))
    }

    @Test
    fun unavailableUnderTests_becauseLauncherIsPlainJava() {
        // Tests run under a plain `java` process, never a packaged launcher, so
        // the capability must gate itself off (and the UI hides the toggle).
        assertFalse(AutoStart.isAvailable())
        assertFalse(AutoStart.isEnabled())
        assertFalse(AutoStart.setEnabled(true))
    }
}
