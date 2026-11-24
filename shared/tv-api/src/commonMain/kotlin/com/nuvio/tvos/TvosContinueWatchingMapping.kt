package com.nuvio.tvos

import com.nuvio.app.features.home.components.ContinueWatchingProgressBadge
import com.nuvio.app.features.home.components.continueWatchingProgressBadge
import com.nuvio.app.features.watching.application.canMarkWatched
import com.nuvio.app.features.watchprogress.AirDateBadge
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.airDateBadge
import com.nuvio.app.features.watchprogress.continueWatchingItemKey
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_continue_watching
import nuvio.composeapp.generated.resources.cw_airs_date
import nuvio.composeapp.generated.resources.cw_airs_in_days
import nuvio.composeapp.generated.resources.cw_airs_today
import nuvio.composeapp.generated.resources.cw_airs_tomorrow
import nuvio.composeapp.generated.resources.cw_new_episode
import nuvio.composeapp.generated.resources.cw_new_season
import nuvio.composeapp.generated.resources.home_continue_watching_hours_minutes_left
import nuvio.composeapp.generated.resources.home_continue_watching_minutes_left
import nuvio.composeapp.generated.resources.home_continue_watching_up_next
import nuvio.composeapp.generated.resources.home_continue_watching_watched
import nuvio.composeapp.generated.resources.media_movie
import nuvio.composeapp.generated.resources.upcoming_section_title
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

internal suspend fun tvosContinueWatchingRows(
    mainItems: List<ContinueWatchingItem>,
    upcomingItems: List<ContinueWatchingItem>,
    todayIsoDate: String,
): TvosContinueWatchingRows =
    TvosContinueWatchingRows(
        continueWatching = mainItems.toRow(
            getString(Res.string.compose_settings_page_continue_watching),
            todayIsoDate,
        ),
        upcoming = upcomingItems.toRow(getString(Res.string.upcoming_section_title), todayIsoDate),
    )

private suspend fun List<ContinueWatchingItem>.toRow(
    title: String,
    todayIsoDate: String,
): TvosContinueWatchingRow? {
    if (isEmpty()) return null
    return TvosContinueWatchingRow(
        title = title,
        items = map { item -> item.toTvosContinueWatchingItem(todayIsoDate) },
    )
}

private suspend fun ContinueWatchingItem.toTvosContinueWatchingItem(
    todayIsoDate: String,
): TvosContinueWatchingItem =
    TvosContinueWatchingItem(
        key = continueWatchingItemKey(this),
        title = title,
        subtitle = subtitle.ifBlank { getString(Res.string.media_movie) },
        imageUrl = imageUrl,
        progressFraction = progressFraction,
        progressLabel = progressLabel() ?: statusLabel(airDateLabel(todayIsoDate)),
        isNextUp = isNextUp,
        target = streamTarget(),
        canMarkWatched = canMarkWatched(todayIsoDate),
    )

private suspend fun ContinueWatchingItem.progressLabel(): String? =
    when (val badge = continueWatchingProgressBadge()) {
        is ContinueWatchingProgressBadge.WatchedPercent ->
            getString(Res.string.home_continue_watching_watched, "${badge.percent}%")
        is ContinueWatchingProgressBadge.HoursMinutesLeft ->
            getString(
                Res.string.home_continue_watching_hours_minutes_left,
                badge.hours,
                badge.minutes,
            )
        is ContinueWatchingProgressBadge.MinutesLeft ->
            getString(Res.string.home_continue_watching_minutes_left, badge.minutes)
        null -> null
    }

private suspend fun ContinueWatchingItem.statusLabel(airDateLabel: String?): String =
    when {
        isReleaseAlert && isNewSeasonRelease -> getString(Res.string.cw_new_season)
        isReleaseAlert -> getString(Res.string.cw_new_episode)
        airDateLabel != null -> airDateLabel
        else -> getString(Res.string.home_continue_watching_up_next)
    }

private suspend fun ContinueWatchingItem.airDateLabel(todayIsoDate: String): String? {
    val isUnstartedEpisode = progressFraction <= 0f && seasonNumber != null && episodeNumber != null
    if (!isUnstartedEpisode) return null
    return when (val badge = airDateBadge(released, todayIsoDate)) {
        AirDateBadge.Today -> getString(Res.string.cw_airs_today)
        AirDateBadge.Tomorrow -> getString(Res.string.cw_airs_tomorrow)
        is AirDateBadge.InDays ->
            getPluralString(Res.plurals.cw_airs_in_days, badge.days, badge.days)
        is AirDateBadge.OnDate -> getString(Res.string.cw_airs_date, badge.formattedDate)
        null -> null
    }
}

private suspend fun ContinueWatchingItem.streamTarget(): TvosStreamTarget =
    TvosStreamTarget(
        type = parentMetaType,
        videoId = videoId,
        parentMetaId = parentMetaId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title,
        subtitle = episodeSubtitle(seasonNumber, episodeNumber, episodeTitle),
        backdropUrl = background ?: poster,
        posterUrl = poster,
        logoUrl = logo,
        episodeTitle = episodeTitle,
        episodeThumbnailUrl = episodeThumbnail,
    )
