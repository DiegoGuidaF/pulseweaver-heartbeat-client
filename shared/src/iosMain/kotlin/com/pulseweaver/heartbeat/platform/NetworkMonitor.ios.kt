package com.pulseweaver.heartbeat.platform

// Stage later: implement with NWPathMonitor.
actual class NetworkMonitor actual constructor() {
    actual fun startMonitoring(onNetworkChange: () -> Unit) = Unit
    actual fun stopMonitoring() = Unit
}
