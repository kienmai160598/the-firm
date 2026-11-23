package com.kien.movies.ui.screens.home

import kotlinx.coroutines.flow.first

/**
 * The next-up overlay that can stand in for enriching [item] again: one for the same episode that
 * was enriched in this session. An entry restored from the disk snapshot only seeds the first
 * render; reusing it would keep stale data for good, such as an episode description the add-on
 * added after the snapshot was written.
 */
internal fun reusableNextUpOverlay(
    overlay: NextUpInfo?,
    item: NextUpInfo,
    seededFromDisk: Boolean
): NextUpInfo? = overlay?.takeIf {
    !seededFromDisk && it.season == item.season && it.episode == item.episode
}

/**
 * Keeps what a card showed for its series when the card moves on to another episode.
 *
 * The title, artwork, genres and title ratings belong to the series, so they stay from the card
 * enriched earlier until the new episode is enriched. Without this the card showed the raw progress
 * title (e.g. the Simkl one) between the two. Episode fields are left to the new episode.
 */
internal fun NextUpInfo.withSeriesFieldsFrom(source: NextUpInfo): NextUpInfo = copy(
    name = source.name.takeIf { it.isNotBlank() } ?: name,
    poster = source.poster ?: poster,
    backdrop = source.backdrop ?: backdrop,
    logo = source.logo ?: logo,
    genres = source.genres.ifEmpty { genres },
    releaseInfo = source.releaseInfo ?: releaseInfo,
    contentLanguage = source.contentLanguage ?: contentLanguage,
    mdbListRatings = source.mdbListRatings ?: mdbListRatings
)

/** [withSeriesFieldsFrom] for a card that was in progress, e.g. an episode marked watched mid-way. */
internal fun NextUpInfo.withSeriesFieldsFrom(source: ContinueWatchingItem.InProgress): NextUpInfo = copy(
    name = source.progress.name.takeIf { it.isNotBlank() } ?: name,
    poster = source.progress.poster ?: poster,
    backdrop = source.progress.backdrop ?: backdrop,
    logo = source.progress.logo ?: logo,
    genres = source.genres.ifEmpty { genres },
    releaseInfo = source.releaseInfo ?: releaseInfo,
    contentLanguage = source.contentLanguage ?: contentLanguage,
    mdbListRatings = source.mdbListRatings ?: mdbListRatings
)
