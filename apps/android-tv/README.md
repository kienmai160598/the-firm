# Movies for Android TV

Being rebuilt from scratch to mirror `apps/apple-tv`, on the Kotlin logic in `shared/`. The plan,
acceptance criteria, slices and the target folder tree are in `docs/android-tv-rebuild.md`.

What is here before the app lands:

- `box/install.sh`: installs an APK on the Mi Box and applies its baseline profile.
- `box/perf/measure.sh`: a fixed remote scenario that records jank and memory. The old app's runs
  are in `box/perf/results/legacy-agp8-movies-1625/` (not in git).
- `box/legal-test-addon/`: a static addon of openly licensed torrent films for tests.
- `box/brand/`: renders the launcher icon, banner and wordmarks.
- `app/libs/lib-decoder-ffmpeg-release.aar`: the Media3 FFmpeg audio decoder with the downmix;
  its source and licence are in `third_party/ffmpeg-decoder-downmix/`.

The old app is kept under the git tag `android-tv-legacy`. To read it:
`git worktree add ../the-firm-legacy android-tv-legacy`.
