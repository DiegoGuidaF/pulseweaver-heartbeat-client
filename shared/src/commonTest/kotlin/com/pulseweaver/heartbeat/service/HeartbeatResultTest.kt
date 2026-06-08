package com.pulseweaver.heartbeat.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartbeatResultTest {
    // ── success factory ─────────────────────────────────────────────

    @Test
    fun success_manualTrigger_saysIpUpdated() {
        val result = HeartbeatResult.success("1.2.3.4", trigger = "manual")

        assertTrue(result.success)
        assertEquals("IP updated", result.message)
        assertEquals("1.2.3.4", result.ip)
        assertEquals("manual", result.trigger)
        assertNull(result.hint)
    }

    @Test
    fun success_scheduledTrigger_saysHeartbeatSent() {
        val result = HeartbeatResult.success("10.0.0.1", trigger = "scheduled")

        assertTrue(result.success)
        assertEquals("Heartbeat sent", result.message)
        assertEquals("10.0.0.1", result.ip)
    }

    // ── error factory ───────────────────────────────────────────────

    @Test
    fun error_carriesMessageAndHint() {
        val result =
            HeartbeatResult.error(
                "Invalid API key",
                "Check your device settings on the server",
                trigger = "scheduled",
            )

        assertFalse(result.success)
        assertEquals("Invalid API key", result.message)
        assertEquals("Check your device settings on the server", result.hint)
        assertNull(result.ip)
    }

    @Test
    fun error_defaultTriggerIsScheduled() {
        val result = HeartbeatResult.error("Connection failed", "Try again later")

        assertEquals("scheduled", result.trigger)
    }

    // ── equality ────────────────────────────────────────────────────

    @Test
    fun sameValues_areEqual() {
        val a = HeartbeatResult.success("1.2.3.4", "manual")
        val b = HeartbeatResult.success("1.2.3.4", "manual")
        assertEquals(a, b)
    }
}
