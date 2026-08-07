package com.pulseweaver.heartbeat.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.pulseweaver.heartbeat.ApplicationContextHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "UpdateInstall"

/** Session entry name; arbitrary, it only has to be stable within the session. */
private const val APK_ENTRY = "update"

actual object UpdateInstaller {
    actual fun isAvailable(): Boolean = true

    actual val assetSuffix: String? = "-android.apk"

    actual suspend fun install(downloadUrl: String): InstallOutcome {
        val context = ApplicationContextHolder.context

        // Sideloading became a per-app grant in API 26; below that it is one global setting
        // and there is nothing app-specific to ask for.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            openUnknownSourcesSettings(context)
            return InstallOutcome.NEEDS_PERMISSION
        }

        return withContext(Dispatchers.IO) { stageAndCommit(context, downloadUrl) }
    }
}

/**
 * Streams the APK straight from the network into the install session — it is never written to
 * app storage, so there is no temp file to clean up and no `FileProvider` to declare.
 */
private suspend fun stageAndCommit(
    context: Context,
    downloadUrl: String,
): InstallOutcome {
    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    var sessionId = -1
    val client = downloadClient()

    return try {
        sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            client.prepareGet(downloadUrl).execute { response ->
                check(response.status.value == 200) { "download returned ${response.status.value}" }
                session.openWrite(APK_ENTRY, 0, response.contentLength() ?: -1L).use { sink ->
                    response.bodyAsChannel().toInputStream().copyTo(sink)
                    session.fsync(sink)
                }
            }
            // Commit hands the staged APK to the system and returns immediately; the install
            // prompt arrives at UpdateInstallReceiver as STATUS_PENDING_USER_ACTION.
            session.commit(statusIntentSender(context, sessionId))
        }
        InstallOutcome.STARTED
    } catch (e: CancellationException) {
        abandon(installer, sessionId)
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "APK download or staging failed", e)
        abandon(installer, sessionId)
        InstallOutcome.FAILED
    } finally {
        client.close()
    }
}

/**
 * No whole-request timeout: an APK over slow mobile data legitimately takes minutes. The socket
 * timeout is what catches a genuinely stalled transfer.
 */
private fun downloadClient(): HttpClient =
    HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

private fun abandon(
    installer: PackageInstaller,
    sessionId: Int,
) {
    if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
}

private fun statusIntentSender(
    context: Context,
    sessionId: Int,
): IntentSender {
    val intent = Intent(context, UpdateInstallReceiver::class.java)
    // Mutable by necessity: the system fills in the status extras and the confirmation Intent.
    // An immutable PendingIntent would arrive empty and the prompt would never appear.
    val flags =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return PendingIntent.getBroadcast(context, sessionId, intent, flags).intentSender
}

private fun openUnknownSourcesSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Log.w(TAG, "no unknown-sources settings screen on this device", it) }
}
