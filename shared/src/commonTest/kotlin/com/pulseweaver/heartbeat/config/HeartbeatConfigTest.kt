package com.pulseweaver.heartbeat.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class HeartbeatConfigTest {
    @Test
    fun defaultValues() {
        val config = HeartbeatConfig()
        assertEquals("", config.serverUrl)
        assertEquals("", config.apiKey)
        assertEquals(900, config.intervalSeconds)
        assertFalse(config.enabled)
        assertFalse(config.biometricEnabled)
        assertEquals(ThemeMode.AUTO, config.themeMode)
    }

    @Test
    fun copyPreservesUnchangedFields() {
        val original =
            HeartbeatConfig(
                serverUrl = "https://example.com",
                apiKey = "secret",
                intervalSeconds = 300,
                enabled = true,
            )
        val updated = original.copy(intervalSeconds = 600)

        assertEquals("https://example.com", updated.serverUrl)
        assertEquals("secret", updated.apiKey)
        assertEquals(600, updated.intervalSeconds)
        assertEquals(true, updated.enabled)
    }

    @Test
    fun equalityByValue() {
        val a = HeartbeatConfig(serverUrl = "https://a.com", enabled = true)
        val b = HeartbeatConfig(serverUrl = "https://a.com", enabled = true)
        assertEquals(a, b)
    }

    @Test
    fun inequalityOnDifference() {
        val a = HeartbeatConfig(themeMode = ThemeMode.LIGHT)
        val b = HeartbeatConfig(themeMode = ThemeMode.DARK)
        assertNotEquals(a, b)
    }

    @Test
    fun defaultIntervalIsTheFloor() {
        assertEquals(MIN_INTERVAL_SECONDS, HeartbeatConfig().intervalSeconds)
    }

    @Test
    fun normalizeIntervalRaisesLegacyValuesToTheFloor() {
        assertEquals(MIN_INTERVAL_SECONDS, normalizeInterval(60))
        assertEquals(MIN_INTERVAL_SECONDS, normalizeInterval(300))
        assertEquals(MIN_INTERVAL_SECONDS, normalizeInterval(MIN_INTERVAL_SECONDS - 1))
    }

    @Test
    fun normalizeIntervalCapsAboveCeiling() {
        assertEquals(MAX_INTERVAL_SECONDS, normalizeInterval(MAX_INTERVAL_SECONDS + 1))
        assertEquals(MAX_INTERVAL_SECONDS, normalizeInterval(Int.MAX_VALUE))
    }

    @Test
    fun normalizeIntervalLeavesInRangeValuesUnchanged() {
        assertEquals(MIN_INTERVAL_SECONDS, normalizeInterval(MIN_INTERVAL_SECONDS))
        assertEquals(3600, normalizeInterval(3600))
        assertEquals(MAX_INTERVAL_SECONDS, normalizeInterval(MAX_INTERVAL_SECONDS))
    }
}
