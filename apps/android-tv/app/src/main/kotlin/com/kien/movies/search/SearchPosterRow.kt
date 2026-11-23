package com.kien.movies.search

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.Palette
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.design.cardArtworkShape
import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow

// Search's own copy of tvOS CatalogRowView, ShelfHeader and PosterCard. Home's CatalogRow and
// PosterCard (slice A4) are built in parallel; once they merge, Search switches to them and this
// file goes.

/** A titled shelf of poster cards that scrolls sideways (tvOS CatalogRowView). */
@Composable
internal fun SearchPosterRow(row: TvosRow, onOpen: (TvosItem) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.testTag(searchRowTag(row.key)), verticalArrangement = Arrangement.spacedBy(SHELF_HEADER_GAP)) {
        SearchShelfHeader(row.title)
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = shelfPadding,
            horizontalArrangement = Arrangement.spacedBy(Spacing.shelfItem)
        ) {
            items(row.items, key = { item -> "${item.type}:${item.metaId}" }, contentType = { "poster" }) { item ->
                SearchPosterCard(item = item, onClick = { onOpen(item) })
            }
        }
    }
}

/** tvOS ShelfHeader: 31 pt semibold, secondary. */
@Composable
internal fun SearchShelfHeader(title: String) {
    Text(
        text = title,
        style = TextStyles.shelfHeader,
        color = Palette.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = RailSize.contentStart, end = Spacing.screenHorizontal)
    )
}

/** tvOS PosterCard: the poster at the card size and shape, the first genre on a shade at the bottom. */
@Composable
private fun SearchPosterCard(item: TvosItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        modifier = Modifier.size(CardSize.poster).semantics { contentDescription = item.name },
        shape = CardFocus.shape(),
        colors = CardFocus.colors(),
        scale = CardFocus.scale(),
        border = CardFocus.border(),
        glow = CardFocus.glow(),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize().cardArtworkShape()) {
            ArtworkImage(model = item.posterUrl, modifier = Modifier.fillMaxSize(), fallbackTitle = item.name)
            item.genres.firstOrNull()?.let { genre -> GenreBand(genre, isFocused, Modifier.align(Alignment.BottomCenter)) }
        }
    }
}

/** One gradient per card, drawn only over the text band (docs section 4, "Not ported literally"). */
@Composable
private fun GenreBand(genre: String, isFocused: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = GENRE_SHADE_HEIGHT).background(genreShade),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = genre,
            style = TextStyles.cardTitle,
            color = Palette.textPrimary.copy(alpha = if (isFocused) 1f else GENRE_REST_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = GENRE_INSET, end = GENRE_INSET, bottom = GENRE_BOTTOM)
        )
    }
}

internal fun searchRowTag(key: String): String = "search-row-$key"

private val shelfPadding = PaddingValues(
    start = RailSize.contentStart,
    end = Spacing.screenHorizontal,
    top = Spacing.shelfRowPadding,
    bottom = Spacing.shelfRowPadding
)

// tvOS PosterCard and CatalogRowView literals, pt / 2.
private val SHELF_HEADER_GAP = 2.dp // VStack spacing 4
private val GENRE_SHADE_HEIGHT = 65.dp // shadeHeight 130
private val GENRE_INSET = 8.dp // horizontal padding 16
private val GENRE_BOTTOM = 11.dp // bottom padding 22
private const val GENRE_REST_ALPHA = 0.72f

private val genreShade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Black.copy(alpha = 0.6f),
    1f to Color.Black.copy(alpha = 0.88f)
)
