package com.nuvio.tvos

import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import kotlin.test.Test
import kotlin.test.assertEquals

class TvosSearchRowsTest {
    @Test
    fun rowsAreTitledByTypeAndNameTheAddonOnlyWhenTwoShareAType() {
        val sections = listOf(
            section(addon = "Cinemeta", type = "movie"),
            section(addon = "Cinemeta", type = "series"),
            section(addon = "Anime Kitsu", type = "series"),
        )

        val titles = searchRows(sections).map(TvosRow::title)

        assertEquals(listOf("Movies", "Cinemeta Series", "Anime Kitsu Series"), titles)
    }

    private fun section(addon: String, type: String) =
        HomeCatalogSection(
            key = "$addon:$type",
            title = "Popular $type",
            subtitle = addon,
            addonName = addon,
            target = CatalogTarget.Addon(
                manifestUrl = "https://$addon/manifest.json",
                contentType = type,
                catalogId = "top",
            ),
            items = emptyList(),
        )
}
