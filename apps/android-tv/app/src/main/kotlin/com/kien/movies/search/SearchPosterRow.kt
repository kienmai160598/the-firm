package com.kien.movies.search

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow

// Search's own poster row and card on the spec's grid (6 columns, 2:3, corner 8 dp, focus 1.1).
// Home's CatalogRow and PosterCard get the same look from android-shell-home; once both merge,
// Search can switch to them.

/** A titled section: the row title in Callout bold, secondary, then [content] under it. */
@Composable
internal fun SearchSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = TvType.rowTitle,
            color = TvColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TvGrid.sideMargin)
        )
        Spacer(Modifier.height(TITLE_TO_ITEMS))
        content()
    }
}

/** A titled shelf of posters that scrolls sideways (6 columns of the grid). */
@Composable
internal fun SearchPosterRow(row: TvosRow, onOpen: (TvosItem) -> Unit, modifier: Modifier = Modifier) {
    SearchSection(title = row.title, modifier = modifier.testTag(searchRowTag(row.key))) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusRestorer(),
            contentPadding = PaddingValues(horizontal = TvGrid.sideMargin),
            horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter)
        ) {
            items(row.items, key = { item -> "${item.type}:${item.metaId}" }, contentType = { "poster" }) { item ->
                SearchPosterCard(item = item, onClick = { onOpen(item) })
            }
        }
    }
}

/** The poster; focused, it grows 1.1 and shows "TV Show · Sci-Fi" in a dark band at the bottom. */
@Composable
private fun SearchPosterCard(item: TvosItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        modifier = Modifier.size(posterSize()).semantics { contentDescription = item.name },
        shape = CardDefaults.shape(shape = TvShapes.card),
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = CardDefaults.scale(focusedScale = CARD_FOCUS_SCALE),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        glow = CardDefaults.glow(focusedGlow = focusShadow),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(TvShapes.card)) {
            ArtworkImage(model = item.posterUrl, modifier = Modifier.fillMaxSize(), fallbackTitle = item.name)
            if (isFocused) GenreBand(posterCaption(item), Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** One flat dark band, drawn on the focused card only. */
@Composable
private fun GenreBand(caption: String, modifier: Modifier) {
    if (caption.isEmpty()) return
    Text(
        text = caption,
        style = TvType.caption2,
        color = TvColors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .background(GENRE_BAND)
            .padding(horizontal = GENRE_INSET, vertical = GENRE_VERTICAL)
    )
}

internal fun searchRowTag(key: String): String = "search-row-$key"

/** Six columns of the grid at 2:3 (tvOS 260 x 390 pt). */
private fun posterSize(): DpSize {
    val width = TvGrid.itemWidth(POSTER_COLUMNS)
    return DpSize(width, width * POSTER_HEIGHT_RATIO)
}

/** Spec section 2: focus scale of a card. */
private const val CARD_FOCUS_SCALE = 1.1f
private const val POSTER_COLUMNS = 6
private const val POSTER_HEIGHT_RATIO = 1.5f

/** Title to the top of its cards: 43 pt from cap top to card top on 38 and 39-search-results.png. */
private val TITLE_TO_ITEMS = 5.5.dp
private val GENRE_BAND = Color.Black.copy(alpha = 0.6f)
private val GENRE_INSET = 8.dp
private val GENRE_VERTICAL = 6.dp
