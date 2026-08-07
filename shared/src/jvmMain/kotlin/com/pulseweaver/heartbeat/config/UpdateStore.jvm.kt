package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.platform.channelSuffix
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("com/pulseweaver/heartbeat${channelSuffix()}/update")

actual class UpdateStore actual constructor() {
    actual suspend fun load(): UpdateState =
        UpdateState(
            lastCheckedAtMs = prefs.getLong("lastCheckedAtMs", 0L),
            skippedVersion = prefs.get("skippedVersion", ""),
        )

    actual suspend fun save(state: UpdateState) {
        prefs.putLong("lastCheckedAtMs", state.lastCheckedAtMs)
        prefs.put("skippedVersion", state.skippedVersion)
        prefs.flush()
    }
}
