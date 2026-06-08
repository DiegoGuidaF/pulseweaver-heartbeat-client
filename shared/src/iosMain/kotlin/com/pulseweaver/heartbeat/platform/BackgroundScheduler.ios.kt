package com.pulseweaver.heartbeat.platform

// Stage later: implement with BGAppRefreshTask.
actual class BackgroundScheduler {
    actual fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Unit,
    ) = Unit

    actual fun cancelHeartbeat() = Unit
}
