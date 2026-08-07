package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.BuildInfo
import com.pulseweaver.heartbeat.platform.Log
import com.pulseweaver.heartbeat.platform.UpdateInstaller
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "UpdateCheck"

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/DiegoGuidaF/pulseweaver-heartbeat-client/releases/latest"

/** How long a check stays fresh. One call a day keeps GitHub's 60/hour anonymous limit irrelevant. */
const val UPDATE_CHECK_INTERVAL_MS: Long = 24 * 60 * 60 * 1000L

/** Outcome of one check against the GitHub releases API. */
sealed interface UpdateCheck {
    /** Running the newest release, or on a channel that has nothing to compare against. */
    data object UpToDate : UpdateCheck

    /** The request or the response failed; the app keeps working, the user can retry. */
    data object Failed : UpdateCheck

    /**
     * A newer release exists. [downloadUrl] is null when this platform installs nothing
     * itself *or* when the asset is not uploaded yet — see the publish race below.
     */
    data class Available(
        val version: String,
        val releaseUrl: String,
        val downloadUrl: String?,
    ) : UpdateCheck
}

/**
 * Checks GitHub for a newer companion release.
 *
 * Everything is injectable so tests can drive a build identity the test binary does not have:
 * a test JVM always reports the `local` channel, which is exactly the case that must issue no
 * request at all.
 */
class UpdateChecker(
    private val client: HttpClient = HeartbeatClient.defaultClient(),
    private val currentVersion: String = BuildInfo.version,
    channel: String = BuildInfo.channel,
    private val assetSuffix: String? = UpdateInstaller.assetSuffix,
) {
    /**
     * Only a release build has a version comparable to a release tag. A `dev` package is a
     * separate app versioned `1.0.<run number>` inside its own channel, and a `local` build
     * carries the placeholder `1.0.0` — comparing either against a real tag would be reading
     * an unrelated number line. The UI hides the whole feature when this is false.
     */
    val isSupported: Boolean = channel == "release"

    suspend fun check(): UpdateCheck {
        if (!isSupported) return UpdateCheck.UpToDate
        val current = parseVersion(currentVersion) ?: return UpdateCheck.UpToDate

        return try {
            val response = client.get(LATEST_RELEASE_URL) { accept(ContentType.Application.Json) }
            if (response.status.value != 200) {
                Log.w(TAG, "GitHub returned ${response.status.value}")
                return UpdateCheck.Failed
            }
            val release = response.body<GithubRelease>()
            val latest = parseVersion(release.tagName) ?: return UpdateCheck.UpToDate
            if (latest <= current) return UpdateCheck.UpToDate

            UpdateCheck.Available(
                version = release.tagName.trim().removePrefix("v"),
                releaseUrl = release.htmlUrl,
                // The release is published before its packages finish uploading — release.yml
                // creates it first and says so in the body ("artifacts ... within ~15 minutes").
                // During that window there is a newer version with nothing to download, so the
                // notice must fall back to the release page rather than offer a dead link.
                downloadUrl =
                    assetSuffix?.let { suffix ->
                        release.assets.firstOrNull { it.name.endsWith(suffix) }?.downloadUrl
                    },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "update check failed", e)
            UpdateCheck.Failed
        }
    }
}

/**
 * True when the last check is older than [UPDATE_CHECK_INTERVAL_MS].
 *
 * A negative elapsed time also passes: the stored stamp is wall-clock, so a device whose clock
 * moved backwards (manual change, NTP correction) would otherwise sit out the difference before
 * ever checking again.
 */
fun shouldCheckNow(
    nowMs: Long,
    lastCheckedAtMs: Long,
): Boolean {
    val elapsed = nowMs - lastCheckedAtMs
    return elapsed >= UPDATE_CHECK_INTERVAL_MS || elapsed < 0
}

/**
 * True when this release should still be surfaced after the user skipped [skippedVersion].
 * Skipping is per version, not permanent: skipping v1.5.0 hides only that release, and a
 * later v1.6.0 shows up again.
 */
fun UpdateCheck.Available.isNewerThanSkipped(skippedVersion: String): Boolean {
    val skipped = parseVersion(skippedVersion) ?: return true
    val found = parseVersion(version) ?: return true
    return found > skipped
}

/** A release version, compared numerically. */
internal data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
}

/**
 * Parses `v1.2.3` or `1.2.3` into something comparable. Tags carry a leading `v` while
 * [BuildInfo.version] does not (`_build.yml` strips it for `-PappVersion`), so both forms
 * have to land on the same value — and string comparison cannot be used at all, since
 * lexically `1.10.0` sorts below `1.9.0`.
 *
 * Any prerelease or build suffix is dropped; `/releases/latest` never returns a prerelease,
 * so this only has to avoid choking on one. Returns null for anything not three numbers.
 */
internal fun parseVersion(raw: String): Version? {
    val core =
        raw
            .trim()
            .removePrefix("v")
            .substringBefore('-')
            .substringBefore('+')
    val parts = core.split('.')
    if (parts.size != 3) return null
    val numbers = parts.map { it.toIntOrNull() ?: return null }
    return Version(numbers[0], numbers[1], numbers[2])
}

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
)
