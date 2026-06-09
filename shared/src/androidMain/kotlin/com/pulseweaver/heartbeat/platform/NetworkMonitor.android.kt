package com.pulseweaver.heartbeat.platform

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.getSystemService
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual class NetworkMonitor actual constructor() {
    private var callback: ConnectivityManager.NetworkCallback? = null

    // ACCESS_NETWORK_STATE is declared in the app manifest; lint cannot see it from this library module.
    @SuppressLint("MissingPermission")
    actual fun startMonitoring(onNetworkChange: () -> Unit) {
        val cm =
            ApplicationContextHolder.context.getSystemService<ConnectivityManager>()
                ?: return

        val cb =
            object : ConnectivityManager.NetworkCallback() {
                // registerDefaultNetworkCallback fires onAvailable immediately for the current
                // network — skip it so we only react to actual connectivity changes.
                private var initialCallbackFired = false

                override fun onAvailable(network: Network) {
                    if (!initialCallbackFired) {
                        initialCallbackFired = true
                        return
                    }
                    onNetworkChange()
                }

                override fun onLost(network: Network) = onNetworkChange()
            }
        callback = cb
        cm.registerDefaultNetworkCallback(cb)
    }

    actual fun stopMonitoring() {
        val cm =
            ApplicationContextHolder.context.getSystemService<ConnectivityManager>()
                ?: return
        callback?.let { cm.unregisterNetworkCallback(it) }
        callback = null
    }
}
