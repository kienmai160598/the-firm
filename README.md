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
  torrent/             Kotlin API of the new torrent engine (Gradle :torrent)
  Version.xcconfig     version read by core
engine/                the new torrent engine (C++20, libtorrent), see docs/engine-rebuild.md.
                       build.sh still builds the old one (NuvioMedia/nuvio-engine, pinned, plus
                       a tvOS patch) into nuvio-engine/, which is not committed
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
make engine                                  # old torrent engine for Apple TV, once (about 16 minutes)
make engine-next engine-test                 # new torrent engine: xcframework, unit tests
make apple-tv-sim                            # Apple TV app for the simulator
make apple-tv-install APPLE_TEAM=<team id> APPLE_TV_DEVICE=<devicectl id>
make apple-tv-install NUVIO_TVOS_ENGINE=next ...   # same, with the new engine (payload in RAM)
make android-tv-test shared-test
make check                                   # shared, engine and Android TV tests, ktlint, TvRuntime init check
make format                                  # ktlint --format on the Kotlin sources
```

Kotlin style is ktlint 1.8.0 (`scripts/ktlint` fetches it and checks its hash) with the rules in
`.editorconfig`.

Without `make engine` the Apple TV app still builds, with torrents turned off. With
`NUVIO_TVOS_ENGINE=next` it needs only `make engine-next`, which the apple-tv targets run anyway.
The first `make engine-next` downloads Boost 1.86 and libtorrent 2.0.12 (pinned by SHA-256) into
`engine/.deps` and compiles libtorrent for each slice, a few minutes.

`shared/local.properties` holds local settings (signing values, `ABIS`) and is not committed.

## History and license

Both upstream histories are kept: the old `apps/android-tv` (tag `android-tv-legacy`) comes from
[NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV), `shared/` and `apps/apple-tv` from
[NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) (its phone apps were removed).
Everything is licensed under the GNU General Public License v3.0, see `LICENSE`.
