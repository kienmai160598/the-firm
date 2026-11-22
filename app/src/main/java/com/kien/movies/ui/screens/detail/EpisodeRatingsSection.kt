package com.kien.movies.ui.screens.detail

import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.kien.movies.R
import com.kien.movies.domain.model.Video
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.moviesCardBorder
import com.kien.movies.ui.components.expressive.moviesCardScale


// The episode label grows to the TV floor on the box, so its cell grows to keep both lines.
private val RatingCellHeight = 52.dp

// A 16 sp label is 20 sp tall, so 14 dp above and below makes the 48 dp TV control height.
private val SeasonChipPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EpisodeRatingsSection(
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, Double>,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    title: String = "Ratings",
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    firstItemFocusRequester: FocusRequester? = null,
    ratingsGridFocusRequester: FocusRequester? = null
) {
    val seasonNumbers = remember(episodes) {
        episodes
            .mapNotNull { it.season }
            .filter { it > 0 } // Never show specials season (S0)
            .distinct()
            .sorted()
    }
    val seasonSignature = remember(seasonNumbers) { seasonNumbers.joinToString(",") }
    val seasonFocusRequesters = remember(seasonNumbers) {
        seasonNumbers.associateWith { FocusRequester() }
    }
    val internalRatingsGridFocusRequester = remember { FocusRequester() }
    val effectiveRatingsGridFocusRequester = ratingsGridFocusRequester ?: internalRatingsGridFocusRequester
    val firstEpisodeRatingFocusRequester = remember { FocusRequester() }
    val defaultSeason = remember(seasonNumbers) {
        seasonNumbers.firstOrNull { it > 0 } ?: seasonNumbers.firstOrNull() ?: 0
    }
    var selectedSeason by rememberSaveable(seasonSignature) {
        mutableIntStateOf(defaultSeason)
    }

    LaunchedEffect(seasonNumbers, defaultSeason) {
        if (selectedSeason !in seasonNumbers) {
            selectedSeason = defaultSeason
        }
    }

    val episodesForSeason = remember(episodes, selectedSeason) {
        episodes
            .filter { it.season == selectedSeason && it.episode != null }
            .distinctBy { it.season to it.episode }
            .sortedBy { it.episode }
    }
    val defaultChipColor = MoviesTheme.colors.BackgroundCard
    val defaultChipTextColor = MoviesTheme.colors.TextSecondary
    val seasonRatings = remember(episodesForSeason, ratings) {
        episodesForSeason.mapNotNull { episode ->
            val season = episode.season ?: return@mapNotNull null
            val episodeNumber = episode.episode ?: return@mapNotNull null
            val rating = ratings[season to episodeNumber]
            val ratingText = rating?.let { String.format("%.1f", it) } ?: "—"
            val chipColor = rating?.let(::ratingColor) ?: defaultChipColor
            val chipTextColor = rating?.let(::ratingTextColor) ?: defaultChipTextColor
            EpisodeRatingChipUi(
                id = episode.id,
                seasonNumber = season,
                episodeNumber = episodeNumber,
                ratingText = ratingText,
                chipColor = chipColor,
                chipTextColor = chipTextColor
            )
        }
    }
    val hasTitle = title.isNotBlank()
    val currentUpFocusRequester by rememberUpdatedState(upFocusRequester)
    val currentDownFocusRequester by rememberUpdatedState(downFocusRequester)
    val selectedSeasonRequester = firstItemFocusRequester
        ?: seasonFocusRequesters[selectedSeason]
        ?: FocusRequester.Default

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (hasTitle) 14.dp else 6.dp, bottom = MoviesTheme.spacing.sm)
    ) {
        if (hasTitle) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MoviesTheme.colors.TextPrimary,
                modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xxxl)
            )
        }

        when {
            isLoading -> {
                Text(
                    text = stringResource(R.string.ratings_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.md)
                )
            }
            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.md)
                )
            }
            seasonNumbers.isEmpty() -> {
                Text(
                    text = stringResource(R.string.ratings_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.md)
                )
            }
            else -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer { selectedSeasonRequester }
                        .focusGroup(),
                    contentPadding = PaddingValues(horizontal = MoviesTheme.spacing.xxxl, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(seasonNumbers, key = { it }) { season ->
                        val isSelected = season == selectedSeason
                        val focusRequester = if (isSelected) {
                            selectedSeasonRequester
                        } else {
                            seasonFocusRequesters.getValue(season)
                        }

                        MoviesCard(
                            onClick = { selectedSeason = season },
                            selected = isSelected,
                            shape = M3eShapes.capsule,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .focusProperties {
                                    val upRequester = currentUpFocusRequester
                                    if (upRequester != null) up = upRequester
                                    down = effectiveRatingsGridFocusRequester
                                }
                                .onFocusChanged { state ->
                                    if (state.isFocused && selectedSeason != season) {
                                        selectedSeason = season
                                    }
                                }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                        ) {
                            Text(
                                text = stringResource(R.string.ratings_season_label, season),
                                style = MaterialTheme.typography.labelMedium
                                    .atLeastOnBox(TvSizeFloors.bodyText),
                                modifier = Modifier.padding(SeasonChipPadding)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.ratings_season_summary, selectedSeason, episodesForSeason.size),
                    style = MaterialTheme.typography.labelSmall
                        .atLeastOnBox(TvSizeFloors.metaText),
                    color = MoviesTheme.colors.TextTertiary,
                    modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.xxs)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(effectiveRatingsGridFocusRequester)
                        .focusRestorer(firstEpisodeRatingFocusRequester)
                        .focusGroup(),
                    contentPadding = PaddingValues(horizontal = MoviesTheme.spacing.xxxl, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(seasonRatings, key = { "${it.seasonNumber}:${it.episodeNumber}" }) { episodeRating ->
                        val isFirstEpisode = episodeRating == seasonRatings.firstOrNull()

                        // These chips only display a rating and have no action, but their focus is
                        // load-bearing: the season tabs point "down" at this row and the row's
                        // focusRestorer resolves to the first chip, so dropping them from the D-pad
                        // order would strand the whole ratings row. Keep the stop and name it.
                        val ratingDescription =
                            "S${episodeRating.seasonNumber}E${episodeRating.episodeNumber}, ${episodeRating.ratingText}"

                        Card(
                            onClick = { },
                            modifier = Modifier
                                .focusProperties {
                                    up = selectedSeasonRequester
                                    val downRequester = currentDownFocusRequester
                                    if (downRequester != null) down = downRequester
                                }
                                .then(
                                    if (isFirstEpisode) Modifier.focusRequester(firstEpisodeRatingFocusRequester) else Modifier
                                )
                                // (tv-material3's Card only has the clickable overload, so the
                                // empty onClick stays; clearAndSetSemantics replaces the chip's two
                                // Texts with one name that carries both the episode and its rating.)
                                .clearAndSetSemantics { contentDescription = ratingDescription },
                            shape = CardDefaults.shape(shape = M3eShapes.card),
                            colors = CardDefaults.colors(
                                containerColor = episodeRating.chipColor,
                                focusedContainerColor = episodeRating.chipColor
                            ),
                            border = moviesCardBorder(),
                            scale = moviesCardScale
                        ) {
                            Column(
                                modifier = Modifier
                                    .size(width = 72.dp, height = RatingCellHeight)
                                    .padding(horizontal = MoviesTheme.spacing.sm, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.ratings_episode_label, episodeRating.episodeNumber),
                                    style = MaterialTheme.typography.labelSmall
                                        .atLeastOnBox(TvSizeFloors.metaText),
                                    color = episodeRating.chipTextColor
                                )
                                Text(
                                    text = episodeRating.ratingText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = episodeRating.chipTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ratingColor(value: Double): androidx.compose.ui.graphics.Color {
    return when {
        value >= 9.7 -> androidx.compose.ui.graphics.Color(0xFF1DA1F3)
        value >= 9.0 -> androidx.compose.ui.graphics.Color(0xFF186A3B)
        value >= 8.0 -> androidx.compose.ui.graphics.Color(0xFF28B463)
        value >= 7.5 -> androidx.compose.ui.graphics.Color(0xFFF4D03F)
        value >= 7.0 -> androidx.compose.ui.graphics.Color(0xFFF39C12)
        value >= 6.0 -> androidx.compose.ui.graphics.Color(0xFFE74C3C)
        else -> androidx.compose.ui.graphics.Color(0xFF633974)
    }
}

private fun ratingTextColor(value: Double): Color {
    return when {
        value >= 7.0 && value < 8.0 -> Color(0xFF1D1D1F)
        else -> Color.White
    }
}

private data class EpisodeRatingChipUi(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val ratingText: String,
    val chipColor: Color,
    val chipTextColor: Color
)
