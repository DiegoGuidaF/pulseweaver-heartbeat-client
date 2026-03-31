package com.pulseweaver.heartbeat.platform

/**
 * Opens a URL in the platform's default browser.
 *
 * Desktop: java.awt.Desktop.browse().
 * Android: Intent(ACTION_VIEW) (Stage 4).
 * iOS: UIApplication.openURL (Stage later).
 */
expect object UrlOpener {
    fun open(url: String)
}
