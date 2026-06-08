package com.pulseweaver.heartbeat.platform

import java.util.Calendar

actual fun currentTimeForDisplay(): String {
    val cal = Calendar.getInstance()
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)
    return "%02d:%02d:%02d".format(h, m, s)
}

actual fun currentEpochMs(): Long = System.currentTimeMillis()

actual val platformHasBackgroundLimit: Boolean = true
