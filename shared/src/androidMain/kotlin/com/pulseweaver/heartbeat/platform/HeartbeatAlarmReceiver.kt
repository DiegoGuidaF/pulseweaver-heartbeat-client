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
                // Arm the retry link BEFORE sending: goAsync() only protects the process for
                // ~10 s, so a send that hangs — or a process kill mid-send — must already have
                // the next alarm armed or the chain dies until the next app open / reboot.
                // Over-polling is harmless (the server only needs one refresh per interval), and
                // allow-while-idle alarms are OS-clamped to ~9 min apart in deep Doze anyway.
                scheduleHeartbeatAlarm(context, RETRY_DELAY_SECONDS)
                val client = HeartbeatClient()
                try {
                    val result = client.send(config, "scheduled")
                    ResultStore().save(result, currentTimeForDisplay(), currentEpochMs())
                    // A success stretches the pre-armed retry back to full cadence; a failure
                    // keeps the short retry so a transient blip recovers quickly.
                    if (result.success) scheduleHeartbeatAlarm(context, config.intervalSeconds)
                } finally {
                    client.close()
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
