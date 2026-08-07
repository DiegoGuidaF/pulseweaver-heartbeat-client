package com.pulseweaver.heartbeat.config

// Stage later: implement with NSUserDefaults. Harmless while stubbed — iOS installs no
// updates in-app, so the notice never renders there.
actual class UpdateStore actual constructor() {
    actual suspend fun load(): UpdateState = UpdateState()

    actual suspend fun save(state: UpdateState) = Unit
}
