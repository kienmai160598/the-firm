package com.kien.movies.details

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosStreamTarget
import kotlinx.coroutines.launch

/**
 * Details in the Apple TV app's arrangement (docs/tv-look-spec.md, sections 3 and 4): a full
 * screen hero over the backdrop, then Episodes, Cast & Crew, About and Information on the flat
 * details background. Or a loading or error state that holds focus (AC D5).
 *
 * Box GPU: the backdrop is one opaque image with its shades baked into the bitmap (BakedScrim),
 * shown only while the hero holds focus; below the hero the window colour is the background. No
 * gradient, blur or translucent layer is drawn per frame.
 *
 * Play and an episode open Streams exactly as tvOS does: `AppRoute.Streams(target)` with
 * `resumesDirectly = false`, so Streams asks Resume or Start Over (AC S5).
 */
@Composable
fun DetailsScreen(route: AppRoute.Details, router: Router) {
    val viewModel = viewModel { DetailsViewModel(route.type, route.id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Saved with the entry, so coming back from Streams to an episode keeps the hero hidden.
    var heroHasFocus by rememberSaveable { mutableStateOf(true) }
    // A placeholder that held focus hands it to the content when it arrives. Not saved: when the
    // entry comes back from Streams, the shell restores the focused element instead (AC D3).
    var placeholderHadFocus by remember { mutableStateOf(false) }
    val details = (state as? DetailsScreenState.Loaded)?.details

    Box(modifier = Modifier.fillMaxSize()) {
        details?.backdropUrl?.let { url -> DetailsBackdrop(url = url, isVisible = heroHasFocus) }
        when (val current = state) {
            // One call site for both, so the one focusable node survives Loading to Error and back.
            DetailsScreenState.Loading, is DetailsScreenState.Error -> DetailsStatus(
                errorMessage = (current as? DetailsScreenState.Error)?.message,
                onFocused = { placeholderHadFocus = true }
            )

            is DetailsScreenState.Loaded -> DetailsContent(
                state = current,
                heroHasFocus = heroHasFocus,
                takesFocus = placeholderHadFocus,
                onFocusTaken = { placeholderHadFocus = false },
                onHeroFocusChanged = { focused -> heroHasFocus = focused },
                onOpen = { target -> router.open(AppRoute.Streams(target)) },
                onSelectSeason = viewModel::selectSeason
            )
        }
    }

    if (details != null) {
        LaunchedEffect(details) {
            DebugLaunch.streamsRouteOnce(details)?.let(router::open)
        }
    }
}

/** The full screen art with the leading and bottom shades baked in; fades out below the hero. */
@Composable
private fun DetailsBackdrop(url: String, isVisible: Boolean) {
    val alpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(BACKDROP_FADE_MILLIS), label = "backdrop")
    if (alpha == 0f) return
    val request = rememberBakedImage(url, SCREEN_SIZE, BACKDROP_SCRIM)
    ArtworkImage(
        model = request,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                // One image: alpha on its draw, no offscreen buffer while it fades.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
    )
}

@Composable
private fun DetailsContent(
    state: DetailsScreenState.Loaded,
    heroHasFocus: Boolean,
    takesFocus: Boolean,
    onFocusTaken: () -> Unit,
    onHeroFocusChanged: (Boolean) -> Unit,
    onOpen: (TvosStreamTarget) -> Unit,
    onSelectSeason: (Int) -> Unit
) {
    val details = state.details
    val listState = rememberLazyListState()
    val playFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val aboutFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val entries = remember(details) { infoEntries(details) }
    val logo = rememberLogoRequest(details.logoUrl)
    val hasSeasons = details.seasons.isNotEmpty()
    val hasAbout = details.overview != null || details.genres.isNotEmpty()
    val sections = remember(details) { detailsSections(details, hasAbout, entries.isNotEmpty()) }

    LaunchedEffect(Unit) {
        if (!takesFocus) return@LaunchedEffect
        withFrameNanos {}
        if (details.primaryAction != null) playFocus.requestFocus() else contentFocus.requestFocus()
        onFocusTaken()
    }

    fun scrollTo(section: DetailsSection) {
        val index = sections.indexOf(section)
        val topPx = with(density) { section.top.toPx() }
        val bottomMarginPx = with(density) { OVERSCAN_BOTTOM.toPx() }
        scope.launch { listState.scrollSection(index, section, topPx, bottomMarginPx) }
    }

    // The About item may not be composed yet: bring it in, then focus it (it scrolls into place).
    fun focusAbout() {
        scope.launch {
            listState.scrollToItem(sections.indexOf(DetailsSection.About))
            withFrameNanos {}
            aboutFocus.requestFocus()
        }
    }

    // The page scrolls each section to its own place (see DetailsSection.top), not by the default
    // bring into view; rows set their own spec for their horizontal scroll.
    CompositionLocalProvider(LocalBringIntoViewSpec provides NoScrollSpec) {
        LazyColumn(
            state = listState,
            // No content padding: the hero's height is a share of the whole viewport.
            modifier = Modifier.fillMaxSize().focusRequester(contentFocus).focusGroup().testTag(DETAILS_CONTENT_TAG)
        ) {
            item(key = DetailsSection.Top.name, contentType = DetailsSection.Top.name) {
                DetailsTop(
                    details = details,
                    logo = logo,
                    selectedSeasonNumber = state.selectedSeason?.number,
                    heroHasFocus = heroHasFocus,
                    playFocus = playFocus,
                    onHeroFocusChanged = { focused ->
                        onHeroFocusChanged(focused)
                        if (focused) scrollTo(DetailsSection.Top)
                    },
                    onSeasonsFocused = { scrollTo(DetailsSection.Episodes) },
                    onOpen = onOpen,
                    onInfo = if (hasAbout) ::focusAbout else null,
                    onSelectSeason = onSelectSeason,
                    modifier = Modifier.fillParentMaxHeight(topFraction(details))
                )
            }
            if (hasSeasons) {
                item(key = DetailsSection.Episodes.name, contentType = DetailsSection.Episodes.name) {
                    val episodes = state.selectedSeason?.episodes.orEmpty()
                    // A new season starts its shelf at the first episode (tvOS `.id(season.number)`).
                    key(state.selectedSeason?.number) {
                        EpisodeShelf(episodes = episodes, onOpen = onOpen, modifier = Modifier.onSectionFocus { scrollTo(DetailsSection.Episodes) })
                    }
                }
            }
            if (details.cast.isNotEmpty()) {
                item(key = DetailsSection.Cast.name, contentType = DetailsSection.Cast.name) {
                    CastSection(cast = details.cast, modifier = Modifier.sectionSpacing().onSectionFocus { scrollTo(DetailsSection.Cast) })
                }
            }
            if (hasAbout) {
                item(key = DetailsSection.About.name, contentType = DetailsSection.About.name) {
                    AboutSection(details = details, focusRequester = aboutFocus, modifier = Modifier.sectionSpacing().onSectionFocus { scrollTo(DetailsSection.About) })
                }
            }
            if (entries.isNotEmpty()) {
                item(key = DetailsSection.Info.name, contentType = DetailsSection.Info.name) {
                    DetailsInfoSection(entries = entries, modifier = Modifier.sectionSpacing().onSectionFocus { scrollTo(DetailsSection.Info) })
                }
            }
            item(key = END_KEY, contentType = END_KEY) { Spacer(modifier = Modifier.height(BOTTOM_PADDING)) }
        }
    }
}

/**
 * The hero item: the hero text over the backdrop and, for a series, the logo and season tabs at
 * its bottom edge, right above the episode cards. The hero shows while it holds focus; the tabs
 * and logo show once focus is below it (reference `14-details-d1.png` against `15-details-d2.png`).
 */
@Composable
private fun DetailsTop(
    details: TvosMetaDetails,
    logo: ImageRequest?,
    selectedSeasonNumber: Int?,
    heroHasFocus: Boolean,
    playFocus: FocusRequester,
    onHeroFocusChanged: (Boolean) -> Unit,
    onSeasonsFocused: () -> Unit,
    onOpen: (TvosStreamTarget) -> Unit,
    onInfo: (() -> Unit)?,
    onSelectSeason: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val heroAlpha by animateFloatAsState(if (heroHasFocus) 1f else 0f, tween(CONTENT_FADE_MILLIS), label = "hero")
    Box(modifier = modifier.fillMaxWidth()) {
        DetailsHero(
            details = details,
            logo = logo,
            playFocus = playFocus,
            onOpen = onOpen,
            onInfo = onInfo,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = TvGrid.margin, end = TvGrid.margin, bottom = HERO_BOTTOM)
                .graphicsLayer { alpha = heroAlpha }
                .onFocusChanged { focusState -> onHeroFocusChanged(focusState.hasFocus) }
                .focusGroup()
        )
        if (details.seasons.isNotEmpty()) {
            EpisodesHeader(
                name = details.name,
                logo = logo,
                seasons = details.seasons,
                selectedNumber = selectedSeasonNumber,
                onSelectSeason = onSelectSeason,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = TABS_BOTTOM)
                    .graphicsLayer { alpha = 1f - heroAlpha }
                    .onSectionFocus(onSeasonsFocused)
            )
        }
    }
}

/** The sections of the page, in order, each with where its top sits when it takes focus. */
internal enum class DetailsSection(val top: Dp) {
    // Scrolled to the very top.
    Top(0.dp),

    // Episode cards at 304 pt, tabs at 210 pt (15-details-d2.png).
    Episodes(152.dp),

    // Row title at about 340 pt (17-details-d4.png, 18-details-d5.png), less the section gap.
    Cast(SECTION_TOP),
    About(SECTION_TOP),
    Info(SECTION_TOP)
}

private fun detailsSections(details: TvosMetaDetails, hasAbout: Boolean, hasInfo: Boolean): List<DetailsSection> = listOfNotNull(
    DetailsSection.Top,
    DetailsSection.Episodes.takeIf { details.seasons.isNotEmpty() },
    DetailsSection.Cast.takeIf { details.cast.isNotEmpty() },
    DetailsSection.About.takeIf { hasAbout },
    DetailsSection.Info.takeIf { hasInfo }
)

/**
 * Scrolls so item [index] starts [topPx] below the top of the list, or as far as the list goes.
 * An item that does not fit below that line is brought up so its bottom clears the overscan.
 */
private suspend fun LazyListState.scrollSection(index: Int, section: DetailsSection, topPx: Float, bottomMarginPx: Float) {
    if (index < 0) return
    if (section == DetailsSection.Top) {
        if (firstVisibleItemIndex != 0 || firstVisibleItemScrollOffset != 0) animateScrollToItem(0)
        return
    }
    if (layoutInfo.visibleItemsInfo.none { item -> item.index == index }) scrollToItem(index)
    val item = layoutInfo.visibleItemsInfo.firstOrNull { info -> info.index == index } ?: return
    val fitsBelowLine = layoutInfo.viewportEndOffset - bottomMarginPx - item.size
    val target = if (section == DetailsSection.Episodes) topPx else minOf(topPx, maxOf(fitsBelowLine, 0f))
    val distance = item.offset - target
    if (distance != 0f) animateScrollBy(distance)
}

/** Calls [onFocused] when focus enters this subtree. */
private fun Modifier.onSectionFocus(onFocused: () -> Unit): Modifier = onFocusChanged { focusState -> if (focusState.hasFocus) onFocused() }

private fun Modifier.sectionSpacing(): Modifier = padding(top = SECTION_GAP)

/**
 * The hero item's share of the screen: the episode cards (or the next shelf's cards) start at
 * 1040 pt, so only their top shows in the hero state.
 */
private fun topFraction(details: TvosMetaDetails): Float {
    val cardsTop = CARDS_PEEK_TOP
    val top = if (details.seasons.isNotEmpty()) cardsTop else cardsTop - SECTION_GAP_VALUE - SHELF_TITLE_BLOCK
    return top / SCREEN_HEIGHT
}

/** One logo decode for the hero and the episodes header. */
@Composable
private fun rememberLogoRequest(url: String?): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, density) {
        url?.let { data ->
            val (width, height) = with(density) { LOGO_DECODE.width.roundToPx() to LOGO_DECODE.height.roundToPx() }
            ImageRequest.Builder(context).data(data).size(width, height).build()
        }
    }
}

/**
 * Loading (tvOS ProgressView) or error (tvOS DetailsErrorView) as one focusable element, so the
 * remote is never stranded and focus stays put when loading turns into an error (AC D5).
 */
@Composable
private fun DetailsStatus(errorMessage: String?, onFocused: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .onFocusChanged { state -> if (state.isFocused) onFocused() }
                .focusable()
                .semantics { if (errorMessage == null) contentDescription = LOADING_DESCRIPTION }
                .testTag(if (errorMessage == null) DETAILS_LOADING_TAG else DETAILS_ERROR_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ERROR_GAP)
        ) {
            if (errorMessage == null) {
                LoadingSpinner()
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_details_error),
                    contentDescription = null,
                    tint = TvColors.textSecondary,
                    modifier = Modifier.size(ERROR_ICON)
                )
                Text(text = errorMessage, style = TvType.headline, color = TvColors.textSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

/** An arc turned in the graphics layer: the rotation is read in the draw phase, nothing recomposes. */
@Composable
private fun LoadingSpinner(modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(SPINNER_TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "spinner-rotation"
    )
    Canvas(modifier = modifier.size(SPINNER_SIZE).graphicsLayer { rotationZ = rotation }) {
        val stroke = SPINNER_STROKE.toPx()
        drawArc(
            color = TvColors.textSecondary,
            startAngle = 0f,
            sweepAngle = SPINNER_SWEEP,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

const val DETAILS_CONTENT_TAG = "details-content"
const val DETAILS_LOADING_TAG = "details-loading"
const val DETAILS_ERROR_TAG = "details-error"

private const val LOADING_DESCRIPTION = "Loading"
private const val END_KEY = "end"
private const val FULL_TURN = 360f
private const val SPINNER_SWEEP = 270f
private const val SPINNER_TURN_MILLIS = 1_000
private const val BACKDROP_FADE_MILLIS = 300
private const val CONTENT_FADE_MILLIS = 200

private val BACKDROP_SCRIM = BakedScrim.backdrop(TvColors.detailsBackground)
private val SCREEN_SIZE = DpSize(960.dp, 540.dp)
private val LOGO_DECODE = DpSize(210.dp, 55.dp)

// Measured on 14-details-d1.png and 15-details-d2.png (pt / 2): cards peek at 1040 pt, the
// button row ends at 926 pt, the tabs end 34 pt above the cards; 75 pt from one shelf's cards
// to the next title; the next title at about 340 pt once focused.
private const val SCREEN_HEIGHT = 540f
private const val CARDS_PEEK_TOP = 520f
private const val SECTION_GAP_VALUE = 37f
private const val SHELF_TITLE_BLOCK = 31f
private val SECTION_GAP = SECTION_GAP_VALUE.dp
private val SECTION_TOP = 133.dp
private val HERO_BOTTOM = 57.dp
private val TABS_BOTTOM = 17.dp
private val BOTTOM_PADDING = 40.dp
private val OVERSCAN_BOTTOM = 27.dp

private val ERROR_GAP = 12.dp
private val ERROR_ICON = 40.dp
private val SPINNER_SIZE = 32.dp
private val SPINNER_STROKE = 3.dp
