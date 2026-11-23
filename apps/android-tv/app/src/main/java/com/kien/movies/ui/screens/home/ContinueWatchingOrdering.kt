package com.kien.movies.ui.screens.home

import com.kien.movies.core.util.parseEpisodeReleaseInstant
import com.kien.movies.domain.model.ContinueWatchingSortMode
import com.kien.movies.ui.util.parseEpisodeReleaseDate
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.map

private val upcomingReleaseComparator = compareBy<ContinueWatchingItem, LocalDate?>(
    nullsLast()
) { item ->
    parseEpisodeReleaseDate((item as? ContinueWatchingItem.NextUp)?.info?.released)
}.thenBy(nullsLast()) { item ->
    parseTimedEpisodeReleaseInstant((item as? ContinueWatchingItem.NextUp)?.info?.released)
}

private fun parseTimedEpisodeReleaseInstant(raw: String?): Instant? {
    val value = raw?.trim() ?: return null
    if (runCatching { DateTimeFormatter.ISO_DATE_TIME.parse(value) }.isFailure) {
        return runCatching { Instant.parse(value) }.getOrNull()
    }
    return parseEpisodeReleaseInstant(value)
}

internal fun sortContinueWatchingItems(
    items: List<ContinueWatchingItem>,
    mode: ContinueWatchingSortMode
): List<ContinueWatchingItem> {
    return when (mode) {
        ContinueWatchingSortMode.DEFAULT, ContinueWatchingSortMode.SPLIT_UPCOMING -> items.sortedByDescending {
                item
            ->
            when (item) {
                is ContinueWatchingItem.InProgress -> item.progress.lastWatched
                is ContinueWatchingItem.NextUp -> item.info.sortTimestamp
            }
        }

        ContinueWatchingSortMode.STREAMING_STYLE -> {
            val (released, unreleased) = items.partition { item ->
                when (item) {
                    is ContinueWatchingItem.InProgress -> true

                    // in-progress is always released
                    is ContinueWatchingItem.NextUp -> item.info.hasAired
                }
            }

            val sortedReleased = released.sortedByDescending { item ->
                when (item) {
                    is ContinueWatchingItem.InProgress -> item.progress.lastWatched
                    is ContinueWatchingItem.NextUp -> if (item.info.isReleaseAlert) item.info.sortTimestamp else item.info.lastWatched
                }
            }

            val sortedUnreleased = unreleased.sortedWith(upcomingReleaseComparator)

            sortedReleased + sortedUnreleased
        }
    }
}

internal fun mergeContinueWatchingItems(
    inProgressItems: List<ContinueWatchingItem.InProgress>,
    nextUpItems: List<ContinueWatchingItem.NextUp>,
    mode: ContinueWatchingSortMode = ContinueWatchingSortMode.DEFAULT
): List<ContinueWatchingItem> {
    val allInProgressIds = inProgressItems
        .asSequence()
        .map { it.progress }
        .filter { isSeriesType(it.contentType) }
        .map { it.contentId }
        .filter { it.isNotBlank() }
        .toSet()

    val filteredNextUpItems = nextUpItems.filter { item ->
        item.info.contentId !in allInProgressIds
    }

    val combined = inProgressItems + filteredNextUpItems

    val seen = mutableSetOf<String>()
    val deduplicated = combined.filter { item ->
        val contentId = when (item) {
            is ContinueWatchingItem.InProgress -> item.progress.contentId
            is ContinueWatchingItem.NextUp -> item.info.contentId
        }
        contentId.isBlank() || seen.add(contentId)
    }

    return sortContinueWatchingItems(deduplicated, mode)
}

/**
 * Splits items into (mainRow, upcomingRow) for [ContinueWatchingSortMode.SPLIT_UPCOMING].
 * Upcoming items are unaired NextUp episodes; everything else stays in the main row.
 * For other modes, all items go into mainRow and upcomingRow is empty.
 */
internal fun splitUpcomingItems(
    items: List<ContinueWatchingItem>,
    mode: ContinueWatchingSortMode
): Pair<List<ContinueWatchingItem>, List<ContinueWatchingItem>> {
    if (mode != ContinueWatchingSortMode.SPLIT_UPCOMING) {
        return items to emptyList()
    }
    val (upcoming, main) = items.partition { item ->
        item is ContinueWatchingItem.NextUp && !item.info.hasAired
    }
    val sortedUpcoming = upcoming.sortedWith(upcomingReleaseComparator)
    return main to sortedUpcoming
}
