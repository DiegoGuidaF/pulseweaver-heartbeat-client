package com.pulseweaver.heartbeat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.service.HeartbeatResult

private val ActiveAmber = Color(0xFFFFA94D)
private val StoppedGrey = Color(0xFF9E9E9E)
private val ErrorRed = Color(0xFFFA5252)

fun main() =
    application {
        val appScope = rememberCoroutineScope()
        val scheduler = remember { BackgroundScheduler(appScope) }
        var lastResult by remember { mutableStateOf<HeartbeatResult?>(null) }
        var isWindowVisible by remember { mutableStateOf(true) }
        var sendNowTrigger by remember { mutableStateOf(0) }

        val trayColor =
            when {
                lastResult?.success == false -> ErrorRed
                lastResult?.success == true -> ActiveAmber
                else -> StoppedGrey
            }
        val trayTooltip =
            when {
                lastResult?.success == false -> "PulseWeaver — Error: ${lastResult?.message}"
                lastResult?.success == true -> "PulseWeaver — Active · IP: ${lastResult?.ip ?: "unknown"}"
                else -> "PulseWeaver — Stopped"
            }

        Tray(
            icon = remember(trayColor) { trayIcon(trayColor) },
            tooltip = trayTooltip,
            menu = {
                Item(
                    text = lastResult?.let { if (it.success) "✓ ${it.message}" else "✗ ${it.message}" } ?: "Idle",
                    enabled = false,
                    onClick = {},
                )
                Separator()
                Item("Send Now", onClick = { sendNowTrigger++ })
                Item("Show Window", onClick = { isWindowVisible = true })
                Separator()
                Item("Quit", onClick = { exitApplication() })
            },
        )

        if (isWindowVisible) {
            Window(
                onCloseRequest = { isWindowVisible = false },
                title = "PulseWeaver Heartbeat",
                state = rememberWindowState(size = DpSize(460.dp, 720.dp)),
            ) {
                App(
                    scheduler = scheduler,
                    onLastResultChange = { lastResult = it },
                    sendNowTrigger = sendNowTrigger,
                )
            }
        }
    }

/** Draws a filled circle in the given colour as the system tray icon. */
private fun trayIcon(color: Color): BitmapPainter {
    val size = 32
    val bitmap = ImageBitmap(size, size)
    val canvas =
        androidx.compose.ui.graphics
            .Canvas(bitmap)
    val paint =
        androidx.compose.ui.graphics.Paint().apply {
            this.color = color
            isAntiAlias = true
        }
    canvas.drawCircle(
        center =
            androidx.compose.ui.geometry
                .Offset(size / 2f, size / 2f),
        radius = size / 2f - 1f,
        paint = paint,
    )
    return BitmapPainter(bitmap)
}
