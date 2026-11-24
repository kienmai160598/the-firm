rootProject.name = "Nuvio"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Project names stay as upstream named them, so task names and generated resource packages
// (nuvio.composeapp.generated.resources) do not change; the folders say what each one is.
include(":composeApp")
project(":composeApp").projectDir = file("core")
include(":tvosShared")
project(":tvosShared").projectDir = file("apple-tv-bridge")
include(":androidTvShared")
project(":androidTvShared").projectDir = file("android-tv-bridge")
include(":androidTvApp")
project(":androidTvApp").projectDir = file("../apps/android-tv/app")
include(":torrent")
