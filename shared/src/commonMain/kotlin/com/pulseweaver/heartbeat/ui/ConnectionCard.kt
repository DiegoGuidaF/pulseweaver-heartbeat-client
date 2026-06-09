package com.pulseweaver.heartbeat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.config.HeartbeatConfig

@Composable
internal fun ConnectionCard(
    config: HeartbeatConfig,
    expanded: Boolean,
    isConfigValid: Boolean,
    showSaved: Boolean,
    isApiKeyVisible: Boolean,
    locked: Boolean,
    onExpandToggle: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiKeyVisibilityToggle: () -> Unit,
    onEnterSetupCode: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.CONNECTION_CARD),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Header row — always visible, tappable when configured
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (isConfigValid) Modifier.clickable { onExpandToggle() } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Connection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(visible = showSaved, enter = fadeIn(tween(200)), exit = fadeOut(tween(600))) {
                        Text("Saved ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "Enter a setup code",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onEnterSetupCode),
                    )
                    if (isConfigValid) {
                        Text(
                            text = if (expanded) "▲" else "▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Collapsed summary — URL only, no editing
            if (isConfigValid && !expanded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = config.serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Expanded fields
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text("Server URL") },
                        placeholder = { Text("https://server.example.com") },
                        singleLine = true,
                        enabled = !locked,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.SERVER_URL_FIELD),
                    )
                    if (!locked) {
                        OutlinedTextField(
                            value = config.apiKey,
                            onValueChange = onApiKeyChange,
                            label = { Text("API Key") },
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = onApiKeyVisibilityToggle) {
                                    Text(if (isApiKeyVisible) "Hide" else "Show")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.API_KEY_FIELD),
                        )
                    }
                }
            }
        }
    }
}
