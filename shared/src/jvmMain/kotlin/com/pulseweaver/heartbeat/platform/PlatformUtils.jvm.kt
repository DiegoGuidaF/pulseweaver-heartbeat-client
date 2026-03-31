package com.pulseweaver.heartbeat.platform

import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")

actual fun currentTimeForDisplay(): String = LocalTime.now().format(TIME_FMT)

actual val platformHasBackgroundLimit: Boolean = false
