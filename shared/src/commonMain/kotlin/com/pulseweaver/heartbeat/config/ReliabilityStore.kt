package com.pulseweaver.heartbeat.config

/**
 * Bookkeeping for the battery-optimization prompt. The default means "never prompted", so a
 * fresh [ReliabilityState] is always safe to use.
 */
data class ReliabilityState(
    val promptSeen: Boolean = false,
)

/**
 * Persists whether the reliability prompt has already had its one modal appearance.
 *
 * Kept apart from [HeartbeatConfig] on purpose: this is app state, not user configuration. It
 * has no place in the settings UI, and it must keep working while settings are locked.
 * [UpdateStore] and [ResultStore] are the same shape for the same reason.
 *
 * The flag survives upgrades but not a data wipe, which is the intent — a fresh install has
 * not yet asked this user for the exemption.
 */
expect class ReliabilityStore() {
    suspend fun load(): ReliabilityState

    suspend fun save(state: ReliabilityState)
}
