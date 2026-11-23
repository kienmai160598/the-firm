# the-firm

"Movies": a streaming app for TVs, a modified version of Nuvio by NuvioMedia.

## Apps

| App | Platform | Path | Stack | Status |
|---|---|---|---|---|
| Movies for Android TV | Android TV, Google TV, Mi Box (Android 7+, 32-bit boxes) | `apps/android-tv/` | Kotlin, Compose for TV, Material 3 Expressive | In daily use on a Mi Box |
| Movies for Apple TV | tvOS 17+ | `apps/apple-tv/` | SwiftUI, XcodeGen, Kotlin core through `shared/` | Port in progress; plays HTTP and torrent streams: AVPlayer for MP4, MOV and HLS, mpv (MPVKit-GPL) for MKV, WebM, AVI and anything AVPlayer rejects |

## Layout

```
apps/
  android-tv/          Android TV app. Its own Gradle build.
    app/               the app module
    box/               install and perf scripts for the Mi Box
    box/legal-test-addon/  builds an addon of openly licensed torrent films for tests
  apple-tv/            Apple TV app. project.yml -> NuvioTv.xcodeproj (XcodeGen)
shared/                Kotlin Multiplatform Gradle build (AGP 9.2, Kotlin 2.4)
  core/                shared logic: models, addons, Trakt, sync, player state (Gradle :composeApp)
  apple-tv-bridge/     builds core for tvOS as a framework the Apple TV app links (Gradle :tvosShared)
  torrent/             Kotlin API of the new torrent engine (Gradle :torrent)
  Version.xcconfig     version read by core
engine/                the new torrent engine (C++20, libtorrent), see docs/engine-rebuild.md.
                       build.sh still builds the old one (NuvioMedia/nuvio-engine, pinned, plus
                       a tvOS patch) into nuvio-engine/, which is not committed
Makefile               one entry point for every build
```

Both Gradle builds use Gradle 9.4.1, AGP 9.2 and Kotlin 2.4.10; `make toolchain-check` fails
when they drift apart. The Android TV app does not use
`shared/` yet: it still carries its own copy of the logic.

## Build

`make help` lists every target. The common ones (macOS, JDK 17 from Homebrew):

```bash
make android-tv                              # box APK
ANDROID_SERIAL=<ip>:5555 make box-install    # install it on the box with its baseline profile
make engine                                  # old torrent engine for Apple TV, once (about 16 minutes)
make engine-next engine-test                 # new torrent engine: xcframework, unit tests
make apple-tv-sim                            # Apple TV app for the simulator
make apple-tv-install APPLE_TEAM=<team id> APPLE_TV_DEVICE=<devicectl id>
make android-tv-test shared-test
make check                                   # toolchain, layering, ktlint, engine and Android TV unit tests
make format                                  # ktlint --format on the Android TV sources
```

Kotlin style is ktlint 1.8.0 (`scripts/ktlint` fetches it and checks its hash) with the rules in
`apps/android-tv/.editorconfig`. `max-line-length` and the naming rules are off until the dead code
is gone; the formatter still wraps at 100 columns.

`scripts/check-layering` keeps the Android TV layers apart: code under `data/`, `core/` and
`domain/` must not reference `com.kien.movies.ui`, and `domain/` must not reference
`com.kien.movies.data`. Move the shared piece down a layer instead of importing upward.

Without `make engine` the Apple TV app still builds, with torrents turned off.

`apps/android-tv/local.properties` and `shared/local.properties` hold local settings and are not
committed.

## History and license

Both upstream histories are kept: `apps/android-tv` comes from
[NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV), `shared/` and `apps/apple-tv` from
[NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) (its phone apps were removed).
Everything is licensed under the GNU General Public License v3.0, see `LICENSE`.
