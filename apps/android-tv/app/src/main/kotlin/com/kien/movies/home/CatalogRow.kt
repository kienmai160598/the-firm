package com.kien.movies.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.design.ShelfTitleLift
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.design.liftedBy
import com.kien.movies.design.liftsShelfTitle
import com.kien.movies.design.rememberInsetBringIntoViewSpec
import com.kien.movies.design.rememberShelfTitleLift
import com.kien.movies.shell.AppRoute

/** A catalog shelf of posters on 6 columns (spec sections 1 and 3, "Shelf row"). */
@Composable
fun CatalogRow(row: CatalogRowState, focusMemory: HomeFocusMemory, onOpen: (AppRoute) -> Unit, modifier: Modifier = Modifier, titleTop: Dp = ShelfMetrics.titleTop) {
    val lift = rememberShelfTitleLift(height = PosterSize.height)
    Column(modifier = modifier) {
        ShelfHeader(row.title, titleTop = titleTop, lift = lift)
        ShelfRow(rowKey = row.key, focusMemory = focusMemory) {
            items(row.items, key = PosterItem::metaId, contentType = { POSTER_CONTENT_TYPE }) { item ->
                val cardModifier = Modifier.rememberedFocus(focusMemory, cardFocusKey(row.key, item.metaId), row.key).liftsShelfTitle(lift)
                PosterCard(item = item, onOpen = onOpen, modifier = cardModifier)
            }
        }
    }
}

/**
 * A shelf title: Callout bold, secondary, at the side margin. Placed by its baseline:
 * [titleTop] below the end of the row above (or the hero), [ShelfMetrics.titleToCards] above the
 * cards. [lift] moves it up while the card under it has focus.
 */
@Composable
fun ShelfHeader(title: String, modifier: Modifier = Modifier, titleTop: Dp = ShelfMetrics.titleTop, lift: ShelfTitleLift? = null) {
    Text(
        text = title,
        style = TvType.rowTitle,
        color = TvColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(start = TvGrid.sideMargin, end = TvGrid.sideMargin)
            .liftedBy(lift)
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

private const val POSTER_CONTENT_TYPE = "poster"
