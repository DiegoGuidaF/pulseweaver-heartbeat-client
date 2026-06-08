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
}
