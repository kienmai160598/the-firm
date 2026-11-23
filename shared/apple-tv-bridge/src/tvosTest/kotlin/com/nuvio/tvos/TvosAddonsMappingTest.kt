package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.ManagedAddon
import kotlin.test.Test
import kotlin.test.assertEquals

class TvosAddonsMappingTest {
    @Test
    fun capabilitiesFollowAFixedOrderAndCountCatalogsDeclaredOutsideResources() {
        val manifest = manifest(
            resources = listOf("subtitles", "stream", "addon_catalog", "stream"),
            catalogs = listOf(AddonCatalog(type = "movie", id = "top", name = "Popular")),
        )

        val capabilities = addonCapabilities(manifest)

        assertEquals(listOf("Catalogs", "Streams", "Subtitles"), capabilities)
    }

    @Test
    fun anAddonWhoseManifestFailedToLoadShowsItsUrlAndNoCapabilities() {
        val brokenUrl = "https://example.invalid/manifest.json"
        val broken = ManagedAddon(manifestUrl = brokenUrl, errorMessage = "Timed out")

        val addon = broken.toTvosAddon()

        assertEquals(brokenUrl, addon.name)
        assertEquals(emptyList(), addon.capabilities)
        assertEquals("Timed out", addon.errorMessage)
    }

    private fun manifest(resources: List<String>, catalogs: List<AddonCatalog>) =
        AddonManifest(
            id = "test.addon",
            name = "Test",
            description = "",
            version = "1.0.0",
            resources = resources.map { name -> AddonResource(name, types = listOf("movie")) },
            types = listOf("movie"),
            catalogs = catalogs,
            transportUrl = "https://example.invalid",
        )
}
