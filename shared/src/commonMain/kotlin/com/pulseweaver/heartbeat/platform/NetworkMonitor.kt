package com.pulseweaver.heartbeat.platform

/**
 * Monitors network connectivity changes and fires [onNetworkChange] on any change.
 *
 * Desktop: polls NetworkInterface every 30 s, fires when the set of IP addresses changes.
 * Android: ConnectivityManager callback (Stage 4).
 * iOS: NWPathMonitor (Stage later).
 */
expect class NetworkMonitor() {
    fun startMonitoring(onNetworkChange: () -> Unit)
    fun stopMonitoring()
}
