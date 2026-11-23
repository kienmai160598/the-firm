package com.kien.movies.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.TextStyles
import com.kien.movies.shell.AppRoute

/**
 * A poster that opens Details (tvOS PosterCard.swift): the art at 130 x 195 dp, decoded at that
 * size, and the first genre over one shade on the bottom band. Focus scales the card in the draw
 * phase (CardFocus); the genre's brightness follows focus in the draw phase too, so a focus move
 * recomposes nothing.
 */
@Composable
fun PosterCard(item: PosterItem, onOpen: (AppRoute) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = { onOpen(item.route) },
        modifier = modifier
            .semantics { contentDescription = item.name }
            .testTag(POSTER_CARD_TAG),
        shape = CardFocus.shape(),
        colors = CardFocus.colors(),
        scale = CardFocus.scale(),
        border = CardFocus.border(),
        glow = CardFocus.glow(),
        interactionSource = interaction
    ) {
        Box(modifier = Modifier.size(CardSize.poster)) {
            ArtworkImage(
                model = rememberSizedImage(item.posterUrl, CardSize.poster),
                modifier = Modifier.fillMaxSize(),
                fallbackTitle = item.name
            )
            item.genre?.let { genre ->
                GenreBand(genre, interaction.collectIsFocusedAsState(), Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun GenreBand(genre: String, isFocused: State<Boolean>, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GENRE_SHADE_HEIGHT)
            .drawBehind { drawRect(genreShade) },
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = genre,
            style = TextStyles.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = GENRE_INSET, end = GENRE_INSET, bottom = GENRE_BOTTOM)
                .graphicsLayer { alpha = if (isFocused.value) 1f else GENRE_REST_ALPHA }
        )
    }
}

/**
 * A Coil request decoded at [size] in pixels, so a card never decodes the full resolution art.
 * Null when there is no URL (the artwork shows its fallback).
 */
@Composable
internal fun rememberSizedImage(url: String?, size: DpSize): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, size, density) {
        url?.let { data ->
            val widthPx = with(density) { size.width.roundToPx() }
            val heightPx = with(density) { size.height.roundToPx() }
            ImageRequest.Builder(context).data(data).size(widthPx, heightPx).build()
        }
    }
}

const val POSTER_CARD_TAG = "poster-card"

// tvOS genre shade: 130 pt band, clear to black 60% at the middle to 88% at the bottom; label
// 26 pt medium, 16 pt side and 22 pt bottom padding, white 72% at rest.
private val GENRE_SHADE_HEIGHT = 65.dp
private val GENRE_INSET = 8.dp
private val GENRE_BOTTOM = 11.dp
private const val GENRE_REST_ALPHA = 0.72f
private val genreShade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Black.copy(alpha = 0.6f),
    1f to Color.Black.copy(alpha = 0.88f)
)
