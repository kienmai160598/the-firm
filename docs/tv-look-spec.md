# TV look spec: Apple TV app as the reference

One spec for the look and motion of both apps. The reference is Apple's TV app (`com.apple.TVWatchList`)
on a physical Apple TV 4K running tvOS 27.0, captured on 2026-11-23. Section 4 of
`android-tv-rebuild.md` points here for sizes, colours and motion; its performance and focus rules
still hold.

## 0. Sources and confidence

- **Captures.** 4K screenshots through `xcrun devicectl device capture screenshot`, kept outside git
  because they hold Apple artwork: `~/Pictures/the-firm-atv-ref/` (`4k/`, `1280/`, `grid/`, `motion/`).
  File names below (`01-home.png`, ...) refer to that folder.
- **Measured.** Edges come from pixel scans of the 4K files (`edges.py`, `inkbox.py` in the same
  folder): 4K px / 2 = pt, accurate to about 2 pt. Text sizes come from cap height
  (SF Pro cap height is about 0.705 of the point size), then snapped to the tvOS text style.
- **System.** Where the TV app uses a tvOS system component, the component is the spec on tvOS.
  It is not re-measured.
- **Unmeasured.** Motion of the TV app could not be recorded. The Apple TV refuses screen recording
  ("Screen Recording is not supported by this device"). The TV app on the Mi Box is FLAG_SECURE,
  so `screencap` returns 0 bytes. The headless simulator draws no focus lift and turns the zoom
  transition into a cross-fade. Every motion value carries one of three tags: `measured`,
  `system`, `unmeasured`.
- **Not captured.** Player chrome (playback controls never opened from the remote helper), Store,
  and a Library with content (this account's Library is empty).

## 1. Units and grid

| | tvOS | Android TV |
|---|---|---|
| Canvas | 1920 x 1080 pt | 960 x 540 dp |
| Rule | as measured | `dp = pt / 2`, `sp = pt / 2`, rounded |
| Side margin | 80 pt | 48 dp (Android overscan floor; 40 dp would be literal) |
| Content width | 1760 pt | 864 dp |
| Gutter between items | 40 pt | 20 dp |

Every shelf in the TV app sits on one column grid: content width 1760, gutter 40. The item width
follows from the column count, which is why it reads as tidy. Measured on `19-details-d6.png`,
`17-details-d4.png`, `39-search-results.png`, `01-home.png`.

| Columns | tvOS item width | Used for | Android item width (864 dp, 20 dp gutter) |
|---|---|---|---|
| 3 | 560 pt | Search result cards (560 x 170) | 275 dp |
| 4 | 410 pt | Episodes, trailers, bonus content (16:9, 410 x 230) | 201 dp |
| 5 | 320 pt | Continue Watching (16:9 picture 320 x 180, band below inside the card) | 157 dp |
| 6 | 260 pt | Posters (2:3, 260 x 390), Top 10, cast circles (260), Browse genres | 127 dp |

Rule for both platforms: the code holds the column count and the aspect ratio. The width is
`(contentWidth - (n - 1) * gutter) / n`. No literal card widths in screens. The next card peeks past
the right margin because the row scrolls under it; the row is not clipped at the margin.

## 2. Tokens

### Colour

Sampled from the 4K files. Backgrounds are gradients in the TV app; the Android column is the flat
colour the box can afford (decided in `android-tv-rebuild.md`, box performance rules).

| Token | Reference | tvOS | Android TV |
|---|---|---|---|
| Home rows background | slate under the rows once the hero scrolls away, measured `#353A3C` (`10-row3.png`) | slate gradient `#253035` top to `#333637` | `#353A3C` flat |
| Details background | navy, lighter in the centre: `#0B131B` edges, `#263642` centre | gradient as measured | `#141C24` flat |
| Search and Library background | grey slate `#273137` top to `#35393A` | gradient as measured | `#2E3437` flat |
| Text primary | white | `.primary` | `#FFFFFF` |
| Text secondary (row titles, overview, captions) | white about 60% | `.secondary` | `#FFFFFF` at 60% |
| Text tertiary (placeholder, price caption) | white about 35% | `.tertiary` | `#FFFFFF` at 35% |
| Focused control fill | `#EAEAE8` | system | `#EBEBEA` |
| Focused control content | near black | system | `#141414` |
| Unfocused pill or card platter (result card, season pill) | white at 22 to 30% over the background: `#5C5F60` on search, `#58656E` on details | system material | white 22% (one flat layer) plus the glass edge |
| Sidebar panel | near black glass `#161616` with blur | system | `#1A1A1A` at 96%, no blur, plus the glass edge |
| Glass edge (Android only) | the bright rim of Liquid Glass | not drawn: the system glass has it | 1 dp inside stroke, white 16% unfocused; on every surface that is glass on tvOS: sidebar panel, nav pill, circular buttons, unfocused pills, platters, description panel. Same idea as the YouTube TV app's light outlines. Dropped when the control turns solid white on focus |

### Type

tvOS text styles from the HIG (tvOS table: Title 1 76/96, Title 2 57/66, Title 3 48/56, Headline 38/46,
Callout 31/38, Body 29/36, Caption 1 25/32, Caption 2 23/30; size/leading in pt, Medium, emphasized
Bold). Android uses `sp = pt / 2`.

| Role | Measured cap height | tvOS | Android TV |
|---|---|---|---|
| Search field text and placeholder | 40 (ascender) | Title 2, 57 | 28 sp |
| Empty state title | | Headline, 38, bold | 19 sp bold |
| Row title ("Continue Watching", "Trailers", "Top Results") | 21 to 22 | Callout, 31, bold, secondary | 16 sp bold, secondary |
| Nav pill label, result card title | 20 | Body, 29, bold | 15 sp bold |
| Metadata line, sidebar item | 17 to 18 | Caption 1, 25 | 13 sp |
| Episode title | 17 | Caption 1, 25, bold | 13 sp bold |
| Overview, episode description, result subtitle | 15 to 16 | Caption 2, 23, leading 30, secondary | 12 sp, line height 15 sp |
| Overline ("EPISODE 2") and price caption | 13.5 | 19 pt uppercase, secondary (custom) | 11 sp (badge size floor) |

### Shape, elevation, focus

| Token | tvOS | Android TV |
|---|---|---|
| Card corner (poster, 16:9 cards, Continue Watching) | 16 pt, continuous | 8 dp |
| Panel corner (result card, description panel) | 24 pt | 12 dp |
| Sidebar panel corner | 32 pt | 16 dp |
| Pills (buttons, nav pill, season tab, suggestion) | capsule | `RoundedCornerShape(50)` |
| Focus scale of a card | system lift; measured 1.09 to 1.10 on posters and 16:9 cards | 1.10 in `graphicsLayer` |
| Focus shadow | system | one soft shadow, 12 dp blur equivalent, black 40% |
| Focused control | system: white fill, dark content | white fill, `#141414` content, scale 1.05, no edge |

## 3. Components

### Navigation: collapsed pill and sidebar

Reference: `01-home.png`, `30-sidebar.png`, `60-lib-hover.png`.

- **Collapsed:** a capsule at x 50, y 50 to 110 (height 60): "‹", a 50 pt circle with the tab icon, the tab
  name (Body bold). It floats over the content and is focusable; Up from the top control reaches it.
- **Open:** a floating panel at x 34, y 34, 340 x 1016 pt, corner 32: profile avatar (50) and name,
  clock on the right, then one row per tab. Rows are 72 pt tall on a 78 pt pitch; icon at x 80,
  label at x 120. Focused row: white capsule inset 10 pt from the panel, dark text. Current tab
  when focus is elsewhere: grey capsule.
- **Back** from a tab's content opens the panel; Back on the panel leaves the app (tvOS).
- tvOS: `TabView` with `.tabViewStyle(.sidebarAdaptable)` draws exactly this on tvOS 27 hardware
  (checked: `our-device-home.png`). Keep it; the tvOS 27.2 simulator shows a top tab bar instead,
  so judge this component on the device only.
- Android: tv-material `ModalNavigationDrawer`. `drawerContent` gets the `DrawerValue`: when
  `Closed` it draws the collapsed pill at the top left, when `Open` it draws the panel. Scrim off;
  the panel is one opaque layer with the glass edge.

### Hero (Home and Details top)

Reference: `01-home.png`, `05-up-hero.png`, `13-details-top.png`.

- Full bleed backdrop, opaque. On Home it autoplays a muted trailer after a pause (screenshots show
  it black). A bottom left scrim keeps the text readable.
- Text block at x 80 (Home) or x 160 (Details card), bottom aligned:
  - Logo image, max 420 pt wide and up to 123 pt tall (Home, bottom near y 505); trim transparent edges before sizing.
  - Metadata line: 36 to 40 pt provider circle, "Movie · Action · Comedy", rating badge.
  - Overview: Caption 2, max 3 lines on Home, 4 on Details, width 540 to 660 pt.
  - Details only: tech line "2022 · 57m" plus outlined badges (4K, Dolby, CC, SDH, AD), height 22.
  - Button row, height 68: primary white capsule (306 pt wide on Home), then 66 pt circular
    buttons (add, info), then a chevron to the next hero item. 24 pt between buttons (re-measured;
    first read 64 and 26). Primary is 296 x 64 unfocused and 306 x 68 focused.
  - Caption under the primary button (price), tertiary.
- Home pager: dots centred at y 868; Left and Right on the chevron change item.
- Details: "Starring ..." block on the right at x 1350, Caption 2, secondary label then primary names.
- Box: the scrim is baked into the backdrop bitmap once (`BakedScrim` Coil transformation, commit
  `3b64836ac0`), never drawn per frame. Trailer autoplay stays off on the box (decode and fill cost).

### Shelf row

- Title: Callout bold, secondary, at the side margin; its baseline sits 22 pt above the cards (re-measured on both platforms; first read 30).
- Between the bottom of one row's cards and the next title: about 75 pt.
- Items on the column grid of section 1. Focused item scales about 1.1 from its centre; neighbours
  do not move.

### Cards

| Card | Size | Inside the card | Below the card |
|---|---|---|---|
| Continue Watching | 320 x 226 to 228: picture 180 plus a 46 to 48 pt band (re-measured) | band: play glyph, progress bar, "S1, E1 · 17m", provider badge | nothing |
| Featured 16:9 (trailers, bonus) | 410 x 230 | bottom band over a soft shade: title (Body), "▶ 3m" (Caption 2) | nothing |
| Episode | 410 x 230 | duration bottom left ("57m", Caption 2) | overline "EPISODE 2", title (Caption 1 bold), description (Caption 2, up to 5 lines), date and rating badge |
| Poster | 260 x 390 | provider badge top right | focused only: genre caption ("TV Show · Sci-Fi") inside a dark bottom band |
| Top 10 | 260 x 390 poster with a large numeral to its left | | |
| Cast | 260 circle | | name and role |
| Search result | 560 x 170 platter, corner 24 | 86 x 130 poster at 20 pt inset, title (Body bold), "Movie · Sci-Fi · 2021" (Caption 2) | |

- Focused episode: its text block gets a translucent rounded panel (white about 10%) behind it.

### Buttons and pills

- Primary: white capsule, dark text, height 68 (Home) or 64 (Details).
- Secondary: 66 pt circle, glyph only; dark translucent when unfocused (Android: plus the glass edge), white when focused.
- Season tabs: capsules height 60, Callout; selected tab grey capsule, others plain text.
- Search suggestions: capsules height 64 with a magnifier glyph.

### Empty state

Reference: `61-library.png`. Title (Headline bold) and one line of Caption 2 secondary, centred on
the screen. The nav pill stays reachable.

### Search

Reference: `38-search.png`, `39-search-results.png`.

- Field: magnifier glyph 56 pt at x 105, text at x 200 (Title 2), "Hold (mic) to dictate" on the right.
- One row keyboard at y 225 to 300: "123", "SPACE", a to z, delete. Focused key: white rounded
  square 66 x 76 with a dark letter.
- Divider at y 342 across the content width. Then suggestions, "Top Results" (3 columns of result
  cards, 40 pt between rows), then "Movies" and "TV Shows" poster rows.
- Empty query: "Recently Searched" result cards and "Browse" genre posters.
- tvOS: `.searchable` draws the field and the keyboard. Android: no system inline keyboard exists;
  build the one row keyboard in Compose (one `LazyRow` of keys) and keep the IME as fallback for
  "123".

## 4. Screens

| Screen | Order top to bottom | Notes |
|---|---|---|
| Home | nav pill; hero (logo, metadata, overview, buttons, dots); Continue Watching peeking at y 930; then featured, Top 10 and genre rows | Focus into the rows scrolls the page up so the focused row sits under the top; the backdrop gives way to the rows background |
| Details (show) | hero; Episodes (season tabs, episode cards); Trailers; Bonus Content; Related (posters); Cast & Crew (circles); How to Watch; About card; Information, Languages, Accessibility | Opens from a card as an inset card (x 120 to 1800, top 40) that grows to full screen; Left and Right on the top move to the neighbour title as a card pager |
| Details (movie) | hero with Buy and Rent; Related; How to Watch; About | `41-movie-details.png` to `49-play2.png` |
| Search | field, keyboard, results as in section 3 | |
| Library | empty state when empty | with content: not captured |
| Player | not captured | tvOS: the TV app uses the system player (AVKit). Our mpv path keeps its own chrome; match AVKit's layout when it is captured |

## 5. Motion

| Interaction | Reference behaviour | tvOS | Android TV | Tag |
|---|---|---|---|---|
| Focus moves to a card | card scales about 1.1 with a shadow, specular sheen settles | system: `.buttonStyle(.borderless)` with image hover effect, or `.card` for platters | `graphicsLayer` scale 1.0 to 1.1, `spring(dampingRatio = 1f, stiffness = 500f)` | scale `measured`, curve `unmeasured` |
| Highlight settle after a focus move | sheen animation until the frame stops changing | system | none (no specular on the box) | `measured` in the simulator: 530 to 560 ms of changing frames |
| Push to Details | inset card grows from the source card to full screen | our app uses the system push, a cross fade in place (simulator recording, 2026-11-24); no zoom | the same cross fade, no scale | `measured` in the simulator |
| Push and pop | the two screens cross fade in place, no slide; what both share (the Settings art) stays put | system | `AnimatedContent` in the shell, alpha in the draw phase without an offscreen layer: push 300 ms, pop 220 ms, `FastOutSlowInEasing`; a tab change does not fade | `measured` on our app in the simulator: Settings to About about 300 ms, Poster to Details about 220 ms, Back about 200 ms, ease in out (the probe app's system push measured 470 and 400 ms) |
| Card pager on Details | Left or Right at the top slides the neighbour title in | custom `TabView(.page)` of details cards | `HorizontalPager` | `unmeasured` |
| Sidebar open and close | panel fades and slides in from the left over the content | system | `ModalNavigationDrawer` default | `system` |
| Row scroll | focused card kept in place, row slides | system | `LazyRow` with `BringIntoViewSpec` keeping the item at the start margin | `system` |
| Home page scroll into rows | hero slides up, its art's bottom edge rises from the screen bottom to a hard edge above the first row, rows take the screen | `ScrollView` scroll target; edge animated with the focus change (`tvos/look2`, `.smooth(0.35)`) | focus bring-into-view (default `spring()`); the art's clip follows the list scroll offset over the first 240 dp, read in the draw phase; rows within a screen stay composed (`LazyLayoutCacheWindow`, idle fill) so the first Down composes no cards | `unmeasured`; box, longest Down frame (perfetto, 2026-11-24): warm 52 to 14 ms, cold 143 to 33 ms (the rest is the render thread drawing new content the first time) |

How the open motion values get measured: a capture card on the Apple TV's HDMI output, or a 240 fps
slow motion phone video of the TV, stepped frame by frame with `ffmpeg`. Until then the Android
values above are the defaults, and the tvOS app uses the system components so it already matches.

## 6. Platform mapping

| Element | tvOS | Android TV |
|---|---|---|
| Navigation | `TabView` + `.sidebarAdaptable` | `ModalNavigationDrawer` (tv-material) with pill and panel content |
| Shelf | `ScrollView(.horizontal)` + `LazyHStack`, `.scrollClipDisabled()` | `LazyRow`, `focusRestorer`, stable keys |
| Card | `Button` + `.buttonStyle(.borderless)` (image lift) or `.card` (platter) | `androidx.tv.material3.Card` or `Surface` with `scale` 1.1 |
| Primary button | `Button` + `.borderedProminent` capsule | tv-material `Button` with white container |
| Circular button | `Button` + `.buttonBorderShape(.circle)` | tv-material `IconButton` |
| Search | `.searchable` | custom one row keyboard + `BasicTextField` |
| Push to Details | system push (cross fade) | shell cross fade |
| Player | AVKit or mpv chrome | Media3 with own chrome |

## 7. Matching procedure

A screen is done when its screenshot matches the reference within 4 pt (2 dp) on every edge listed
in sections 1 to 3.

1. Open the same kind of screen on both: Apple TV (`devicectl device capture screenshot`) and the box
   (`adb exec-out screencap -p`, 1920 x 1080 px, so px / 2 = dp).
2. Scale both to 1920 x 1080 and overlay the grid:
   `ffmpeg -i in.png -vf "scale=1920:1080,drawgrid=w=40:h=40:t=1:c=red@0.5,drawgrid=w=200:h=200:t=2:c=yellow@0.8" out.png`.
3. Scan edges on the same lines: `python edges.py shot.png h <y_pt>` and `v <x_pt>` (tools in the
   captures folder; need Pillow and numpy).
4. Put the reference and our shot side by side in the PR, with the edge lists.
5. Motion: record the box with `adb shell screenrecord`, step frames with
   `ffmpeg -fps_mode passthrough -vf showinfo`, compare with the tagged values in section 5.

## 8. Corrections from the build (2026-11-24)

Values the first read got wrong, re-measured by the agents that built each screen on both
platforms; sections above are already corrected: Home rows background, nav pill and panel sizes,
logo height, circular button size and gap, row title gap, Continue Watching card height.
Still to decide: the focused poster in the TV app grows about 1.10 wide and 1.19 tall (it shows its
caption band), a uniform 1.1 scale leaves top and bottom about 16 pt short.

## 9. Open questions

- Motion of the TV app itself: needs a capture card or a slow motion phone video (section 5).
- Player chrome, Store, and a Library with content: not captured.
- Trailer autoplay on the Home hero: the TV app does it; on the box it costs decode and fill. Off on
  the box unless measured as affordable with `box/perf/measure.sh`.
