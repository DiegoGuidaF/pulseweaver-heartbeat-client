package com.pulseweaver.heartbeat.platform

// Stage later: implement with UIApplication.openURL.
actual object UrlOpener {
    actual fun open(url: String) = Unit
}
