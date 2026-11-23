package com.kien.movies.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.design.darkGlassPanel
import com.kien.movies.shell.AppRoute

/**
 * Continue Watching or Upcoming on 5 columns (spec section 3). Select resumes without
 * asking (AC H4). A long press of select opens a small dialog with "Remove from Continue
 * Watching", the stand-in for the tvOS context menu; after removing, focus moves to the card's
 * neighbour (next, else previous: [RowNeighbor]) before the card leaves the row (AC H5).
 */
@Composable
fun ContinueWatchingRow(
    row: WatchingRowState,
    focusMemory: HomeFocusMemory,
    onOpen: (AppRoute) -> Unit,
    onRemove: (key: String) -> Unit,
    modifier: Modifier = Modifier,
    titleTop: Dp = ShelfMetrics.titleTop
) {
    var menuFor by remember { mutableStateOf<WatchingCard?>(null) }
    var removalAfterMenuCloses by remember { mutableStateOf<WatchingCard?>(null) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        ShelfHeader(row.title, titleTop = titleTop)
        ShelfRow(rowKey = row.key, focusMemory = focusMemory) {
            items(row.cards, key = WatchingCard::key, contentType = { WATCHING_CONTENT_TYPE }) { card ->
                ContinueWatchingCard(
                    card = card,
                    onClick = { onOpen(card.route) },
                    onLongClick = { menuFor = card },
                    modifier = Modifier.rememberedFocus(focusMemory, cardFocusKey(row.key, card.key), row.key)
                )
            }
        }
    }

    menuFor?.let { card ->
        RemoveDialog(
            title = card.title,
            onRemove = {
                menuFor = null
                removalAfterMenuCloses = card
            },
            onDismiss = { menuFor = null }
        )
    }

    // As tvOS: remove once the menu has closed and focus is back on the row, after moving focus
    // to the neighbour, so focus never falls out of the row with the removed card.
    removalAfterMenuCloses?.let { card ->
        LaunchedEffect(card.key) {
            withFrameNanos {}
            val neighbour = RowNeighbor.key(beside = card.key, keys = row.cards.map(WatchingCard::key))
            val movedToNeighbour = neighbour?.let { key -> focusMemory.focus(cardFocusKey(row.key, key)) } == true
            if (!movedToNeighbour) {
                // The last card of the row: the row goes with it, so focus leaves the row first.
                focusManager.moveFocus(FocusDirection.Down) || focusManager.moveFocus(FocusDirection.Up)
            }
            removalAfterMenuCloses = null
            onRemove(card.key)
        }
    }
}

/**
 * A Continue Watching card (spec section 3, "Cards"): 16:9 art with the show's logo, over a band
 * inside the card with the play glyph, the progress bar and "S1, E1 · 17m". Nothing below the
 * card. The band's text brightens on focus through a layer alpha, so a focus move recomposes
 * nothing.
 */
@Composable
private fun ContinueWatchingCard(card: WatchingCard, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .semantics {
                contentDescription = card.title
                stateDescription = card.progressLabel.orEmpty()
            }
            .testTag(CONTINUE_WATCHING_CARD_TAG),
        shape = CardFocus.shape(),
        colors = CardFocus.colors(),
        scale = CardFocus.scale(),
        border = CardFocus.border(),
        glow = CardFocus.glow(),
        interactionSource = interaction
    ) {
        Box(modifier = Modifier.size(WatchingCardSize)) {
            ArtworkImage(
                model = rememberBandArt(card.imageUrl),
                modifier = Modifier.fillMaxSize()
            )
            CardTitle(card, Modifier.align(Alignment.TopStart).size(WatchingCardSize.width, WatchingArtHeight))
            CardBand(card, interaction.collectIsFocusedAsState(), Modifier.align(Alignment.BottomStart))
        }
    }
}

/** The show's logo on the art, as the TV app's art carries it; the title in text without a logo. */
@Composable
private fun CardTitle(card: WatchingCard, modifier: Modifier) {
    var showsLogo by remember(card.target.logoUrl) { mutableStateOf(false) }
    Box(modifier = modifier.padding(horizontal = LOGO_INSET), contentAlignment = Alignment.CenterStart) {
        if (!showsLogo) {
            Text(
                text = card.title,
                style = TvType.caption1.copy(fontWeight = FontWeight.Bold, shadow = TITLE_SHADOW),
                color = TvColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        card.target.logoUrl?.let { logo ->
            AsyncImage(
                model = rememberSizedImage(logo, LOGO_MAX),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false },
                modifier = Modifier.sizeIn(maxWidth = LOGO_MAX.width, maxHeight = LOGO_MAX.height)
            )
        }
    }
}

@Composable
private fun CardBand(card: WatchingCard, isFocused: State<Boolean>, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .graphicsLayer {
                alpha = if (isFocused.value) 1f else BAND_REST_ALPHA
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .padding(start = BAND_START, end = BAND_START),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_home_play), contentDescription = null, tint = TvColors.textPrimary, modifier = Modifier.size(PLAY_GLYPH))
        Spacer(Modifier.width(GLYPH_GAP))
        if (!card.isNextUp) {
            WatchProgressBar(fraction = card.progressFraction, modifier = Modifier.width(BAR_WIDTH))
            Spacer(Modifier.width(BAR_GAP))
        }
        Text(
            text = card.details.joinToString(DETAIL_SEPARATOR),
            style = TvType.caption2,
            color = TvColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A capsule track with the watched part in white, one draw call each. */
@Composable
fun WatchProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(PROGRESS_HEIGHT)
            .drawBehind {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(color = PROGRESS_TRACK, cornerRadius = radius)
                val filled = size.width * fraction.coerceIn(0f, 1f)
                if (filled > 0f) drawRoundRect(color = Color.White, size = Size(filled, size.height), cornerRadius = radius)
            }
    )
}

/** The card's art, decoded at the card's size with the band baked in ([BandExtension]). */
@Composable
private fun rememberBandArt(url: String?): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, density) {
        url?.let { data ->
            val widthPx = with(density) { WatchingCardSize.width.roundToPx() }
            val heightPx = with(density) { WatchingCardSize.height.roundToPx() }
            ImageRequest.Builder(context).data(data).size(widthPx, heightPx).transformations(bandExtension).build()
        }
    }
}

/** The tvOS context menu as a small dialog: Remove first, then Cancel. Back closes it. */
@Composable
private fun RemoveDialog(title: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val removeFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .darkGlassPanel()
                .padding(DIALOG_PADDING),
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge)
        ) {
            Text(text = title, style = TextStyles.body, color = Palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onRemove,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(removeFocus)
                    .testTag(REMOVE_BUTTON_TAG)
            ) {
                Text(text = REMOVE_TITLE)
            }
            Button(
                onClick = onDismiss,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = CANCEL_TITLE)
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        removeFocus.requestFocus()
    }
}

const val CONTINUE_WATCHING_CARD_TAG = "continue-watching-card"
const val REMOVE_BUTTON_TAG = "continue-watching-remove"
const val REMOVE_TITLE = "Remove from Continue Watching"
private const val CANCEL_TITLE = "Cancel"
private const val WATCHING_CONTENT_TYPE = "watching"

private const val DETAIL_SEPARATOR = " · "
private const val BAND_REST_ALPHA = 0.75f
private const val WATCHING_COLUMNS = 5

/**
 * 5 columns on the grid. Measured on `03-sidebar-down.png`: the card is 320 x 226 pt (the spec
 * says 320 x 180, which is only its 16:9 art); the band below the art is 46 pt.
 */
internal val WatchingCardSize: DpSize = TvGrid.itemWidth(WATCHING_COLUMNS).let { width -> DpSize(width, width * 226 / 320) }
private val WatchingArtHeight = WatchingCardSize.width * 9 / 16

// Band measured on `03-sidebar-down.png`, pt / 2: text line centred 27 pt above the card's bottom;
// play glyph 14 pt at x 14; progress bar 34 x 6 pt at x 36; label at x 79; label and glyph white
// about 75% at rest, white on focus.
private val BAND_HEIGHT = 27.dp
private val BAND_START = 7.dp
private val PLAY_GLYPH = 7.dp
private val GLYPH_GAP = 4.25.dp
private val BAR_WIDTH = 17.dp
private val BAR_GAP = 4.5.dp
private val PROGRESS_HEIGHT = 3.dp
private val PROGRESS_TRACK = Color.White.copy(alpha = 0.35f)
private val LOGO_INSET = 10.dp
private val LOGO_MAX = DpSize(80.dp, 36.dp)
private val TITLE_SHADOW = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f)
private val bandExtension = BandExtension(artAspect = 16f / 9f, shade = Color.Black.copy(alpha = 0.45f), underlay = TvColors.artworkPlaceholder)
private val DIALOG_PADDING = 24.dp
