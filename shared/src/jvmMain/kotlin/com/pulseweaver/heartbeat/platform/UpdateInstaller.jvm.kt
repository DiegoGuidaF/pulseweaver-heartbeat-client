package com.pulseweaver.heartbeat.platform

/**
 * Desktop installs nothing in-app; the UI gates on [isAvailable] and opens the release page.
 * [install] is unreachable, and returns a failure rather than throwing so a future caller that
 * forgets the gate degrades instead of crashing.
 */
actual object UpdateInstaller {
    actual fun isAvailable(): Boolean = false

    actual val assetSuffix: String? = null

    actual suspend fun install(downloadUrl: String): InstallOutcome = InstallOutcome.FAILED
}
