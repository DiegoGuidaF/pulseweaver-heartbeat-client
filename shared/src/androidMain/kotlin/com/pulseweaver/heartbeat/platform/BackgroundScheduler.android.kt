package com.pulseweaver.heartbeat.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pulseweaver.heartbeat.service.HeartbeatWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "heartbeat"

// Android enforces a 15-minute minimum for PeriodicWorkRequest
private const val ANDROID_MIN_INTERVAL_SECONDS = 15 * 60L

// Called from both BackgroundScheduler and BootReceiver to avoid duplicating WorkManager setup.
internal fun enqueueHeartbeatWork(
    context: Context,
    intervalSeconds: Int,
) {
    val bgInterval = maxOf(intervalSeconds.toLong(), ANDROID_MIN_INTERVAL_SECONDS)
    val request =
        PeriodicWorkRequestBuilder<HeartbeatWorker>(bgInterval, TimeUnit.SECONDS)
            .setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()
    WorkManager
        .getInstance(context)
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}

actual class BackgroundScheduler(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var foregroundJob: Job? = null

    actual fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Unit,
    ) {
        // Foreground coroutine: fires at the requested interval while the app process is alive.
        // This allows sub-15-minute intervals when the app is in the foreground.
        foregroundJob?.cancel()
        foregroundJob =
            scope.launch {
                while (isActive) {
                    delay(intervalSeconds * 1000L)
                    if (isActive) onTick()
                }
            }

        enqueueHeartbeatWork(context, intervalSeconds)
    }

    actual fun cancelHeartbeat() {
        foregroundJob?.cancel()
        foregroundJob = null
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
