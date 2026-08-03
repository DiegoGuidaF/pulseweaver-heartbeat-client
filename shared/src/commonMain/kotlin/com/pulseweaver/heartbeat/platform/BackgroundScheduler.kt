package com.pulseweaver.heartbeat.platform

/**
 * Schedules a periodic heartbeat on each platform.
 *
 * Constructed with platform-specific parameters in each entry point:
 * - Desktop (jvmMain): BackgroundScheduler() — coroutine timer on its own background scope
 * - Android (androidMain): BackgroundScheduler(context) — allow-while-idle alarm chain;
 *   onTick is unused, the alarm receiver drives sends and the UI observes ResultStore
 * - iOS (iosMain): BackgroundScheduler() — BGAppRefreshTask (Stage later)
 *
 * [onTick] reports whether the beat succeeded; a scheduler that drives ticks itself uses
 * a failed beat to retry soon instead of waiting out a full interval.
 *
 * Common code never constructs this; it is always passed in from the entry point.
 */
expect class BackgroundScheduler {
    fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Boolean,
    )

    fun cancelHeartbeat()
}
