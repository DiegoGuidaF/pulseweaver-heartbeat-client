package com.pulseweaver.heartbeat.platform

// Installing outside the App Store is not possible, so this stays a stub permanently —
// unlike the other iOS stubs, there is no later stage that fills it in.
actual object UpdateInstaller {
    actual fun isAvailable(): Boolean = false

    actual val assetSuffix: String? = null

    actual suspend fun install(downloadUrl: String): InstallOutcome = InstallOutcome.FAILED
}
