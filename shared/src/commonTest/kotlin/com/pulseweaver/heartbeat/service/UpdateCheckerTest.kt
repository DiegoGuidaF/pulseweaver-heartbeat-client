package com.pulseweaver.heartbeat.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ANDROID_SUFFIX = "-android.apk"

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun releaseJson(
    tag: String,
    assetNames: List<String> = listOf("PulseWeaver-Companion-$tag$ANDROID_SUFFIX"),
): String {
    val assets =
        assetNames.joinToString(",") { name ->
            """{"name":"$name","browser_download_url":"https://example.test/$name"}"""
        }
    return """
        {"tag_name":"$tag","html_url":"https://example.test/releases/$tag","assets":[$assets]}
    """.trimIndent()
}

private fun checker(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    currentVersion: String = "1.2.3",
    channel: String = "release",
    assetSuffix: String? = ANDROID_SUFFIX,
): UpdateChecker {
    val engine = MockEngine { respond(body, status, jsonHeaders) }
    return UpdateChecker(
        client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
        currentVersion = currentVersion,
        channel = channel,
        assetSuffix = assetSuffix,
    )
}

class UpdateCheckerTest {
    @Test
    fun check_newerTag_reportsAvailableWithAsset() =
        runTest {
            val result = checker(releaseJson("v1.5.0")).check()

            val available = assertIs<UpdateCheck.Available>(result)
            assertEquals("1.5.0", available.version)
            assertEquals("https://example.test/releases/v1.5.0", available.releaseUrl)
            assertEquals(
                "https://example.test/PulseWeaver-Companion-v1.5.0-android.apk",
                available.downloadUrl,
            )
        }

    @Test
    fun check_sameVersion_reportsUpToDate() =
        runTest {
            assertEquals(UpdateCheck.UpToDate, checker(releaseJson("v1.2.3")).check())
        }

    @Test
    fun check_olderTag_reportsUpToDate() =
        runTest {
            assertEquals(UpdateCheck.UpToDate, checker(releaseJson("v1.0.0")).check())
        }

    @Test
    fun check_unparseableTag_reportsUpToDate() =
        runTest {
            assertEquals(UpdateCheck.UpToDate, checker(releaseJson("nightly")).check())
        }

    /**
     * release.yml publishes the release before its four package jobs upload, so a check landing
     * in that window sees a newer tag with no assets. The notice must still appear, without a
     * download link.
     */
    @Test
    fun check_assetsNotUploadedYet_reportsAvailableWithoutDownload() =
        runTest {
            val result = checker(releaseJson("v1.5.0", assetNames = emptyList())).check()

            val available = assertIs<UpdateCheck.Available>(result)
            assertEquals("1.5.0", available.version)
            assertNull(available.downloadUrl)
        }

    @Test
    fun check_noMatchingAssetForPlatform_reportsAvailableWithoutDownload() =
        runTest {
            val result =
                checker(
                    releaseJson("v1.5.0", assetNames = listOf("PulseWeaver-Companion-v1.5.0-windows-x64.msi")),
                ).check()

            assertNull(assertIs<UpdateCheck.Available>(result).downloadUrl)
        }

    /** Desktop passes a null suffix — it opens the release page rather than downloading. */
    @Test
    fun check_platformInstallsNothing_reportsAvailableWithoutDownload() =
        runTest {
            val result = checker(releaseJson("v1.5.0"), assetSuffix = null).check()

            assertNull(assertIs<UpdateCheck.Available>(result).downloadUrl)
        }

    @Test
    fun check_errorStatus_reportsFailed() =
        runTest {
            assertEquals(
                UpdateCheck.Failed,
                checker("""{"message":"rate limited"}""", HttpStatusCode.Forbidden).check(),
            )
        }

    @Test
    fun check_networkFailure_reportsFailed() =
        runTest {
            val engine = MockEngine { throw RuntimeException("No route to host") }
            val checker =
                UpdateChecker(
                    client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
                    currentVersion = "1.2.3",
                    channel = "release",
                    assetSuffix = ANDROID_SUFFIX,
                )

            assertEquals(UpdateCheck.Failed, checker.check())
        }

    @Test
    fun check_devChannel_issuesNoRequest() =
        runTest {
            var requests = 0
            val engine =
                MockEngine {
                    requests++
                    respond(releaseJson("v9.9.9"), HttpStatusCode.OK, jsonHeaders)
                }
            val checker =
                UpdateChecker(
                    client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
                    currentVersion = "1.0.57",
                    channel = "dev",
                    assetSuffix = ANDROID_SUFFIX,
                )

            assertEquals(UpdateCheck.UpToDate, checker.check())
            assertEquals(0, requests)
        }

    @Test
    fun check_localChannel_issuesNoRequest() =
        runTest {
            var requests = 0
            val engine =
                MockEngine {
                    requests++
                    respond(releaseJson("v9.9.9"), HttpStatusCode.OK, jsonHeaders)
                }
            val checker =
                UpdateChecker(
                    client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
                    currentVersion = "1.0.0",
                    channel = "local",
                    assetSuffix = ANDROID_SUFFIX,
                )

            assertEquals(UpdateCheck.UpToDate, checker.check())
            assertEquals(0, requests)
        }
}

class VersionParsingTest {
    @Test
    fun parseVersion_acceptsBothTagAndBuildInfoForms() {
        assertEquals(parseVersion("1.2.3"), parseVersion("v1.2.3"))
    }

    /** The reason string comparison cannot be used: lexically "1.10.0" sorts below "1.9.0". */
    @Test
    fun parseVersion_comparesNumericallyNotLexically() {
        assertTrue(parseVersion("v1.10.0")!! > parseVersion("v1.9.0")!!)
        assertTrue("1.10.0" < "1.9.0")
    }

    @Test
    fun parseVersion_dropsPrereleaseAndBuildSuffixes() {
        assertEquals(parseVersion("1.2.3"), parseVersion("v1.2.3-rc1"))
        assertEquals(parseVersion("1.2.3"), parseVersion("v1.2.3+build7"))
    }

    @Test
    fun parseVersion_rejectsNonNumericAndWrongLength() {
        assertNull(parseVersion("nightly"))
        assertNull(parseVersion("v1.2"))
        assertNull(parseVersion("v1.2.3.4"))
        assertNull(parseVersion("va.b.c"))
        assertNull(parseVersion(""))
    }
}

class UpdatePolicyTest {
    private val day = UPDATE_CHECK_INTERVAL_MS

    @Test
    fun shouldCheckNow_neverChecked_isTrue() {
        assertTrue(shouldCheckNow(nowMs = day, lastCheckedAtMs = 0L))
    }

    @Test
    fun shouldCheckNow_withinTheDay_isFalse() {
        assertTrue(!shouldCheckNow(nowMs = 5_000L + day, lastCheckedAtMs = day))
    }

    @Test
    fun shouldCheckNow_afterTheDay_isTrue() {
        assertTrue(shouldCheckNow(nowMs = day * 2, lastCheckedAtMs = day))
    }

    /** A clock moved backwards must not strand the check until wall time catches up. */
    @Test
    fun shouldCheckNow_clockMovedBackwards_isTrue() {
        assertTrue(shouldCheckNow(nowMs = day, lastCheckedAtMs = day * 10))
    }

    private fun available(version: String) =
        UpdateCheck.Available(version = version, releaseUrl = "https://example.test", downloadUrl = null)

    @Test
    fun isNewerThanSkipped_sameVersion_isFalse() {
        assertTrue(!available("1.5.0").isNewerThanSkipped("1.5.0"))
    }

    @Test
    fun isNewerThanSkipped_laterVersion_isTrue() {
        assertTrue(available("1.6.0").isNewerThanSkipped("1.5.0"))
    }

    @Test
    fun isNewerThanSkipped_nothingSkipped_isTrue() {
        assertTrue(available("1.5.0").isNewerThanSkipped(""))
    }
}
