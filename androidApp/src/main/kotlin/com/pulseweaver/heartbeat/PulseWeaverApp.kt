package com.pulseweaver.heartbeat

import android.app.Application
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.platform.registerNetworkChangeCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PulseWeaverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.init(this)
        // The system-held network callback is dropped when the process is killed; re-establish it
        // on cold start so connectivity changes keep triggering heartbeats while enabled.
        MainScope().launch(Dispatchers.IO) {
            if (ConfigStore().load().enabled) {
                registerNetworkChangeCallback(this@PulseWeaverApp)
            }
        }
    }
}
