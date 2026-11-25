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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.FactLine
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.kien.movies.design.rememberStartAlignedRowSpec
import com.nuvio.tvos.TvosEpisode
import com.nuvio.tvos.TvosSeason
import com.nuvio.tvos.TvosStreamTarget

/**
 * One glass capsule per season, the selected one with a checkmark (tvOS EpisodesSection.swift
 * `SeasonPicker`). Focus entering the row from outside lands on the selected season, not on the
 * nearest tab (tvOS focusSelectedSeasonOnEntry; AC D3). A plain scrolling Row, so the selected
 * tab is always composed and its requester attached.
 */
@Composable
internal fun SeasonPicker(seasons: List<TvosSeason>, selectedNumber: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedFocus = remember { FocusRequester() }
    val hasSelected = seasons.any { season -> season.number == selectedNumber }
    Row(
        modifier = modifier
            .focusProperties { if (hasSelected) onEnter = { selectedFocus.requestFocus() } }
            .focusGroup()
            .testTag(SEASON_PICKER_TAG)
            .horizontalScroll(rememberScrollState())
            .padding(start = TvGrid.sideMargin - SEASON_TAB_PADDING_H, end = TvGrid.sideMargin, top = SEASON_ROW_PADDING_V, bottom = SEASON_ROW_PADDING_V),
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

/** A glass button (tvOS `.glass`): dark capsule with the glass edge, white when focused. */
@Composable
private fun SeasonTab(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        colors = controlColors(),
        scale = ButtonDefaults.scale(focusedScale = Focus.buttonScale),
        shape = ButtonDefaults.shape(shape = TvShapes.pill),
        contentPadding = PaddingValues(horizontal = SEASON_TAB_PADDING_H),
        interactionSource = interaction,
        modifier = modifier
            .height(SEASON_TAB_HEIGHT)
            .glassEdge(TvShapes.pill, visible = !isFocused)
            .semantics { if (isSelected) stateDescription = SELECTED_SEASON_STATE }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SEASON_CHECK_GAP), verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(painter = painterResource(R.drawable.ic_player_check), contentDescription = null, modifier = Modifier.size(SEASON_CHECK))
            }
            Text(text = title, style = SEASON_TAB_STYLE, maxLines = 1)
        }
    }
}

/** The selected season's episodes (tvOS `EpisodeShelf`): 200 dp cards, 20 dp apart. */
@Composable
internal fun EpisodeShelf(episodes: List<TvosEpisode>, onOpen: (TvosStreamTarget) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides rememberStartAlignedRowSpec()) {
        LazyRow(
            modifier = modifier.focusRestorer().focusGroup().testTag(EPISODE_SHELF_TAG),
            contentPadding = PaddingValues(horizontal = TvGrid.sideMargin, vertical = SHELF_PADDING_V),
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
 * Thumbnail, then "E1  Pilot", three lines of overview and the facts line (tvOS `EpisodeCard`).
 * On focus the thumbnail lifts 1.1 and the text moves down with its edge, in the draw phase.
 */
@Composable
private fun EpisodeCard(episode: TvosEpisode, onOpen: (TvosStreamTarget) -> Unit) {
    val heading = episodeHeading(episode)
    val watchState = episodeWatchState(episode)
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val focus by animateFloatAsState(if (isFocused) 1f else 0f, tween(Focus.animationMillis), label = "episode-focus")
    Column(modifier = Modifier.width(EPISODE_SIZE.width)) {
        Card(
            onClick = { onOpen(episode.streamTarget) },
            shape = CardDefaults.shape(shape = EPISODE_SHAPE),
            colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            scale = CardDefaults.scale(focusedScale = Focus.cardScale),
            border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
            glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = TvColors.focusShadow, elevation = Focus.shadowElevation)),
            interactionSource = interaction,
            modifier = Modifier
                .size(EPISODE_SIZE)
                .testTag(episodeTag(episode.videoId))
                .semantics {
                    contentDescription = heading
                    if (watchState.isNotEmpty()) stateDescription = watchState
                }
        ) {
            EpisodeThumbnail(episode)
        }
        EpisodeText(episode = episode, heading = heading, modifier = Modifier.graphicsLayer { translationY = focus * (Focus.cardScale - 1f) * EPISODE_SIZE.height.toPx() / 2f })
    }
}

@Composable
private fun EpisodeThumbnail(episode: TvosEpisode) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(episode.thumbnailUrl, density) {
        episode.thumbnailUrl?.let { url ->
            with(density) { ImageRequest.Builder(context).data(url).size(EPISODE_SIZE.width.roundToPx(), EPISODE_SIZE.height.roundToPx()).build() }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ArtworkImage(model = request, modifier = Modifier.fillMaxSize(), colorFilter = if (episode.isReleased) null else unreleasedFilter)
        if (episode.isWatched) {
            Image(
                painter = painterResource(R.drawable.ic_details_watched),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).padding(CARD_INSET).size(WATCHED_MARK)
            )
        }
        if (episodeShowsProgress(episode)) {
            WatchProgressBar(fraction = episode.progressFraction, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(CARD_INSET))
        }
    }
}

@Composable
private fun EpisodeText(episode: TvosEpisode, heading: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = CAPTION_GAP), verticalArrangement = Arrangement.spacedBy(CAPTION_LINE_GAP)) {
        Text(text = heading, style = HEADING_STYLE, color = TvColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        episode.overview?.let { overview ->
            Text(text = overview, style = OVERVIEW_STYLE, color = OVERVIEW_COLOR, maxLines = OVERVIEW_LINES, overflow = TextOverflow.Ellipsis)
        }
        val facts = episodeFacts(episode)
        if (facts.isNotEmpty()) {
            CompositionLocalProvider(LocalContentColor provides FACTS_COLOR) {
                ProvideTextStyle(FACTS_STYLE) { FactLine(facts) }
            }
        }
    }
}

fun seasonTag(number: Int): String = "season-$number"

fun episodeTag(videoId: String): String = "episode-$videoId"

const val SEASON_PICKER_TAG = "season-picker"
const val EPISODE_SHELF_TAG = "episode-shelf"
const val SELECTED_SEASON_STATE = "Selected"

private const val EPISODE_CONTENT_TYPE = "episode"
private const val OVERVIEW_LINES = 3

// Opacity 0.5 of an unreleased episode, as a filter on the image: no layer, no extra pass.
private val unreleasedFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(1f, 1f, 1f, 0.5f) })

// tvOS (pt / 2, simulator 2026-11-24): tabs 68 pt tall, 24 pt apart, the row 12 pt above and
// below; check 12 pt before the title. Cards 400 x 225 pt with a 24 pt corner, the shelf 24 pt
// above and below; text 22 pt under the card, lines 10 pt apart: heading 25 pt semibold,
// overview 21 pt white 60%, facts 20 pt medium white 45%. Marks 20 pt inside the card.
private val SEASON_TAB_HEIGHT = 34.dp
private val SEASON_TAB_PADDING_H = 15.dp
private val SEASON_TAB_GAP = 12.dp
private val SEASON_ROW_PADDING_V = 6.dp
private val SEASON_CHECK = 16.dp
private val SEASON_CHECK_GAP = 6.dp
private val SEASON_TAB_STYLE = TvType.body
private val EPISODE_SIZE = DpSize(200.dp, 112.5.dp)
private val EPISODE_SHAPE = RoundedCornerShape(12.dp)
private val SHELF_PADDING_V = 12.dp
private val CARD_INSET = 10.dp
private val WATCHED_MARK = 22.dp
private val CAPTION_GAP = 11.dp
private val CAPTION_LINE_GAP = 5.dp
private val HEADING_STYLE = TvType.body.copy(fontSize = 12.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold)
private val OVERVIEW_STYLE = TvType.body.copy(fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.Normal)
private val OVERVIEW_COLOR = Color.White.copy(alpha = 0.6f)
private val FACTS_STYLE = TvType.body.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
private val FACTS_COLOR = Color.White.copy(alpha = 0.45f)
