package com.pulseweaver.heartbeat.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.ResultStore
import com.pulseweaver.heartbeat.platform.currentEpochMs
import com.pulseweaver.heartbeat.platform.currentTimeForDisplay

class HeartbeatWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore().load()
        if (!config.enabled) return Result.success()
        val result = HeartbeatClient().send(config, "background_worker")
        ResultStore().save(result, currentTimeForDisplay(), currentEpochMs())
        // Always return success — heartbeat failures are non-fatal background events
        return Result.success()
    }
}
