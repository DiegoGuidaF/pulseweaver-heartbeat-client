package com.pulseweaver.heartbeat.platform

import kotlin.time.TimeSource

/**
 * The JVM exposes no portable suspend-aware clock, so this falls back to
 * [TimeSource.Monotonic]. Nothing on desktop depends on the distinction: the only caller
 * is the biometric lock, and [BiometricAuth.isAvailable] is false here.
 */
actual val sleepAwareTimeSource: TimeSource = TimeSource.Monotonic
