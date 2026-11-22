package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.home.CompletedSeriesCandidate
import com.nuvio.app.features.home.HomeContinueWatchingMaxRecentProgressItems
import com.nuvio.app.features.home.HomeNextUpResolutionAttempt
import com.nuvio.app.features.home.planHomeNextUpResolutionCandidates
import com.nuvio.app.features.home.resolveHomeNextUpCandidate
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val NEXT_UP_RESOLUTION_CONCURRENCY = 4

internal typealias NextUpItemsBySeries = Map<String, Pair<Long, ContinueWatchingItem>>

internal data class NextUpSeedIdentity(
    val contentId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
)

internal data class NextUpRequest(
    val seeds: List<NextUpSeedIdentity>,
    val preferFurthestEpisode: Boolean,
    val showUnairedNextUp: Boolean,
    val isSeedSourceLoaded: Boolean,
    val addonSignature: List<String>,
)

internal class NextUpSnapshot(
    val sources: ContinueWatchingSources,
    val seeds: NextUpSeeds,
)

private data class NextUpMemoKey(
    val seed: NextUpSeedIdentity,
    val preferFurthestEpisode: Boolean,
    val showUnairedNextUp: Boolean,
    val todayIsoDate: String,
)

internal class TvosNextUpResolver {
    private val log = Logger.withTag("TvosNextUp")
    private val conclusiveAttempts = mutableMapOf<NextUpMemoKey, HomeNextUpResolutionAttempt>()
    private var memoAddonSignature: List<String>? = null
    private var lookupCount = 0

    var latestItems: NextUpItemsBySeries = emptyMap()
        private set

    fun resolve(request: NextUpRequest, snapshot: NextUpSnapshot): Flow<NextUpItemsBySeries> =
        flow<NextUpItemsBySeries> {
            if (!request.isSeedSourceLoaded) {
                log.i { "Next Up waits for the seed source to load" }
                return@flow
            }
            TvosHome.awaitStartupAddons()
            forgetWhenAddonsChange(request.addonSignature)
            val candidates = snapshot.seeds.candidates
            if (candidates.isEmpty()) emit(emptyMap())
            val plan = planHomeNextUpResolutionCandidates(candidates)
            val resolved = linkedMapOf<String, Pair<Long, ContinueWatchingItem>>()
            for (batch in listOf(plan.initialCandidates, plan.deferredCandidates)) {
                if (batch.isEmpty()) continue
                resolveBatch(batch, request, snapshot).forEach { attempt -> resolved.add(attempt) }
                emit(resolved.toMap())
            }
            log.i { "Next Up resolved ${resolved.size} of ${candidates.describe()}" }
        }.onEach { items -> latestItems = items }

    private fun forgetWhenAddonsChange(addonSignature: List<String>) {
        if (memoAddonSignature == addonSignature) return
        conclusiveAttempts.clear()
        memoAddonSignature = addonSignature
    }

    private suspend fun resolveBatch(
        batch: List<CompletedSeriesCandidate>,
        request: NextUpRequest,
        snapshot: NextUpSnapshot,
    ): List<HomeNextUpResolutionAttempt> = coroutineScope {
        val semaphore = Semaphore(NEXT_UP_RESOLUTION_CONCURRENCY)
        batch
            .map { candidate ->
                async { semaphore.withPermit { attemptFor(candidate, request, snapshot) } }
            }
            .awaitAll()
    }

    private suspend fun attemptFor(
        candidate: CompletedSeriesCandidate,
        request: NextUpRequest,
        snapshot: NextUpSnapshot,
    ): HomeNextUpResolutionAttempt {
        val memoKey = NextUpMemoKey(
            seed = candidate.identity(),
            preferFurthestEpisode = request.preferFurthestEpisode,
            showUnairedNextUp = request.showUnairedNextUp,
            todayIsoDate = CurrentDateProvider.todayIsoDate(),
        )
        conclusiveAttempts[memoKey]?.let { return it }
        lookupCount += 1
        log.i { "Next Up lookup #$lookupCount for ${listOf(candidate).describe()}" }
        val attempt = freshAttempt(candidate, memoKey.todayIsoDate, request, snapshot)
        if (attempt.isConclusive) conclusiveAttempts[memoKey] = attempt
        return attempt
    }

    private suspend fun freshAttempt(
        candidate: CompletedSeriesCandidate,
        todayIsoDate: String,
        request: NextUpRequest,
        snapshot: NextUpSnapshot,
    ): HomeNextUpResolutionAttempt =
        try {
            resolveHomeNextUpCandidate(
                completedEntry = candidate,
                watchProgressEntries = snapshot.sources.progress.entries,
                watchedItems = snapshot.seeds.nextUpWatchedItems,
                cachedFallbackItem = null,
                todayIsoDate = todayIsoDate,
                preferFurthestEpisode = request.preferFurthestEpisode,
                showUnairedNextUp = request.showUnairedNextUp,
                dismissedNextUpKeys = emptySet(),
                providerOwnsCompletedHistory = snapshot.seeds.providerOwnsCompletedHistory,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            log.e(error) { "Next Up failed for ${candidate.content.id}" }
            HomeNextUpResolutionAttempt.transientFailure()
        }

    private fun MutableMap<String, Pair<Long, ContinueWatchingItem>>.add(
        attempt: HomeNextUpResolutionAttempt,
    ) {
        val (contentId, item) = attempt.resolved ?: return
        if (size < HomeContinueWatchingMaxRecentProgressItems) put(contentId, item)
    }
}

internal fun NextUpItemsBySeries.stampedWithSeedWatchedAt(
    candidates: List<CompletedSeriesCandidate>,
): NextUpItemsBySeries {
    val watchedAtBySeries = candidates.associate { it.content.id to it.markedAtEpochMs }
    return mapValues { (contentId, sortedItem) ->
        val (_, item) = sortedItem
        val watchedAt = watchedAtBySeries[contentId]
        if (item.isReleaseAlert || watchedAt == null) sortedItem else watchedAt to item
    }
}

internal fun CompletedSeriesCandidate.identity(): NextUpSeedIdentity =
    NextUpSeedIdentity(content.id, seasonNumber, episodeNumber)

private fun List<CompletedSeriesCandidate>.describe(): String =
    joinToString(prefix = "[", postfix = "]") { candidate ->
        with(candidate) { "${content.id} S${seasonNumber}E$episodeNumber@$markedAtEpochMs" }
    }
