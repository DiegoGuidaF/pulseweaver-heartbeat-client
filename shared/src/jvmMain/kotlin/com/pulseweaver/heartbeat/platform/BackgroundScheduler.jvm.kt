package com.pulseweaver.heartbeat.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Desktop scheduler. Runs on its own background scope rather than the Compose UI
 * scope: `delay` on the AWT event thread is throttled by macOS App Nap when the
 * app is backgrounded, and tying the loop to the UI scope let a window close
 * cancel it. A dedicated `Dispatchers.Default` scope keeps beating regardless of
 * window visibility or App Nap.
 *
 * The scope is injectable so tests can drive it with a controlled dispatcher.
 */
actual class BackgroundScheduler(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var job: Job? = null

    actual fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Unit,
    ) {
        job?.cancel()
        Log.i("Scheduler", "scheduled: every ${intervalSeconds}s")
        job =
            scope.launch {
                while (isActive) {
                    delay(intervalSeconds * 1_000L)
                    if (isActive) {
                        Log.d("Scheduler", "tick")
                        onTick()
                    }
                }
            }
    }

    actual fun cancelHeartbeat() {
        if (job != null) Log.i("Scheduler", "cancelled")
        job?.cancel()
        job = null
    }
}
