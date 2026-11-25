package com.kien.movies.streams

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.rememberInsetBringIntoViewSpec

/**
 * The streams per addon, as they load (tvOS StreamGroupList.swift): addon name, its error when it
 * found nothing, and its rows (S1). "Searching N more addons" is in the header column, as on tvOS.
 * Groups are flattened into one lazy list with stable keys, so a new addon does not rebuild the
 * rows above it.
 *
 * Android addition: when no group has a row (every addon failed), Try Again closes the list so
 * the state still holds focus.
 *
 * @param restoreKey the row to focus once it is on screen (after the player or the sheet closes);
 *   [onRestored] is called when focus was placed, on that row or, if it is gone, on the list.
 * @param contentPadding the column's margins: the list itself runs to the screen edges, so rows
 *   scroll under the top edge and a focused row's scale is never clipped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamGroupList(
    content: StreamsContent.Groups,
    selection: StreamSelection?,
    listState: LazyListState,
    restoreKey: String?,
    onRestored: () -> Unit,
    onSelect: (StreamRowItem) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
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
    // A focused row scrolls only as far as needed and rests inside the column's top and bottom
    // margins, as on tvOS, instead of the TV default's pivot line.
    val scrollSpec = rememberInsetBringIntoViewSpec(
        leading = contentPadding.calculateTopPadding(),
        trailing = contentPadding.calculateBottomPadding()
    )
    CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().focusRequester(listFocus).testTag(STREAMS_LIST_TAG),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
        ) {
            items(entries, key = ListEntry::key, contentType = ListEntry::contentType) { entry ->
                when (entry) {
                    is ListEntry.Header -> StreamAddonHeading(entry.addonName, isFirst = entry.isFirst)

                    is ListEntry.Error -> StreamGroupError(entry.message)

                    is ListEntry.Stream -> StreamButton(
                        row = entry.row,
                        progress = selection?.progressFor(entry.row.key),
                        isDimmed = selection?.dims(entry.row.key) == true,
                        restores = entry.row.key == restoreKey,
                        onRestored = onRestored,
                        onSelect = onSelect
                    )

                    ListEntry.Retry -> StreamsRetryButton(onRetry = onRetry, modifier = Modifier.padding(top = ADDON_SPACING))
                }
            }
        }
    }
}

/** The addon's name (tvOS StreamAddonHeading): 27 pt semibold, secondary, 30 pt above the next addon. */
@Composable
private fun StreamAddonHeading(addonName: String, isFirst: Boolean) {
    Text(
        text = addonName,
        style = ADDON_HEADING,
        color = TvColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = HEADING_START, bottom = HEADING_BOTTOM, top = if (isFirst) 0.dp else ADDON_SPACING)
            .semantics { heading() }
    )
}

@Composable
private fun StreamGroupError(message: String) {
    Row(
        modifier = Modifier.padding(start = HEADING_START),
        horizontalArrangement = Arrangement.spacedBy(ICON_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_streams_warning),
            contentDescription = null,
            tint = TvColors.textSecondary,
            modifier = Modifier.size(ERROR_SYMBOL)
        )
        Text(text = message, style = TvType.caption1, color = TvColors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The `.bordered` button of tvOS with its concentric corner: a platter (white 22% plus the glass edge),
 * solid white when focused. [STREAM_ROW_SHAPE] is the resolution badge radius plus its inset.
 */
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
            .testTag(streamRowTag(row.key))
            .focusLift(Focus.wideRowScale),
        shape = TvGlass.surfaceShape(STREAM_ROW_SHAPE),
        colors = TvGlass.surfaceColors(containerColor = TvColors.streamRow),
        scale = TvGlass.noSurfaceScale(),
        border = TvGlass.surfaceBorder(STREAM_ROW_SHAPE)
    ) {
        StreamsRow(row = row, progress = progress)
    }
}

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
    if (!content.hasRows) add(ListEntry.Retry)
}

private const val DIMMED_ALPHA = 0.45f

private val ADDON_HEADING = TvType.body.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold) // 27 pt semibold

// tvOS literals, pt / 2.
private val ROW_SPACING = 7.dp // LazyVStack spacing 14
private val ADDON_SPACING = 15.dp // padding(.top, 30) above every addon but the first

// Text in a row and plain text share one edge: the heading starts where the row's badge does, so
// the row platter hangs out by its own padding.
private val HEADING_START = STREAM_ROW_CONTENT_INSET
private val HEADING_BOTTOM = 2.dp // padding(.bottom, 4)
private val ICON_GAP = 6.dp
private val ERROR_SYMBOL = 14.dp
