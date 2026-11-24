package com.nuvio.tvos

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibrarySection
import com.nuvio.app.features.library.LibraryUiState
import com.nuvio.app.features.tracking.TrackingMembershipApplyResult
import com.nuvio.app.features.tracking.TrackingMembershipRemovalConfirmation
import com.nuvio.app.features.tracking.TrackingProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvosLibraryMappingTest {
    @Test
    fun eachNonEmptySectionBecomesARowInOrderWithItsItems() {
        val library = LibraryUiState(
            sections = listOf(
                LibrarySection("movie", "Movies", listOf(item("tt1", "movie"), item("tt2", "movie"))),
                LibrarySection("series", "Series", emptyList()),
                LibrarySection("watchlist", "Watchlist", listOf(item("tt3", "series"))),
            ),
            isLoaded = true,
        )

        val rows = tvosLibraryState(library).rows

        assertEquals(listOf("Movies", "Watchlist"), rows.map(TvosRow::title))
        assertEquals(listOf("tt1", "tt2"), rows[0].items.map(TvosItem::metaId))
        assertEquals("series", rows[1].items.single().type)
    }

    @Test
    fun anEmptyLibraryIsLoadingOnlyUntilItHasLoaded() {
        assertTrue(tvosLibraryState(LibraryUiState(isLoaded = false)).isLoading)
        assertFalse(tvosLibraryState(LibraryUiState(isLoaded = true)).isLoading)
    }

    @Test
    fun aLibraryWithRowsIsNotLoadingWhileItRefreshes() {
        val library = LibraryUiState(
            sections = listOf(LibrarySection("movie", "Movies", listOf(item("tt1", "movie")))),
            isLoaded = true,
            isLoading = true,
        )

        assertFalse(tvosLibraryState(library).isLoading)
    }

    @Test
    fun toggleOutcomeFollowsTheStateBeforeThePress() {
        val done = TrackingMembershipApplyResult()

        assertEquals(TvosLibraryToggle.SAVED, tvosLibraryToggle(wasSaved = false, result = done))
        assertEquals(TvosLibraryToggle.REMOVED, tvosLibraryToggle(wasSaved = true, result = done))
    }

    @Test
    fun aRemovalThatNeedsConfirmationIsReportedAsSuch() {
        val needsConfirmation = TrackingMembershipApplyResult(
            requiredRemovalConfirmations = listOf(
                TrackingMembershipRemovalConfirmation(TrackingProviderId.TRAKT, impacts = emptySet()),
            ),
        )

        assertEquals(
            TvosLibraryToggle.NEEDS_CONFIRMATION,
            tvosLibraryToggle(wasSaved = true, result = needsConfirmation),
        )
    }

    private fun item(id: String, type: String) =
        LibraryItem(
            id = id,
            type = type,
            name = "Title $id",
            poster = "https://img/$id.jpg",
            savedAtEpochMs = 0,
        )
}
