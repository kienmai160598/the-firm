package com.kien.movies.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvSheet
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.kien.movies.design.glassEdgeBorder
import com.kien.movies.design.rememberInsetBringIntoViewSpec
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.ReportScrollToNavPill
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosItem
import kotlinx.coroutines.launch

/**
 * Search tab root, laid out as the TV app's Search (docs/tv-look-spec.md section 3 "Search",
 * 38-search.png, 39-search-results.png): the field, the one row keyboard, suggestion capsules,
 * a divider, then "Top Results" cards and the poster rows; for a blank query "Recently Searched"
 * cards and the discover row. The whole page scrolls as one list.
 *
 * Focus: the keyboard is the first focusable and lands on "a" (then on the last key used), so it
 * is there in every state (loading, empty, error). Down from the keyboard reaches the capsules or
 * the first section. Opening a card records the query as a recent search, then opens Details.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(router: Router) {
    val viewModel = viewModel { SearchViewModel() }
    val content by viewModel.content.collectAsStateWithLifecycle()
    val keyboardFocus = remember { SearchKeyboardFocus() }
    var isEditing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val open: (TvosItem) -> Unit = { item ->
        viewModel.resultOpened()
        router.open(AppRoute.Details(type = item.type, id = item.metaId))
    }
    // The picked card goes away with the content it sits in; the keyboard keeps focus meanwhile.
    val pickQuery: (String) -> Unit = { text ->
        keyboardFocus.row.requestFocus()
        viewModel.pickQuery(text)
    }
    var removalTarget by remember { mutableStateOf<String?>(null) }
    var removedIndex by remember { mutableStateOf<Int?>(null) }
    val recentSearches = (content as? SearchContent.Start)?.recentSearches.orEmpty()
    val recentFocus = remember(recentSearches) { recentSearches.associateWith { FocusRequester() } }

    LaunchedEffect(recentSearches) {
        val index = removedIndex ?: return@LaunchedEffect
        removedIndex = null
        withFrameNanos {}
        val neighbour = recentSearches.getOrNull(index.coerceAtMost(recentSearches.lastIndex))
        (neighbour?.let(recentFocus::get) ?: keyboardFocus.row).requestFocus()
    }

    // One inset spec for the page and every row in it: a focused child stays inside the side
    // margins (rows) or the margin band at top and bottom (page) with the least scroll, instead
    // of the TV default that pulls every focused child to a pivot line.
    ReportScrollToNavPill(listState)
    val scrollSpec = rememberInsetBringIntoViewSpec(leading = TvGrid.sideMargin)
    CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LIST_BOTTOM)
        ) {
            item(key = "header", contentType = "header") {
                Column(
                    modifier = Modifier.onFocusChanged { state ->
                        if (state.hasFocus) scope.launch { listState.animateScrollToItem(0) }
                    }
                ) {
                    Spacer(Modifier.height(FIELD_TOP))
                    SearchField(state = viewModel.query, isEditing = isEditing, onEditingEnd = { isEditing = false })
                    Spacer(Modifier.height(FIELD_TO_KEYBOARD))
                    SearchKeyboard(
                        focus = keyboardFocus,
                        onType = viewModel::type,
                        onDelete = viewModel::deleteLast,
                        onOpenIme = { isEditing = true }
                    )
                    Spacer(Modifier.height(KEYBOARD_BOTTOM))
                }
            }
            val suggestions = (content as? SearchContent.Results)?.suggestions.orEmpty()
            if (suggestions.isNotEmpty()) {
                item(key = "suggestions", contentType = "suggestions") {
                    SearchSuggestions(suggestions = suggestions, onPick = pickQuery, modifier = Modifier.padding(bottom = SUGGESTIONS_BOTTOM))
                }
            }
            item(key = "divider", contentType = "divider") { SearchDivider() }
            when (val current = content) {
                is SearchContent.Start -> startItems(
                    content = current,
                    recentFocus = recentFocus,
                    onPick = pickQuery,
                    onLongPress = { recentQuery -> removalTarget = recentQuery },
                    onOpen = open
                )

                is SearchContent.Status -> item(key = "status", contentType = "status") { SearchStatus(current) }

                is SearchContent.Results -> resultItems(current, onOpen = open)
            }
        }
    }

    removalTarget?.let { target ->
        RemoveRecentSearchDialog(
            query = target,
            onRemove = {
                removedIndex = recentSearches.indexOf(target)
                removalTarget = null
                viewModel.removeRecentSearch(target)
            },
            onDismiss = { removalTarget = null }
        )
    }
}

/** "Top Results" in two rows of three columns that scroll sideways, then the bridge's rows. */
private fun LazyListScope.resultItems(content: SearchContent.Results, onOpen: (TvosItem) -> Unit) {
    if (content.topResults.isNotEmpty()) {
        item(key = "top", contentType = "top") {
            SearchSection(title = TOP_RESULTS_TITLE, modifier = Modifier.padding(top = FIRST_SECTION_TOP).testTag(SEARCH_RESULTS_TAG)) {
                TopResultsGrid(items = content.topResults, onOpen = onOpen)
            }
        }
    }
    content.rows.forEachIndexed { index, row ->
        val isFirst = index == 0 && content.topResults.isEmpty()
        item(key = row.key, contentType = "row") {
            Box(modifier = Modifier.padding(top = sectionTop(isFirst)).testTag(SEARCH_RESULTS_TAG)) {
                SearchPosterRow(row = row, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun TopResultsGrid(items: List<TvosItem>, onOpen: (TvosItem) -> Unit) {
    val rows = if (items.size > RESULT_COLUMNS) TOP_RESULTS_ROWS else 1
    val ordered = remember(items, rows) { rowMajorForColumnGrid(items, rows) }
    // New results reorder the cards; a fresh state starts them at the first column instead of
    // following the old first card to wherever it moved.
    val gridState = remember(ordered) { LazyGridState() }
    LazyHorizontalGrid(
        rows = GridCells.Fixed(rows),
        state = gridState,
        modifier = Modifier.fillMaxWidth().height(gridHeight(rows)).focusRestorer(),
        contentPadding = PaddingValues(horizontal = TvGrid.sideMargin),
        horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter),
        verticalArrangement = Arrangement.spacedBy(RESULT_ROW_GAP)
    ) {
        gridItems(ordered, key = { item -> "${item.type}:${item.metaId}" }, contentType = { "result" }) { item ->
            SearchResultCard(item = item, onClick = { onOpen(item) })
        }
    }
}

/** Blank query: "Recently Searched" cards, then the discover row as the browse posters. */
private fun LazyListScope.startItems(
    content: SearchContent.Start,
    recentFocus: Map<String, FocusRequester>,
    onPick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onOpen: (TvosItem) -> Unit
) {
    if (content.recentSearches.isNotEmpty()) {
        item(key = "recent", contentType = "recent") {
            SearchSection(title = RECENT_SEARCHES_TITLE, modifier = Modifier.padding(top = FIRST_SECTION_TOP)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().focusRestorer(),
                    contentPadding = PaddingValues(horizontal = TvGrid.sideMargin),
                    horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter)
                ) {
                    items(content.recentSearches, key = { recentQuery -> recentQuery }, contentType = { "recent" }) { recentQuery ->
                        RecentSearchCard(
                            query = recentQuery,
                            onClick = { onPick(recentQuery) },
                            onLongClick = { onLongPress(recentQuery) },
                            modifier = Modifier.focusRequester(recentFocus.getValue(recentQuery)).testTag(recentSearchTag(recentQuery))
                        )
                    }
                }
            }
        }
    }
    content.discover?.let { discover ->
        item(key = discover.key, contentType = "row") {
            Box(modifier = Modifier.padding(top = sectionTop(isFirst = content.recentSearches.isEmpty())).testTag(SEARCH_DISCOVER_TAG)) {
                SearchPosterRow(row = discover, onOpen = onOpen)
            }
        }
    }
}

/** The hairline under the keyboard or the capsules, across the content width. */
@Composable
private fun SearchDivider() {
    Box(
        modifier = Modifier
            .padding(top = DIVIDER_TOP, start = TvGrid.sideMargin, end = TvGrid.sideMargin)
            .fillMaxWidth()
            .height(DIVIDER_HEIGHT)
            .background(DIVIDER_COLOR)
    )
}

/** Long press fallback for the tvOS context menu (docs section 4: tv-material 1.1.0 has no menu). */
@Composable
private fun RemoveRecentSearchDialog(query: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val removeFocus = remember { FocusRequester() }
        Column(
            modifier = Modifier
                .background(TvColors.dialogPanel, DIALOG_SHAPE)
                .glassEdge(DIALOG_SHAPE, visible = true)
                .padding(DIALOG_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DIALOG_GAP)
        ) {
            Text(text = query, style = TvType.headline, color = TvColors.textPrimary, textAlign = TextAlign.Center)
            DialogButton(label = REMOVE_LABEL, onClick = onRemove, modifier = Modifier.focusRequester(removeFocus).testTag(SEARCH_REMOVE_TAG))
            DialogButton(label = CANCEL_LABEL, onClick = onDismiss)
        }
        LaunchedEffect(Unit) { removeFocus.requestFocus() }
    }
}

/** A pill: flat fill with the glass edge, solid light and no edge when focused. */
@Composable
private fun DialogButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(TvControls.pillHeight),
        shape = ButtonDefaults.shape(shape = TvShapes.pill),
        contentPadding = TvGlass.pillPadding,
        colors = ButtonDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ButtonDefaults.scale(focusedScale = CONTROL_FOCUS_SCALE),
        border = ButtonDefaults.border(border = glassEdgeBorder(TvShapes.pill), focusedBorder = Border.None)
    ) {
        Text(text = label, style = TvType.bodyEmphasized)
    }
}

/** A loading line or the bridge's message; the keyboard above keeps focus. */
@Composable
private fun SearchStatus(content: SearchContent.Status) {
    val text = if (content.isLoading) LOADING_LABEL else content.message
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = STATUS_MIN_HEIGHT).padding(horizontal = TvGrid.sideMargin),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                style = TvType.caption1,
                color = TvColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(SEARCH_STATUS_TAG)
            )
        }
    }
}

private fun sectionTop(isFirst: Boolean): Dp = if (isFirst) FIRST_SECTION_TOP else SECTION_TOP

private fun gridHeight(rows: Int): Dp = RESULT_CARD_HEIGHT * rows + RESULT_ROW_GAP * (rows - 1)

fun recentSearchTag(query: String): String = "search-recent-$query"

const val SEARCH_RESULTS_TAG = "search-results"
const val SEARCH_DISCOVER_TAG = "search-discover"
const val SEARCH_STATUS_TAG = "search-status"
const val SEARCH_REMOVE_TAG = "search-remove"

private const val TOP_RESULTS_TITLE = "Top Results"
private const val RECENT_SEARCHES_TITLE = "Recently Searched"
private const val REMOVE_LABEL = "Remove"
private const val CANCEL_LABEL = "Cancel"
private const val LOADING_LABEL = "Searching"
private const val TOP_RESULTS_ROWS = 2

// Vertical rhythm measured on 38-search.png and 39-search-results.png (pt / 2 = dp):
// field centre y 81, keyboard keys centre y 131.5, capsules y 168.5 to 201.5, divider y 170.5
// (no capsules) or 221.5, row title cap 29.5 under the divider, cards 51 under it, 40 pt between
// card rows, 83 pt from a row's cards to the next title's cap.
private val FIELD_TOP = 62.dp
private val FIELD_TO_KEYBOARD = 16.5.dp
private val KEYBOARD_BOTTOM = 21.dp
private val SUGGESTIONS_BOTTOM = 18.dp
private val DIVIDER_TOP = 2.dp
private val DIVIDER_HEIGHT = 0.5.dp
private val DIVIDER_COLOR = TvColors.textPrimary.copy(alpha = 0.3f)

/** Divider to the top of the first row title's line box (cap 29.5 dp under the divider). */
private val FIRST_SECTION_TOP = 25.5.dp

/** A row's cards to the next title's line box (cap 41.5 dp under the cards). */
private val SECTION_TOP = 37.5.dp
private val RESULT_ROW_GAP = 20.dp
private val LIST_BOTTOM = 30.dp
private val STATUS_MIN_HEIGHT = 150.dp
private val DIALOG_PADDING = 24.dp

// Concentric with the capsule buttons: 16 dp pill radius + 24 dp padding.
private val DIALOG_SHAPE = TvSheet.shapeFor(DIALOG_PADDING)
private val DIALOG_GAP = 12.dp
