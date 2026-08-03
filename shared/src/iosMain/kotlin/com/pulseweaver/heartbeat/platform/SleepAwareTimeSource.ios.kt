package com.pulseweaver.heartbeat.platform

import kotlin.time.TimeSource

/**
 * Stub, matching the [BiometricAuth] iOS actual: the biometric lock is the only caller and
 * it is not implemented on iOS, so nothing reads this clock. Should that change, swap in
 * `mach_continuous_time()` — Kotlin/Native's [TimeSource.Monotonic] uses
 * `mach_absolute_time()`, which stops while the device sleeps.
 */
actual val sleepAwareTimeSource: TimeSource = TimeSource.Monotonic
