package com.pulseweaver.heartbeat.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulseweaver.heartbeat.config.ConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Invoked by the system via PendingIntent whenever the set of internet-capable networks changes
// (e.g. Wi-Fi <-> cellular). Wakes the app to refresh the heartbeat so the server sees the new
// public IP promptly, even when the process was not running.
class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (ConfigStore().load().enabled) {
                    enqueueNetworkChangeHeartbeat(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
