package com.nuvio.tvos

import com.nuvio.app.core.i18n.localizedMediaTypeLabel
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.enabledAddons
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_catalog_default_title
import org.jetbrains.compose.resources.getString

private const val GENRE_EXTRA = "genre"
private val yearOption = Regex("""^\d{4}$""")

/** A catalog that needs a year as its genre, such as Cinemeta's "New", asked for its newest year. */
internal data class NewReleaseCatalog(
    val key: String,
    val groupPrefix: String,
    val title: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val year: String,
)

/** A loaded New row and the key prefix of the addon rows of the same type it sits after. */
internal data class NewReleaseRow(
    val groupPrefix: String,
    val row: TvosRow,
)

internal fun newReleaseCatalogs(addons: List<ManagedAddon>): List<NewReleaseCatalog> =
    addons
        .enabledAddons()
        .flatMap { addon ->
            val manifest = addon.manifest ?: return@flatMap emptyList()
            manifest.catalogs.mapNotNull { catalog ->
                newReleaseCatalog(addon.manifestUrl, manifest.id, catalog)
            }
        }.distinctBy(NewReleaseCatalog::key)

private fun newReleaseCatalog(
    manifestUrl: String,
    manifestId: String,
    catalog: AddonCatalog,
): NewReleaseCatalog? {
    val required = catalog.extra.filter { it.isRequired }.singleOrNull() ?: return null
    if (required.name != GENRE_EXTRA) return null
    val newestYear = required.options.filter(yearOption::matches).maxOrNull() ?: return null
    val groupPrefix = "$manifestId:${catalog.type}:"
    return NewReleaseCatalog(
        key = groupPrefix + catalog.id,
        groupPrefix = groupPrefix,
        title =
            runBlocking {
                getString(
                    Res.string.home_catalog_default_title,
                    catalog.name,
                    localizedMediaTypeLabel(catalog.type),
                )
            },
        manifestUrl = manifestUrl,
        type = catalog.type,
        catalogId = catalog.id,
        year = newestYear,
    )
}

/**
 * Puts each New row right after the first row of its addon and type, so Popular Movies is
 * followed by New Movies. A New row with no such row goes last. Nothing is added before the
 * addon rows have loaded, so New rows never stand alone at the top of Home.
 */
internal fun withNewReleaseRows(
    rows: List<TvosRow>,
    newRows: List<NewReleaseRow>,
): List<TvosRow> {
    if (rows.isEmpty()) return rows
    val result = rows.toMutableList()
    newRows.forEach { newRow ->
        if (result.any { it.key == newRow.row.key }) return@forEach
        val anchor = result.indexOfFirst { it.key.startsWith(newRow.groupPrefix) }
        if (anchor < 0) result += newRow.row else result.add(anchor + 1, newRow.row)
    }
    return result
}
