package com.pulseweaver.heartbeat.platform

/** What [UpdateInstaller.install] managed to do. */
enum class InstallOutcome {
    /** The OS installer took over; the app has nothing left to do. */
    STARTED,

    /** Sideloading is not permitted yet. The user has been sent to the settings screen that grants it. */
    NEEDS_PERMISSION,

    /** Download or session failed. The caller should fall back to the release page. */
    FAILED,
}

/**
 * Installs a newer release in place, on the one platform where that is possible.
 *
 * Android: streams the APK into a `PackageInstaller` session and raises the system install
 * prompt. Signature continuity holds because release APKs are signed with the stable release
 * key (see the dev-builds feature doc).
 *
 * Desktop: unavailable, deliberately. A running app cannot overwrite the bundle it is
 * executing from, an MSI needs UAC and a `.deb` needs root, and the DMG is neither
 * Developer-ID signed nor notarized. Doing this properly needs a helper process that outlives
 * the app — Sparkle/Squirrel territory. The UI opens the release page with [UrlOpener] instead.
 *
 * iOS: unavailable — installing outside the App Store is not possible, and the release
 * pipeline builds no iOS artifact.
 */
expect object UpdateInstaller {
    fun isAvailable(): Boolean

    /**
     * Suffix identifying this platform's release asset, e.g. `-android.apk`; null where
     * nothing is installed in-app.
     *
     * A suffix rather than a full filename: the GitHub API already hands back every asset's
     * name and download URL, so matching the list survives a rename and still works against
     * older releases, where reconstructing `PulseWeaver-Companion-<tag>-android.apk` would not.
     */
    val assetSuffix: String?

    /** Never throws for "unsupported" — unavailable platforms return [InstallOutcome.FAILED]. */
    suspend fun install(downloadUrl: String): InstallOutcome
}
