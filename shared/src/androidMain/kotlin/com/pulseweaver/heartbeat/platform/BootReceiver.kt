package com.pulseweaver.heartbeat.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulseweaver.heartbeat.config.ConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = ConfigStore().load()
                if (config.enabled) {
                    enqueueHeartbeatWork(context, config.intervalSeconds)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
