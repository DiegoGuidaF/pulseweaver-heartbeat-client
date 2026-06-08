package com.pulseweaver.heartbeat.platform

/**
 * Schedules a periodic heartbeat on each platform.
 *
 * Constructed with platform-specific parameters in each entry point:
 * - Desktop (jvmMain): BackgroundScheduler(appScope: CoroutineScope)
 * - Android (androidMain): BackgroundScheduler() — WorkManager (Stage 4)
 * - iOS (iosMain): BackgroundScheduler() — BGAppRefreshTask (Stage later)
 *
 * Common code never constructs this; it is always passed in from the entry point.
 */
expect class BackgroundScheduler {
    fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Unit,
    )

    fun cancelHeartbeat()
}
