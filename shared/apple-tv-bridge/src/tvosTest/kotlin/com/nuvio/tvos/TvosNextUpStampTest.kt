package com.nuvio.tvos

import com.nuvio.app.features.home.CompletedSeriesCandidate
import com.nuvio.app.features.watching.domain.WatchingContentRef
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SERIES = "tt4000001"
private const val ALERT_SERIES = "tt4000002"
private const val FIRST_WATCHED_AT = 1_795_000_000_000L
private const val REWATCHED_AT = FIRST_WATCHED_AT + 7 * 86_400_000L
private const val RELEASE_AT = FIRST_WATCHED_AT + 86_400_000L

class TvosNextUpStampTest {
    @Test
    fun aMemoizedNextUpTakesTheLatestTimeItsSeedWasWatched() {
        val memoized = mapOf(SERIES to (FIRST_WATCHED_AT to nextUpItem(SERIES)))

        val stamped = memoized.stampedWithSeedWatchedAt(listOf(seed(SERIES, REWATCHED_AT)))

        assertEquals(REWATCHED_AT, stamped.getValue(SERIES).first)
    }

    @Test
    fun aReleaseAlertKeepsItsReleaseTime() {
        val alert = nextUpItem(ALERT_SERIES).copy(isReleaseAlert = true)
        val memoized = mapOf(ALERT_SERIES to (RELEASE_AT to alert))

        val stamped = memoized.stampedWithSeedWatchedAt(listOf(seed(ALERT_SERIES, REWATCHED_AT)))

        assertEquals(RELEASE_AT, stamped.getValue(ALERT_SERIES).first)
    }

    @Test
    fun aNextUpWithoutACurrentSeedKeepsItsTime() {
        val memoized = mapOf(SERIES to (FIRST_WATCHED_AT to nextUpItem(SERIES)))

        val stamped = memoized.stampedWithSeedWatchedAt(emptyList())

        assertEquals(FIRST_WATCHED_AT, stamped.getValue(SERIES).first)
    }
}

private fun seed(contentId: String, watchedAt: Long): CompletedSeriesCandidate =
    CompletedSeriesCandidate(
        content = WatchingContentRef(type = "series", id = contentId),
        seasonNumber = 1,
        episodeNumber = 1,
        markedAtEpochMs = watchedAt,
    )

private fun nextUpItem(contentId: String): ContinueWatchingItem =
    ContinueWatchingItem(
        parentMetaId = contentId,
        parentMetaType = "series",
        videoId = "$contentId:1:2",
        title = contentId,
        subtitle = "S1 E2",
        imageUrl = null,
        seasonNumber = 1,
        episodeNumber = 2,
        isNextUp = true,
        nextUpSeedSeasonNumber = 1,
        nextUpSeedEpisodeNumber = 1,
        resumePositionMs = 0L,
        durationMs = 0L,
        progressFraction = 0f,
    )
