package com.pulseweaver.heartbeat.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.ResultStore
import com.pulseweaver.heartbeat.service.HeartbeatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Fired by AlarmManager via an allow-while-idle PendingIntent. Unlike a WorkManager/JobScheduler
// job, this is released even in Doze, which is what keeps the heartbeat alive on an idle phone.
// Each fire sends one heartbeat and arms the next alarm — the chain is the periodic schedule.
class HeartbeatAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_HEARTBEAT_ALARM) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = ConfigStore().load()
                if (!config.enabled) return@launch
                var success = false
                try {
                    val result = HeartbeatClient().send(config, "scheduled")
                    ResultStore().save(result, currentTimeForDisplay(), currentEpochMs())
                    success = result.success
                } finally {
                    // Always re-arm so the chain can't die — on a failed (or thrown) send, retry
                    // soon instead of waiting a full interval; a later success returns to cadence.
                    // Over-polling is harmless (the server only needs one refresh per interval), and
                    // allow-while-idle alarms are OS-clamped to ~9 min apart in deep Doze anyway.
                    val nextDelaySeconds = if (success) config.intervalSeconds else RETRY_DELAY_SECONDS
                    scheduleHeartbeatAlarm(context, nextDelaySeconds)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HEARTBEAT_ALARM = "com.pulseweaver.heartbeat.ACTION_HEARTBEAT_ALARM"

        // Retry delay after a failed heartbeat — short so a transient network/server blip recovers
        // well before the next full interval would.
        private const val RETRY_DELAY_SECONDS = 60
    }
}
