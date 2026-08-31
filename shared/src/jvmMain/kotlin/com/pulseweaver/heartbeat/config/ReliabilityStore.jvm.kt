package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.platform.channelSuffix
import java.util.prefs.Preferences

private val prefs: Preferences =
    Preferences.userRoot().node("com/pulseweaver/heartbeat${channelSuffix()}/reliability")

// Desktop is always exempt, so nothing ever reads a meaningful value here. Implemented rather
// than stubbed so the store behaves the same on every target that has real persistence.
actual class ReliabilityStore actual constructor() {
    actual suspend fun load(): ReliabilityState = ReliabilityState(promptSeen = prefs.getBoolean("promptSeen", false))

    actual suspend fun save(state: ReliabilityState) {
        prefs.putBoolean("promptSeen", state.promptSeen)
        prefs.flush()
    }
}
