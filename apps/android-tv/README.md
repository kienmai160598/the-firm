# Movies for Android TV

Mirrors `apps/apple-tv` on Compose for TV, on the Kotlin logic in `shared/`, and streams torrents
through the engine in `engine/` from RAM. The plan, acceptance criteria, slices and the folder
tree are in `docs/android-tv-rebuild.md`.

Build and install (the app is a project of the `shared/` Gradle build):

- `make android-tv`: the `box` APK (R8, armeabi-v7a, signed from `shared/local.properties`),
  `app/build/outputs/apk/box/androidTvApp-box.apk`, package `com.kien.movies.next`.
- `make box-install`: builds it and installs it on the box with `box/install.sh`
  (`ANDROID_SERIAL=<ip>:5555`).
- `make android-tv-test`: unit tests of the app and of the bridge (`shared/android-tv-bridge`).
- `make box-roundtrip`: the persistence round trip (`box/roundtrip.sh`) on the box, against the
  test addon served from `apps/apple-tv/TestAddon` on `127.0.0.1:8765`.
- `make lint`, `make format`: ktlint over the app and bridge sources.

The app (slice A3): `shell/` holds the sidebar, the router and the per screen state; `design/`
the tokens and shared parts mirrored from tvOS; `bridge/` the flow wrappers over the `Tvos*`
API; one package per feature. Who may edit what while the feature slices run in parallel is in
`OWNERS-WAVE.md`. Debug builds take launch options as intent extras (the table is in
`app/src/debug/kotlin/com/kien/movies/debug/DebugLaunch.kt`), e.g.
`adb shell am start -n com.kien.movies.next/com.kien.movies.MainActivity -e MoviesDebugRoute tab/addons`.

Also here:

- `box/install.sh`: installs an APK on the Mi Box and applies its baseline profile.
- `box/perf/measure.sh`: a fixed remote scenario that records jank and memory. The previous app's
  runs are in `box/perf/results/agp8-movies-1625/` (not in git).
- `box/legal-test-addon/`: a static addon of openly licensed torrent films for tests.
- `box/brand/`: renders the launcher icon, banner and wordmarks.
- `app/libs/lib-decoder-ffmpeg-release.aar`: the Media3 FFmpeg audio decoder with the downmix;
  its source and licence are in `third_party/ffmpeg-decoder-downmix/`.

The previous app is kept under the git tag `android-tv-legacy`. To read it:
`git worktree add ../the-firm-legacy android-tv-legacy`.
