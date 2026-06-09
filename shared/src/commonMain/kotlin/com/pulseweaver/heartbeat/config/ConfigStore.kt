package com.pulseweaver.heartbeat.config

expect class ConfigStore() {
    suspend fun load(): HeartbeatConfig

    suspend fun save(config: HeartbeatConfig)
}
