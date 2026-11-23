package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonExtraProperty
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.ManagedAddon
import kotlin.test.Test
import kotlin.test.assertEquals

class TvosNewReleasesTest {
    @Test
    fun aCatalogThatNeedsAYearAsksForTheNewestYear() {
        val addon =
            addon(
                catalog("top", "Popular"),
                catalog("year", "New", genre(isRequired = true, "2024", "2026", "2025")),
                catalog("genre", "By genre", genre(isRequired = true, "Action", "Drama")),
                catalog("imdbRating", "Featured", genre(isRequired = false, "2026")),
            )

        val catalogs = newReleaseCatalogs(listOf(addon))

        assertEquals(listOf("cinemeta:movie:year"), catalogs.map(NewReleaseCatalog::key))
        assertEquals("2026", catalogs.single().year)
        assertEquals("cinemeta:movie:", catalogs.single().groupPrefix)
    }

    @Test
    fun aDisabledAddonGivesNoNewRows() {
        val addon = addon(catalog("year", "New", genre(isRequired = true, "2026"))).copy(enabled = false)

        assertEquals(emptyList(), newReleaseCatalogs(listOf(addon)))
    }

    @Test
    fun eachNewRowFollowsTheFirstRowOfItsAddonAndType() {
        val rows = listOf(row("cinemeta:movie:top"), row("cinemeta:series:top"), row("other:movie:top"))
        val newRows =
            listOf(
                NewReleaseRow("cinemeta:series:", row("cinemeta:series:year")),
                NewReleaseRow("cinemeta:movie:", row("cinemeta:movie:year")),
                NewReleaseRow("kitsu:anime:", row("kitsu:anime:year")),
            )

        val keys = withNewReleaseRows(rows, newRows).map(TvosRow::key)

        assertEquals(
            listOf(
                "cinemeta:movie:top",
                "cinemeta:movie:year",
                "cinemeta:series:top",
                "cinemeta:series:year",
                "other:movie:top",
                "kitsu:anime:year",
            ),
            keys,
        )
    }

    @Test
    fun newRowsWaitForTheAddonRows() {
        val newRows = listOf(NewReleaseRow("cinemeta:movie:", row("cinemeta:movie:year")))

        assertEquals(emptyList(), withNewReleaseRows(emptyList(), newRows))
    }

    private fun addon(vararg catalogs: AddonCatalog) =
        ManagedAddon(
            manifestUrl = "https://cinemeta/manifest.json",
            manifest =
                AddonManifest(
                    id = "cinemeta",
                    name = "Cinemeta",
                    description = "",
                    version = "1",
                    resources = emptyList(),
                    types = listOf("movie"),
                    catalogs = catalogs.toList(),
                    transportUrl = "https://cinemeta/manifest.json",
                ),
        )

    private fun catalog(
        id: String,
        name: String,
        vararg extra: AddonExtraProperty,
    ) = AddonCatalog(type = "movie", id = id, name = name, extra = extra.toList())

    private fun genre(
        isRequired: Boolean,
        vararg options: String,
    ) = AddonExtraProperty(name = "genre", isRequired = isRequired, options = options.toList())

    private fun row(key: String) = TvosRow(key = key, title = key, items = emptyList())
}
