package com.pulseweaver.heartbeat.platform

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the debug-only interval override: it must fire beats early only when debug mode is on,
 * so a normal install can never shorten its heartbeat below the [MIN_INTERVAL_SECONDS] floor.
 */
class BackgroundSchedulerTest {
    @AfterTest
    fun clearOverride() {
        System.clearProperty("pw.debug")
        System.clearProperty("pw.interval")
    }

    @Test
    fun overrideHonouredWhenDebugOn() =
        runTest {
            System.setProperty("pw.debug", "1")
            System.setProperty("pw.interval", "2")
            val scheduler = BackgroundScheduler(this)
            var ticks = 0
            scheduler.schedulePeriodicHeartbeat(900) {
                ticks++
                true
            }
            advanceTimeBy(2_100)
            runCurrent()
            assertEquals(1, ticks, "should beat at the 2s override, not the 900s floor")
            scheduler.cancelHeartbeat()
        }

    @Test
    fun overrideIgnoredWhenDebugOff() =
        runTest {
            System.setProperty("pw.interval", "2")
            val scheduler = BackgroundScheduler(this)
            var ticks = 0
            scheduler.schedulePeriodicHeartbeat(900) {
                ticks++
                true
            }
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(0, ticks, "no debug flag → interval override must not apply")
            scheduler.cancelHeartbeat()
        }

    @Test
    fun failedBeatRetriesEarlyThenSuccessReturnsToCadence() =
        runTest {
            val scheduler = BackgroundScheduler(this)
            var ticks = 0
            // First beat fails, every later one succeeds.
            scheduler.schedulePeriodicHeartbeat(900) {
                ticks++
                ticks > 1
            }

            advanceTimeBy(900_050)
            runCurrent()
            assertEquals(1, ticks, "first beat at the full interval")

            advanceTimeBy(59_000)
            runCurrent()
            assertEquals(1, ticks, "retry must not fire before the 60s retry delay")

            advanceTimeBy(1_100)
            runCurrent()
            assertEquals(2, ticks, "failed beat retries after 60s, not a full interval")

            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(2, ticks, "successful beat returns to full cadence")

            advanceTimeBy(840_000)
            runCurrent()
            assertEquals(3, ticks, "next beat lands a full interval after the success")
            scheduler.cancelHeartbeat()
        }
}
