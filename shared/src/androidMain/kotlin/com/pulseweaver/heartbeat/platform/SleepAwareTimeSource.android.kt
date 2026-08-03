package com.pulseweaver.heartbeat.platform

import android.os.SystemClock
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * Backed by `SystemClock.elapsedRealtimeNanos()` (`CLOCK_BOOTTIME`) — Android's
 * recommended basis for general-purpose interval timing, and the only one of the three
 * system clocks that both counts deep sleep and cannot be moved by the user or the network.
 */
actual val sleepAwareTimeSource: TimeSource =
    object : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
        override fun read(): Long = SystemClock.elapsedRealtimeNanos()
    }
