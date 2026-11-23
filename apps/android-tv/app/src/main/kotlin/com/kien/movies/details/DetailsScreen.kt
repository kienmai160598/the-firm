package com.kien.movies.details

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.design.Backdrop
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosStreamTarget
import kotlinx.coroutines.launch

/**
 * Details (tvOS DetailsView.swift): backdrop, hero, episodes, cast and information, or a loading
 * or error state that holds focus (AC D5).
 *
 * Box GPU: the window colour is the screen colour (set by the shell), the backdrop is one opaque
 * image behind the list with the one leading scrim of [Backdrop], shown only while Play has focus;
 * otherwise the backdrop recedes (tiny decode, darkened by a colour filter). No full screen
 * gradient: tvOS DetailsBackground and the bottom shade are not ported.
 *
 * Play and an episode open Streams exactly as tvOS does: `AppRoute.Streams(target)` with
 * `resumesDirectly = false`, so Streams asks Resume or Start Over (AC S5). Only Continue Watching
 * resumes directly.
 */
@Composable
fun DetailsScreen(route: AppRoute.Details, router: Router) {
    val viewModel = viewModel { DetailsViewModel(route.type, route.id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isPlayFocused by remember { mutableStateOf(false) }
    // A placeholder that held focus hands it to the content when it arrives. Not saved: when the
    // entry comes back from Streams, the shell restores the focused element instead (AC D3).
    var placeholderHadFocus by remember { mutableStateOf(false) }
    val details = (state as? DetailsScreenState.Loaded)?.details

    Box(modifier = Modifier.fillMaxSize()) {
        details?.backdropUrl?.let { url -> Backdrop(url = url, isReceded = !isPlayFocused) }
        when (val current = state) {
            // One call site for both, so the one focusable node survives Loading to Error and back.
            DetailsScreenState.Loading, is DetailsScreenState.Error -> DetailsStatus(
                errorMessage = (current as? DetailsScreenState.Error)?.message,
                onFocused = { placeholderHadFocus = true }
            )

            is DetailsScreenState.Loaded -> DetailsContent(
                state = current,
                takesFocus = placeholderHadFocus,
                onFocusTaken = { placeholderHadFocus = false },
                onPlayFocusChanged = { focused -> isPlayFocused = focused },
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

@Composable
private fun DetailsContent(
    state: DetailsScreenState.Loaded,
    takesFocus: Boolean,
    onFocusTaken: () -> Unit,
    onPlayFocusChanged: (Boolean) -> Unit,
    onOpen: (TvosStreamTarget) -> Unit,
    onSelectSeason: (Int) -> Unit
) {
    val details = state.details
    val listState = rememberLazyListState()
    val playFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val entries = remember(details) { infoEntries(details) }

    LaunchedEffect(Unit) {
        if (!takesFocus) return@LaunchedEffect
        withFrameNanos {}
        if (details.primaryAction != null) playFocus.requestFocus() else contentFocus.requestFocus()
        onFocusTaken()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().focusRequester(contentFocus).focusGroup().testTag(DETAILS_CONTENT_TAG),
        contentPadding = PaddingValues(bottom = BOTTOM_PADDING_80PT),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING_56PT)
    ) {
        item(key = HERO_KEY, contentType = HERO_KEY) {
            DetailsHero(
                details = details,
                playFocus = playFocus,
                onPlayFocusChanged = { focused ->
                    onPlayFocusChanged(focused)
                    if (focused && !listState.isAtTop()) scope.launch { listState.animateScrollToItem(0) }
                },
                onOpen = onOpen,
                modifier = Modifier.fillParentMaxHeight(HERO_SCREEN_FRACTION).padding(horizontal = Spacing.screenHorizontal)
            )
        }
        if (details.seasons.isNotEmpty()) {
            item(key = EPISODES_KEY, contentType = EPISODES_KEY) {
                EpisodesSection(
                    seasons = details.seasons,
                    selected = state.selectedSeason,
                    onSelectSeason = onSelectSeason,
                    onOpen = onOpen
                )
            }
        }
        if (details.cast.isNotEmpty()) {
            item(key = CAST_KEY, contentType = CAST_KEY) { CastSection(cast = details.cast) }
        }
        if (entries.isNotEmpty()) {
            item(key = INFO_KEY, contentType = INFO_KEY) { DetailsInfoSection(entries = entries) }
        }
    }
}

private fun LazyListState.isAtTop(): Boolean = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

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
            verticalArrangement = Arrangement.spacedBy(ERROR_GAP_24PT)
        ) {
            if (errorMessage == null) {
                LoadingSpinner()
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_details_error),
                    contentDescription = null,
                    tint = Palette.textSecondary,
                    modifier = Modifier.size(ERROR_ICON_80PT)
                )
                Text(text = errorMessage, style = ERROR_STYLE, color = Palette.textSecondary, textAlign = TextAlign.Center)
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
            color = Palette.textSecondary,
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
private const val HERO_KEY = "hero"
private const val EPISODES_KEY = "episodes"
private const val CAST_KEY = "cast"
private const val INFO_KEY = "info"

// tvOS DetailsContentView.heroScreenFraction.
private const val HERO_SCREEN_FRACTION = 0.88f

private const val FULL_TURN = 360f
private const val SPINNER_SWEEP = 270f
private const val SPINNER_TURN_MILLIS = 1_000

// tvOS literals, pt / 2.
private val SECTION_SPACING_56PT = 28.dp
private val BOTTOM_PADDING_80PT = 40.dp
private val ERROR_GAP_24PT = 12.dp
private val ERROR_ICON_80PT = 40.dp

// tvOS .headline is 38 pt semibold.
private val ERROR_STYLE = TextStyles.body.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
private val SPINNER_SIZE = 32.dp
private val SPINNER_STROKE = 3.dp
