import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

// tvOS: built by `make engine-next` (engine/scripts/build-xcframework.sh).
val engineFramework = rootProject.file("../engine/build/MoviesEngine.xcframework")
val engineSlices = mapOf(
    "tvosArm64" to "tvos-arm64",
    "tvosSimulatorArm64" to "tvos-arm64-simulator"
)

val checkEngineBuilt = tasks.register("checkEngineBuilt") {
    val libraries = engineSlices.values.map { slice -> engineFramework.resolve("$slice/libmovies_engine.a") }
    doLast {
        check(libraries.all(File::isFile)) { "Build the engine first: make engine-next" }
    }
}

// Android: libmovies_engine.so per ABI, built by `make engine-android`
// (engine/scripts/build-android.sh) for the ABIs in shared/local.properties, as the box build.
val androidEngineLibs = rootProject.file("../engine/build/android/jniLibs")
val androidAbis: List<String> = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}.getProperty("ABIS").orEmpty().split(",").map(String::trim).filter(String::isNotEmpty)
    .ifEmpty { listOf("armeabi-v7a") }

// Copies the engine into the jniLibs the library packages, and fails when an ABI is missing, so
// an APK can never assemble without the engine and crash on the box when a torrent starts.
abstract class CopyAndroidEngine : DefaultTask() {
    @get:InputDirectory
    @get:Optional
    abstract val engineLibs: DirectoryProperty

    @get:Input
    abstract val abis: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val files: FileSystemOperations

    @TaskAction
    fun copy() {
        val source = engineLibs.orNull?.asFile
        val missing = abis.get().filterNot { abi -> source?.resolve("$abi/libmovies_engine.so")?.isFile == true }
        check(missing.isEmpty()) { "No torrent engine for $missing. Build it first: make engine-android" }
        files.sync {
            from(source)
            include(abis.get().map { abi -> "$abi/libmovies_engine.so" })
            into(outputDir)
        }
    }
}

val copyAndroidEngine = tasks.register<CopyAndroidEngine>("copyAndroidEngine") {
    if (androidEngineLibs.isDirectory) engineLibs.set(androidEngineLibs)
    abis.set(androidAbis)
    outputDir.set(layout.buildDirectory.dir("generated/engine-jniLibs"))
}

kotlin {
    tvosArm64()
    tvosSimulatorArm64()

    android {
        namespace = "com.nuvio.torrent"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-rules.pro")
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        val slice = engineFramework.resolve(engineSlices.getValue(name))
        val interop = compilations.getByName("main").cinterops.create("moviesengine") {
            defFile(project.file("src/nativeInterop/cinterop/moviesengine.def"))
            compilerOpts("-I${slice.resolve("Headers").absolutePath}")
            extraOpts("-libraryPath", slice.absolutePath)
        }
        // The klib embeds the static library, so a rebuilt engine must rerun cinterop.
        tasks.named(interop.interopProcessingTaskName) {
            dependsOn(checkEngineBuilt)
            inputs.dir(slice).withPropertyName("engineSlice")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.androidx.testRunner)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(copyAndroidEngine, CopyAndroidEngine::outputDir)
    }
}
