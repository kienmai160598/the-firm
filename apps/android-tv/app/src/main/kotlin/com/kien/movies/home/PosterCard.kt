package com.kien.movies.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.shell.AppRoute

/**
 * A poster that opens Details (spec section 3, "Cards"): 2:3 on the 6 column grid, decoded at its
 * size. Focused only, "TV Show · Comedy" in a dark band inside the bottom of the card. Focus scales
 * the card by 1.1 in the draw phase (CardFocus) and shows the band through a layer alpha, so a
 * focus move recomposes nothing.
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
        Box(modifier = Modifier.size(PosterSize)) {
            ArtworkImage(
                model = rememberSizedImage(item.posterUrl, PosterSize),
                modifier = Modifier.fillMaxSize(),
                fallbackTitle = item.name
            )
            if (item.caption.isNotEmpty()) {
                CaptionBand(item.caption, interaction.collectIsFocusedAsState(), Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun CaptionBand(caption: String, isFocused: State<Boolean>, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .graphicsLayer {
                alpha = if (isFocused.value) 1f else 0f
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawBehind { drawRect(bandShade) },
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = caption,
            style = TvType.caption2,
            color = TvColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().paddingFromBaseline(bottom = CAPTION_BASELINE_TO_BOTTOM)
        )
    }
}

/** 2:3 poster on the 6 column grid (tvOS 260 x 390 pt). */
internal val PosterSize: DpSize = TvGrid.itemWidth(POSTER_COLUMNS).let { width -> DpSize(width, width * 3 / 2) }

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
private const val POSTER_COLUMNS = 6

// Measured on `10-row3.png` (focused poster, at 1.1): band shade over the bottom 60 pt, caption
// baseline 15 pt above the bottom; 7 dp unscaled.
private val BAND_HEIGHT = 32.dp
private val CAPTION_BASELINE_TO_BOTTOM = 7.dp
private val bandShade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.45f to Color.Black.copy(alpha = 0.55f),
    1f to Color.Black.copy(alpha = 0.8f)
)
