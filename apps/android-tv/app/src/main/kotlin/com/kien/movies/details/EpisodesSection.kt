package com.kien.movies.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.FactLine
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.nuvio.tvos.TvosEpisode
import com.nuvio.tvos.TvosSeason
import com.nuvio.tvos.TvosStreamTarget

/**
 * Season picker and the selected season's episode shelf (tvOS EpisodesSection.swift). The selected
 * season lives in [DetailsViewModel], so it is kept while Streams is open (AC D3).
 */
@Composable
internal fun EpisodesSection(
    seasons: List<TvosSeason>,
    selected: TvosSeason?,
    onSelectSeason: (Int) -> Unit,
    onOpen: (TvosStreamTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SECTION_GAP_8PT)) {
        SeasonPicker(seasons = seasons, selectedNumber = selected?.number, onSelect = onSelectSeason)
        if (selected != null) {
            // A new season starts its shelf at the first episode (tvOS `.id(season.number)`).
            key(selected.number) {
                EpisodeShelf(episodes = selected.episodes, onOpen = onOpen)
            }
        }
    }
}

/**
 * One button per season. Focus entering the row from outside lands on the selected season, not
 * on the nearest button (tvOS focusSelectedSeasonOnEntry; AC D3). A plain scrolling Row, as tvOS
 * uses an HStack, so the selected button is always composed and its requester attached.
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
            .padding(horizontal = Spacing.screenHorizontal, vertical = PICKER_PADDING_12PT),
        horizontalArrangement = Arrangement.spacedBy(PICKER_SPACING_24PT)
    ) {
        seasons.forEach { season ->
            key(season.number) {
                val isSelected = season.number == selectedNumber
                SeasonButton(
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

@Composable
private fun SeasonButton(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = GlassStyle.buttonColors(),
        scale = GlassStyle.buttonScale(),
        shape = GlassStyle.buttonShape(),
        modifier = modifier.semantics { if (isSelected) stateDescription = SELECTED_SEASON_STATE }
    ) {
        if (isSelected) {
            Icon(painter = painterResource(R.drawable.ic_details_check), contentDescription = null, modifier = Modifier.size(CHECK_ICON))
            Spacer(modifier = Modifier.width(CHECK_GAP_12PT))
        }
        Text(text = title, style = TextStyles.button, maxLines = 1)
    }
}

@Composable
private fun EpisodeShelf(episodes: List<TvosEpisode>, onOpen: (TvosStreamTarget) -> Unit) {
    LazyRow(
        modifier = Modifier.focusRestorer().focusGroup().testTag(EPISODE_SHELF_TAG),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = SHELF_PADDING_24PT),
        horizontalArrangement = Arrangement.spacedBy(Spacing.shelfItem)
    ) {
        // Addon data does not promise unique video ids, and a repeated lazy key throws.
        itemsIndexed(episodes, key = { index, episode -> "$index:${episode.videoId}" }, contentType = { _, _ -> EPISODE_CONTENT_TYPE }) { _, episode ->
            EpisodeCard(episode = episode, onOpen = onOpen)
        }
    }
}

/** Thumbnail in the shared card shape, then heading, overview and facts (tvOS EpisodeCard). */
@Composable
private fun EpisodeCard(episode: TvosEpisode, onOpen: (TvosStreamTarget) -> Unit) {
    val heading = episodeHeading(episode)
    val watchState = episodeWatchState(episode)
    Column(modifier = Modifier.width(CardSize.wide.width), verticalArrangement = Arrangement.spacedBy(Spacing.textGap)) {
        Card(
            onClick = { onOpen(episode.streamTarget) },
            shape = CardFocus.shape(),
            colors = CardFocus.colors(),
            scale = CardFocus.scale(),
            border = CardFocus.border(),
            glow = CardFocus.glow(),
            modifier = Modifier
                .size(CardSize.wide)
                .testTag(episodeTag(episode.videoId))
                .semantics {
                    contentDescription = heading
                    if (watchState.isNotEmpty()) stateDescription = watchState
                }
        ) {
            EpisodeThumbnail(episode)
        }
        EpisodeCaptions(episode, heading)
    }
}

@Composable
private fun EpisodeThumbnail(episode: TvosEpisode) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(episode.thumbnailUrl, density) {
        episode.thumbnailUrl?.let { url ->
            val (width, height) = with(density) { CardSize.wide.width.roundToPx() to CardSize.wide.height.roundToPx() }
            ImageRequest.Builder(context).data(url).size(width, height).build()
        }
    }
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
                modifier = Modifier.align(Alignment.TopEnd).padding(CardSize.contentInset).size(WATCHED_MARK_44PT)
            )
        }
        if (episodeShowsProgress(episode)) {
            WatchProgressBar(
                fraction = episode.progressFraction,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(CardSize.contentInset)
            )
        }
    }
}

@Composable
private fun EpisodeCaptions(episode: TvosEpisode, heading: String) {
    Column(verticalArrangement = Arrangement.spacedBy(CAPTION_GAP_10PT)) {
        Text(text = heading, style = HEADING_STYLE, color = Palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        episode.overview?.let { overview ->
            Text(text = overview, style = TextStyles.small, color = Palette.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        val facts = episodeFacts(episode)
        if (facts.isNotEmpty()) {
            CompositionLocalProvider(LocalContentColor provides Palette.textTertiary) {
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

// Opacity 0.5 of an unreleased episode, as a filter on the image: no layer, no extra pass.
private val unreleasedFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(1f, 1f, 1f, 0.5f) })

// tvOS literals, pt / 2.
private val SECTION_GAP_8PT = 4.dp
private val PICKER_SPACING_24PT = 12.dp
private val PICKER_PADDING_12PT = 6.dp
private val CHECK_ICON = 15.dp
private val CHECK_GAP_12PT = 6.dp
private val SHELF_PADDING_24PT = 12.dp
private val WATCHED_MARK_44PT = 22.dp
private val CAPTION_GAP_10PT = 5.dp
private val HEADING_STYLE = TextStyles.cardTitle.copy(fontWeight = FontWeight.SemiBold)
private val FACTS_STYLE = TextStyles.small.copy(fontWeight = FontWeight.Medium)
