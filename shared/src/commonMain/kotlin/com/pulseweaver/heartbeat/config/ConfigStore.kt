package com.pulseweaver.heartbeat.config

import com.pulseweaver.heartbeat.config.HeartbeatConfig

expect class ConfigStore() {
    suspend fun load(): HeartbeatConfig

    suspend fun save(config: HeartbeatConfig)
}
