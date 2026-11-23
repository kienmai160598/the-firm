import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// :androidTvShared compiles shared/core's logic sources into its own module, like the tvOS
// bridge (apple-tv-bridge/build.gradle.kts), so internal core objects stay reachable and the
// phone UI, Compose Multiplatform and core's resources stay out of the Android TV app.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

val upstreamSrc = rootProject.file("core/src")
val upstreamLibs = rootProject.file("core/libs")
val upstreamRuntimeConfig = rootProject.file("core/build/generated/runtime-config/kotlin")
val tvApiSrc = rootProject.file("tv-api/src")
val tvosBridge = rootProject.file("apple-tv-bridge")

// The English Res.string stand-in and its getString/stringResource functions; the real Compose
// resources library must never be on this module's classpath. TvosColor.kt is not used: Android
// has the real androidx.compose.ui.graphics.Color.
val stringStandIn = tvosBridge.resolve("src/commonMain/kotlin/org/jetbrains/compose/resources")

// Media3 fork, torrent engine and libass from core/libs. The media3 AARs are `api`, so the app's
// player compiles against the same single copy. Not linked here: core's FFmpeg decoder (the app
// links the downmix build from app/libs), lib-ui (the app links it for SubtitleView) and QuickJS
// (plugins are off in the playstore sources).
val upstreamMedia3Aars = listOf(
    "lib-common-release.aar",
    "lib-datasource-release.aar",
    "lib-datasource-okhttp-release.aar",
    "lib-decoder-av1-release.aar",
    "lib-decoder-mpegh-release.aar",
    "lib-exoplayer-release.aar",
    "lib-exoplayer-hls-release.aar",
    "lib-extractor-release.aar"
)
val upstreamOtherAars = listOf(
    "lib-nuvio-engine-android-0.1.6.aar",
    "nuvio-libass-release.aar"
)

fun excludedKeys(): Set<String> {
    val tvosLists = listOf("excluded-by-rule.txt", "excluded-by-compiler.txt")
        .map(tvosBridge::resolve)
    val androidList = project.file("excluded-android.txt")
    return (tvosLists + androidList)
        .filter(File::exists)
        .flatMap(File::readLines)
        .map { line -> line.substringBefore('\t').trim() }
        .filter { key -> key.isNotEmpty() && !key.startsWith("#") }
        .toSet()
}

// The Android KMP plugin hands the Kotlin compiler whole source directories, so a source set
// exclude is ignored (checked: compileAndroidMain saw all 1,033 files). The included core files
// are therefore copied into build/upstream-src/<source set>/kotlin; edit them in shared/core.
// One Sync per source set, so each output folder holds only Kotlin (AGP looks for profiles and
// resources next to a Kotlin folder).
val upstreamSourceSets = listOf("commonMain", "androidMain", "androidPlaystore")
val excludedKeyFiles = listOf(
    tvosBridge.resolve("excluded-by-rule.txt"),
    tvosBridge.resolve("excluded-by-compiler.txt"),
    project.file("excluded-android.txt")
)
val syncUpstreamSources = upstreamSourceSets.associateWith { set ->
    tasks.register<Sync>("syncUpstream${set.replaceFirstChar(Char::uppercase)}") {
        val excluded = excludedKeys()
        from(upstreamSrc.resolve("$set/kotlin")) {
            include("**/*.kt")
            exclude { element -> "$set/kotlin/${element.relativePath.pathString}" in excluded }
        }
        inputs.files(excludedKeyFiles)
        into(layout.buildDirectory.dir("upstream-src/$set/kotlin"))
    }
}

fun upstreamSources(set: String) = syncUpstreamSources.getValue(set).map { task -> task.destinationDir }

abstract class GenerateEnglishResShim : DefaultTask() {
    @get:InputDirectory
    abstract val valuesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val xml = valuesDir.get().asFile.listFiles { file -> file.extension == "xml" }
            .orEmpty().sortedBy(File::getName).joinToString("\n", transform = File::readText)
        val strings = stringEntry.findAll(xml).map { match ->
            stringProperty(match.groupValues[1], match.groupValues[2])
        }
        val plurals = pluralEntry.findAll(xml).map { match ->
            pluralProperty(match.groupValues[1], match.groupValues[2])
        }
        val target = outputDir.get().asFile.resolve("TvosEnglishRes.kt")
        target.parentFile.mkdirs()
        target.writeText((listOf(header) + strings + plurals).joinToString("\n"))
    }

    private fun stringProperty(name: String, text: String): String = "val Res.string.$name: StringResource get() = StringResource(\"$name\", ${literal(text)})"

    private fun pluralProperty(name: String, body: String): String {
        val items = pluralItem.findAll(body).associate { it.groupValues[1] to it.groupValues[2] }
        val other = items["other"].orEmpty()
        val one = items["one"] ?: other
        return "val Res.plurals.$name: PluralStringResource get() = " +
            "PluralStringResource(\"$name\", ${literal(one)}, ${literal(other)})"
    }

    private fun literal(xmlText: String): String {
        val text = xmlText.trim()
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&apos;", "'").replace("&amp;", "&")
            .replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("$", "\\$").replace("\n", "\\n")
        return "\"$escaped\""
    }

    private companion object {
        val stringEntry = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val pluralEntry = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
        val pluralItem = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
        val header = listOf(
            "package nuvio.composeapp.generated.resources",
            "import org.jetbrains.compose.resources.PluralStringResource",
            "import org.jetbrains.compose.resources.StringResource",
            "object Res { object string\n object plurals }"
        ).joinToString("\n")
    }
}

val generateEnglishResShim = tasks.register<GenerateEnglishResShim>("generateEnglishResShim") {
    valuesDir.set(upstreamSrc.resolve("commonMain/composeResources/values"))
    outputDir.set(layout.buildDirectory.dir("generated/android-tv-res-shim/kotlin"))
}

kotlin {
    android {
        namespace = "com.nuvio.tvos.android"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        // Core's storages are expect/actual objects (Beta); same opt-in as core.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(upstreamSources("commonMain"))
            kotlin.srcDir(upstreamRuntimeConfig)
            kotlin.srcDir(generateEnglishResShim.map { task -> task.outputDir })
            kotlin.srcDir(stringStandIn)
            kotlin.srcDir(tvApiSrc.resolve("commonMain/kotlin"))
            dependencies {
                implementation(project.dependencies.platform(libs.jetpack.compose.bom))
                implementation(libs.jetpack.compose.runtime)
                implementation(libs.jetpack.compose.ui.graphics)
                implementation(libs.jetpack.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kermit)
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.auth)
                implementation(libs.supabase.functions)
                implementation(libs.supabase.storage)
                implementation(libs.supabase.realtime)
            }
        }
        androidMain {
            kotlin.srcDir(upstreamSources("androidMain"))
            kotlin.srcDir(upstreamSources("androidPlaystore"))
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.okhttp)
                implementation(libs.guava)
                // Breadcrumb calls only; without sentry-android nothing starts or reports.
                implementation(libs.sentry.core)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.media3.decoder)
                implementation(libs.androidx.media3.database)
                // Stock 1.8.0 modules the fork AARs call but do not ship, as in the old app:
                // container (NalUnitUtil, ObuParser for the extractor and ExoPlayer) and effect
                // (GlEffect for libass). Their stock media3-common is excluded below.
                api(libs.androidx.media3.container)
                implementation(libs.androidx.media3.effect)
                upstreamMedia3Aars.forEach { aar -> api(files(upstreamLibs.resolve(aar))) }
                upstreamOtherAars.forEach { aar -> implementation(files(upstreamLibs.resolve(aar))) }
            }
        }
        commonTest {
            kotlin.srcDir(tvApiSrc.resolve("commonTest/kotlin"))
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Core resolves the stock media3 artifacts away in favour of the fork AARs; repeat it here so a
// stock module pulled in transitively cannot bring a second copy of the same classes.
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

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(":composeApp:generateRuntimeConfigs", syncUpstreamSources.values)
}
