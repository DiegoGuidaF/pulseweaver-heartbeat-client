package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.platform.channelSuffix
import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("com/pulseweaver/heartbeat${channelSuffix()}/result")

actual class ResultStore actual constructor() {
    // The desktop UI is driven directly by the in-process scheduler, so a single emission is enough.
    actual fun observe(): Flow<LastHeartbeatState?> = flow { emit(load()) }

    actual suspend fun load(): LastHeartbeatState? {
        val success = prefs.get("success", null) ?: return null
        return LastHeartbeatState(
            result =
                HeartbeatResult(
                    success = success.toBoolean(),
                    message = prefs.get("message", ""),
                    hint = prefs.get("hint", null),
                    ip = prefs.get("ip", null),
                    trigger = prefs.get("trigger", ""),
                ),
            time = prefs.get("time", ""),
            epochMs = prefs.getLong("epochMs", 0L),
        )
    }

    actual suspend fun save(
        result: HeartbeatResult,
        time: String,
        epochMs: Long,
    ) {
        prefs.put("success", result.success.toString())
        prefs.put("message", result.message)
        if (result.hint != null) prefs.put("hint", result.hint) else prefs.remove("hint")
        if (result.ip != null) prefs.put("ip", result.ip) else prefs.remove("ip")
        prefs.put("trigger", result.trigger)
        prefs.put("time", time)
        prefs.putLong("epochMs", epochMs)
        prefs.flush()
    }
}
