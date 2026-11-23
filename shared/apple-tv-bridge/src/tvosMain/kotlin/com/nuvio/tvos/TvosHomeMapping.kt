package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonsUiState
import com.nuvio.app.features.addons.firstEnabledManifestError
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.HomeUiState
import com.nuvio.app.features.home.MetaPreview

internal fun tvosHomeState(
    continueWatchingRows: TvosContinueWatchingRows,
    home: HomeUiState,
    addons: AddonsUiState,
    newReleaseRows: List<NewReleaseRow>,
): TvosHomeState =
    TvosHomeState(
        continueWatching = continueWatchingRows.continueWatching,
        upcoming = continueWatchingRows.upcoming,
        rows = withNewReleaseRows(home.sections.map(HomeCatalogSection::toTvosRow), newReleaseRows),
        isLoading = home.isLoading || addons.addons.hasPendingEnabledManifests(),
        errorMessage = home.errorMessage ?: addons.addons.firstEnabledManifestError(),
    )

internal fun HomeCatalogSection.toTvosRow(): TvosRow =
    TvosRow(
        key = key,
        title = title,
        items = items.map(MetaPreview::toTvosItem),
    )

internal fun MetaPreview.toTvosItem(): TvosItem =
    TvosItem(
        metaId = id,
        type = type,
        name = name,
        posterUrl = poster,
        backdropUrl = banner?.takeIf(String::isNotBlank),
        logoUrl = logo?.takeIf(String::isNotBlank),
        overview = description?.takeIf(String::isNotBlank),
        releaseInfo = releaseInfo?.takeIf(String::isNotBlank),
        imdbRating = imdbRating?.takeIf(String::isNotBlank),
        genres = genres,
    )
