package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonRepository

private val loopbackManifestPrefixes = listOf("http://127.0.0.1", "http://localhost")

object TvosDebugAddon {
    private val log = Logger.withTag("TvosDebugAddon")

    fun sync(manifestUrl: String?) {
        TvosHome.afterDefaultAddon { syncNow(manifestUrl?.trim()?.takeIf(String::isNotEmpty)) }
    }

    private suspend fun syncNow(manifestUrl: String?) {
        AddonRepository.uiState.value.addons
            .map { addon -> addon.manifestUrl }
            .filter { installedUrl -> installedUrl.isLoopback() && installedUrl != manifestUrl }
            .forEach { staleUrl ->
                log.i { "Removing debug addon $staleUrl" }
                AddonRepository.removeAddon(staleUrl)
            }
        if (manifestUrl == null) return
        when (val result = AddonRepository.addAddon(manifestUrl)) {
            is AddAddonResult.Success -> log.i { "Installed debug addon ${result.manifest.name}" }
            else -> log.i { "Debug addon $manifestUrl: $result" }
        }
    }

    private fun String.isLoopback(): Boolean =
        loopbackManifestPrefixes.any { prefix -> startsWith(prefix, ignoreCase = true) }
}
