package com.pulseweaver.heartbeat.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual class BackgroundScheduler(private val appScope: CoroutineScope) {

    private var job: Job? = null

    actual fun schedulePeriodicHeartbeat(intervalSeconds: Int, onTick: suspend () -> Unit) {
        job?.cancel()
        job = appScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1_000L)
                if (isActive) onTick()
            }
        }
    }

    actual fun cancelHeartbeat() {
        job?.cancel()
        job = null
    }
}
