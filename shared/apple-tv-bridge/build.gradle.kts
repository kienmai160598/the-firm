import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

val upstreamSrc = rootProject.file("core/src")
val upstreamRuntimeConfig = rootProject.file("core/build/generated/runtime-config/kotlin")
val cinteropDir = upstreamSrc.resolve("nativeInterop/cinterop")
val nuvioEngineAppleFramework =
    rootProject.file("../nuvio-engine/platform/apple/NuvioEngine.xcframework")
val nuvioEngineSlices = mapOf(
    "tvosArm64" to "tvos-arm64",
    "tvosSimulatorArm64" to "tvos-arm64-simulator",
)
val engineBuilt = nuvioEngineSlices.values.all { slice ->
    nuvioEngineAppleFramework.resolve("$slice/libCNuvioEngine.a").isFile
}
// Which torrent engine the app links (docs/engine-rebuild.md D2): "legacy" (nuvio-engine) or
// "next" (engine/). Both bring their own libtorrent, so a build links exactly one.
val torrentEngine = providers.gradleProperty("nuvio.tvos.engine")
    .orElse(providers.environmentVariable("NUVIO_TVOS_ENGINE"))
    .getOrElse("legacy")
check(torrentEngine == "legacy" || torrentEngine == "next") {
    "nuvio.tvos.engine must be legacy or next, not $torrentEngine"
}
val nextEngine = torrentEngine == "next"
val p2pEngineEnabled = !nextEngine &&
    (providers.gradleProperty("nuvio.tvos.p2p").orNull?.toBoolean() ?: engineBuilt)
val p2pEngineStub =
    upstreamSrc.resolve("iosAppStore/kotlin/com/nuvio/app/features/p2p/P2pStreamingEngine.ios.kt")
val trailerResolverStub = upstreamSrc.resolve(
    "iosAppStore/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.ios.kt",
)
val trailerExtractorSources = listOf(
    "fullCommonMain/kotlin/com/nuvio/app/features/trailer/InAppYouTubeExtractor.kt",
    "fullCommonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.kt",
    "iosFull/kotlin/com/nuvio/app/features/trailer/TrailerExtractionPlatform.ios.kt",
).map(upstreamSrc::resolve)
val missingEngineMessage = "Build the Nuvio Engine XCFramework with " +
    "nuvio-engine/scripts/build-apple-xcframework.sh, or pass -Pnuvio.tvos.p2p=false."

fun excludedUpstreamPaths(): Set<String> =
    listOf("excluded-by-rule.txt", "excluded-by-compiler.txt")
        .map(project::file)
        .filter(File::exists)
        .flatMap(File::readLines)
        .map { line -> line.substringBefore('\t').trim() }
        .filter(String::isNotEmpty)
        .map { key -> upstreamSrc.resolve(key).absolutePath }
        .toSet()

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

    private fun stringProperty(name: String, text: String): String =
        "val Res.string.$name: StringResource get() = StringResource(\"$name\", ${literal(text)})"

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
            "object Res { object string\n object plurals }",
        ).joinToString("\n")
    }
}

// The full iOS build's YouTube trailer extractor, without the rest of fullCommonMain (plugins, JS).
val trailerExtractorDir = layout.buildDirectory.dir("generated/tvos-trailer/kotlin")
val syncTrailerExtractor = tasks.register<Sync>("syncTrailerExtractor") {
    from(trailerExtractorSources)
    into(trailerExtractorDir.map { dir -> dir.dir("com/nuvio/app/features/trailer") })
}

val generateEnglishResShim = tasks.register<GenerateEnglishResShim>("generateEnglishResShim") {
    valuesDir.set(upstreamSrc.resolve("commonMain/composeResources/values"))
    outputDir.set(layout.buildDirectory.dir("generated/tvos-res-shim/kotlin"))
}

kotlin {
    tvosArm64()
    tvosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("commoncrypto") {
            defFile(cinteropDir.resolve("commoncrypto.def"))
            compilerOpts("-I${cinteropDir.absolutePath}")
        }
        if (p2pEngineEnabled) {
            val engineSlice = nuvioEngineAppleFramework.resolve(nuvioEngineSlices.getValue(name))
            check(engineSlice.resolve("libCNuvioEngine.a").isFile) { missingEngineMessage }
            compilations.getByName("main").cinterops.create("nuvioengine") {
                defFile(cinteropDir.resolve("nuvioengine.def"))
                compilerOpts("-I${engineSlice.resolve("Headers").absolutePath}")
                extraOpts("-libraryPath", engineSlice.absolutePath)
                extraOpts("-Xccall-mode", "direct")
            }
        }
        binaries.framework {
            baseName = "NuvioTvShared"
            isStatic = true
            freeCompilerArgs += "-Xbinary=bundleId=com.nuvio.tvos.shared"
        }
    }

    sourceSets {
        val replacedByEngine = if (p2pEngineEnabled) setOf(p2pEngineStub.absolutePath) else emptySet()
        val excludedPaths =
            excludedUpstreamPaths() + replacedByEngine + trailerResolverStub.absolutePath
        val isExcluded = Spec<FileTreeElement> { element ->
            element.file.absolutePath in excludedPaths
        }
        commonMain {
            kotlin.srcDir(upstreamSrc.resolve("commonMain/kotlin"))
            kotlin.srcDir(upstreamRuntimeConfig)
            kotlin.srcDir(generateEnglishResShim.map { task -> task.outputDir })
            kotlin.srcDir(rootProject.file("tv-api/src/commonMain/kotlin"))
            kotlin.exclude(isExcluded)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.androidx.lifecycle.runtimeCompose)
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
        tvosMain {
            kotlin.srcDir(upstreamSrc.resolve("iosMain/kotlin"))
            kotlin.srcDir(upstreamSrc.resolve("iosAppStore/kotlin"))
            kotlin.srcDir(syncTrailerExtractor.map { trailerExtractorDir })
            if (p2pEngineEnabled) {
                kotlin.srcDir(upstreamSrc.resolve("appleP2pEngine/kotlin"))
                kotlin.srcDir(project.file("src/tvosP2pEngine/kotlin"))
            }
            kotlin.srcDir(project.file(if (nextEngine) "src/tvosNextEngine/kotlin" else "src/tvosLegacyEngine/kotlin"))
            kotlin.exclude(isExcluded)
            dependencies {
                implementation(libs.ktor.client.darwin)
                if (nextEngine) implementation(project(":torrent"))
            }
        }
        commonTest {
            kotlin.srcDir(rootProject.file("tv-api/src/commonTest/kotlin"))
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(":composeApp:generateRuntimeConfigs")
}
