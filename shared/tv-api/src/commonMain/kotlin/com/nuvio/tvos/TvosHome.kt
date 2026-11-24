package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.AddonsUiState
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.isWaitingForFirstEnabledManifest
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.HomeRepository
import com.nuvio.app.features.home.buildAddonCatalogRefreshSignature
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val CINEMETA_MANIFEST_URL = "https://v3-cinemeta.strem.io/manifest.json"
private const val ADDONS_READY_TIMEOUT_MS = 15_000L

object TvosHome {
    private val log = Logger.withTag("TvosHome")
    private val mainScope = MainScope()
    private var started = false
    private var defaultAddonJob: Job? = null
    private val startupAddonJobs = mutableListOf<Job>()

    fun start() {
        if (started) return
        started = true
        AddonRepository.initialize()
        defaultAddonJob = mainScope.launch { installDefaultAddonWhenNoneInstalled() }
        mainScope.launch { refreshHomeOnAddonChanges() }
        mainScope.launch { TvosNewReleases.refreshOnAddonChanges() }
        TvosContinueWatching.ensureLoaded()
        mainScope.launch { TvosContinueWatching.refreshWatchSource() }
    }

    fun observeHome(onChange: (TvosHomeState) -> Unit): TvosCancellable {
        TvosContinueWatching.ensureLoaded()
        val job = mainScope.launch {
            combine(
                TvosContinueWatching.rows(),
                HomeRepository.uiState,
                AddonRepository.uiState,
                TvosNewReleases.rows,
                ::tvosHomeState,
            )
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun removeFromContinueWatching(key: String) {
        TvosContinueWatching.remove(key)
    }

    fun markContinueWatchingWatched(key: String) {
        TvosContinueWatching.markWatched(key)
    }

    internal fun afterDefaultAddon(block: suspend () -> Unit) {
        startupAddonJobs += mainScope.launch {
            defaultAddonJob?.join()
            block()
        }
    }

    internal suspend fun awaitStartupAddons() {
        defaultAddonJob?.join()
        startupAddonJobs.toList().joinAll()
    }

    /** Waits, up to a limit, until an enabled addon can answer meta requests. */
    internal suspend fun awaitAddonsReadyForMeta() {
        withTimeoutOrNull(ADDONS_READY_TIMEOUT_MS) {
            awaitStartupAddons()
            AddonRepository.uiState.first(AddonsUiState::isReadyForMeta)
        }
    }

    private suspend fun installDefaultAddonWhenNoneInstalled() {
        if (AddonRepository.uiState.value.addons.isNotEmpty()) return
        when (val result = AddonRepository.addAddon(CINEMETA_MANIFEST_URL)) {
            is AddAddonResult.Success -> log.i { "Installed default addon ${result.manifest.name}" }
            else -> log.e { "Default addon install failed: $result" }
        }
    }

    private suspend fun refreshHomeOnAddonChanges() {
        AddonRepository.uiState
            .map { state -> buildAddonCatalogRefreshSignature(state.addons) }
            .distinctUntilChanged()
            .collectLatest {
                val enabledAddons = AddonRepository.uiState.value.addons.enabledAddons()
                if (enabledAddons.isWaitingForFirstEnabledManifest()) return@collectLatest
                HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
                HomeRepository.refresh(enabledAddons)
            }
    }
}

private fun AddonsUiState.isReadyForMeta(): Boolean {
    val enabledAddons = addons.enabledAddons()
    if (enabledAddons.isEmpty()) return false
    return enabledAddons.any { addon -> addon.manifest != null } ||
        !enabledAddons.hasPendingEnabledManifests()
}
