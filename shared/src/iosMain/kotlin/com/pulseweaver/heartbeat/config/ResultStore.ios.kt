package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.service.HeartbeatResult

// Stage later: implement with NSUserDefaults.
actual class ResultStore actual constructor() {
    actual suspend fun load(): LastHeartbeatState? = null

    actual suspend fun save(
        result: HeartbeatResult,
        time: String,
        epochMs: Long,
    ) = Unit
}
