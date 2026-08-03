package com.pulseweaver.heartbeat.platform

import kotlin.time.TimeSource

/**
 * Interval clock that keeps counting while the device sleeps.
 *
 * [TimeSource.Monotonic] does not: on Android it reads `System.nanoTime()`, which shares
 * `SystemClock.uptimeMillis()` semantics and freezes in deep sleep. A security timeout
 * measured with it silently stops elapsing exactly when the device is unattended — a
 * phone left in a pocket overnight comes back reporting seconds.
 *
 * Use this for any timeout whose whole point is that wall time passed. Plain
 * [TimeSource.Monotonic] is still the right choice for measuring work the app itself did.
 */
expect val sleepAwareTimeSource: TimeSource
