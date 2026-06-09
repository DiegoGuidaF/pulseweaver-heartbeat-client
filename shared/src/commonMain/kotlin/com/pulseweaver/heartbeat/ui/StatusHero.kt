package com.pulseweaver.heartbeat.ui

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.service.HeartbeatResult

@Composable
internal fun StatusHero(
    enabled: Boolean,
    lastResult: HeartbeatResult?,
    lastResultTime: String,
    elapsedDisplay: String,
    nextInDisplay: String,
    isSending: Boolean = false,
    isConfigValid: Boolean = false,
    onTap: () -> Unit = {},
) {
    val statusColor =
        when {
            !enabled -> AppColors.StoppedGrey
            lastResult?.success == false -> AppColors.ErrorRed
            else -> AppColors.Amber
        }

    // Pulsing ripple — only computed and running when active and healthy
    val showPulse = enabled && lastResult?.success != false
    val pulseScale: Float
    val pulseAlpha: Float
    if (showPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        pulseScale =
            infiniteTransition
                .animateFloat(
                    initialValue = 1f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(tween(2400, easing = EaseOut), RepeatMode.Restart),
                    label = "pulseScale",
                ).value
        pulseAlpha =
            infiniteTransition
                .animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
                    label = "pulseAlpha",
                ).value
    } else {
        pulseScale = 1f
        pulseAlpha = 0f
    }

    val canTap = isConfigValid && !isSending

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag(TestTags.STATUS_HERO),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable(enabled = canTap) { onTap() },
        ) {
            // Ripple ring
            if (showPulse) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .background(statusColor.copy(alpha = pulseAlpha), CircleShape),
                )
            }
            // Status circle
            Surface(
                shape = CircleShape,
                color = statusColor,
                shadowElevation = 4.dp,
                modifier = Modifier.size(72.dp),
            ) {
                if (isSending) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Text(
            text =
                when {
                    isSending -> "Sending…"
                    !enabled -> "Stopped"
                    lastResult?.success == false -> "Error"
                    else -> "Active"
                },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            modifier = Modifier.testTag(TestTags.STATUS_LABEL),
        )

        if (isConfigValid) {
            Text(
                text = "Tap to send now",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (enabled && lastResult?.ip != null) {
            Text(
                text = "IP: ${lastResult.ip}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (lastResultTime.isNotEmpty()) {
            val elapsedSuffix = if (elapsedDisplay.isNotEmpty()) " ($elapsedDisplay)" else ""
            Text(
                text =
                    if (nextInDisplay.isNotEmpty()) {
                        "Next in $nextInDisplay · Last $lastResultTime$elapsedSuffix"
                    } else {
                        "Last sent $lastResultTime$elapsedSuffix"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
