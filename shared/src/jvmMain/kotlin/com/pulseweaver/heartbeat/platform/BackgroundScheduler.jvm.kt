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
        val effectiveSeconds = debugIntervalOverrideSeconds() ?: intervalSeconds
        if (effectiveSeconds != intervalSeconds) {
            Log.w("Scheduler", "debug interval override: ${effectiveSeconds}s (configured ${intervalSeconds}s)")
        }
        Log.i("Scheduler", "scheduled: every ${effectiveSeconds}s")
        job =
            scope.launch {
                while (isActive) {
                    delay(effectiveSeconds * 1_000L)
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

/**
 * Debug-only heartbeat interval in seconds, or null when unset. Lets a developer watch a live
 * scheduled beat in seconds instead of waiting out the [MIN_INTERVAL_SECONDS] floor. Honoured
 * only when debug mode is on ([isDebugLoggingEnabled]), so a normal install can never shorten its
 * interval: run with `PW_DEBUG=1 PW_INTERVAL=5` (env, inherited by `gradlew run`) or
 * `-Dpw.debug -Dpw.interval=5`.
 */
private fun debugIntervalOverrideSeconds(): Int? {
    if (!isDebugLoggingEnabled()) return null
    val raw = System.getenv("PW_INTERVAL") ?: System.getProperty("pw.interval") ?: return null
    return raw.toIntOrNull()?.takeIf { it > 0 }
}
