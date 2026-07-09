package com.pulseweaver.heartbeat.platform

/**
 * The build channel baked into the installed desktop app via `-Dpw.channel`. Dev builds
 * (produced by the Dev Build workflow) carry "dev" so they install as a separate app and
 * keep their config, saved heartbeat state, and logs apart from a release install. A
 * release build — or a local `./gradlew shared:run` — has no channel and uses the plain paths.
 */
private val appChannel: String? = System.getProperty("pw.channel")?.takeIf { it.isNotBlank() }

/** Suffix appended to per-channel storage locations, e.g. `-dev`; empty on the release channel. */
internal fun channelSuffix(): String = appChannel?.let { "-$it" } ?: ""
