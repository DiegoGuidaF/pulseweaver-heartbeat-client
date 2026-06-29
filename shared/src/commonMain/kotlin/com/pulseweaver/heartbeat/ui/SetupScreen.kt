package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pulseweaver.heartbeat.platform.Clipboard
import com.pulseweaver.heartbeat.platform.QrScanner
import com.pulseweaver.heartbeat.service.PairingCodeCheck
import com.pulseweaver.heartbeat.service.RegistrationClient
import com.pulseweaver.heartbeat.service.RegistrationResult
import com.pulseweaver.heartbeat.service.sanitizePairingCode
import com.pulseweaver.heartbeat.service.serverHost
import com.pulseweaver.heartbeat.service.validatePairingCode
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onProvisioningComplete: (HeartbeatConfig) -> Unit,
    onManualSetup: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<RegistrationResult.Error?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val registrationClient = remember { RegistrationClient() }
    val configStore = remember { ConfigStore() }
    val canScanQr = remember { QrScanner.isAvailable() }
    val canPaste = remember { Clipboard.isAvailable() }

    // Local, network-free check used only to reassure the user which server a
    // recognized code points at; the actual gating happens inside claim().
    val recognizedHost =
        remember(code) {
            (validatePairingCode(code) as? PairingCodeCheck.Valid)?.let { serverHost(it.serverUrl) }
        }

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
        BrandMark(size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColors.Amber, fontWeight = FontWeight.Bold)) {
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
                code = sanitizePairingCode(it)
                error = null
            },
            placeholder = { Text("Paste code here") },
            singleLine = true,
            isError = error != null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.REGISTRATION_CODE_FIELD),
        )

        if (recognizedHost != null && error == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ready to pair with $recognizedHost",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.SETUP_HOST_CONFIRM),
            )
        }

        if (canPaste) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val pasted = Clipboard.readText()
                        if (pasted != null) {
                            code = sanitizePairingCode(pasted)
                            error = null
                        }
                    }
                },
                enabled = !isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.PASTE_CODE_BUTTON),
            ) {
                Text("Paste from clipboard")
            }
        }

        if (canScanQr) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val scanned = QrScanner.scan()
                        if (scanned != null) {
                            code = sanitizePairingCode(scanned)
                            error = null
                        }
                    }
                },
                enabled = !isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.SCAN_QR_BUTTON),
            ) {
                Text("Scan QR code")
            }
        }

        error?.let { failure ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = failure.reason.userMessage + (failure.reason.userAction?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TestTags.SETUP_ERROR_TEXT),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = failure.reason.code + (failure.detail?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.SETUP_ERROR_DETAIL),
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    error = null
                    when (val result = registrationClient.claim(code)) {
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
                            error = result
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
