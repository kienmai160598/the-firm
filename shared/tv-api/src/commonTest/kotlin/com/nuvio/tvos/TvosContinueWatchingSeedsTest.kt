package com.nuvio.tvos

import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NOW_EPOCH_MS = 1_795_000_000_000L
private const val DAY_MS = 86_400_000L
private const val DAYS_CAP = 30
private const val RECENT_EPOCH_MS = NOW_EPOCH_MS - DAY_MS
private const val OLD_EPOCH_MS = NOW_EPOCH_MS - 40 * DAY_MS

private const val VISIBLE_MOVIE = "tt1000001"
private const val HIDDEN_MOVIE = "tt1000002"
private const val DROPPED_SERIES = "tt1000003"
private const val OLD_MOVIE = "tt1000004"
private const val FRESH_SEED = "tt2000001"
private const val HIDDEN_SEED = "tt2000002"
private const val DROPPED_SEED = "tt2000003"
private const val OLD_SEED = "tt2000004"
private const val OLD_PROGRESS_SEED = "tt2000005"

private class FakePolicy(
    private val droppedIds: Set<String> = emptySet(),
    private val applyDaysCap: Boolean = true,
    private val ownsHistory: Boolean = false,
) : ContinueWatchingPolicy {
    override fun nowEpochMs(): Long = NOW_EPOCH_MS

    override fun ownsCompletedHistory(): Boolean = ownsHistory

    override fun cutoffEpochMs(daysCap: Int, nowEpochMs: Long): Long? =
        if (applyDaysCap) nowEpochMs - daysCap * DAY_MS else null

    override fun isDropped(contentId: String): Boolean = contentId in droppedIds

    override fun shouldUseAsNextUpSeed(entry: WatchProgressEntry, nowEpochMs: Long): Boolean =
        entry.isCompleted
}

class TvosContinueWatchingSeedsTest {
    @Test
    fun hiddenDroppedAndOldEntriesLeaveContinueWatching() {
        val seeds = sources().nextUpSeeds(FakePolicy(droppedIds = setOf(DROPPED_SERIES)))

        assertEquals(listOf(VISIBLE_MOVIE), seeds.visibleEntries.map { it.parentMetaId })
    }

    @Test
    fun hiddenDroppedAndOldSeedsProduceNoNextUp() {
        val policy = FakePolicy(droppedIds = setOf(DROPPED_SEED))
        val seeds = sources().nextUpSeeds(policy)

        assertEquals(listOf(FRESH_SEED), seeds.candidates.map { it.content.id })
    }

    @Test
    fun oldSeedsStillCountAsActiveSoTheirStaleNextUpIsNotDropped() {
        val seeds = sources().nextUpSeeds(FakePolicy(droppedIds = setOf(DROPPED_SEED)))

        assertEquals(
            setOf(FRESH_SEED, OLD_SEED, OLD_PROGRESS_SEED),
            seeds.activeSeedContentIds,
        )
    }

    @Test
    fun withoutDaysCapOldEntriesAndSeedsStay() {
        val seeds = sources().nextUpSeeds(FakePolicy(applyDaysCap = false))
        val visibleIds = seeds.visibleEntries.map { it.parentMetaId }.toSet()
        val candidateIds = seeds.candidates.map { it.content.id }.toSet()

        assertEquals(setOf(VISIBLE_MOVIE, DROPPED_SERIES, OLD_MOVIE), visibleIds)
        assertEquals(
            setOf(FRESH_SEED, DROPPED_SEED, OLD_SEED, OLD_PROGRESS_SEED),
            candidateIds,
        )
    }

    @Test
    fun providerOwnedHistoryIgnoresLocalWatchedItems() {
        val seeds = sources().nextUpSeeds(FakePolicy(applyDaysCap = false, ownsHistory = true))

        assertEquals(listOf(OLD_PROGRESS_SEED), seeds.candidates.map { it.content.id })
        assertTrue(seeds.nextUpWatchedItems.isEmpty())
    }
}

private fun sources(): ContinueWatchingSources =
    ContinueWatchingSources(
        progress = WatchProgressUiState(
            entries = listOf(
                movieInProgress(VISIBLE_MOVIE, RECENT_EPOCH_MS),
                movieInProgress(HIDDEN_MOVIE, RECENT_EPOCH_MS),
                episodeProgress(DROPPED_SERIES, RECENT_EPOCH_MS, isCompleted = false),
                movieInProgress(OLD_MOVIE, OLD_EPOCH_MS),
                episodeProgress(OLD_PROGRESS_SEED, OLD_EPOCH_MS, isCompleted = true),
            ),
            hiddenContentIds = setOf(HIDDEN_MOVIE, HIDDEN_SEED),
            hasLoadedRemoteProgress = true,
        ),
        watched = WatchedUiState(
            items = listOf(
                watchedEpisode(FRESH_SEED, RECENT_EPOCH_MS),
                watchedEpisode(HIDDEN_SEED, RECENT_EPOCH_MS),
                watchedEpisode(DROPPED_SEED, RECENT_EPOCH_MS),
                watchedEpisode(OLD_SEED, OLD_EPOCH_MS),
            ),
            isLoaded = true,
            hasLoadedRemoteItems = true,
        ),
        preferences = ContinueWatchingPreferencesUiState(),
        daysCap = DAYS_CAP,
    )

private fun movieInProgress(contentId: String, updatedEpochMs: Long): WatchProgressEntry =
    WatchProgressEntry(
        contentType = "movie",
        parentMetaId = contentId,
        parentMetaType = "movie",
        videoId = contentId,
        title = contentId,
        lastPositionMs = 600_000L,
        durationMs = 6_000_000L,
        lastUpdatedEpochMs = updatedEpochMs,
    )

private fun episodeProgress(
    contentId: String,
    updatedEpochMs: Long,
    isCompleted: Boolean,
): WatchProgressEntry =
    WatchProgressEntry(
        contentType = "series",
        parentMetaId = contentId,
        parentMetaType = "series",
        videoId = "$contentId:1:1",
        title = contentId,
        seasonNumber = 1,
        episodeNumber = 1,
        lastPositionMs = if (isCompleted) 2_990_000L else 600_000L,
        durationMs = 3_000_000L,
        lastUpdatedEpochMs = updatedEpochMs,
        isCompleted = isCompleted,
    )

private fun watchedEpisode(contentId: String, markedAtEpochMs: Long): WatchedItem =
    WatchedItem(
        id = contentId,
        type = "series",
        name = contentId,
        season = 1,
        episode = 1,
        markedAtEpochMs = markedAtEpochMs,
    )
