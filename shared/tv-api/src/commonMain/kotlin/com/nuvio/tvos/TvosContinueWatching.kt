package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.home.buildAddonCatalogRefreshSignature
import com.nuvio.app.features.home.buildHomeContinueWatchingItems
import com.nuvio.app.features.home.filterNextUpItemsByCurrentSeeds
import com.nuvio.app.features.home.splitUpcomingItems
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingActions
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressSourceCoordinator
import com.nuvio.app.features.watchprogress.continueWatchingItemKey
import com.nuvio.app.features.watchprogress.nextUpDismissKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal object TvosContinueWatching {
    private val log = Logger.withTag("TvosContinueWatching")
    private val resolver = TvosNextUpResolver()
    private var itemsByKey: Map<String, ContinueWatchingItem> = emptyMap()

    fun ensureLoaded() {
        WatchProgressRepository.ensureLoaded()
        WatchedRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.ensureLoaded()
        TrackingSettingsRepository.ensureLoaded()
    }

    suspend fun refreshWatchSource() {
        val result = WatchProgressSourceCoordinator.refreshActiveSource(
            profileId = ProfileRepository.activeProfileId,
            force = false,
        )
        val progress = WatchProgressRepository.uiState.value
        val watched = WatchedRepository.uiState.value
        log.i {
            "Watch source refreshed=${result.succeeded} " +
                "remoteProgress=${progress.hasLoadedRemoteProgress} " +
                "watchedLoaded=${watched.isLoaded} remoteWatched=${watched.hasLoadedRemoteItems}"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun rows(): Flow<TvosContinueWatchingRows> {
        val snapshots = sources().map { sources -> NextUpSnapshot(sources, sources.nextUpSeeds()) }
        val nextUpItems = snapshots
            .combine(addonSignatures()) { snapshot, signature ->
                snapshot.nextUpRequest(signature) to snapshot
            }
            .distinctUntilChanged { old, new -> old.first == new.first }
            .flatMapLatest { (request, snapshot) -> resolver.resolve(request, snapshot) }
            .onStart { emit(resolver.latestItems) }
        return combine(snapshots, nextUpItems, ::row)
    }

    fun remove(key: String) {
        val item = itemsByKey[key]
        if (item == null) {
            log.w { "Remove ignored: no Continue Watching item $key" }
            return
        }
        if (item.isNextUp) {
            ContinueWatchingPreferencesRepository.addDismissedNextUpKey(
                nextUpDismissKey(
                    item.parentMetaId,
                    item.nextUpSeedSeasonNumber,
                    item.nextUpSeedEpisodeNumber,
                ),
            )
        } else {
            WatchProgressRepository.removeProgress(contentId = item.parentMetaId)
        }
        log.i { "Removed ${item.parentMetaId} from Continue Watching, nextUp=${item.isNextUp}" }
    }

    fun markWatched(key: String) {
        val item = itemsByKey[key]
        if (item == null) {
            log.w { "Mark watched ignored: no Continue Watching item $key" }
            return
        }
        WatchingActions.markContinueWatchingWatched(item)
        log.i { "Marked ${item.parentMetaId} ${item.videoId} watched from Continue Watching" }
    }

    private fun sources(): Flow<ContinueWatchingSources> =
        combine(
            WatchProgressRepository.uiState,
            WatchedRepository.uiState,
            ContinueWatchingPreferencesRepository.uiState,
            TrackingSettingsRepository.uiState.map { settings -> settings.continueWatchingDaysCap },
            ::ContinueWatchingSources,
        )

    private fun addonSignatures(): Flow<List<String>> =
        AddonRepository.uiState
            .map { state -> buildAddonCatalogRefreshSignature(state.addons) }
            .distinctUntilChanged()

    private fun NextUpSnapshot.nextUpRequest(addonSignature: List<String>): NextUpRequest =
        NextUpRequest(
            seeds = seeds.candidates.map { candidate -> candidate.identity() },
            preferFurthestEpisode = sources.preferences.upNextFromFurthestEpisode,
            showUnairedNextUp = sources.preferences.showUnairedNextUp,
            isSeedSourceLoaded = seeds.isSeedSourceLoaded,
            addonSignature = addonSignature,
        )

    private suspend fun row(
        snapshot: NextUpSnapshot,
        nextUpItems: NextUpItemsBySeries,
    ): TvosContinueWatchingRows {
        val preferences = snapshot.sources.preferences
        val todayIsoDate = CurrentDateProvider.todayIsoDate()
        if (!preferences.isVisible) return TvosContinueWatchingRows(null, null)
        val items = buildHomeContinueWatchingItems(
            visibleEntries = snapshot.seeds.visibleEntries,
            nextUpItemsBySeries = snapshot.liveNextUpItems(nextUpItems),
            nextUpSuppressedSeriesIds = snapshot.seeds.suppressedSeriesIds,
            sortMode = preferences.sortMode,
            todayIsoDate = todayIsoDate,
        )
        val (mainItems, upcomingItems) = splitUpcomingItems(items, preferences.sortMode)
        itemsByKey = (mainItems + upcomingItems).associateBy(::continueWatchingItemKey)
        return tvosContinueWatchingRows(mainItems, upcomingItems, todayIsoDate)
    }

    private fun NextUpSnapshot.liveNextUpItems(
        nextUpItems: NextUpItemsBySeries,
    ): NextUpItemsBySeries {
        val dismissedKeys = sources.preferences.dismissedNextUpKeys
        return filterNextUpItemsByCurrentSeeds(
            nextUpItemsBySeries = nextUpItems,
            activeSeedContentIds = seeds.activeSeedContentIds,
            currentSeedByContentId = seeds.currentSeedByContentId,
            shouldDropItemsWithoutActiveSeed = seeds.isSeedSourceLoaded,
        ).filterValues { (_, item) ->
            nextUpDismissKey(
                item.parentMetaId,
                item.nextUpSeedSeasonNumber,
                item.nextUpSeedEpisodeNumber,
            ) !in dismissedKeys
        }.stampedWithSeedWatchedAt(seeds.candidates)
    }
}
