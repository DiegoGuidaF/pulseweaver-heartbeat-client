package com.pulseweaver.heartbeat.service

/**
 * Result of a single heartbeat POST. The UI layer is responsible for
 * capturing the wall-clock time when it receives this result.
 */
data class HeartbeatResult(
    val success: Boolean,
    val message: String,
    val hint: String? = null,
    val ip: String? = null,
    val trigger: String,
) {
    companion object {
        fun success(ip: String, trigger: String) = HeartbeatResult(
            success = true,
            message = if (trigger == "manual") "IP updated" else "Heartbeat sent",
            ip = ip,
            trigger = trigger,
        )

        fun error(message: String, hint: String, trigger: String = "scheduled") = HeartbeatResult(
            success = false,
            message = message,
            hint = hint,
            trigger = trigger,
        )
    }
}
