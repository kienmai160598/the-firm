# Wave ownership (after slice A3)

Six agents build the feature screens in parallel on top of the A3 shell. Each may edit only the
paths on its line. Everything under "Frozen" belongs to the shell: an agent that needs a frozen
file changed writes the exact request (file, change, why) into its report and works around it
until it lands. Paths below `app/` are relative to `apps/android-tv/app/`, and
`com/kien/movies/` is short for `src/main/kotlin/com/kien/movies/`.

## Agents

| Agent | Slices | May edit |
|---|---|---|
| home | A4, A5 | `com/kien/movies/home/`; new `design/DpadThrottle.kt` and `design/SemanticsPruning.kt` (create only; frozen once merged); `src/test/kotlin/com/kien/movies/home/`; `src/androidTest/.../HomeTest.kt`, `ContinueWatchingTest.kt` |
| details | A6 | `com/kien/movies/details/`; `src/test/kotlin/com/kien/movies/details/`; `src/androidTest/.../DetailsTest.kt` |
| streams | A7, A10 | `com/kien/movies/streams/`, `com/kien/movies/torrent/`; `shared/tv-api/src/commonMain/kotlin/com/nuvio/tvos/rules/StreamFeatures.kt` and its test in `shared/tv-api/src/commonTest/kotlin/com/nuvio/tvos/rules/`; `src/test/kotlin/com/kien/movies/streams/`, `.../torrent/`; `src/androidTest/.../StreamsTest.kt` |
| player | A8, A9 | `com/kien/movies/player/` except `PlaybackRequest.kt`; `shared/tv-api/.../rules/PlaybackAdvisory.kt`, `OnlineSubtitleChoice.kt`, `ProgressSavePolicy.kt` and their tests; `src/test/kotlin/com/kien/movies/player/`; `src/androidTest/.../PlayerTest.kt`, `PlaybackFailureTest.kt` |
| search | A11 | `com/kien/movies/search/`; `src/test/kotlin/com/kien/movies/search/`; `src/androidTest/.../SearchTest.kt` |
| addons | A12, A12b | `com/kien/movies/addons/` including `addons/phone/`; `shared/tv-api/src/commonMain/kotlin/com/nuvio/tvos/phone/` and `shared/tv-api/src/commonTest/kotlin/com/nuvio/tvos/phone/`; `src/test/kotlin/com/kien/movies/addons/`; `src/androidTest/.../AddonsTest.kt`, `PhoneAddonTest.kt` |

Every agent may also:

- add new drawables under `app/src/main/res/drawable/` whose name starts with its feature
  (`ic_player_audio.xml`, `ic_addons_phone.xml`); existing resources stay frozen;
- keep a size that only its feature uses as a private constant in its own file, named after the
  tvOS literal it mirrors (pt / 2); a value two features share belongs in `design/Tokens.kt`
  (request it);
- edit, in `docs/android-tv-rebuild.md`, only its own AC rows in section 6 and its slice rows
  in section 7, in the same branch as the behaviour change.

## Frozen

- `com/kien/movies/design/` (tokens, theme, card shape and focus, glass, artwork, backdrop,
  metadata line)
- `com/kien/movies/shell/` and `src/test/kotlin/com/kien/movies/shell/`
- `com/kien/movies/bridge/`
- `com/kien/movies/MainActivity.kt`, `MoviesApp.kt`, `DependencySmoke.kt`
- `src/debug/` and `src/box/` (`DebugLaunch` and its box twin)
- `com/kien/movies/player/PlaybackRequest.kt` (`PlaybackRequest`, `StreamNotice`: the seam
  between streams and player)
- The public entry signatures; bodies belong to the owner:
  `HomeScreen(router: Router)`, `SearchScreen(router: Router)`, `AddonsScreen()`,
  `DetailsScreen(route: AppRoute.Details, router: Router)`,
  `StreamsScreen(route: AppRoute.Streams, router: Router)`,
  `PlaybackScreen(request: PlaybackRequest, onFailure: (StreamNotice) -> Unit, onExit: () -> Unit)`
- `src/androidTest/kotlin/com/kien/movies/support/TvRobot.kt`, `NavigationTest.kt`,
  `PersistenceRoundTripTest.kt`, `src/test/kotlin/com/kien/movies/DependencySmokeTest.kt`
- Build and packaging: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/libs/`,
  `app/src/main/AndroidManifest.xml`, existing files under `app/src/main/res/`,
  `shared/gradle/libs.versions.toml`, `shared/settings.gradle.kts`, `shared/gradle.properties`,
  `shared/android-tv-bridge/`, `Makefile`, `scripts/`, `box/`
- `shared/core/` (upstream, never edited for a TV feature) and every `shared/tv-api` file not
  listed on an agent's line
- `apps/android-tv/README.md`, this file

## What the wave must know

- Routes and Back: screens navigate with `router.open(AppRoute.Details(type, id))` or
  `router.open(AppRoute.Streams(target, resumesDirectly))`; they never pop. Back is the shell's;
  a screen adds a `BackHandler` only for an inner step (player order, dialog, panel), enabled only
  while that step is open.
- Focus: the shell focuses the screen's root after every route change and, when an entry comes
  back, the element that had focus when it left. Every state (loading, empty, error) must hold a
  focusable element. Rows use `focusRestorer` and stable `key` and `contentType`.
- ViewModels: `viewModel { DetailsViewModel(route.type, route.id) }` inside the screen; the store
  is per back stack entry. No SavedStateHandle, no Hilt. The player uses
  `viewModel(key = request.id) { ... }` because the cover lives in the Streams entry.
- Data: callback APIs only through `bridge/BridgeFlows` (flows are conflated, one shots cancel
  with the caller); plain synchronous `Tvos*` calls are made directly (list in `BridgeFlows`).
  Expose one `StateFlow` per ViewModel with `stateIn(viewModelScope, WhileSubscribed(5_000), ...)`.
- Look: values from `design/Tokens.kt` and `MaterialTheme` (wired to them); cards use
  `CardFocus` with tv-material `Card`; buttons use `GlassStyle`; artwork through
  `ArtworkImage` at a fixed size; full screen art through `Backdrop`. The screen colour is the
  window background set by the shell: draw no full screen fill, no gradient over the whole
  screen, no blur, no stacked translucent layers. tvOS radii 22 and 18 pt are stale: the card
  corner is 12 dp (tvOS 24 pt, commit 6ad37b6a9f).
- Tab roots (Home, Search, Addons) draw under the closed rail: backgrounds may run to the edge,
  focusable content starts at `RailSize.contentStart`.
- Imports: `androidx.tv.material3.*` for controls, never `androidx.compose.material3.*`.
- Libraries on the classpath: ExoPlayer, HLS and extractors from the media3 fork (through the
  bridge), media3 `lib-ui` (`SubtitleView`), stock `media3-decoder` 1.8.0, the downmix FFmpeg
  decoder, Coil 3.6.3 with the OkHttp fetcher (default singleton image loader), NanoHTTPD 2.3.1,
  zxing core 3.5.3, Compose ui-test-junit4. A new library is a frozen file change: request it.
- Debug launch: `DebugLaunch` (intent extras, table in `src/debug/.../DebugLaunch.kt`) exposes
  `usesStreamFixture`, `opensFirstStream`, `startSecondsBeforeEnd`, `resumeChoice`,
  `seasonNumber`, `streamsRouteOnce(details)`; in the `box` build all are off.
- UI tests: `TvRobot` with `createEmptyComposeRule()`, names start with the AC id in lower case
  (`fun d3_seasonPickerFocusesTheSelectedSeason()`). Never `connectedAndroidTest` on the box: it
  uninstalls the app and wipes its data.
- A file added to `shared/tv-api` is compiled by the tvOS bridge too: run `make shared-test` and
  `make apple-tv-sim` as well as `make check`.
