package com.pulseweaver.heartbeat.config

// Stage later: implement with NSUserDefaults (non-sensitive) + Keychain (API key).
actual class ConfigStore actual constructor() {
    actual suspend fun load(): HeartbeatConfig = HeartbeatConfig()

    actual suspend fun save(config: HeartbeatConfig) = Unit
}
