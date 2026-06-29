package com.pulseweaver.heartbeat.service

/**
 * Result of validating a pairing code locally, before any network call.
 */
sealed interface PairingCodeCheck {
    /** The code is well-formed; [serverUrl] is the address it points at. */
    data class Valid(
        val serverUrl: String,
    ) : PairingCodeCheck

    /** The code is malformed; [reason] explains why. */
    data class Invalid(
        val reason: PairingError,
    ) : PairingCodeCheck
}

private val nonPairingCodeChars = Regex("[^A-Za-z0-9_-]")

/**
 * Strips everything that cannot be part of a pairing code.
 *
 * Codes are base64url (no padding), so the only legitimate characters are
 * `A-Z a-z 0-9 _ -`. Whitespace, line breaks, and `=` padding are common
 * copy/paste and QR artifacts and are removed. This is safe to run on every
 * keystroke: a user cannot type a legitimate character that gets dropped.
 */
fun sanitizePairingCode(raw: String): String = raw.replace(nonPairingCodeChars, "")

/**
 * Validates a (already-sanitized) pairing code without touching the network:
 * it must decode as base64url, carry at least the 33-byte minimum payload, and
 * embed a usable `http`/`https` server URL.
 *
 * Distinguishes [PairingError.FORMAT] (undecodable / too short) from
 * [PairingError.URL] (decoded, but the embedded server address is unusable) so
 * the administrator can tell a garbled paste from a server-side generation
 * problem.
 */
fun validatePairingCode(code: String): PairingCodeCheck {
    val serverUrl =
        try {
            RegistrationClient.decodeServerURL(code)
        } catch (e: IllegalArgumentException) {
            return PairingCodeCheck.Invalid(PairingError.FORMAT)
        }
    if (!isHttpUrl(serverUrl)) {
        return PairingCodeCheck.Invalid(PairingError.URL)
    }
    return PairingCodeCheck.Valid(serverUrl)
}

/**
 * The host portion of a server URL, for showing the user which server a code
 * points at (e.g. `https://pulse.example.com/x` -> `pulse.example.com`).
 * Returns the input unchanged if it has no recognizable scheme.
 */
fun serverHost(serverUrl: String): String =
    serverUrl
        .substringAfter("://", serverUrl)
        .substringBefore('/')
        .substringBefore('?')

private fun isHttpUrl(url: String): Boolean {
    val rest =
        when {
            url.startsWith("https://") -> url.removePrefix("https://")
            url.startsWith("http://") -> url.removePrefix("http://")
            else -> return false
        }
    val host = rest.substringBefore('/').substringBefore('?').substringBefore(':')
    return host.isNotBlank()
}
