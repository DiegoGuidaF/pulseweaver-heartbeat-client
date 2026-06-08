package com.pulseweaver.heartbeat.platform

import android.content.Intent
import android.net.Uri
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual object UrlOpener {
    actual fun open(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        ApplicationContextHolder.context.startActivity(intent)
    }
}
