import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Signing values and ABIs come from shared/local.properties only (not in git).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(key: String): String? = localProperties.getProperty(key)?.trim()?.takeIf(String::isNotEmpty)

val boxAbis: List<String> = localProperty("ABIS").orEmpty()
    .split(",").map(String::trim).filter(String::isNotEmpty)
    .ifEmpty { listOf("armeabi-v7a") }
val releaseStoreFile: String? = localProperty("NUVIO_RELEASE_STORE_FILE")

android {
    namespace = "com.kien.movies"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        applicationId = "com.kien.movies.next"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("box") {
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = localProperty("NUVIO_RELEASE_STORE_PASSWORD")
                keyAlias = localProperty("NUVIO_RELEASE_KEY_ALIAS")
                keyPassword = localProperty("NUVIO_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Same key as box, so the instrumented tests can run against an installed box build
            // without uninstalling it (which would wipe its data).
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("box")
        }
        create("box") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("box")
            ndk { abiFilters += boxAbis }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // The box is 32-bit only; extractNativeLibs keeps the APK installable on API 24.
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":androidTvShared"))
    // The downmix FFmpeg audio decoder; core's own FFmpeg AAR is not linked by the bridge.
    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))

    implementation(platform(libs.jetpack.compose.bom))
    implementation(libs.jetpack.compose.ui)
    implementation(libs.jetpack.compose.foundation)
    implementation(libs.jetpack.tv.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.jetpack.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jetpack.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testRunner)
    androidTestImplementation(libs.androidx.testExt.junit)
}

configurations.all {
    listOf(
        "media3-exoplayer",
        "media3-exoplayer-hls",
        "media3-ui",
        "media3-common",
        "media3-datasource",
        "media3-datasource-okhttp",
        "media3-extractor"
    ).forEach { module -> exclude(group = "androidx.media3", module = module) }
}
