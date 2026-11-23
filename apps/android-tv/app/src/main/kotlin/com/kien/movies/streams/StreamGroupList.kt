package com.kien.movies.streams

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.CardSize
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles

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
    Text(
        text = addonName,
        style = TextStyles.shelfHeader.copy(fontSize = ADDON_NAME_SIZE, fontWeight = FontWeight.SemiBold),
        color = Palette.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = HEADER_INSET, top = if (isFirst) 0.dp else GROUP_SPACING - ROW_SPACING, bottom = HEADER_GAP)
            .semantics { heading() }
    )
}

@Composable
private fun StreamGroupError(message: String) {
    Row(
        modifier = Modifier.padding(start = HEADER_INSET),
        horizontalArrangement = Arrangement.spacedBy(ICON_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_streams_warning),
            contentDescription = null,
            tint = Palette.textSecondary,
            modifier = Modifier.size(ERROR_SYMBOL)
        )
        Text(text = message, style = TextStyles.small, color = Palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** tvOS `.buttonStyle(.bordered)` with the card corner: frosted at rest, white when focused. */
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
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(CardSize.cornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.frostRest,
            contentColor = Palette.textPrimary,
            focusedContainerColor = Palette.focusedContainer,
            focusedContentColor = Palette.focusedContent,
            pressedContainerColor = Palette.focusedContainer,
            pressedContentColor = Palette.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = ROW_FOCUS_SCALE)
    ) {
        StreamsRow(row = row, progress = progress)
    }
}

@Composable
private fun StreamsSearchingFooter(addonCount: Int) {
    Row(
        modifier = Modifier.padding(start = HEADER_INSET, top = GROUP_SPACING - ROW_SPACING),
        horizontalArrangement = Arrangement.spacedBy(FOOTER_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamsSpinner()
        Text(text = searchingText(addonCount), style = TextStyles.small.copy(fontWeight = FontWeight.Medium), color = Palette.textSecondary)
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
    content.groups.forEachIndexed { index, group ->
        add(ListEntry.Header(group.addonId, group.addonName, isFirst = index == 0))
        group.errorMessage?.let { message -> add(ListEntry.Error(group.addonId, message)) }
        group.rows.forEach { row -> add(ListEntry.Stream(group.addonId, row)) }
    }
    if (content.searchingAddonCount > 0) add(ListEntry.Searching(content.searchingAddonCount))
    if (!content.hasRows) add(ListEntry.Retry)
}

private const val DIMMED_ALPHA = 0.45f

// A full width row grows by this much when focused: 2% of about 840 dp keeps the scaled edge
// outside the 48 dp overscan line. Focus.buttonScale (1.05) would push it across.
private const val ROW_FOCUS_SCALE = 1.02f

// tvOS literals, pt / 2. Rows start at the 48 dp floor plus tvOS's 24 pt list padding.
private val LIST_START = Spacing.screenHorizontal + 12.dp
private val LIST_TOP = 6.dp // room for the first row's focus scale
private val LIST_BOTTOM = 30.dp // padding(.bottom, 60)
private val GROUP_SPACING = 22.dp // LazyVStack spacing 44
private val ROW_SPACING = 7.dp // VStack spacing 14
private val HEADER_GAP = 2.dp // VStack spacing 18, less the row spacing
private val HEADER_INSET = 4.dp // padding(.leading, 8)
private val ADDON_NAME_SIZE = 14.sp // 27 pt semibold
private val ICON_GAP = 6.dp
private val ERROR_SYMBOL = 14.dp
private val FOOTER_GAP = 9.dp // HStack spacing 18
