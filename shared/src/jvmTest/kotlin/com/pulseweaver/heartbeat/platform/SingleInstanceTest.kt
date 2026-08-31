package com.pulseweaver.heartbeat.platform

import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleInstanceTest {
    @Test
    fun lockFileName_scopesTheLockToTheChannel() {
        // A dev install and a release install have to run side by side, so the channel
        // suffix separates their locks exactly as it separates config, logs and login items.
        assertTrue(lockFileName().startsWith("pulseweaver-companion${channelSuffix()}-"))
        assertTrue(lockFileName().endsWith(".lock"))
    }

    @Test
    fun lockFileName_staysASingleFileName() {
        // The name is joined onto the temp dir, so a separator coming in through the user
        // name would put the lock in some other directory — and stop guarding anything.
        val name = lockFileName()
        assertFalse(name.contains('/'))
        assertFalse(name.contains('\\'))
        assertFalse(name.contains(".."))
    }

    @Test
    fun lockFileName_sanitisesTheUserName() {
        val previous = System.getProperty("user.name")
        try {
            System.setProperty("user.name", "some/one else")
            assertContains(lockFileName(), "some_one_else")
        } finally {
            System.setProperty("user.name", previous)
        }
    }

    @Test
    fun acquire_takesTheLockAndStaysHeldForThisProcess() {
        assertTrue(SingleInstance.acquire())
        // The app asking twice is not the app meeting a duplicate of itself.
        assertTrue(SingleInstance.acquire())
        // A second lock on a region this JVM already holds is refused outright, which is
        // proof the first call took the lock rather than reporting success off a no-op.
        FileChannel
            .open(SingleInstance.lockFilePath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            .use { channel ->
                assertFailsWith<OverlappingFileLockException> { channel.tryLock() }
            }
    }
}
