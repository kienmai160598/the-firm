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
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.shell.AppRoute

/** A catalog shelf of posters on 6 columns (spec sections 1 and 3, "Shelf row"). */
@Composable
fun CatalogRow(row: CatalogRowState, focusMemory: HomeFocusMemory, onOpen: (AppRoute) -> Unit, modifier: Modifier = Modifier, titleTop: Dp = ShelfMetrics.titleTop) {
    Column(modifier = modifier) {
        ShelfHeader(row.title, titleTop = titleTop)
        ShelfRow(rowKey = row.key, focusMemory = focusMemory) {
            items(row.items, key = PosterItem::metaId, contentType = { POSTER_CONTENT_TYPE }) { item ->
                PosterCard(item = item, onOpen = onOpen, modifier = Modifier.rememberedFocus(focusMemory, cardFocusKey(row.key, item.metaId), row.key))
            }
        }
    }
}

/**
 * A shelf title: Callout bold, secondary, at the side margin. Placed by its baseline:
 * [titleTop] below the end of the row above (or the hero), [ShelfMetrics.titleToCards] above the
 * cards.
 */
@Composable
fun ShelfHeader(title: String, modifier: Modifier = Modifier, titleTop: Dp = ShelfMetrics.titleTop) {
    Text(
        text = title,
        style = TvType.rowTitle,
        color = TvColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(start = TvGrid.sideMargin, end = TvGrid.sideMargin)
            .paddingFromBaseline(top = titleTop, bottom = ShelfMetrics.titleToCards)
    )
}

/**
 * Vertical rhythm of Home's shelves, measured on `09-top10-focus.png`, `03-sidebar-down.png` and
 * `01-home.png`: a title's baseline sits about 21.5 pt above its cards (the spec says 30) and
 * about 105 pt below the cards of the row above; under the hero it sits 81 pt below the slot.
 */
object ShelfMetrics {
    val titleTop: Dp = 52.5.dp
    val titleTopUnderHero: Dp = 40.5.dp
    val titleToCards: Dp = 10.75.dp
}

/**
 * The sideways list of a shelf. Cards start at the side margin, and the next card shows partly
 * at the right edge because the row scrolls under it (AC H2). Entering the row focuses its last
 * focused card ([HomeFocusMemory]), and a focused card is scrolled only as far as needed to sit
 * between the side margins. No vertical padding: a lazy row's clip leaves room across its axis
 * for the 1.1 focus scale.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    rowKey: String,
    focusMemory: HomeFocusMemory,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: LazyListScope.() -> Unit
) {
    val spec = rememberInsetBringIntoViewSpec(leading = TvGrid.sideMargin, trailing = TvGrid.sideMargin)
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { focusMemory.restoreRow(rowKey) } }
                .focusGroup(),
            contentPadding = PaddingValues(horizontal = TvGrid.sideMargin),
            horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter),
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

/**
 * A [BringIntoViewSpec] for Home's list: a focused card is centred on the screen, as every
 * scrolled capture shows (cards at 345 to 735, 426 to 652, 190 to 888 pt on a 1080 pt screen).
 * A child at least [WHOLE_FRACTION] of the screen tall (the hero slot) is aligned to the top.
 */
@OptIn(ExperimentalFoundationApi::class)
internal val CentringBringIntoViewSpec: BringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = centringScrollDistance(offset, size, containerSize)
}

/** How far to scroll so a child at [offset] with [size] is centred, or top aligned when large. */
internal fun centringScrollDistance(offset: Float, size: Float, containerSize: Float): Float = if (size >= containerSize * WHOLE_FRACTION) offset else offset + size / 2 - containerSize / 2

private const val WHOLE_FRACTION = 0.6f
private const val POSTER_CONTENT_TYPE = "poster"
