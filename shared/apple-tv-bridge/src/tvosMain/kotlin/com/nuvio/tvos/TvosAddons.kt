package com.nuvio.tvos

import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.ManagedAddon
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TvosAddon(
    val manifestUrl: String,
    val name: String,
    val summary: String,
    val providesStreams: Boolean,
    val enabled: Boolean,
    val errorMessage: String?,
)

sealed interface TvosAddonInstallResult {
    data class Installed(val name: String) : TvosAddonInstallResult
    data class Failed(val message: String) : TvosAddonInstallResult
}

object TvosAddons {
    private val mainScope = MainScope()

    fun observe(onChange: (List<TvosAddon>) -> Unit): TvosCancellable {
        TvosHome.start()
        val job = mainScope.launch {
            AddonRepository.uiState
                .map { state -> state.addons.map(ManagedAddon::toTvosAddon) }
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun install(rawUrl: String, onResult: (TvosAddonInstallResult) -> Unit) {
        mainScope.launch { onResult(installNow(rawUrl.trim())) }
    }

    fun remove(manifestUrl: String) {
        AddonRepository.removeAddon(manifestUrl)
    }

    fun setEnabled(manifestUrl: String, enabled: Boolean) {
        AddonRepository.setAddonEnabled(manifestUrl, enabled)
    }

    private suspend fun installNow(rawUrl: String): TvosAddonInstallResult =
        when (val result = AddonRepository.addAddon(rawUrl)) {
            is AddAddonResult.Success -> TvosAddonInstallResult.Installed(result.manifest.name)
            is AddAddonResult.TrackingConsentRequired ->
                installWithConsent(AddonRepository.installWithTrackingConsent(result.manifest))
            is AddAddonResult.Error -> TvosAddonInstallResult.Failed(result.message)
        }

    private fun installWithConsent(result: AddAddonResult): TvosAddonInstallResult =
        when (result) {
            is AddAddonResult.Success -> TvosAddonInstallResult.Installed(result.manifest.name)
            is AddAddonResult.Error -> TvosAddonInstallResult.Failed(result.message)
            is AddAddonResult.TrackingConsentRequired ->
                TvosAddonInstallResult.Failed("Tracking consent was not recorded")
        }
}

private fun ManagedAddon.toTvosAddon(): TvosAddon = TvosAddon(
    manifestUrl = manifestUrl,
    name = displayTitle(),
    summary = manifest?.description.orEmpty(),
    providesStreams = manifest?.resources.orEmpty().any { resource -> resource.name == "stream" },
    enabled = enabled,
    errorMessage = errorMessage,
)

private fun ManagedAddon.displayTitle(): String =
    userSetName?.takeIf(String::isNotBlank) ?: manifest?.name ?: manifestUrl
