package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.service.HeartbeatResult
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("com/pulseweaver/heartbeat/result")

actual class ResultStore actual constructor() {

    actual suspend fun load(): LastHeartbeatState? {
        val success = prefs.get("success", null) ?: return null
        return LastHeartbeatState(
            result = HeartbeatResult(
                success = success.toBoolean(),
                message = prefs.get("message", ""),
                hint = prefs.get("hint", null),
                ip = prefs.get("ip", null),
                trigger = prefs.get("trigger", ""),
            ),
            time = prefs.get("time", ""),
        )
    }

    actual suspend fun save(result: HeartbeatResult, time: String) {
        prefs.put("success", result.success.toString())
        prefs.put("message", result.message)
        if (result.hint != null) prefs.put("hint", result.hint) else prefs.remove("hint")
        if (result.ip != null) prefs.put("ip", result.ip) else prefs.remove("ip")
        prefs.put("trigger", result.trigger)
        prefs.put("time", time)
        prefs.flush()
    }
}
