package com.kien.movies.streams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R

/**
 * The streams per addon, as they load (tvOS StreamGroupList.swift): addon name, its error when it
 * found nothing, its rows, and "Searching N more addons" while others have not answered (S1).
 * Groups are flattened into one lazy list with stable keys, so a new addon does not rebuild the
 * rows above it.
 *
 * Android addition: when no group has a row (every addon failed), Try Again closes the list so
 * the state still holds focus.
 *
 * @param restoreKey the row to focus once it is on screen (after the player or the sheet closes);
 *   [onRestored] is called when focus was placed, on that row or, if it is gone, on the list.
 */
@Composable
fun StreamGroupList(
    content: StreamsContent.Groups,
    selection: StreamSelection?,
    listState: LazyListState,
    restoreKey: String?,
    onRestored: () -> Unit,
    onSelect: (StreamRowItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = remember(content) { listEntries(content) }
    val listFocus = remember { FocusRequester() }
    LaunchedEffect(restoreKey, entries) {
        if (restoreKey == null) return@LaunchedEffect
        val index = entries.indexOfFirst { entry -> entry is ListEntry.Stream && entry.row.key == restoreKey }
        if (index < 0) {
            withFrameNanos {}
            listFocus.requestFocus()
            onRestored()
        } else if (listState.layoutInfo.visibleItemsInfo.none { item -> item.index == index }) {
            listState.scrollToItem(index)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().focusRequester(listFocus).testTag(STREAMS_LIST_TAG),
        contentPadding = PaddingValues(
            start = LIST_START,
            end = LIST_START,
            top = LIST_TOP,
            bottom = LIST_BOTTOM
        ),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        items(entries, key = ListEntry::key, contentType = ListEntry::contentType) { entry ->
            when (entry) {
                is ListEntry.Header -> StreamGroupHeader(entry.addonName, isFirst = entry.isFirst)

                is ListEntry.Error -> StreamGroupError(entry.message)

                is ListEntry.Stream -> StreamButton(
                    row = entry.row,
                    progress = selection?.progressFor(entry.row.key),
                    isDimmed = selection?.dims(entry.row.key) == true,
                    restores = entry.row.key == restoreKey,
                    onRestored = onRestored,
                    onSelect = onSelect
                )

                is ListEntry.Searching -> StreamsSearchingFooter(entry.addonCount)

                ListEntry.Retry -> StreamsRetryButton(onRetry = onRetry, modifier = Modifier.padding(top = GROUP_SPACING))
            }
        }
    }
}

@Composable
private fun StreamGroupHeader(addonName: String, isFirst: Boolean) {
    // Shelf title rhythm (spec section 3, Shelf row): baseline 15 dp above the rows, and the next
    // group's title about 37 dp under the previous rows. The list's row spacing is part of both.
    Text(
        text = addonName,
        style = TvType.rowTitle,
        color = TvColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .paddingFromBaseline(top = if (isFirst) FIRST_TITLE_BASELINE else NEXT_TITLE_BASELINE, bottom = TITLE_BASELINE_TO_ROWS)
            .semantics { heading() }
    )
}

@Composable
private fun StreamGroupError(message: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ICON_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_streams_warning),
            contentDescription = null,
            tint = TvColors.textSecondary,
            modifier = Modifier.size(ERROR_SYMBOL)
        )
        Text(text = message, style = TvType.caption2, color = TvColors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A platter (spec: white 22% plus the glass edge, panel corner), solid white when focused. */
@Composable
private fun StreamButton(
    row: StreamRowItem,
    progress: StreamRowProgress?,
    isDimmed: Boolean,
    restores: Boolean,
    onRestored: () -> Unit,
    onSelect: (StreamRowItem) -> Unit
) {
    val focus = remember { FocusRequester() }
    if (restores) {
        LaunchedEffect(Unit) {
            withFrameNanos {}
            focus.requestFocus()
            onRestored()
        }
    }
    Surface(
        onClick = { onSelect(row) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .alpha(if (isDimmed) DIMMED_ALPHA else 1f)
            .testTag(streamRowTag(row.key)),
        shape = TvGlass.surfaceShape(TvShapes.panel),
        colors = TvGlass.surfaceColors(),
        scale = TvGlass.surfaceScale(TvControls.wideRowFocusScale),
        border = TvGlass.surfaceBorder(TvShapes.panel)
    ) {
        StreamsRow(row = row, progress = progress)
    }
}

@Composable
private fun StreamsSearchingFooter(addonCount: Int) {
    Row(
        modifier = Modifier.padding(top = GROUP_SPACING - ROW_SPACING),
        horizontalArrangement = Arrangement.spacedBy(FOOTER_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamsSpinner()
        Text(text = searchingText(addonCount), style = TvType.caption2, color = TvColors.textSecondary)
    }
}

/** "Searching 1 more addon", "Searching 3 more addons" (S1). */
fun searchingText(addonCount: Int): String = if (addonCount == 1) "Searching 1 more addon" else "Searching $addonCount more addons"

fun streamRowTag(streamKey: String): String = "stream-$streamKey"

const val STREAMS_LIST_TAG = "streams-list"

/** One line of the flattened list. */
private sealed interface ListEntry {
    val key: String
    val contentType: String

    data class Header(val addonId: String, val addonName: String, val isFirst: Boolean) : ListEntry {
        override val key get() = "header:$addonId"
        override val contentType get() = "header"
    }

    data class Error(val addonId: String, val message: String) : ListEntry {
        override val key get() = "error:$addonId"
        override val contentType get() = "error"
    }

    data class Stream(val addonId: String, val row: StreamRowItem) : ListEntry {
        override val key get() = "stream:$addonId:${row.key}"
        override val contentType get() = "stream"
    }

    data class Searching(val addonCount: Int) : ListEntry {
        override val key get() = "searching"
        override val contentType get() = "searching"
    }

    data object Retry : ListEntry {
        override val key get() = "retry"
        override val contentType get() = "retry"
    }
}

private fun listEntries(content: StreamsContent.Groups): List<ListEntry> = buildList {
    // A lazy list throws on a duplicate key: an addon id seen twice gets a suffix.
    val seen = mutableMapOf<String, Int>()
    content.groups.forEachIndexed { index, group ->
        val count = (seen[group.addonId] ?: 0) + 1
        seen[group.addonId] = count
        val groupKey = if (count == 1) group.addonId else "${group.addonId}#$count"
        add(ListEntry.Header(groupKey, group.addonName, isFirst = index == 0))
        group.errorMessage?.let { message -> add(ListEntry.Error(groupKey, message)) }
        group.rows.forEach { row -> add(ListEntry.Stream(groupKey, row)) }
    }
    if (content.searchingAddonCount > 0) add(ListEntry.Searching(content.searchingAddonCount))
    if (!content.hasRows) add(ListEntry.Retry)
}

private const val DIMMED_ALPHA = 0.45f

// Rows sit on the content width (spec section 1), from the 48 dp side margin.
private val LIST_START = TvGrid.sideMargin
private val LIST_TOP = 6.dp // room for the first row's focus scale
private val LIST_BOTTOM = 30.dp // padding(.bottom, 60)
private val GROUP_SPACING = 22.dp // LazyVStack spacing 44
private val ROW_SPACING = 8.dp // VStack spacing 14 to 18
private val TITLE_BASELINE_TO_ROWS = 15.dp - ROW_SPACING // spec: baseline 30 pt above the cards
private val FIRST_TITLE_BASELINE = 16.dp // cap height of 16 sp plus a little room
private val NEXT_TITLE_BASELINE = 37.dp + 12.dp - ROW_SPACING // 75 pt to the next title's cap top
private val ICON_GAP = 6.dp
private val ERROR_SYMBOL = 14.dp
private val FOOTER_GAP = 9.dp // HStack spacing 18
