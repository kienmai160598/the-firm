package com.nuvio.tvos

import com.nuvio.app.features.home.splitUpcomingItems
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSortMode
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateByAddingTimeInterval
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SECONDS_PER_DAY = 86_400.0
private const val IN_PROGRESS = "tt3000001"
private const val AIRED_NEXT_UP = "tt3000002"
private const val SOON_NEXT_UP = "tt3000003"
private const val LATER_NEXT_UP = "tt3000004"

class TvosContinueWatchingRowsTest {
    @Test
    fun splitModeMovesUnairedNextUpToUpcomingRow() {
        val rows = rowsFor(ContinueWatchingSortMode.SPLIT_UPCOMING)

        assertEquals(listOf(IN_PROGRESS, AIRED_NEXT_UP), rows.continueWatching.parentIds())
        assertEquals(listOf(SOON_NEXT_UP, LATER_NEXT_UP), rows.upcoming.parentIds())
        assertEquals("Upcoming", rows.upcoming?.title)
    }

    @Test
    fun defaultModeKeepsOneRow() {
        val rows = rowsFor(ContinueWatchingSortMode.DEFAULT)

        assertEquals(4, rows.continueWatching?.items?.size)
        assertNull(rows.upcoming)
    }

    @Test
    fun unairedNextUpShowsAirDateAndAiredNextUpShowsNextUp() {
        val labels = rowsFor(ContinueWatchingSortMode.DEFAULT).labelsByParentId()

        assertEquals("Next Up", labels[AIRED_NEXT_UP])
        assertEquals("Airs in 3 Days", labels[SOON_NEXT_UP])
        val laterLabel = labels[LATER_NEXT_UP].orEmpty()
        assertTrue(laterLabel.startsWith("Airs ") && !laterLabel.startsWith("Airs in"), laterLabel)
        assertTrue(labels[IN_PROGRESS].orEmpty().endsWith("left"), labels[IN_PROGRESS])
    }
}

private fun rowsFor(mode: ContinueWatchingSortMode): TvosContinueWatchingRows {
    val (mainItems, upcomingItems) = splitUpcomingItems(sampleItems(), mode)
    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    return runBlocking { tvosContinueWatchingRows(mainItems, upcomingItems, todayIsoDate) }
}

private fun TvosContinueWatchingRow?.parentIds(): List<String> =
    this?.items?.map { item -> item.target.parentMetaId }.orEmpty()

private fun TvosContinueWatchingRows.labelsByParentId(): Map<String, String?> =
    listOfNotNull(continueWatching, upcoming)
        .flatMap { row -> row.items }
        .associate { item -> item.target.parentMetaId to item.progressLabel }

private fun sampleItems(): List<ContinueWatchingItem> =
    listOf(
        inProgressMovie(),
        nextUpEpisode(AIRED_NEXT_UP, isoDateInDays(-10)),
        nextUpEpisode(SOON_NEXT_UP, isoDateInDays(3)),
        nextUpEpisode(LATER_NEXT_UP, isoDateInDays(40)),
    )

private fun inProgressMovie(): ContinueWatchingItem =
    ContinueWatchingItem(
        parentMetaId = IN_PROGRESS,
        parentMetaType = "movie",
        videoId = IN_PROGRESS,
        title = IN_PROGRESS,
        subtitle = "Movie",
        imageUrl = null,
        resumePositionMs = 600_000L,
        durationMs = 6_000_000L,
        progressFraction = 0.1f,
    )

private fun nextUpEpisode(contentId: String, releasedIso: String): ContinueWatchingItem =
    ContinueWatchingItem(
        parentMetaId = contentId,
        parentMetaType = "series",
        videoId = "$contentId:1:2",
        title = contentId,
        subtitle = "S1 E2",
        imageUrl = null,
        seasonNumber = 1,
        episodeNumber = 2,
        released = releasedIso,
        isNextUp = true,
        nextUpSeedSeasonNumber = 1,
        nextUpSeedEpisodeNumber = 1,
        resumePositionMs = 0L,
        durationMs = 0L,
        progressFraction = 0f,
    )

private fun isoDateInDays(days: Int): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate().dateByAddingTimeInterval(days * SECONDS_PER_DAY))
}
