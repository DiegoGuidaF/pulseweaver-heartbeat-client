package com.pulseweaver.heartbeat.service

/**
 * Stable, user- and admin-facing diagnostics for device-pairing failures.
 *
 * Diagnostic [code] format: `PWC-<AREA>-<CAUSE>` where `PWC` = PulseWeaver
 * Companion and the area for pairing is `PAIR`. The cause is a stable slug, not
 * a raw HTTP status — an end user can read the code back to their administrator
 * and the administrator can look it up regardless of the underlying transport.
 *
 * [userMessage] and [userAction] are plain-language and shown on screen.
 *
 * Keep this enum in sync with the "Pairing error codes" table in `README.md` —
 * the README is the administrator-facing copy of this same mapping.
 */
enum class PairingError(
    val code: String,
    val userMessage: String,
    val userAction: String? = null,
) {
    /** The code could not be decoded, or is too short to be a real code. */
    FORMAT(
        code = "PWC-PAIR-FORMAT",
        userMessage = "This code doesn't look right. Check you copied the whole thing.",
    ),

    /** The code decoded, but the server address baked into it is not a valid URL. */
    URL(
        code = "PWC-PAIR-URL",
        userMessage = "This code doesn't look right. Check you copied the whole thing.",
    ),

    /** The server rejected the code (HTTP 400). */
    REJECTED(
        code = "PWC-PAIR-REJECTED",
        userMessage = "The server rejected this code.",
        userAction = "Ask your administrator for a new code.",
    ),

    /** The code has expired or was already used (HTTP 404 / 410). */
    EXPIRED(
        code = "PWC-PAIR-EXPIRED",
        userMessage = "This code has expired or was already used.",
        userAction = "Ask your administrator for a new code.",
    ),

    /** The server reported an error (HTTP 5xx or another unexpected status). */
    SERVER(
        code = "PWC-PAIR-SERVER",
        userMessage = "The server had a problem. Try again shortly.",
        userAction = "If it keeps happening, contact your administrator.",
    ),

    /** The server could not be reached (network failure / timeout). */
    NETWORK(
        code = "PWC-PAIR-NETWORK",
        userMessage = "Couldn't reach the server. Check your internet connection.",
    ),
}
