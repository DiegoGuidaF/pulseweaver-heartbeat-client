package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Stage later: implement with NSUserDefaults.
actual class ResultStore actual constructor() {
    actual suspend fun load(): LastHeartbeatState? = null

    actual fun observe(): Flow<LastHeartbeatState?> = flow { emit(load()) }

    actual suspend fun save(
        result: HeartbeatResult,
        time: String,
        epochMs: Long,
    ) = Unit
}
