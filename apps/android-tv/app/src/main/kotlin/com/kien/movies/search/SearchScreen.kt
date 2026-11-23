package com.kien.movies.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.kien.movies.design.CardSize
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosItem

/**
 * Search tab root (tvOS SearchView.swift): the search field at the top, and under it the start
 * view (recent searches, discover row) for a blank query, a status line while a query has no rows,
 * or the result rows.
 *
 * Focus: the field is the screen's first focusable, so the shell lands on it, and it is there in
 * every state (loading, empty, error). Down from the field reaches the first row. Opening a card
 * records the query as a recent search, then opens Details.
 */
@Composable
fun SearchScreen(router: Router) {
    val viewModel = viewModel { SearchViewModel() }
    val content by viewModel.content.collectAsStateWithLifecycle()
    val fieldFocus = remember { FocusRequester() }
    val open: (TvosItem) -> Unit = { item ->
        viewModel.resultOpened()
        router.open(AppRoute.Details(type = item.type, id = item.metaId))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            state = viewModel.query,
            buttonFocus = fieldFocus,
            modifier = Modifier.padding(
                start = RailSize.contentStart,
                end = Spacing.screenHorizontal,
                top = Spacing.screenVertical,
                bottom = Spacing.textGapLarge
            )
        )
        when (val current = content) {
            is SearchContent.Start -> SearchStart(
                content = current,
                onPick = { recentQuery ->
                    fieldFocus.requestFocus()
                    viewModel.pickRecentSearch(recentQuery)
                },
                onRemove = viewModel::removeRecentSearch,
                onOpen = open,
                fieldFocus = fieldFocus
            )

            is SearchContent.Status -> SearchStatus(current)

            is SearchContent.Results -> SearchRows(current, onOpen = open)
        }
    }
}

@Composable
private fun SearchRows(content: SearchContent.Results, onOpen: (TvosItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(SEARCH_RESULTS_TAG),
        contentPadding = PaddingValues(bottom = LIST_BOTTOM),
        verticalArrangement = Arrangement.spacedBy(Spacing.shelfGap)
    ) {
        items(content.rows, key = { row -> row.key }, contentType = { "row" }) { row ->
            SearchPosterRow(row = row, onOpen = onOpen)
        }
    }
}

/** Recent searches and the discover row; removing a recent search keeps focus on a neighbour or the field. */
@Composable
private fun SearchStart(
    content: SearchContent.Start,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpen: (TvosItem) -> Unit,
    fieldFocus: FocusRequester
) {
    val recentSearches = content.recentSearches
    val recentFocus = remember(recentSearches) { recentSearches.associateWith { FocusRequester() } }
    var removalTarget by remember { mutableStateOf<String?>(null) }
    var removedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(recentSearches) {
        val index = removedIndex ?: return@LaunchedEffect
        removedIndex = null
        withFrameNanos {}
        val neighbour = recentSearches.getOrNull(index.coerceAtMost(recentSearches.lastIndex))
        val target = neighbour?.let(recentFocus::get) ?: fieldFocus
        target.requestFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LIST_BOTTOM),
        verticalArrangement = Arrangement.spacedBy(Spacing.shelfGap)
    ) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recent", contentType = "recent") {
                RecentSearchesRow(
                    queries = recentSearches,
                    focusRequesters = recentFocus,
                    onPick = onPick,
                    onLongPress = { recentQuery -> removalTarget = recentQuery }
                )
            }
        }
        content.discover?.let { discover ->
            item(key = discover.key, contentType = "row") {
                SearchPosterRow(row = discover, onOpen = onOpen, modifier = Modifier.testTag(SEARCH_DISCOVER_TAG))
            }
        }
    }

    removalTarget?.let { target ->
        RemoveRecentSearchDialog(
            query = target,
            onRemove = {
                removedIndex = recentSearches.indexOf(target)
                removalTarget = null
                onRemove(target)
            },
            onDismiss = { removalTarget = null }
        )
    }
}

/** tvOS RecentSearchesRow: one button per query; a long press offers Remove (tvOS context menu). */
@Composable
private fun RecentSearchesRow(
    queries: List<String>,
    focusRequesters: Map<String, FocusRequester>,
    onPick: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SHELF_HEADER_GAP)) {
        SearchShelfHeader(RECENT_SEARCHES_TITLE)
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(
                start = RailSize.contentStart,
                end = Spacing.screenHorizontal,
                top = RECENT_ROW_VERTICAL,
                bottom = RECENT_ROW_VERTICAL
            ),
            horizontalArrangement = Arrangement.spacedBy(RECENT_SPACING)
        ) {
            items(queries, key = { recentQuery -> recentQuery }, contentType = { "recent" }) { recentQuery ->
                val requester = focusRequesters.getValue(recentQuery)
                Button(
                    onClick = { onPick(recentQuery) },
                    onLongClick = { onLongPress(recentQuery) },
                    colors = GlassStyle.buttonColors(),
                    scale = GlassStyle.buttonScale(),
                    shape = GlassStyle.buttonShape(),
                    modifier = Modifier.focusRequester(requester).testTag(recentSearchTag(recentQuery))
                ) {
                    Text(text = recentQuery, maxLines = 1)
                }
            }
        }
    }
}

/** Long press fallback for the tvOS context menu (docs section 4: tv-material 1.1.0 has no menu). */
@Composable
private fun RemoveRecentSearchDialog(query: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val removeFocus = remember { FocusRequester() }
        Column(
            modifier = Modifier
                .background(Palette.drawerPanel, RoundedCornerShape(CardSize.cornerRadius))
                .padding(DIALOG_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge)
        ) {
            Text(text = query, style = TextStyles.shelfHeader, color = Palette.textPrimary, textAlign = TextAlign.Center)
            Button(
                onClick = onRemove,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape(),
                modifier = Modifier.focusRequester(removeFocus).testTag(SEARCH_REMOVE_TAG)
            ) {
                Text(text = REMOVE_LABEL)
            }
            Button(
                onClick = onDismiss,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape()
            ) {
                Text(text = CANCEL_LABEL)
            }
        }
        LaunchedEffect(Unit) { removeFocus.requestFocus() }
    }
}

/** tvOS SearchStatusView: a loading line or the bridge's message; the field above keeps focus. */
@Composable
private fun SearchStatus(content: SearchContent.Status) {
    val text = if (content.isLoading) LOADING_LABEL else content.message
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = STATUS_MIN_HEIGHT).padding(start = RailSize.contentStart, end = Spacing.screenHorizontal),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                style = TextStyles.body,
                color = Palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(SEARCH_STATUS_TAG)
            )
        }
    }
}

fun recentSearchTag(query: String): String = "search-recent-$query"

const val SEARCH_RESULTS_TAG = "search-results"
const val SEARCH_DISCOVER_TAG = "search-discover"
const val SEARCH_STATUS_TAG = "search-status"
const val SEARCH_REMOVE_TAG = "search-remove"

private const val RECENT_SEARCHES_TITLE = "Recent Searches"
private const val REMOVE_LABEL = "Remove"
private const val CANCEL_LABEL = "Cancel"
private const val LOADING_LABEL = "Searching"

// tvOS SearchView literals, pt / 2.
private val SHELF_HEADER_GAP = 2.dp // VStack spacing 4
private val RECENT_SPACING = 12.dp // LazyHStack spacing 24
private val RECENT_ROW_VERTICAL = 15.dp // vertical padding 30
private val LIST_BOTTOM = 30.dp // bottom padding 60
private val STATUS_MIN_HEIGHT = 150.dp // minHeight 300
private val DIALOG_PADDING = 24.dp // no tvOS literal: the context menu is a system panel
