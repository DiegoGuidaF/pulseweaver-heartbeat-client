package com.pulseweaver.heartbeat

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.platform.Log
import com.pulseweaver.heartbeat.service.HeartbeatResult
import org.jetbrains.compose.resources.painterResource
import pulseweaverheartbeat.shared.generated.resources.Res
import pulseweaverheartbeat.shared.generated.resources.app_icon

private val ActiveAmber = Color(0xFFFFA94D)
private val StoppedGrey = Color(0xFF9E9E9E)
private val ErrorRed = Color(0xFFFA5252)

fun main() =
    application {
        val scheduler = remember { BackgroundScheduler() }
        var lastResult by remember { mutableStateOf<HeartbeatResult?>(null) }
        var isWindowVisible by remember { mutableStateOf(true) }
        var sendNowTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            Log.i("App", "PulseWeaver Companion started on ${System.getProperty("os.name")}")
        }

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

        // Keep the window composed while hidden (visible = false) rather than removing it from
        // composition. Removing it would dispose HeartbeatScreen and cancel the heartbeat/network
        // monitor via its onDispose — so closing to the tray would silently stop heartbeating.
        Window(
            onCloseRequest = { isWindowVisible = false },
            visible = isWindowVisible,
            title = "PulseWeaver Companion",
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(size = DpSize(460.dp, 720.dp)),
        ) {
            App(
                scheduler = scheduler,
                onLastResultChange = { lastResult = it },
                sendNowTrigger = sendNowTrigger,
            )
        }
    }

/** The logo's bolt polygon, in the artwork's 64×64 viewBox coordinates. */
private val boltPoints =
    listOf(
        40f to 10f,
        14f to 34f,
        26f to 34f,
        24f to 52f,
        50f to 30f,
        34f to 30f,
    )

/**
 * Draws the PulseWeaver bolt tinted with the given status colour as the system tray icon.
 * A single-colour silhouette stays legible at the ~16 px the OS renders tray icons at,
 * where the full hexagon-mesh logo would turn to noise.
 */
private fun trayIcon(color: Color): BitmapPainter {
    val size = 32
    val bitmap = ImageBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint =
        Paint().apply {
            this.color = color
            isAntiAlias = true
        }
    val minX = boltPoints.minOf { it.first }
    val maxX = boltPoints.maxOf { it.first }
    val minY = boltPoints.minOf { it.second }
    val maxY = boltPoints.maxOf { it.second }
    val scale = (size - 2f) / maxOf(maxX - minX, maxY - minY)
    val offsetX = (size - (maxX - minX) * scale) / 2f
    val offsetY = (size - (maxY - minY) * scale) / 2f
    val path = Path()
    boltPoints.forEachIndexed { index, (x, y) ->
        val px = (x - minX) * scale + offsetX
        val py = (y - minY) * scale + offsetY
        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    canvas.drawPath(path, paint)
    return BitmapPainter(bitmap)
}
