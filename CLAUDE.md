# the-firm

"Movies": one streaming app for TVs, built twice: `apps/apple-tv` (SwiftUI, tvOS 17+) and
`apps/android-tv` (Compose for TV, API 24+). Both run the same Kotlin logic from `shared/`.
README.md has the layout and every build command.

## Status

- `apps/apple-tv` is the reference app. Its look and behaviour are the spec.
- `apps/android-tv` is being rebuilt from scratch. The plan, acceptance criteria and slices are in
  `docs/android-tv-rebuild.md`. Until slice A0 lands, the old app still sits in that folder:
  read it for reference, never extend it.

## Working on a slice

- One chat, one slice, one branch, one pull request. The slice id (A0, A1, ...) goes in the
  branch name and the PR title.
- Read `docs/android-tv-rebuild.md` first: section 1 (open decisions), the slice row in section 7,
  then the Swift files the slice names. Do not work from memory of an earlier chat.
- A change that alters behaviour on one platform updates the acceptance criteria in the doc in
  the same PR, and says whether the other platform needs the same change.
- Stop and ask when a slice needs a decision listed as open in section 1.

## Where code goes

- Not pixels, not a platform API: it goes to Kotlin in `shared/`, once, for both apps
  (`shared/tv-api` after slice A1, `shared/apple-tv-bridge/src/tvosMain` until then).
- `shared/core` is upstream NuvioMobile code. Do not edit it for a TV feature; split a UI file
  with a patch in the style of `shared/apple-tv-bridge/upstream-prs/`.
- Android files mirror the Swift file names (`HomeHero.swift` becomes `HomeHero.kt`,
  `*Model.swift` becomes `*ViewModel.kt`), so the same feature is easy to find on both sides.
  The full tree and the name mapping table are in section 3 of `docs/android-tv-rebuild.md`.
- Each platform uses its own system components. Share the logic and the layout skeleton, not
  the chrome.

## Done means

1. Tests for the slice's acceptance criteria pass: unit tests, and UI tests named after the AC id
   on the platform the slice touches.
2. `make check` passes (and `make apple-tv-sim` when `shared/` changed).
3. Seen on the device: Apple TV simulator or device, or the Mi Box with the `box` build installed
   by `box/install.sh`. Never take a performance number from a debug build.
4. The PR body lists which of these ran and on what. "Built, not seen on a device" is a valid
   report.

## Android TV box

Weak GPU (fill rate bound), 32-bit armeabi-v7a, 1.9 GB RAM, 59.94 Hz. No full screen gradients,
no runtime blur, no stacked translucent layers. Measure with `box/perf/measure.sh` before naming
a cause. Targets are in section 4 of `docs/android-tv-rebuild.md`.

## Style

- Commit subject: platform prefix and a plain sentence, e.g. `tvOS: ...`, `android-tv: ...`,
  `shared: ...`, `scripts: ...`.
- No em dashes in code, comments, docs or commit messages.
- Kotlin: ktlint 1.8.0 through `make lint` and `make format`.
