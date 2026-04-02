package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.service.HeartbeatResult

data class LastHeartbeatState(
    val result: HeartbeatResult,
    val time: String,
    val epochMs: Long,
)

expect class ResultStore() {
    suspend fun load(): LastHeartbeatState?
    suspend fun save(result: HeartbeatResult, time: String, epochMs: Long)
}
