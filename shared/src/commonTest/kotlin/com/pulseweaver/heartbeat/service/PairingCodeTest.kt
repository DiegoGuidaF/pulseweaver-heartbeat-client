package com.pulseweaver.heartbeat.service

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PairingCodeTest {
    @OptIn(ExperimentalEncodingApi::class)
    private fun buildCode(serverUrl: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
            ByteArray(32) { it.toByte() } + serverUrl.encodeToByteArray(),
        )

    // ── sanitizePairingCode ─────────────────────────────────────────

    @Test
    fun sanitize_stripsWhitespaceAndLineBreaks() {
        assertEquals("AbC0-_9", sanitizePairingCode("  Ab C0\n-_\t9 "))
    }

    @Test
    fun sanitize_stripsBase64PaddingAndStrayPunctuation() {
        assertEquals("AbCd", sanitizePairingCode("Ab=Cd=="))
        assertEquals("AbCd", sanitizePairingCode("Ab.Cd!"))
    }

    @Test
    fun sanitize_keepsFullBase64UrlAlphabet() {
        val intact = "ABCabc012789-_"
        assertEquals(intact, sanitizePairingCode(intact))
    }

    // ── validatePairingCode ─────────────────────────────────────────

    @Test
    fun validate_validCode_isValidWithServerUrl() {
        val check = validatePairingCode(buildCode("https://pulse.example.com"))

        assertIs<PairingCodeCheck.Valid>(check)
        assertEquals("https://pulse.example.com", check.serverUrl)
    }

    @Test
    fun validate_tooShortPayload_isFormatError() {
        @OptIn(ExperimentalEncodingApi::class)
        val shortCode = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(ByteArray(10))

        val check = validatePairingCode(shortCode)

        assertIs<PairingCodeCheck.Invalid>(check)
        assertEquals(PairingError.FORMAT, check.reason)
    }

    @Test
    fun validate_undecodable_isFormatError() {
        // '!' is outside the base64url alphabet, so decoding fails.
        val check = validatePairingCode("not!a!code")

        assertIs<PairingCodeCheck.Invalid>(check)
        assertEquals(PairingError.FORMAT, check.reason)
    }

    @Test
    fun validate_decodesButNonHttpUrl_isUrlError() {
        val check = validatePairingCode(buildCode("ftp://pulse.example.com"))

        assertIs<PairingCodeCheck.Invalid>(check)
        assertEquals(PairingError.URL, check.reason)
    }

    @Test
    fun validate_decodesButEmptyHost_isUrlError() {
        val check = validatePairingCode(buildCode("https:///no-host"))

        assertIs<PairingCodeCheck.Invalid>(check)
        assertEquals(PairingError.URL, check.reason)
    }

    // ── serverHost ──────────────────────────────────────────────────

    @Test
    fun serverHost_extractsHostFromUrl() {
        assertEquals("pulse.example.com", serverHost("https://pulse.example.com/api/v1"))
        assertEquals("pulse.example.com", serverHost("http://pulse.example.com?x=1"))
    }
}
