package com.pulseweaver.heartbeat.platform

import android.content.Intent
import androidx.core.net.toUri
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual object UrlOpener {
    actual fun open(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        ApplicationContextHolder.context.startActivity(intent)
    }
}
