package com.pulseweaver.heartbeat.platform

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual class NetworkMonitor actual constructor() {

    private var callback: ConnectivityManager.NetworkCallback? = null

    actual fun startMonitoring(onNetworkChange: () -> Unit) {
        val cm = ApplicationContextHolder.context.getSystemService<ConnectivityManager>()
            ?: return

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onNetworkChange()
            override fun onLost(network: Network) = onNetworkChange()
        }
        callback = cb
        cm.registerDefaultNetworkCallback(cb)
    }

    actual fun stopMonitoring() {
        val cm = ApplicationContextHolder.context.getSystemService<ConnectivityManager>()
            ?: return
        callback?.let { cm.unregisterNetworkCallback(it) }
        callback = null
    }
}

