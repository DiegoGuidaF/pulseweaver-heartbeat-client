package com.pulseweaver.heartbeat

import android.app.Application
import com.pulseweaver.heartbeat.ApplicationContextHolder

class PulseWeaverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.init(this)
    }
}
