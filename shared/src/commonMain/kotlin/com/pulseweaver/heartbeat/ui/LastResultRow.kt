package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.service.HeartbeatResult

@Composable
internal fun LastResultRow(
    modifier: Modifier = Modifier,
    lastResult: HeartbeatResult,
    lastResultTime: String,
    elapsedDisplay: String,
    nextInDisplay: String,
) {
    val resultColor =
        when {
            lastResult.success -> AppColors.Amber
            lastResult.message.contains("limited", ignoreCase = true) -> AppColors.WarningYellow
            else -> AppColors.ErrorRed
        }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Last response",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = lastResult.message,
            style = MaterialTheme.typography.bodyMedium,
            color = resultColor,
        )
        if (lastResultTime.isNotEmpty()) {
            val elapsedSuffix = if (elapsedDisplay.isNotEmpty()) " ($elapsedDisplay)" else ""
            Text(
                text = "at $lastResultTime$elapsedSuffix  ·  trigger: ${lastResult.trigger}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (lastResult.hint != null) {
            Text(
                text = lastResult.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
