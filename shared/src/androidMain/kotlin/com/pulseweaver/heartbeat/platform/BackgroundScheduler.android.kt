package com.pulseweaver.heartbeat.platform

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pulseweaver.heartbeat.service.HeartbeatWorker

// Legacy unique name of the pre-PW-92 periodic WorkManager schedule. Kept only so it can be
// cancelled — a request persisted by an older install would otherwise double-fire alongside the
// alarm chain after an app upgrade.
private const val LEGACY_PERIODIC_WORK_NAME = "heartbeat"
private const val NETWORK_CHANGE_WORK_NAME = "heartbeat_network_change"
private const val NETWORK_CALLBACK_REQUEST_CODE = 1001
private const val HEARTBEAT_ALARM_REQUEST_CODE = 1002

// Arms (or re-arms) the self-rescheduling heartbeat alarm. AlarmManager's allow-while-idle alarms
// fire during Doze — unlike WorkManager/JobScheduler work — so this is what keeps an idle phone
// beating. The alarm is one-shot; HeartbeatAlarmReceiver schedules the next one after each send.
// Called from BackgroundScheduler, BootReceiver, and the receiver itself (alarms don't survive
// reboot or force-stop, so the chain is re-established on app start and on boot).
internal fun scheduleHeartbeatAlarm(
    context: Context,
    intervalSeconds: Int,
) {
    // Evict any periodic schedule left by a pre-alarm install so it can't fire in parallel.
    WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)

    val alarmManager = context.getSystemService<AlarmManager>() ?: return
    val triggerAtMillis = System.currentTimeMillis() + intervalSeconds * 1000L
    // Inexact (the OS may coalesce and enforces a ~9-min floor while idle), which is fine for a
    // >=15-min heartbeat and needs no exact-alarm permission.
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        heartbeatAlarmPendingIntent(context),
    )
}

internal fun cancelHeartbeatAlarm(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
    val alarmManager = context.getSystemService<AlarmManager>() ?: return
    alarmManager.cancel(heartbeatAlarmPendingIntent(context))
}

// Stable request code + immutable PendingIntent: re-arming replaces the pending alarm rather than
// stacking. The receiver needs no extras — it reads config fresh on each fire.
private fun heartbeatAlarmPendingIntent(context: Context): PendingIntent {
    val intent =
        Intent(context, HeartbeatAlarmReceiver::class.java).apply {
            action = HeartbeatAlarmReceiver.ACTION_HEARTBEAT_ALARM
        }
    return PendingIntent.getBroadcast(
        context,
        HEARTBEAT_ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
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
    // Android ticks come from the allow-while-idle alarm chain (and the network-change callback).
    // The open UI stays in sync by observing ResultStore. onTick is unused here — it drives the
    // desktop coroutine scheduler.
    actual fun schedulePeriodicHeartbeat(
        intervalSeconds: Int,
        onTick: suspend () -> Unit,
    ) {
        scheduleHeartbeatAlarm(context, intervalSeconds)
        registerNetworkChangeCallback(context)
    }

    actual fun cancelHeartbeat() {
        cancelHeartbeatAlarm(context)
        unregisterNetworkChangeCallback(context)
    }
}
