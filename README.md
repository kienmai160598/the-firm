# the-firm

"Movies": a streaming app for TVs, a modified version of Nuvio by NuvioMedia.

## Apps

| App | Platform | Path | Stack | Status |
|---|---|---|---|---|
| Movies for Android TV | Android TV, Google TV, Mi Box (Android 7+, 32-bit boxes) | `apps/android-tv/` | Kotlin, Compose for TV, Kotlin core through `shared/` | Being rebuilt from scratch to mirror the Apple TV app, see `docs/android-tv-rebuild.md`. The old app is kept under the git tag `android-tv-legacy` |
| Movies for Apple TV | tvOS 17+ | `apps/apple-tv/` | SwiftUI, XcodeGen, Kotlin core through `shared/` | Port in progress; plays HTTP and torrent streams: AVPlayer for MP4, MOV and HLS, mpv (MPVKit-GPL) for MKV, WebM, AVI and anything AVPlayer rejects |

## Layout

```
apps/
  android-tv/          Android TV app, being rebuilt (docs/android-tv-rebuild.md has the target tree)
    box/               install and perf scripts for the Mi Box
    box/legal-test-addon/  builds an addon of openly licensed torrent films for tests
    third_party/       source of the FFmpeg audio decoder AAR (GPL)
  apple-tv/            Apple TV app. project.yml -> NuvioTv.xcodeproj (XcodeGen)
shared/                Kotlin Multiplatform Gradle build (AGP 9.2, Kotlin 2.4)
  core/                shared logic: models, addons, Trakt, sync, player state (Gradle :composeApp)
  apple-tv-bridge/     builds core for tvOS as a framework the Apple TV app links (Gradle :tvosShared)
  torrent/             Kotlin API of the torrent engine (Gradle :torrent)
  Version.xcconfig     version read by core
engine/                the torrent engine (C++20, libtorrent, payload in RAM), see
                       docs/engine-rebuild.md
docs/                  plans that span both apps
Makefile               one entry point for every build
```

The new Android TV app joins the `shared/` Gradle build, so there is one toolchain: Gradle 9.4.1,
AGP 9.2, Kotlin 2.4.10.

## Build

`make help` lists every target. The common ones (macOS, JDK 17 from Homebrew):

```bash
make android-tv                              # box APK
ANDROID_SERIAL=<ip>:5555 make box-install    # install it on the box with its baseline profile
make engine-next engine-test                 # torrent engine: xcframework, unit tests
make engine-android                          # torrent engine for Android: libmovies_engine.so per ABI
make apple-tv-sim                            # Apple TV app for the simulator
make apple-tv-install APPLE_TEAM=<team id> APPLE_TV_DEVICE=<devicectl id>
make android-tv-test shared-test
make check                                   # shared, engine and Android TV tests, Android engine build, ktlint, TvRuntime init check
make format                                  # ktlint --format on the Kotlin sources
```

Kotlin style is ktlint 1.8.0 (`scripts/ktlint` fetches it and checks its hash) with the rules in
`.editorconfig`.

The Apple TV app links the engine that `make engine-next` builds; the apple-tv targets run it.
`make engine-android` cross builds the same sources with the Android NDK (29.0.14206865, under
`$ANDROID_HOME/ndk`) for the ABIs in `shared/local.properties`; `make android-tv` runs it first,
and an Android build fails when the library is missing. The first engine build downloads Boost 1.86
and libtorrent 2.0.12 (pinned by SHA-256) into `engine/.deps` and compiles libtorrent for each
slice or ABI, a minute or two each.

`shared/local.properties` holds local settings (signing values, `ABIS`) and is not committed.

## History and license

Both upstream histories are kept: the old `apps/android-tv` (tag `android-tv-legacy`) comes from
[NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV), `shared/` and `apps/apple-tv` from
[NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) (its phone apps were removed).
Everything is licensed under the GNU General Public License v3.0, see `LICENSE`.
