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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
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
import coil3.request.transformations
import coil3.size.Precision
import com.kien.movies.R
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.NoScrollSpec
import com.kien.movies.design.TrimTransparentEdges
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.kien.movies.trailer.HeroTrailer
import com.kien.movies.trailer.PlayTrailerWhile
import com.kien.movies.trailer.TrailerBackdrop
import com.kien.movies.trailer.rememberHeroTrailer
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosStreamTarget
import kotlinx.coroutines.launch

/**
 * Details as the tvOS app lays it out (DetailsView.swift): one column over the backdrop, the hero
 * in the top 88% of the screen, then the season tabs and episodes, Cast & Crew and Information,
 * 28 dp apart. Or a loading or error state that holds focus (AC D5).
 *
 * Box GPU: the backdrop is one opaque image with its shades baked into the bitmap (BakedScrim),
 * shown while the hero holds focus and faded out below it, where the window colour shows. tvOS
 * shows a blurred, dimmed copy there (`isReceded`); a baked copy measured p50 36 ms a frame on the
 * box while the page left the hero, against 24 ms without it (2026-11-24), so it waits.
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
    val trailer = rememberHeroTrailer()
    // While Play or Library has focus, as tvOS (AC D6); it stops below the hero and when Streams opens.
    PlayTrailerWhile(trailer, route.type, route.id, isDue = heroHasFocus && details != null)

    Box(modifier = Modifier.fillMaxSize()) {
        details?.backdropUrl?.let { url -> DetailsBackdrop(url = url, isVisible = heroHasFocus, trailer = trailer, itemId = route.id) }
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

/**
 * The full screen art with the leading and bottom shades baked in, faded into the title's
 * [trailer] while the hero has focus; fades out below the hero.
 */
@Composable
private fun DetailsBackdrop(url: String, isVisible: Boolean, trailer: HeroTrailer, itemId: String) {
    val alpha = animateFloatAsState(if (isVisible) 1f else 0f, tween(BACKDROP_FADE_MILLIS), label = "backdrop")
    val isGone by remember { derivedStateOf { alpha.value == 0f } }
    if (isGone) return
    val request = rememberBakedImage(url, SCREEN_SIZE, BACKDROP_SCRIM)
    TrailerBackdrop(trailer = trailer, itemId = itemId, scrim = BACKDROP_SCRIM, modifier = Modifier.fillMaxSize(), opacity = { alpha.value }) { imageModifier ->
        ArtworkImage(model = request, modifier = imageModifier)
    }
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val entries = remember(details) { infoEntries(details) }
    val logo = rememberLogoRequest(details.logoUrl)
    val hasSeasons = details.seasons.isNotEmpty()
    val sections = remember(details) { detailsSections(details, entries.isNotEmpty()) }
    // Saved with the entry: focus entering the page from outside, as when it comes back from
    // Streams, goes to this section, whose own restorer finds the card (AC D3). Without it the
    // window hands focus to Play, the first focusable, before the shell's restore runs.
    var focusedSection by rememberSaveable { mutableStateOf(DetailsSection.Top) }
    val sectionFocus = remember { DetailsSection.entries.associateWith { FocusRequester() } }

    LaunchedEffect(Unit) {
        if (!takesFocus) return@LaunchedEffect
        withFrameNanos {}
        if (details.primaryAction != null) playFocus.requestFocus() else contentFocus.requestFocus()
        onFocusTaken()
    }

    fun scrollTo(section: DetailsSection) {
        focusedSection = section
        val index = sections.indexOf(section)
        val topPx = with(density) { section.top.toPx() }
        val bottomMarginPx = with(density) { OVERSCAN_BOTTOM.toPx() }
        scope.launch { listState.scrollSection(index, section, topPx, bottomMarginPx) }
    }

    /** The section's requester on its focus group, and its scroll when focus enters it. */
    fun Modifier.section(section: DetailsSection): Modifier = focusRequester(sectionFocus.getValue(section)).onSectionFocus { scrollTo(section) }

    // The page scrolls each section to its own place (see DetailsSection.top), not by the default
    // bring into view; rows set their own spec for their horizontal scroll.
    CompositionLocalProvider(LocalBringIntoViewSpec provides NoScrollSpec) {
        LazyColumn(
            state = listState,
            // No content padding: the hero's height is a share of the whole viewport.
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(contentFocus)
                .focusProperties { if (focusedSection != DetailsSection.Top) onEnter = { sectionFocus.getValue(focusedSection).requestFocus() } }
                .focusGroup()
                .testTag(DETAILS_CONTENT_TAG)
        ) {
            item(key = DetailsSection.Top.name, contentType = DetailsSection.Top.name) {
                DetailsHero(
                    details = details,
                    logo = logo,
                    playFocus = playFocus,
                    onOpen = onOpen,
                    modifier = Modifier
                        .heroFrame()
                        .padding(horizontal = TvGrid.sideMargin)
                        .onFocusChanged { focusState ->
                            onHeroFocusChanged(focusState.hasFocus)
                            if (focusState.hasFocus) scrollTo(DetailsSection.Top)
                        }
                        .focusGroup()
                )
            }
            if (hasSeasons) {
                item(key = DetailsSection.Seasons.name, contentType = DetailsSection.Seasons.name) {
                    SeasonPicker(
                        seasons = details.seasons,
                        selectedNumber = state.selectedSeason?.number,
                        onSelect = onSelectSeason,
                        modifier = Modifier.sectionSpacing().section(DetailsSection.Seasons)
                    )
                }
                item(key = DetailsSection.Episodes.name, contentType = DetailsSection.Episodes.name) {
                    val episodes = state.selectedSeason?.episodes.orEmpty()
                    // A new season starts its shelf at the first episode (tvOS `.id(season.number)`).
                    key(state.selectedSeason?.number) {
                        EpisodeShelf(
                            episodes = episodes,
                            onOpen = onOpen,
                            modifier = Modifier.padding(top = EPISODES_GAP).section(DetailsSection.Episodes)
                        )
                    }
                }
            }
            if (details.cast.isNotEmpty()) {
                item(key = DetailsSection.Cast.name, contentType = DetailsSection.Cast.name) {
                    CastSection(cast = details.cast, modifier = Modifier.sectionSpacing().section(DetailsSection.Cast))
                }
            }
            if (entries.isNotEmpty()) {
                item(key = DetailsSection.Info.name, contentType = DetailsSection.Info.name) {
                    DetailsInfoSection(entries = entries, modifier = Modifier.sectionSpacing().section(DetailsSection.Info))
                }
            }
            item(key = END_KEY, contentType = END_KEY) { Spacer(modifier = Modifier.height(BOTTOM_PADDING)) }
        }
    }
}

/** The hero's frame, the Top item: [HERO_FRAME_HEIGHT] tall, the hero bottom aligned in it. */
private fun Modifier.heroFrame(): Modifier = layout { measurable, constraints ->
    val frame = HERO_FRAME_HEIGHT.roundToPx()
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = frame))
    layout(constraints.maxWidth, frame) { placeable.place(0, frame - placeable.height) }
}

/**
 * The sections of the page, in order, each with where its item's top sits once it takes focus,
 * measured on the tvOS simulator (pt / 2, 2026-11-24): the tabs at 828 pt with focus on them and
 * at 390 pt with focus on an episode; Cast & Crew's title near 394 pt, which is where the page
 * ends for a series. Each item starts with its 28 dp gap, the tabs 6 dp further down.
 */
internal enum class DetailsSection(val top: Dp) {
    Top(0.dp),
    Seasons(380.dp),
    Episodes(235.dp),
    Cast(SECTION_TOP),
    Info(SECTION_TOP)
}

private fun detailsSections(details: TvosMetaDetails, hasInfo: Boolean): List<DetailsSection> = listOfNotNull(
    DetailsSection.Top,
    DetailsSection.Seasons.takeIf { details.seasons.isNotEmpty() },
    DetailsSection.Episodes.takeIf { details.seasons.isNotEmpty() },
    DetailsSection.Cast.takeIf { details.cast.isNotEmpty() },
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
    val exact = section == DetailsSection.Seasons || section == DetailsSection.Episodes
    val target = if (exact) topPx else minOf(topPx, maxOf(fitsBelowLine, 0f))
    val distance = item.offset - target
    if (distance != 0f) animateScrollBy(distance)
}

/** Calls [onFocused] when focus enters this subtree. */
private fun Modifier.onSectionFocus(onFocused: () -> Unit): Modifier = onFocusChanged { focusState -> if (focusState.hasFocus) onFocused() }

private fun Modifier.sectionSpacing(): Modifier = padding(top = SECTION_GAP)

/**
 * One logo decode for the hero, its transparent margin trimmed so the logo's ink starts on the
 * Play button's edge, as on Home. Decoded at twice the logo box so the trimmed ink stays sharp
 * once fitted to the box.
 */
@Composable
private fun rememberLogoRequest(url: String?): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, density) {
        url?.let { data ->
            val (width, height) = with(density) { (LOGO_DECODE.width * LOGO_DECODE_FACTOR).roundToPx() to (LOGO_DECODE.height * LOGO_DECODE_FACTOR).roundToPx() }
            ImageRequest.Builder(context).data(data).size(width, height).precision(Precision.EXACT).transformations(TrimTransparentEdges).build()
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

private val BACKDROP_SCRIM = BakedScrim.backdrop(TvColors.details)
private val SCREEN_SIZE = DpSize(960.dp, 540.dp)

// The hero's logo box (tvOS 720 x 220 pt). Decoded at exact precision, so a small logo is scaled up
// to fit the box as tvOS `resizable().scaledToFit()` does, not left at its source size.
private val LOGO_DECODE = DpSize(360.dp, 110.dp)
private const val LOGO_DECODE_FACTOR = 2

// tvOS: 56 pt between the sections, 8 pt between the tabs and the shelf, 80 pt under the last
// plus the 60 pt bottom safe area the scroll view keeps;
// Cast & Crew's title near 394 pt once focused, less the gap.
private val SECTION_GAP = 28.dp
private val EPISODES_GAP = 4.dp
private val SECTION_TOP = 160.dp

/**
 * tvOS gives the hero 88% of the screen and bottom aligns it there (DetailsView.swift); its text
 * ends near y 905 pt on the simulator, its logo near 677 pt and its Play button at 718 pt. The hero
 * sits at the top of the Top item, bottom aligned in this frame, so a movie and a series place it
 * the same. 451 dp puts the logo and Play within 2 px of those on the TCL.
 */
private val HERO_FRAME_HEIGHT = 451.dp
private val BOTTOM_PADDING = 70.dp
private val OVERSCAN_BOTTOM = 27.dp

private val ERROR_GAP = 12.dp
private val ERROR_ICON = 40.dp
private val SPINNER_SIZE = 32.dp
private val SPINNER_STROKE = 3.dp
