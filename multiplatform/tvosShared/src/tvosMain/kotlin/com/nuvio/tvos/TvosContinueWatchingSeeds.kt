package com.nuvio.tvos

import com.nuvio.app.features.home.CompletedSeriesCandidate
import com.nuvio.app.features.home.HomeContinueWatchingMaxRecentProgressItems
import com.nuvio.app.features.home.buildHomeNextUpSeedCandidates
import com.nuvio.app.features.home.buildHomeNextUpSuppressedSeriesIds
import com.nuvio.app.features.home.filterEntriesForContinueWatchingWindow
import com.nuvio.app.features.home.filterHomeNextUpCandidatesForContinueWatchingWindow
import com.nuvio.app.features.home.isHomeNextUpSeedSourceLoaded
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.features.watchprogress.WatchProgressClock
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import com.nuvio.app.features.watchprogress.continueWatchingEntries

internal data class ContinueWatchingSources(
    val progress: WatchProgressUiState,
    val watched: WatchedUiState,
    val preferences: ContinueWatchingPreferencesUiState,
    val daysCap: Int,
)

internal data class NextUpSeeds(
    val visibleEntries: List<WatchProgressEntry>,
    val nextUpWatchedItems: List<WatchedItem>,
    val activeSeedContentIds: Set<String>,
    val currentSeedByContentId: Map<String, Pair<Int, Int>>,
    val suppressedSeriesIds: Set<String>,
    val candidates: List<CompletedSeriesCandidate>,
    val providerOwnsCompletedHistory: Boolean,
    val isSeedSourceLoaded: Boolean,
)

internal interface ContinueWatchingPolicy {
    fun nowEpochMs(): Long
    fun ownsCompletedHistory(): Boolean
    fun cutoffEpochMs(daysCap: Int, nowEpochMs: Long): Long?
    fun isDropped(contentId: String): Boolean
    fun shouldUseAsNextUpSeed(entry: WatchProgressEntry, nowEpochMs: Long): Boolean
}

internal object ActiveProviderPolicy : ContinueWatchingPolicy {
    override fun nowEpochMs(): Long = WatchProgressClock.nowEpochMs()

    override fun ownsCompletedHistory(): Boolean =
        WatchProgressRepository.activeProviderOwnsCompletedHistoryProjection()

    override fun cutoffEpochMs(daysCap: Int, nowEpochMs: Long): Long? =
        WatchProgressRepository.activeProviderContinueWatchingCutoffEpochMs(daysCap, nowEpochMs)

    override fun isDropped(contentId: String): Boolean =
        WatchProgressRepository.isDroppedShow(contentId)

    override fun shouldUseAsNextUpSeed(entry: WatchProgressEntry, nowEpochMs: Long): Boolean =
        WatchProgressRepository.shouldUseAsNextUpSeed(entry, nowEpochMs)
}

internal fun ContinueWatchingSources.nextUpSeeds(
    policy: ContinueWatchingPolicy = ActiveProviderPolicy,
): NextUpSeeds {
    val nowEpochMs = policy.nowEpochMs()
    val providerOwnsHistory = policy.ownsCompletedHistory()
    val cutoffEpochMs = policy.cutoffEpochMs(daysCap = daysCap, nowEpochMs = nowEpochMs)
    val isHiddenOrDropped = { contentId: String ->
        contentId in progress.hiddenContentIds || policy.isDropped(contentId)
    }
    val watchedItems = if (providerOwnsHistory) emptyList() else watched.items
    val allCandidates = buildHomeNextUpSeedCandidates(
        progressEntries = progress.entries,
        watchedItems = watchedItems,
        providerOwnsCompletedHistory = providerOwnsHistory,
        preferFurthestEpisode = preferences.upNextFromFurthestEpisode,
        nowEpochMs = nowEpochMs,
        shouldUseProgressSeed = policy::shouldUseAsNextUpSeed,
        isContentHidden = isHiddenOrDropped,
    )
    val visibleEntries = filterEntriesForContinueWatchingWindow(
        entries = progress.entries.filterNot { entry -> isHiddenOrDropped(entry.parentMetaId) },
        cutoffEpochMs = cutoffEpochMs,
    ).continueWatchingEntries(limit = HomeContinueWatchingMaxRecentProgressItems)
    val suppressedSeriesIds = buildHomeNextUpSuppressedSeriesIds(
        visibleEntries = visibleEntries,
        seedCandidates = allCandidates,
    )
    val recentCandidates = filterHomeNextUpCandidatesForContinueWatchingWindow(
        candidates = allCandidates,
        cutoffEpochMs = cutoffEpochMs,
    )

    return NextUpSeeds(
        visibleEntries = visibleEntries,
        nextUpWatchedItems = watchedItems,
        activeSeedContentIds = allCandidates.mapTo(mutableSetOf()) { it.content.id },
        currentSeedByContentId = allCandidates.associate { candidate ->
            candidate.content.id to (candidate.seasonNumber to candidate.episodeNumber)
        },
        suppressedSeriesIds = suppressedSeriesIds,
        candidates = recentCandidates.filter { it.content.id !in suppressedSeriesIds },
        providerOwnsCompletedHistory = providerOwnsHistory,
        isSeedSourceLoaded = isHomeNextUpSeedSourceLoaded(
            providerOwnsCompletedHistory = providerOwnsHistory,
            hasLoadedRemoteProgress = progress.hasLoadedRemoteProgress,
            hasLoadedWatchedItems = watched.isLoaded,
            hasLoadedRemoteWatchedItems = watched.hasLoadedRemoteItems,
        ),
    )
}
