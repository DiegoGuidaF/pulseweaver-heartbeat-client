package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.HeartbeatConfig
import com.pulseweaver.heartbeat.service.RegistrationClient
import com.pulseweaver.heartbeat.service.RegistrationResult
import kotlinx.coroutines.launch

private val Amber = Color(0xFFFFA94D)

@Composable
fun SetupScreen(
    onProvisioningComplete: (HeartbeatConfig) -> Unit,
    onManualSetup: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val registrationClient = remember { RegistrationClient() }
    val configStore = remember { ConfigStore() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .testTag(TestTags.SETUP_SCREEN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Branding
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(Amber, CircleShape),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold)) {
                        append("Pulse")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("Weaver")
                    }
                },
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Set up your device",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Paste the registration code from your administrator",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it
                errorMessage = ""
            },
            placeholder = { Text("Paste code here") },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.REGISTRATION_CODE_FIELD),
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TestTags.SETUP_ERROR_TEXT),
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = ""
                    when (val result = registrationClient.claim(code.trim())) {
                        is RegistrationResult.Success -> {
                            val r = result.response
                            val config =
                                HeartbeatConfig(
                                    serverUrl = r.serverUrl,
                                    apiKey = r.apiKey,
                                    intervalSeconds = r.intervalSeconds,
                                    enabled = true,
                                    biometricEnabled = r.appBiometricEnabled,
                                    settingsLocked = r.appSettingsLocked,
                                )
                            configStore.save(config)
                            onProvisioningComplete(config)
                        }
                        is RegistrationResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                    isLoading = false
                }
            },
            enabled = code.isNotBlank() && !isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.ACTIVATE_BUTTON),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text("Activate")
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(
            onClick = onManualSetup,
            modifier = Modifier.testTag(TestTags.MANUAL_SETUP_LINK),
        ) {
            Text(
                text = "Configure manually",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
