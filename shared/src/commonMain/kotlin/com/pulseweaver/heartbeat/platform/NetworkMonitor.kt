package com.pulseweaver.heartbeat.platform

/**
 * Monitors network connectivity and fires [onNetworkChange] when a change could mean a
 * new public IP the server should hear about.
 *
 * Desktop: polls NetworkInterface every 30 s, fires when the set of IP addresses changes.
 * Android: default-network callback — fires on arrival/switch (Wi-Fi <-> cellular), not
 * on loss: a heartbeat into a network that is already gone is doomed by definition.
 * iOS: NWPathMonitor (Stage later).
 */
expect class NetworkMonitor() {
    fun startMonitoring(onNetworkChange: () -> Unit)

    fun stopMonitoring()
}
