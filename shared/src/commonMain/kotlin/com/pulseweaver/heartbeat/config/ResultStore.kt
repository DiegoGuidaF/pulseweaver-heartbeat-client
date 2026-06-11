package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.flow.Flow

data class LastHeartbeatState(
    val result: HeartbeatResult,
    val time: String,
    val epochMs: Long,
)

expect class ResultStore() {
    suspend fun load(): LastHeartbeatState?

    /**
     * Emits the stored result, then re-emits on every write. On Android the worker writes from the
     * same process, so an open screen observes its background heartbeats; jvm/iOS emit once (their
     * UI is already driven directly by the in-process scheduler).
     */
    fun observe(): Flow<LastHeartbeatState?>

    suspend fun save(
        result: HeartbeatResult,
        time: String,
        epochMs: Long,
    )
}
