import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

// Built by `make engine-next` (engine/scripts/build-xcframework.sh).
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

kotlin {
    tvosArm64()
    tvosSimulatorArm64()

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
        tvosTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
