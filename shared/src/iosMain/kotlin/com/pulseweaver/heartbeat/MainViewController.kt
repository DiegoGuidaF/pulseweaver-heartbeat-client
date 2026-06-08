package com.pulseweaver.heartbeat

import androidx.compose.ui.window.ComposeUIViewController
import com.pulseweaver.heartbeat.platform.BackgroundScheduler

fun mainViewController() =
    ComposeUIViewController {
        App(scheduler = BackgroundScheduler())
    }
