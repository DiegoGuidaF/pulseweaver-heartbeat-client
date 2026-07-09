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

val appVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"

// Dev builds ship as a separate app that installs alongside a release, with isolated
// config/state/logs (see -Dpw.channel below). Release builds leave everything untouched.
val isDevChannel = project.findProperty("appChannel") == "dev"

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

            macOS {
                bundleID = if (isDevChannel) "com.pulseweaver.companion.dev" else "com.pulseweaver.companion"

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
