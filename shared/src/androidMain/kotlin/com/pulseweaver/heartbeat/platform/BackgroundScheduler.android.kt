package com.pulseweaver.heartbeat.platform

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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
private const val NETWORK_CHANGE_WORK_NAME = "heartbeat_network_change"
private const val NETWORK_CALLBACK_REQUEST_CODE = 1001

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

// One-shot expedited heartbeat fired when connectivity changes. Unique + REPLACE collapses a
// burst of rapid transitions (e.g. Wi-Fi dropping then cellular coming up) into a single send.
internal fun enqueueNetworkChangeHeartbeat(context: Context) {
    val request =
        OneTimeWorkRequestBuilder<HeartbeatWorker>()
            .setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(HeartbeatWorker.KEY_REASON to "network_change"))
            .build()
    WorkManager
        .getInstance(context)
        .enqueueUniqueWork(NETWORK_CHANGE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
}

// Registers a system-held network callback so connectivity changes wake the app to send a
// heartbeat even when the process is dead. The registration does not survive a reboot, so it is
// re-established from BootReceiver and on app start. Idempotent: the stable PendingIntent means
// re-registering replaces the existing request rather than stacking callbacks.
// Public because PulseWeaverApp (in the :androidApp module) re-registers it on cold start.
// ACCESS_NETWORK_STATE is declared in the app manifest; lint cannot see it from this library module.
@SuppressLint("MissingPermission")
fun registerNetworkChangeCallback(context: Context) {
    // registerNetworkCallback(NetworkRequest, PendingIntent) requires API 26. Older devices fall
    // back to periodic WorkManager only.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val cm = context.getSystemService<ConnectivityManager>() ?: return
    val request =
        NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
    val pending = networkChangePendingIntent(context, allowCreate = true) ?: return
    try {
        cm.registerNetworkCallback(request, pending)
    } catch (_: RuntimeException) {
        // TooManyRequestsException or an already-active registration — the callback is live, ignore.
    }
}

internal fun unregisterNetworkChangeCallback(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val cm = context.getSystemService<ConnectivityManager>() ?: return
    val pending = networkChangePendingIntent(context, allowCreate = false) ?: return
    try {
        cm.unregisterNetworkCallback(pending)
    } catch (_: IllegalArgumentException) {
        // Callback was not registered, ignore.
    }
}

// The PendingIntent must be mutable so the system can attach the EXTRA_NETWORK extras when it
// fires. A stable request code + component keeps it identical across register/unregister calls.
private fun networkChangePendingIntent(
    context: Context,
    allowCreate: Boolean,
): PendingIntent? {
    val intent = Intent(context, NetworkChangeReceiver::class.java)
    var flags = if (allowCreate) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        flags = flags or PendingIntent.FLAG_MUTABLE
    }
    return PendingIntent.getBroadcast(context, NETWORK_CALLBACK_REQUEST_CODE, intent, flags)
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
        registerNetworkChangeCallback(context)
    }

    actual fun cancelHeartbeat() {
        foregroundJob?.cancel()
        foregroundJob = null
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        unregisterNetworkChangeCallback(context)
    }
}
