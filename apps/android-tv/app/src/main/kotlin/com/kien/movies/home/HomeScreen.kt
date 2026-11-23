package com.kien.movies.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.design.SemanticsForScreenReaders
import com.kien.movies.design.Spacing
import com.kien.movies.design.dpadRepeatThrottle
import com.kien.movies.design.prunesUnreadSemantics
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import kotlinx.coroutines.launch

/**
 * Home tab root (tvOS HomeView.swift): the hero, then Continue Watching and Upcoming, then the
 * catalog shelves, in one vertical list of sideways rows. The hero and the art run to the screen
 * edge under the closed rail; cards and text start at the rail's content line.
 *
 * Focus: entering Home (launch, Back from a pushed screen, right from the sidebar) focuses the
 * element that had focus last, else the hero's button (AC N1, N4; [HomeFocusMemory]), and each
 * row returns to its last focused card. Held arrows are throttled, and the semantics tree is pruned on the box
 * while no screen reader can read it (section 4, box performance rules).
 */
@Composable
fun HomeScreen(router: Router) {
    val viewModel = viewModel { HomeViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prune = remember(context) { context.prunesUnreadSemantics() }
    SemanticsForScreenReaders(prune = prune) {
        HomeContent(state = state, onOpen = router::open, onRemove = viewModel::removeFromContinueWatching)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(state: HomeUiState, onOpen: (AppRoute) -> Unit, onRemove: (String) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusMemory = rememberSaveable(saver = HomeFocusMemory.Saver) { HomeFocusMemory() }
    val homeFocus = remember { FocusRequester() }
    val mode = state.heroMode
    val columnSpec = rememberInsetBringIntoViewSpec(leading = ROW_LEADING_INSET, trailing = ROW_TRAILING_INSET)

    LoseNoFocusWhenTheHeroGoes(mode, homeFocus)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .dpadRepeatThrottle()
            .focusRequester(homeFocus)
            .focusProperties { onEnter = { focusMemory.restore() } }
            .focusGroup()
    ) {
        val heroHeight = maxHeight - Spacing.nextShelfPeek
        CompositionLocalProvider(LocalBringIntoViewSpec provides columnSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag(HOME_ROWS_TAG),
                contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING)
            ) {
                if (mode != HeroMode.None) {
                    item(key = HERO_KEY, contentType = HERO_KEY) {
                        HomeHero(
                            mode = mode,
                            items = state.hero,
                            statusMessage = state.errorMessage,
                            focusMemory = focusMemory,
                            onOpen = onOpen,
                            onFocused = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.height(heroHeight)
                        )
                    }
                }
                items(state.watchingRows, key = WatchingRowState::key, contentType = { WATCHING_ROW_TYPE }) { row ->
                    ContinueWatchingRow(row = row, focusMemory = focusMemory, onOpen = onOpen, onRemove = onRemove, modifier = Modifier.padding(bottom = Spacing.shelfGap))
                }
                items(state.catalogRows, key = CatalogRowState::key, contentType = { CATALOG_ROW_TYPE }) { row ->
                    CatalogRow(row = row, focusMemory = focusMemory, onOpen = onOpen, modifier = Modifier.padding(bottom = Spacing.shelfGap))
                }
            }
        }
    }
}

/**
 * When loading ends and the first row has no backdrop, the hero (and the button that held focus
 * since launch) leaves the list; focus then moves to the first card instead of being lost.
 */
@Composable
private fun LoseNoFocusWhenTheHeroGoes(mode: HeroMode, homeFocus: FocusRequester) {
    val previous = remember { arrayOf(mode) }
    LaunchedEffect(mode) {
        val wasLoading = previous[0] == HeroMode.Loading
        previous[0] = mode
        if (wasLoading && mode == HeroMode.None) {
            withFrameNanos {}
            homeFocus.requestFocus()
        }
    }
}

/** The Home list, for UI tests. */
const val HOME_ROWS_TAG = "home-rows"

private const val HERO_KEY = "hero"
private const val WATCHING_ROW_TYPE = "watching-row"
private const val CATALOG_ROW_TYPE = "catalog-row"

// tvOS bottom padding 60 pt under the last shelf.
private val LIST_BOTTOM_PADDING = 30.dp

// A focused card stays inside the overscan with its shelf header and the room for its scale in
// view: above it the top overscan, the header line and the row padding; below it the row padding
// and the bottom overscan.
private val ROW_LEADING_INSET = Spacing.screenVertical + 22.dp + Spacing.shelfRowPadding
private val ROW_TRAILING_INSET = Spacing.screenVertical + Spacing.shelfRowPadding
