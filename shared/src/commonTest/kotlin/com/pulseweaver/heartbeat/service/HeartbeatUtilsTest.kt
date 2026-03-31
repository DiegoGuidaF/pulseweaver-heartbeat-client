package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartbeatUtilsTest {

    // ── formatDuration ──────────────────────────────────────────────

    @Test
    fun formatDuration_zeroSeconds_showsZero() {
        assertEquals("0s", HeartbeatUtils.formatDuration(0))
    }

    @Test
    fun formatDuration_secondsOnly_noMinutePrefix() {
        assertEquals("59s", HeartbeatUtils.formatDuration(59))
    }

    @Test
    fun formatDuration_exactMinute_showsZeroPaddedSeconds() {
        assertEquals("1m 00s", HeartbeatUtils.formatDuration(60))
    }

    @Test
    fun formatDuration_mixedMinutesAndSeconds_padsSeconds() {
        assertEquals("1m 05s", HeartbeatUtils.formatDuration(65))
    }

    @Test
    fun formatDuration_largeValue_noHourConversion() {
        // The app doesn't need hour-level display; minutes keep growing.
        assertEquals("61m 01s", HeartbeatUtils.formatDuration(3661))
    }

    // ── isConfigValid ───────────────────────────────────────────────

    @Test
    fun isConfigValid_httpsUrlAndKey_returnsTrue() {
        assertTrue(HeartbeatUtils.isConfigValid("https://example.com", "abc-123"))
    }

    @Test
    fun isConfigValid_httpUrlAndKey_returnsTrue() {
        assertTrue(HeartbeatUtils.isConfigValid("http://10.0.0.1:8080", "key"))
    }

    @Test
    fun isConfigValid_emptyUrl_returnsFalse() {
        assertFalse(HeartbeatUtils.isConfigValid("", "key"))
    }

    @Test
    fun isConfigValid_emptyKey_returnsFalse() {
        assertFalse(HeartbeatUtils.isConfigValid("https://example.com", ""))
    }

    @Test
    fun isConfigValid_nonHttpUrl_returnsFalse() {
        assertFalse(HeartbeatUtils.isConfigValid("ftp://example.com", "key"))
    }

    @Test
    fun isConfigValid_urlWithoutScheme_returnsFalse() {
        assertFalse(HeartbeatUtils.isConfigValid("example.com", "key"))
    }

    // ── shouldUseDarkTheme ──────────────────────────────────────────

    @Test
    fun shouldUseDarkTheme_darkMode_alwaysTrue() {
        assertTrue(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.DARK, systemIsDark = false))
        assertTrue(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.DARK, systemIsDark = true))
    }

    @Test
    fun shouldUseDarkTheme_lightMode_alwaysFalse() {
        assertFalse(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.LIGHT, systemIsDark = true))
        assertFalse(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.LIGHT, systemIsDark = false))
    }

    @Test
    fun shouldUseDarkTheme_autoMode_followsSystem() {
        assertTrue(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.AUTO, systemIsDark = true))
        assertFalse(HeartbeatUtils.shouldUseDarkTheme(ThemeMode.AUTO, systemIsDark = false))
    }
}
