package com.pulseweaver.heartbeat.platform

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Keeps one Companion process per channel, per user.
 *
 * Registering start-at-login bootstraps a launch agent whose `RunAtLoad` fires immediately,
 * so the app has to be able to meet a copy of itself: without this, switching the toggle on
 * leaves a second process with its own tray icon and its own heartbeat timer.
 *
 * The lock is named per channel, so a dev install and a release install take different locks
 * and run side by side — the same split their config, logs and login items already use.
 */
object SingleInstance {
    // Held for the life of the process: closing the channel, or letting it be collected,
    // releases the lock with it.
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /**
     * True when this process owns the channel's lock, including when it already did. False
     * means another Companion is running on this channel and this one should exit.
     *
     * The lock is advisory but process-scoped: the OS drops it when the holder dies, so a
     * crash or a kill leaves nothing to clean up.
     */
    @Synchronized
    fun acquire(): Boolean {
        if (lock != null) return true
        return runCatching {
            val opened = FileChannel.open(lockFilePath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            val acquired =
                try {
                    opened.tryLock()
                } catch (_: OverlappingFileLockException) {
                    null
                }
            if (acquired == null) {
                opened.close()
                false
            } else {
                channel = opened
                lock = acquired
                true
            }
        }.getOrElse { error ->
            // A lock that cannot be taken at all must not stop the app from starting:
            // failing open costs a duplicate instance, failing closed costs the whole app.
            Log.w("SingleInstance", "could not take the instance lock — ${error.message}")
            true
        }
    }

    internal fun lockFilePath(): Path = Paths.get(System.getProperty("java.io.tmpdir"), lockFileName())
}

/**
 * Channel- and user-scoped lock file name. The channel keeps a dev install from locking out
 * a release one; the user name matters on Linux, where every account shares `/tmp`. Anything
 * outside the safe set is replaced rather than passed through — the name is joined onto the
 * temp dir, so a separator in it would put the lock somewhere else entirely.
 */
internal fun lockFileName(): String {
    val user = System.getProperty("user.name").orEmpty().replace(Regex("[^A-Za-z0-9_.-]"), "_")
    return "pulseweaver-companion${channelSuffix()}-$user.lock"
}
