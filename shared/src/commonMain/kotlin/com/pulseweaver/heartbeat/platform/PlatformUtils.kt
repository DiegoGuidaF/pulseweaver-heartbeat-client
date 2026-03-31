package com.pulseweaver.heartbeat.platform

/** Returns the current wall-clock time formatted as HH:mm:ss for display in the UI. */
expect fun currentTimeForDisplay(): String

/** True on mobile platforms where background intervals are OS-clamped to ≥15 minutes. */
expect val platformHasBackgroundLimit: Boolean
