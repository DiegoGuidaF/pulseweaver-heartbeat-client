package com.pulseweaver.heartbeat.config

/**
 * Bookkeeping for the update check. Both fields have defaults, so a fresh [UpdateState] means
 * "never checked, nothing skipped" and is always safe to use.
 */
data class UpdateState(
    val lastCheckedAtMs: Long = 0L,
    val skippedVersion: String = "",
)

/**
 * Persists update-check bookkeeping.
 *
 * Kept apart from [HeartbeatConfig] on purpose: this is app state, not user configuration. It
 * has no place in the settings UI, and it must keep working while settings are locked.
 * [ResultStore] is the same shape for the same reason.
 */
expect class UpdateStore() {
    suspend fun load(): UpdateState

    suspend fun save(state: UpdateState)
}
