package com.pulseweaver.heartbeat.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.NetworkInterface

actual class NetworkMonitor actual constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    actual fun startMonitoring(onNetworkChange: () -> Unit) {
        job?.cancel()
        var lastAddresses = currentAddresses()
        job = scope.launch {
            while (isActive) {
                delay(30_000)
                val current = currentAddresses()
                if (current != lastAddresses) {
                    lastAddresses = current
                    onNetworkChange()
                }
            }
        }
    }

    actual fun stopMonitoring() {
        job?.cancel()
        job = null
    }

    fun close() {
        scope.cancel()
    }

    private fun currentAddresses(): Set<String> =
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.filter { it.isUp && !it.isLoopback }
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.map { it.hostAddress ?: "" }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()
}
