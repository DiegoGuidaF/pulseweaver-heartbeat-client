package com.pulseweaver.heartbeat.config

// Stage later: implement with NSUserDefaults. Harmless while stubbed — iOS reports itself
// exempt from battery optimization, so the reliability prompt never renders there.
actual class ReliabilityStore actual constructor() {
    actual suspend fun load(): ReliabilityState = ReliabilityState()

    actual suspend fun save(state: ReliabilityState) = Unit
}
