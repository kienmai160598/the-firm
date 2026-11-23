package com.kien.movies.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.design.Palette
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.shell.AppRoute

/** A catalog shelf of posters (tvOS CatalogRowView.swift). */
@Composable
fun CatalogRow(row: CatalogRowState, onOpen: (AppRoute) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CATALOG_HEADER_GAP)) {
        ShelfHeader(row.title)
        ShelfRow {
            items(row.items, key = PosterItem::metaId, contentType = { POSTER_CONTENT_TYPE }) { item ->
                PosterCard(item = item, onOpen = onOpen)
            }
        }
    }
}

/** A shelf title (tvOS `ShelfHeader`): 31 pt semibold, white 60%. */
@Composable
fun ShelfHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TextStyles.shelfHeader,
        color = Palette.textSecondary,
        maxLines = 1,
        modifier = modifier.padding(start = RailSize.contentStart, end = Spacing.screenHorizontal)
    )
}

/**
 * The sideways list of a shelf. Cards start where tab content starts and the next card shows
 * partly at the right edge (AC H2). The row remembers its last focused card (focusRestorer), and
 * a focused card is scrolled only as far as needed to sit between the rail and the right
 * overscan line, never under the closed rail.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(modifier: Modifier = Modifier, verticalAlignment: Alignment.Vertical = Alignment.Top, content: LazyListScope.() -> Unit) {
    val spec = rememberInsetBringIntoViewSpec(leading = RailSize.contentStart, trailing = Spacing.screenHorizontal)
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyRow(
            modifier = modifier.fillMaxWidth().focusRestorer().focusGroup(),
            contentPadding = PaddingValues(
                start = RailSize.contentStart,
                end = Spacing.screenHorizontal,
                top = Spacing.shelfRowPadding,
                bottom = Spacing.shelfRowPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.shelfItem),
            verticalAlignment = verticalAlignment,
            content = content
        )
    }
}

/** A [BringIntoViewSpec] that keeps a focused child between [leading] and [trailing] insets. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberInsetBringIntoViewSpec(leading: Dp, trailing: Dp): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(density, leading, trailing) {
        val leadingPx = with(density) { leading.toPx() }
        val trailingPx = with(density) { trailing.toPx() }
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = insetScrollDistance(offset, size, containerSize, leadingPx, trailingPx)
        }
    }
}

/**
 * How far to scroll so a child at [offset] with [size] sits inside the container minus the
 * insets: zero when it already does, the least distance otherwise. A child larger than the space
 * is aligned to the leading inset.
 */
internal fun insetScrollDistance(offset: Float, size: Float, containerSize: Float, leading: Float, trailing: Float): Float {
    val end = containerSize - trailing
    return when {
        offset < leading -> offset - leading
        offset + size > end -> minOf(offset + size - end, offset - leading)
        else -> 0f
    }
}

private const val POSTER_CONTENT_TYPE = "poster"

// tvOS VStack spacing 4 pt between the header and the shelf.
private val CATALOG_HEADER_GAP = 2.dp
