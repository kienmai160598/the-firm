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

// Two variants: debug (tests, DebugLaunch extras) and box (R8, what users get). The default
// release build type has no DebugLaunch source set and no use, so it is switched off.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant -> variant.enable = false }
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
    // Media3 fork: ExoPlayer, common, datasource and extractor come from the bridge (`api`);
    // lib-ui (SubtitleView, AspectRatioFrameLayout) is linked once, here, from core/libs. A local
    // AAR carries no POM, so its runtime dependency (recyclerview) is declared by hand.
    implementation(files(rootProject.file("core/libs/lib-ui-release.aar")))
    implementation(libs.androidx.recyclerview)
    // Stock media3-decoder at the fork's version: the FFmpeg renderer's supertypes live there.
    implementation(libs.androidx.media3.decoder)

    implementation(platform(libs.jetpack.compose.bom))
    implementation(libs.jetpack.compose.ui)
    implementation(libs.jetpack.compose.foundation)
    implementation(libs.jetpack.tv.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.jetpack.lifecycle.runtimeCompose)
    implementation(libs.jetpack.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jetpack.profileinstaller)

    // Images: Coil 3 with the OkHttp fetcher (without a fetcher AsyncImage loads nothing).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Add addons from a phone (docs/android-tv-rebuild.md, 5b): local server and QR matrix.
    implementation(libs.nanohttpd)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.androidx.testRunner)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(platform(libs.jetpack.compose.bom))
    androidTestImplementation(libs.jetpack.compose.ui.test.junit4)
    debugImplementation(libs.jetpack.compose.ui.test.manifest)
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
