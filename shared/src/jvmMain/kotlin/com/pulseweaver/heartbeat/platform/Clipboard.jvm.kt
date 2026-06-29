package com.pulseweaver.heartbeat.platform

import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

actual object Clipboard {
    actual fun isAvailable(): Boolean = !GraphicsEnvironment.isHeadless()

    actual suspend fun readText(): String? =
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                (clipboard.getData(DataFlavor.stringFlavor) as? String)?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
}
