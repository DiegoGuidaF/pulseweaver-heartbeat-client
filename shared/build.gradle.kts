import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ktlint)
}

val appVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"

// Dev builds ship as a separate app that installs alongside a release, with isolated
// config/state/logs (see -Dpw.channel below). Release builds leave everything untouched.
val isDevChannel = project.findProperty("appChannel") == "dev"

// A release is a versioned build on the default channel. Everything else — the dev-channel
// packages and a bare `gradlew :shared:run` — is a throwaway that a version number cannot
// tell apart, so those carry a commit instead.
val isReleaseBuild = project.hasProperty("appVersion") && !isDevChannel

val buildChannel =
    when {
        isReleaseBuild -> "release"
        isDevChannel -> "dev"
        else -> "local"
    }

// Resolving the commit only off the release path keeps release outputs reproducible and off
// the git call entirely, and the release UI shows a version number anyway. CI can pass
// -PappCommit to skip shelling out; the git fallback covers local builds. The `.git` guard
// keeps a build from a source archive (no repository) working.
val commitSha: Provider<String> =
    when {
        isReleaseBuild -> providers.provider { "" }
        project.hasProperty("appCommit") ->
            providers.provider { project.property("appCommit").toString().take(7) }
        rootProject.file(".git").exists() ->
            providers
                .exec {
                    workingDir = rootProject.projectDir
                    commandLine("git", "rev-parse", "--short=7", "HEAD")
                    isIgnoreExitValue = true
                }.standardOutput
                .asText
                .map { it.trim() }
        else -> providers.provider { "" }
    }

val buildInfoDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")

// The version is generated into commonMain because every place a build normally records it is
// unreachable from there: Android's BuildConfig, jpackage's packageVersion, iOS's Info.plist.
// `gradlew :shared:run` packages nothing at all, so on the dev inner loop it exists nowhere.
// One generated file is the only spot all three targets share.
val generateBuildInfo by tasks.registering {
    val versionValue = appVersion
    val channelValue = buildChannel
    val commitValue = commitSha
    val outputDir = buildInfoDir

    inputs.property("version", versionValue)
    inputs.property("channel", channelValue)
    inputs.property("commit", commitValue)
    outputs.dir(outputDir)

    doLast {
        val target = outputDir.get().file("com/pulseweaver/heartbeat/BuildInfo.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(
            """
            package com.pulseweaver.heartbeat

            /**
             * Build identity, stamped in by the `generateBuildInfo` Gradle task.
             *
             * Declared as `val`, never `const val`: a const is inlined into every call site, so a
             * commit that changes each build would recompile every consumer rather than this file.
             */
            object BuildInfo {
                /** Semantic version; "1.0.0" on a build given no -PappVersion. */
                val version: String = "$versionValue"

                /** "release", "dev" (the side-by-side dev channel), or "local" (an unversioned build). */
                val channel: String = "$channelValue"

                /** Short commit hash; empty on a release build, which its version already identifies. */
                val commit: String = "${commitValue.get()}"

                /** One-line build identity: "v1.2.3" on release, "v1.0.0-local · a1b2c3d" otherwise. */
                val display: String =
                    buildString {
                        append("v", version)
                        if (channel != "release") append("-", channel)
                        if (commit.isNotEmpty()) append(" · ", commit)
                    }
            }

            """.trimIndent(),
        )
    }
}

kotlin {
    // Suppress Beta warning for expect/actual classes — stable in Kotlin 2.x
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    android {
        namespace = "com.pulseweaver.heartbeat.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.gms.code.scanner)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
        }
        commonMain {
            kotlin.srcDir(generateBuildInfo)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Networking & serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.ktor.client.mock)
            implementation(libs.compose.ui.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.pulseweaver.heartbeat.MainKt"

        if (isDevChannel) {
            jvmArgs += "-Dpw.channel=dev"
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = if (isDevChannel) "PulseWeaver Companion Dev" else "PulseWeaver Companion"
            packageVersion = appVersion

            windows {
                menu = true
                shortcut = true
                menuGroup = "PulseWeaver"
                // Permanent per-channel MSI identity: the release GUID makes newer MSIs
                // upgrade in place, the dev GUID keeps the dev channel installing alongside.
                // Changing either turns every future MSI into a separate product.
                upgradeUuid =
                    if (isDevChannel) {
                        "DFC41FE5-5975-46C5-B86A-D81DAC327CC6"
                    } else {
                        "62351BA0-7280-4064-904B-CC9DA466A19E"
                    }
                iconFile.set(project.file("desktop-icons/pulseweaver.ico"))
            }

            linux {
                iconFile.set(project.file("desktop-icons/pulseweaver.png"))
            }

            macOS {
                bundleID = if (isDevChannel) "com.pulseweaver.companion.dev" else "com.pulseweaver.companion"
                iconFile.set(project.file("desktop-icons/pulseweaver.icns"))

                // The Companion's whole job is periodic network beats, so opt out of App Nap:
                // otherwise macOS suspends the process (and its timers) once its window is hidden
                // to the tray, silently stalling the heartbeat.
                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>NSAppSleepDisabled</key>
                        <true/>
                        """.trimIndent()
                }
            }
        }
    }
}

// Forward the doc-screenshot flags to the test JVM so `make screenshots`
// (-Dpw.screenshots / -Dpw.screenshotDir) reaches DocScreenshotsTest.
tasks.withType<Test>().configureEach {
    systemProperty("pw.screenshots", providers.systemProperty("pw.screenshots").getOrElse(""))
    systemProperty("pw.screenshotDir", providers.systemProperty("pw.screenshotDir").getOrElse(""))
}

ktlint {
    filter {
        exclude { it.file.path.contains("build/generated") }
    }
}
