package com.pulseweaver.heartbeat.platform

import android.content.ClipboardManager
import android.content.Context
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual object Clipboard {
    actual fun isAvailable(): Boolean = true

    actual suspend fun readText(): String? {
        val context = ApplicationContextHolder.context
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip
            .getItemAt(0)
            .coerceToText(context)
            ?.toString()
            ?.takeIf { it.isNotEmpty() }
    }
}
