package com.nuvio.tvos

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibrarySection
import com.nuvio.app.features.library.LibraryUiState
import com.nuvio.app.features.library.toMetaPreview
import com.nuvio.app.features.tracking.TrackingMembershipApplyResult

internal fun tvosLibraryState(library: LibraryUiState): TvosLibraryState {
    val rows = library.sections
        .filter { section -> section.items.isNotEmpty() }
        .map(LibrarySection::toTvosRow)
    return TvosLibraryState(
        rows = rows,
        isLoading = rows.isEmpty() && (library.isLoading || !library.isLoaded),
        errorMessage = library.errorMessage?.takeIf(String::isNotBlank),
    )
}

internal fun LibrarySection.toTvosRow(): TvosRow =
    TvosRow(
        key = "library:$type",
        title = displayTitle,
        items = items.map(LibraryItem::toTvosItem),
    )

internal fun LibraryItem.toTvosItem(): TvosItem = toMetaPreview().toTvosItem()

internal fun tvosLibraryToggle(
    wasSaved: Boolean,
    result: TrackingMembershipApplyResult,
): TvosLibraryToggle =
    when {
        result.requiresRemovalConfirmation -> TvosLibraryToggle.NEEDS_CONFIRMATION
        wasSaved -> TvosLibraryToggle.REMOVED
        else -> TvosLibraryToggle.SAVED
    }
