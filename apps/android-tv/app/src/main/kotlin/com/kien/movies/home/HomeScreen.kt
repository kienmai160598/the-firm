package com.kien.movies.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
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
import com.kien.movies.design.dpadRepeatThrottle
import com.kien.movies.design.prunesUnreadSemantics
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.LocalNavPillState
import com.kien.movies.shell.Router

/**
 * Home tab root (docs/tv-look-spec.md, section 4, "Home"): the hero, then Continue Watching and
 * Upcoming, then the catalog shelves, in one vertical list of sideways rows. The hero's art runs
 * to the screen edge; text and cards start at the side margin, under the floating nav pill.
 *
 * Scroll: a focused card is centred on the screen ([CentringBringIntoViewSpec]), so focus into the
 * rows scrolls the page up and the rows background (the window colour) takes the screen; a focus
 * in the hero scrolls back to the top.
 *
 * Focus: entering Home (launch, Back from a pushed screen, right from the sidebar) focuses the
 * element that had focus last, else the hero's button (AC N1, N4; [HomeFocusMemory]), and each
 * row returns to its last focused card. Held arrows are throttled, and the semantics tree is
 * pruned on the box while no screen reader can read it (box performance rules).
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
    ReportsScrollToNavPill(listState)
    val focusMemory = rememberSaveable(saver = HomeFocusMemory.Saver) { HomeFocusMemory() }
    val homeFocus = remember { FocusRequester() }
    val heroHasFocus = remember { mutableStateOf(false) }
    val mode = state.heroMode

    LoseNoFocusWhenTheHeroGoes(mode, homeFocus)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dpadRepeatThrottle()
            .focusRequester(homeFocus)
            .focusProperties { onEnter = { focusMemory.restore() } }
            .focusGroup()
    ) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides CentringBringIntoViewSpec) {
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
                            showsArtBelow = heroHasFocus,
                            onFocusChanged = { hasFocus -> heroHasFocus.value = hasFocus },
                            scrollOffset = { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 0f }
                        )
                    }
                }
                val firstTitleTop = if (mode == HeroMode.None) ShelfMetrics.titleTop else ShelfMetrics.titleTopUnderHero
                itemsIndexed(state.watchingRows, key = { _, row -> row.key }, contentType = { _, _ -> WATCHING_ROW_TYPE }) { index, row ->
                    ContinueWatchingRow(
                        row = row,
                        focusMemory = focusMemory,
                        onOpen = onOpen,
                        onRemove = onRemove,
                        titleTop = if (index == 0) firstTitleTop else ShelfMetrics.titleTop
                    )
                }
                val catalogsFirst = state.watchingRows.isEmpty()
                itemsIndexed(state.catalogRows, key = { _, row -> row.key }, contentType = { _, _ -> CATALOG_ROW_TYPE }) { index, row ->
                    CatalogRow(
                        row = row,
                        focusMemory = focusMemory,
                        onOpen = onOpen,
                        titleTop = if (catalogsFirst && index == 0) firstTitleTop else ShelfMetrics.titleTop
                    )
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

// Room under the last shelf so its posters can be centred too: half the screen minus half a poster.
private val LIST_BOTTOM_PADDING = 175.dp

/** Hides the shell's collapsed pill while the page is scrolled away from the hero (RootTabs). */
@Composable
private fun ReportsScrollToNavPill(listState: LazyListState) {
    val pillState = LocalNavPillState.current
    LaunchedEffect(listState, pillState) {
        snapshotFlow { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
            .collect { isScrolled -> pillState.isContentScrolled = isScrolled }
    }
    DisposableEffect(pillState) {
        onDispose { pillState.isContentScrolled = false }
    }
}
