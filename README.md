# the-firm

"Movies": a streaming app for TVs, a modified version of Nuvio by NuvioMedia.

## Apps

| App | Platform | Path | Stack | Status |
|---|---|---|---|---|
| Movies for Android TV | Android TV, Google TV, Mi Box (Android 7+, 32-bit boxes) | `apps/android-tv/` | Kotlin, Compose for TV, Material 3 Expressive | In daily use on a Mi Box |
| Movies for Apple TV | tvOS 17+ | `apps/apple-tv/` | SwiftUI, XcodeGen, Kotlin core through `shared/` | Port in progress; plays direct HTTP streams |

## Layout

```
apps/
  android-tv/          Android TV app. Its own Gradle build (AGP 8.13, Kotlin 2.3).
    app/               the app module
    box/               install and perf scripts for the Mi Box
  apple-tv/            Apple TV app. project.yml -> NuvioTv.xcodeproj (XcodeGen)
shared/                Kotlin Multiplatform Gradle build (AGP 9.2, Kotlin 2.4)
  core/                shared logic: models, addons, Trakt, sync, player state (Gradle :composeApp)
  apple-tv-bridge/     builds core for tvOS as a framework the Apple TV app links (Gradle :tvosShared)
  Version.xcconfig     version read by core
```

The Android TV app does not use `shared/` yet: it still carries its own copy of the logic. Moving
it onto `shared/core` needs the Android TV build on AGP 9 and Kotlin 2.4 first.

## Build

Android TV (box APK, macOS):

```bash
cd apps/android-tv
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleFullBox
```

Apple TV (Xcode and XcodeGen; the Xcode build runs `shared/gradlew` for the Kotlin framework):

```bash
cd apps/apple-tv
xcodegen generate
xcodebuild -project NuvioTv.xcodeproj -scheme NuvioTv -configuration Release \
  -destination 'generic/platform=tvOS' build
```

`apps/android-tv/local.properties` and `shared/local.properties` hold local settings and are not
committed.

## History and license

Both upstream histories are kept: `apps/android-tv` comes from
[NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV), `shared/` and `apps/apple-tv` from
[NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) (its phone apps were removed).
Everything is licensed under the GNU General Public License v3.0, see `LICENSE`.
