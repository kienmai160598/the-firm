# the-firm

Monorepo for "Movies", a modified version of Nuvio for TVs.

| Path | What | Upstream |
|---|---|---|
| `android-tv/` | Movies for Android TV and Google TV boxes (Compose for TV, Material 3 Expressive look). Gradle project; see `android-tv/README.md` and `android-tv/box/`. | [NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV) |
| `multiplatform/` | Kotlin Multiplatform app. The Apple TV port lives in `multiplatform/tvosApp` (SwiftUI, XcodeGen) and `multiplatform/tvosShared` (Kotlin shared code). | [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) |

Both projects keep their full upstream history; this repo joined them on 2026-11-22.

## Build

Android TV box APK (macOS):

```bash
cd android-tv
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleFullBox
```

Apple TV (needs Xcode and XcodeGen):

```bash
cd multiplatform/tvosApp
xcodegen generate
xcodebuild -project NuvioTv.xcodeproj -scheme NuvioTv -configuration Release \
  -destination 'generic/platform=tvOS' build
```

Each project reads its own `local.properties` (not committed).

## License

Movies is a modified version of Nuvio by NuvioMedia. Both projects are licensed under the
GNU General Public License v3.0; see `android-tv/LICENSE` and `multiplatform/LICENSE`.
