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
    fun unavailableUnderTests_becauseLauncherIsPlainJava() {
        // Tests run under a plain `java` process, never a packaged launcher, so
        // the capability must gate itself off (and the UI hides the toggle).
        assertFalse(AutoStart.isAvailable())
        assertFalse(AutoStart.isEnabled())
    }
}
