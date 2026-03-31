package com.pulseweaver.heartbeat

import androidx.compose.ui.window.ComposeUIViewController
import com.pulseweaver.heartbeat.platform.BackgroundScheduler

fun MainViewController() = ComposeUIViewController {
    App(scheduler = BackgroundScheduler())
}
