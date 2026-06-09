package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

// HexBolt geometry in the 64x64 space of the source mark (project resources mark-*.svg).
private val OuterHex = listOf(32f to 4f, 56.25f to 18f, 56.25f to 46f, 32f to 60f, 7.75f to 46f, 7.75f to 18f)
private val MiddleHex = listOf(32f to 14.6f, 47f to 23.3f, 47f to 40.7f, 32f to 49.4f, 17f to 40.7f, 17f to 23.3f)
private val InnerHex = listOf(32f to 23f, 39.8f to 27.5f, 39.8f to 36.5f, 32f to 41f, 24.2f to 36.5f, 24.2f to 27.5f)
private val Bolt = listOf(40f to 10f, 14f to 34f, 26f to 34f, 24f to 52f, 50f to 30f, 34f to 30f)

/**
 * The PulseWeaver HexBolt mark. Rings take the theme's onSurface colour so they read on
 * either a light or dark background; the bolt stays amber. Scales to [size].
 */
@Composable
fun BrandMark(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / 64f
        val strokeWidth = 1.4f * scale

        fun at(point: Pair<Float, Float>) = Offset(point.first * scale, point.second * scale)

        fun polygon(points: List<Pair<Float, Float>>) =
            Path().apply {
                val start = at(points.first())
                moveTo(start.x, start.y)
                points.drop(1).forEach { lineTo(at(it).x, at(it).y) }
                close()
            }

        drawPath(polygon(OuterHex), ringColor, style = Stroke(strokeWidth))
        drawPath(polygon(MiddleHex), ringColor.copy(alpha = 0.55f), style = Stroke(strokeWidth))
        drawPath(polygon(InnerHex), ringColor.copy(alpha = 0.35f), style = Stroke(strokeWidth))

        val center = at(32f to 32f)
        OuterHex.forEach { vertex ->
            drawLine(ringColor.copy(alpha = 0.55f), center, at(vertex), strokeWidth)
        }

        drawPath(polygon(Bolt), AppColors.Amber)
    }
}
