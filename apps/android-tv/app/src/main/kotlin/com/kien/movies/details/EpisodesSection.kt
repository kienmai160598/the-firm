package com.kien.movies.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.Focus
import com.kien.movies.design.ScrimLayer
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.design.rememberStartAlignedRowSpec
import com.nuvio.tvos.TvosEpisode
import com.nuvio.tvos.TvosSeason
import com.nuvio.tvos.TvosStreamTarget

/**
 * What sits above the episode cards (reference `15-details-d2.png`): the title logo centred and
 * the season tabs. It lives at the bottom of the hero item and shows only once focus leaves the
 * hero, so the cards peek at the bottom of the screen in the hero state, as in the reference.
 */
@Composable
internal fun EpisodesHeader(
    name: String,
    logo: ImageRequest?,
    seasons: List<TvosSeason>,
    selectedNumber: Int?,
    onSelectSeason: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TitleArtwork(
            name = name,
            logo = logo,
            maxWidth = HEADER_LOGO_SIZE.width,
            maxHeight = HEADER_LOGO_SIZE.height,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(HEADER_LOGO_SIZE.height)
        )
        Spacer(modifier = Modifier.height(HEADER_LOGO_GAP))
        SeasonPicker(seasons = seasons, selectedNumber = selectedNumber, onSelect = onSelectSeason)
    }
}

/**
 * One capsule per season. Focus entering the row from outside lands on the selected season, not
 * on the nearest tab (tvOS focusSelectedSeasonOnEntry; AC D3). A plain scrolling Row, so the
 * selected tab is always composed and its requester attached.
 */
@Composable
private fun SeasonPicker(seasons: List<TvosSeason>, selectedNumber: Int?, onSelect: (Int) -> Unit) {
    val selectedFocus = remember { FocusRequester() }
    val hasSelected = seasons.any { season -> season.number == selectedNumber }
    Row(
        modifier = Modifier
            .focusProperties { if (hasSelected) onEnter = { selectedFocus.requestFocus() } }
            .focusGroup()
            .testTag(SEASON_PICKER_TAG)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = TvGrid.sideMargin),
        horizontalArrangement = Arrangement.spacedBy(SEASON_TAB_GAP)
    ) {
        seasons.forEach { season ->
            key(season.number) {
                val isSelected = season.number == selectedNumber
                SeasonTab(
                    title = season.title,
                    isSelected = isSelected,
                    onClick = { onSelect(season.number) },
                    modifier = Modifier
                        .then(if (isSelected) Modifier.focusRequester(selectedFocus) else Modifier)
                        .testTag(seasonTag(season.number))
                )
            }
        }
    }
}

/** Selected: grey capsule with the glass edge. Others: plain secondary text. Focused: white. */
@Composable
private fun SeasonTab(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) TvColors.platter else Color.Transparent,
            contentColor = if (isSelected) TvColors.textPrimary else TvColors.textSecondary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ButtonDefaults.scale(focusedScale = Focus.buttonScale),
        shape = ButtonDefaults.shape(shape = TvShapes.pill),
        contentPadding = PaddingValues(horizontal = SEASON_TAB_PADDING_H),
        interactionSource = interaction,
        modifier = modifier
            .height(SEASON_TAB_HEIGHT)
            .glassEdge(TvShapes.pill, visible = isSelected && !isFocused)
            .semantics { if (isSelected) stateDescription = SELECTED_SEASON_STATE }
    ) {
        Text(text = title, style = SEASON_TAB_STYLE, maxLines = 1)
    }
}

/** The selected season's episodes, four per screen width on the column grid. */
@Composable
internal fun EpisodeShelf(episodes: List<TvosEpisode>, onOpen: (TvosStreamTarget) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides rememberStartAlignedRowSpec()) {
        LazyRow(
            modifier = modifier.focusRestorer().focusGroup().testTag(EPISODE_SHELF_TAG),
            contentPadding = PaddingValues(start = TvGrid.sideMargin, end = TvGrid.sideMargin, bottom = PANEL_LIFT),
            horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter)
        ) {
            // Addon data does not promise unique video ids, and a repeated lazy key throws.
            itemsIndexed(episodes, key = { index, episode -> "$index:${episode.videoId}" }, contentType = { _, _ -> EPISODE_CONTENT_TYPE }) { _, episode ->
                EpisodeCard(episode = episode, onOpen = onOpen)
            }
        }
    }
}

/**
 * Thumbnail with the run time inside, then overline, title, description and date. On focus the
 * thumbnail lifts 1.1 and the text gets a translucent panel, lifted 1.05 and moved down clear of
 * the card (reference `c-episodes.png`). Both are draw phase changes: the layout never moves.
 */
@Composable
private fun EpisodeCard(episode: TvosEpisode, onOpen: (TvosStreamTarget) -> Unit) {
    val heading = episodeHeading(episode)
    val watchState = episodeWatchState(episode)
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val focus by animateFloatAsState(if (isFocused) 1f else 0f, tween(FOCUS_MILLIS), label = "episode-focus")
    val size = episodeCardSize()
    Column(modifier = Modifier.width(size.width)) {
        Card(
            onClick = { onOpen(episode.streamTarget) },
            shape = CardDefaults.shape(shape = TvShapes.card),
            colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            scale = CardDefaults.scale(focusedScale = Focus.cardScale),
            border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
            glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = TvColors.focusShadow, elevation = Focus.shadowElevation)),
            interactionSource = interaction,
            modifier = Modifier
                .size(size)
                .testTag(episodeTag(episode.videoId))
                .semantics {
                    contentDescription = heading
                    if (watchState.isNotEmpty()) stateDescription = watchState
                }
        ) {
            EpisodeThumbnail(episode, size)
        }
        EpisodeText(episode = episode, focus = { focus })
    }
}

@Composable
private fun EpisodeThumbnail(episode: TvosEpisode, size: DpSize) {
    val request = rememberBakedImage(episode.thumbnailUrl, size, THUMBNAIL_SHADE)
    Box(modifier = Modifier.fillMaxSize()) {
        ArtworkImage(
            model = request,
            modifier = Modifier.fillMaxSize(),
            colorFilter = if (episode.isReleased) null else unreleasedFilter
        )
        if (episode.isWatched) {
            Image(
                painter = painterResource(R.drawable.ic_details_watched),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).padding(CARD_INSET).size(WATCHED_MARK)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(CARD_INSET),
            verticalArrangement = Arrangement.spacedBy(CARD_LINE_GAP)
        ) {
            episode.runtime?.let { runtime ->
                Text(text = runtime, style = TvType.caption2, color = TvColors.textPrimary, maxLines = 1)
            }
            if (episodeShowsProgress(episode)) {
                WatchProgressBar(fraction = episode.progressFraction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The text under the card; [focus] (0 to 1) is read in the draw phase only. */
@Composable
private fun EpisodeText(episode: TvosEpisode, focus: () -> Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val progress = focus()
                val scale = 1f + (Focus.buttonScale - 1f) * progress
                scaleX = scale
                scaleY = scale
                translationY = PANEL_LIFT.toPx() * progress
            }
            .drawBehind {
                val progress = focus()
                if (progress > 0f) {
                    val radius = CornerRadius(PANEL_CORNER.toPx())
                    drawRoundRect(color = PANEL_FILL, cornerRadius = radius, alpha = progress)
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        color = TvColors.glassEdge,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = radius,
                        style = Stroke(width = stroke),
                        alpha = progress
                    )
                }
            }
            .padding(start = PANEL_PADDING_H, end = PANEL_PADDING_H, top = PANEL_PADDING_TOP, bottom = PANEL_PADDING_BOTTOM)
    ) {
        Text(text = episodeOverline(episode), style = TvType.overline, color = TvColors.textSecondary, maxLines = 1)
        Text(text = episode.title, style = EPISODE_TITLE_STYLE, color = TvColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        episode.overview?.let { overview ->
            Text(
                text = overview,
                style = TvType.caption2,
                color = TvColors.textSecondary,
                minLines = DESCRIPTION_LINES,
                maxLines = DESCRIPTION_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
        val facts = episodeFacts(episode)
        if (facts.isNotEmpty()) {
            Text(text = facts.joinToString(" · "), style = TvType.caption2, color = TvColors.textSecondary, maxLines = 1)
        }
    }
}

/** 16:9 at four columns: 201 x 113 dp. */
private fun episodeCardSize(): DpSize {
    val width = TvGrid.itemWidth(EPISODE_COLUMNS)
    return DpSize(width, width * 9 / 16)
}

fun seasonTag(number: Int): String = "season-$number"

fun episodeTag(videoId: String): String = "episode-$videoId"

const val SEASON_PICKER_TAG = "season-picker"
const val EPISODE_SHELF_TAG = "episode-shelf"
const val SELECTED_SEASON_STATE = "Selected"

private const val EPISODE_CONTENT_TYPE = "episode"
private const val EPISODE_COLUMNS = 4
private const val DESCRIPTION_LINES = 5
private const val FOCUS_MILLIS = 150

// Opacity 0.5 of an unreleased episode, as a filter on the image: no layer, no extra pass.
private val unreleasedFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(1f, 1f, 1f, 0.5f) })

// The soft shade under the run time, baked into the thumbnail once (no per frame gradient).
private val THUMBNAIL_SHADE = BakedScrim(
    underlay = TvColors.details,
    layers = listOf(ScrimLayer.vertical(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f)))
)

// Measured on 15-details-d2.png (pt / 2): logo 100 pt tall, 60 pt above the tabs; tabs 60 pt
// tall, text inset 30 pt, 12 pt apart; run time 18 pt inside the card; text 18 pt inside the
// card edge; focused panel white about 10%, lifted 31 pt.
private val HEADER_LOGO_SIZE = DpSize(190.dp, 50.dp)
private val HEADER_LOGO_GAP = 30.dp
private val SEASON_TAB_HEIGHT = 30.dp
private val SEASON_TAB_PADDING_H = 15.dp
private val SEASON_TAB_GAP = 6.dp
private val SEASON_TAB_STYLE = TvType.rowTitle.copy(fontWeight = FontWeight.SemiBold)
private val CARD_INSET = 9.dp
private val CARD_LINE_GAP = 4.dp
private val WATCHED_MARK = 22.dp
private val PANEL_PADDING_H = 9.dp
private val PANEL_PADDING_TOP = 7.dp
private val PANEL_PADDING_BOTTOM = 9.dp
private val PANEL_LIFT = 15.dp
private val PANEL_CORNER = 12.dp
private val PANEL_FILL = Color.White.copy(alpha = 0.1f)
private val EPISODE_TITLE_STYLE = TvType.caption1Emphasized
