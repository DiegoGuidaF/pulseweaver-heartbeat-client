package com.pulseweaver.heartbeat.platform

// Android relaunches via BootReceiver/WorkManager; there is no login item to manage.
actual object AutoStart {
    actual fun isAvailable(): Boolean = false

    actual fun isEnabled(): Boolean = false

    actual fun setEnabled(enabled: Boolean): Boolean = false
}
