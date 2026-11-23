package com.kien.movies.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosAddon
import com.nuvio.tvos.TvosAddonInstallResult
import com.nuvio.tvos.TvosAddons
import com.nuvio.tvos.phone.AddonInstallOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The install panel's last result (tvOS `AddonInstallStatus`). */
sealed interface AddonInstallStatus {
    data object Installing : AddonInstallStatus

    data class Installed(val name: String) : AddonInstallStatus

    data class Failed(val message: String) : AddonInstallStatus
}

enum class AddonMove(val offset: Int) {
    Up(-1),
    Down(1)
}

data class AddonsUiState(val addons: List<TvosAddon>, val installStatus: AddonInstallStatus? = null) {
    val isInstalling: Boolean get() = installStatus == AddonInstallStatus.Installing
}

/**
 * Installed addons and the install panel (tvOS AddonsModel.swift). Typing a URL and "Add from
 * phone" install through the same call, `TvosAddons.install` via [BridgeFlows.installAddon];
 * core's `normalizeManifestUrl` turns `stremio://` into `https://`.
 */
class AddonsViewModel(
    initial: List<TvosAddon> = TvosAddons.current(),
    addons: Flow<List<TvosAddon>> = BridgeFlows.addons(),
    private val installer: suspend (String) -> TvosAddonInstallResult = BridgeFlows::installAddon
) : ViewModel() {
    private val installStatus = MutableStateFlow<AddonInstallStatus?>(null)

    val state: StateFlow<AddonsUiState> = combine(addons, installStatus, ::AddonsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AddonsUiState(initial))

    fun install(rawUrl: String) {
        val url = rawUrl.trim()
        if (url.isEmpty() || installStatus.value == AddonInstallStatus.Installing) return
        installStatus.value = AddonInstallStatus.Installing
        viewModelScope.launch { installStatus.value = installer(url).toStatus() }
    }

    /** Installs what a phone proposed and the TV confirmed, one URL after the other. */
    fun installFromPhone(urls: List<String>, onDone: (List<AddonInstallOutcome>) -> Unit) {
        viewModelScope.launch { onDone(urls.map { url -> installer(url).toOutcome(url) }) }
    }

    fun toggle(addon: TvosAddon) = TvosAddons.setEnabled(addon.manifestUrl, !addon.enabled)

    fun remove(addon: TvosAddon) = TvosAddons.remove(addon.manifestUrl)

    fun move(addon: TvosAddon, move: AddonMove) {
        val addons = state.value.addons
        val to = moveDestination(addons, addon.manifestUrl, move) ?: return
        TvosAddons.move(addons.indexOfFirst { candidate -> candidate.manifestUrl == addon.manifestUrl }, to)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Index [manifestUrl] moves to, or null at the edge of the list (tvOS `destination(of:_:)`). */
fun moveDestination(addons: List<TvosAddon>, manifestUrl: String, move: AddonMove): Int? {
    val from = addons.indexOfFirst { addon -> addon.manifestUrl == manifestUrl }
    if (from < 0) return null
    return (from + move.offset).takeIf { to -> to in addons.indices }
}

/** The row that takes focus after [removedUrl] leaves: the next one, else the one above. */
fun neighbourAfterRemoval(addons: List<TvosAddon>, removedUrl: String): String? {
    val index = addons.indexOfFirst { addon -> addon.manifestUrl == removedUrl }
    if (index < 0) return null
    return (addons.getOrNull(index + 1) ?: addons.getOrNull(index - 1))?.manifestUrl
}

private fun TvosAddonInstallResult.toStatus(): AddonInstallStatus = when (this) {
    is TvosAddonInstallResult.Installed -> AddonInstallStatus.Installed(name)
    is TvosAddonInstallResult.Failed -> AddonInstallStatus.Failed(message)
}

private fun TvosAddonInstallResult.toOutcome(url: String): AddonInstallOutcome = when (this) {
    is TvosAddonInstallResult.Installed -> AddonInstallOutcome.Installed(url, name)
    is TvosAddonInstallResult.Failed -> AddonInstallOutcome.Failed(url, message)
}
