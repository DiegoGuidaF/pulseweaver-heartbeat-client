import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.foundation)
}

val appVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"
val versionParts = appVersion.split(".")
// CI always passes -PappVersion; local dev builds don't. Use Int.MAX_VALUE for dev
// so a debug APK can always be installed on a device that has a release version.
val computedVersionCode = if (project.hasProperty("appVersion")) {
    (versionParts.getOrNull(0)?.toIntOrNull() ?: 1) * 10000 +
        (versionParts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 +
        (versionParts.getOrNull(2)?.toIntOrNull() ?: 0)
} else {
    Int.MAX_VALUE
}

android {
    namespace = "com.pulseweaver.heartbeat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pulseweaver.heartbeat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = computedVersionCode
        versionName = appVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        val ksFile = System.getenv("KEYSTORE_FILE")
        if (ksFile != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "pulseweaver"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
