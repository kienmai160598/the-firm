package com.kien.movies.details

import com.nuvio.tvos.TvosDetailsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DetailsScreenStateTest {
    @Test
    fun aRestartedObservationKeepsTheLoadedDetails() {
        val loaded = details()

        val kept = keptDetails(previous = loaded, state = TvosDetailsState(details = null, isLoading = true, errorMessage = null))

        assertSame(loaded, kept)
    }

    @Test
    fun detailsWinOverLoadingAndError() {
        val loaded = details()

        val state = detailsScreenState(loaded, TvosDetailsState(loaded, isLoading = true, errorMessage = "Timed out"), selectedSeasonNumber = null)

        assertEquals(DetailsScreenState.Loaded(loaded, selectedSeason = null), state)
    }

    @Test
    fun noDetailsWhileLoadingIsLoading() {
        val state = detailsScreenState(null, TvosDetailsState(null, isLoading = true, errorMessage = "Timed out"), selectedSeasonNumber = null)

        assertEquals(DetailsScreenState.Loading, state)
    }

    @Test
    fun anErrorWithoutAMessageShowsTheTvosText() {
        val state = detailsScreenState(null, TvosDetailsState(null, isLoading = false, errorMessage = null), selectedSeasonNumber = null)

        assertEquals(DetailsScreenState.Error("Could not load details"), state)
    }

    @Test
    fun thePickedSeasonWinsOverThePlayActionSeason() {
        val show = details(seasons = listOf(season(1), season(2), season(3)), primaryAction = playAction(seasonNumber = 2))

        assertEquals(3, selectedSeason(show, selectedNumber = 3)?.number)
    }

    @Test
    fun withoutAPickTheSeasonOfResumeOrNextUpIsSelected() {
        val show = details(seasons = listOf(season(1), season(2)), primaryAction = playAction(seasonNumber = 2))

        assertEquals(2, selectedSeason(show, selectedNumber = null)?.number)
    }

    @Test
    fun anUnknownSeasonFallsBackToTheFirst() {
        val show = details(seasons = listOf(season(1), season(2)), primaryAction = playAction(seasonNumber = 9))

        assertEquals(1, selectedSeason(show, selectedNumber = 7)?.number)
    }

    @Test
    fun aTitleWithoutSeasonsSelectsNone() {
        assertNull(selectedSeason(details(), selectedNumber = 1))
    }
}
