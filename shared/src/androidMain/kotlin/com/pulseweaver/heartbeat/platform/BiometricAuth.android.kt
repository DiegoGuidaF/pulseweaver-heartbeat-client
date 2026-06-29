package com.pulseweaver.heartbeat.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.pulseweaver.heartbeat.ActivityHolder
import com.pulseweaver.heartbeat.ApplicationContextHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object BiometricAuth {
    private val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    actual fun isAvailable(): Boolean {
        val bm = BiometricManager.from(ApplicationContextHolder.context)
        return bm.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String): Boolean {
        val activity = ActivityHolder.get() ?: return false

        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        // Single attempt failed — prompt stays open, don't resolve yet
                    }
                }

            val prompt = BiometricPrompt(activity, executor, callback)
            val info =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setSubtitle("Verify your identity to access PulseWeaver Companion")
                    .setAllowedAuthenticators(authenticators)
                    .build()

            cont.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info)
        }
    }
}
