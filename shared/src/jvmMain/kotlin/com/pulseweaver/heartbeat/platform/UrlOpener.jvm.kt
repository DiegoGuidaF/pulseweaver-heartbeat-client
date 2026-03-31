package com.pulseweaver.heartbeat.platform

import java.awt.Desktop
import java.net.URI

actual object UrlOpener {
    actual fun open(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            }
        }
    }
}
