package com.pulseweaver.heartbeat.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pulseweaver.heartbeat.platform.sleepAwareTimeSource
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Process-scoped unlock state behind [AuthGate].
 *
 * The unlock deliberately lives outside the composition. An Activity recreation —
 * rotation, theme switch, font-scale change — drops the whole composition, and state
 * held in `remember` would come back cleared and re-prompt for a fingerprint the user
 * already gave.
 *
 * Only a real trip to the background starts the grace-period clock. The platform entry
 * point decides what counts as one by calling [onEnteredBackground]; a configuration
 * change must not.
 *
 * Timing runs on [sleepAwareTimeSource], not [TimeSource.Monotonic], so the grace period
 * keeps elapsing while the device sleeps — see that declaration for why the distinction
 * decides whether the lock engages at all.
 */
object AuthSession {
    /** Swapped for a `TestTimeSource` in tests. */
    internal var timeSource: TimeSource = sleepAwareTimeSource

    private var lastAuthMark: TimeMark? = null
    private var backgroundedAt: TimeMark? = null

    var isUnlocked: Boolean by mutableStateOf(false)
        private set

    fun markUnlocked() {
        isUnlocked = true
        lastAuthMark = timeSource.markNow()
        backgroundedAt = null
    }

    fun lock() {
        isUnlocked = false
        lastAuthMark = null
        backgroundedAt = null
    }

    /**
     * Records that the app really left the foreground. Repeated calls keep the earliest
     * mark, so the grace period measures the whole absence rather than its last leg.
     */
    fun onEnteredBackground() {
        if (backgroundedAt == null) backgroundedAt = timeSource.markNow()
    }

    /**
     * Whether returning to the foreground should re-prompt: true only when the app came
     * back from a real background trip and [grace] has elapsed since the last successful
     * authentication.
     *
     * Consumes the pending background trip, so the resume that recreated the Activity and
     * the `ON_RESUME` that follows it cannot both relock.
     */
    fun shouldRelock(grace: Duration): Boolean {
        if (!isUnlocked) return false
        // No pending trip means a configuration change brought us back, not the user.
        if (backgroundedAt == null) return false
        backgroundedAt = null
        val mark = lastAuthMark ?: return true
        return mark.elapsedNow() > grace
    }
}
