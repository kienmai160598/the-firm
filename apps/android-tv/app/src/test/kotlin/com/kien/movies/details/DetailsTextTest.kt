package com.kien.movies.details

import com.nuvio.tvos.TvosCastMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsTextTest {
    @Test
    fun noResumePointShowsNoResumeBar() {
        assertNull(resumeProgress(playAction(resume = null)))
    }

    @Test
    fun theResumeLabelIsTheBridgeLabelWithItsFraction() {
        val action = playAction(resume = resumePoint(label = "Resume from 12:00", fraction = 0.25f))

        assertEquals(ResumeProgress(fraction = 0.25f, label = "Resume from 12:00"), resumeProgress(action))
    }

    @Test
    fun aResumeFractionOutOfRangeIsClamped() {
        assertEquals(1f, resumeProgress(playAction(resume = resumePoint(fraction = 1.4f)))?.fraction)
        assertEquals(0f, resumeProgress(playAction(resume = resumePoint(fraction = Float.NaN)))?.fraction)
    }

    @Test
    fun aPartWatchedEpisodeShowsItsRoundedPercentage() {
        val partWatched = episode(progressFraction = 0.416f)

        assertEquals("42% watched", episodeWatchState(partWatched))
        assertEquals(true, episodeShowsProgress(partWatched))
    }

    @Test
    fun aWatchedEpisodeSaysWatchedAndDrawsNoBar() {
        val watched = episode(isWatched = true, progressFraction = 0.95f)

        assertEquals("Watched", episodeWatchState(watched))
        assertEquals(false, episodeShowsProgress(watched))
    }

    @Test
    fun anUnstartedEpisodeHasNoWatchState() {
        assertEquals("", episodeWatchState(episode(progressFraction = 0f)))
    }

    @Test
    fun anEpisodeHeadingJoinsCodeAndTitleWithTwoSpaces() {
        assertEquals("E1  Pilot", episodeHeading(episode(code = "E1", title = "Pilot")))
        assertEquals("Pilot", episodeHeading(episode(code = "", title = "Pilot")))
    }

    @Test
    fun anUnreleasedEpisodeSaysUpcomingFirst() {
        assertEquals(listOf("Upcoming", "2026-12-01"), episodeFacts(episode(isReleased = false, releaseDate = "2026-12-01")))
    }

    @Test
    fun heroFactsTakeTwoGenresThenReleaseAndRunTime() {
        val show = details(genres = listOf("Drama", "Crime", "Thriller"), releaseLine = "2008", runtime = "47 min")

        assertEquals(listOf("Drama, Crime", "2008", "47 min"), heroFacts(show))
    }

    @Test
    fun starringNamesTheFirstThreeCastMembers() {
        val cast = listOf("A", "B", "C", "D").map { name -> TvosCastMember(name, role = null, photoUrl = null) }

        assertEquals("Starring A, B, C", starring(cast))
        assertNull(starring(emptyList()))
    }

    @Test
    fun informationLeavesOutMissingValues() {
        val show = details(releaseLine = "2008", directors = listOf("Vince Gilligan", "Adam Bernstein"))

        assertEquals(
            listOf(InfoEntry("Released", "2008"), InfoEntry("Director", "Vince Gilligan, Adam Bernstein")),
            infoEntries(show)
        )
    }

    @Test
    fun initialsTakeTheFirstLetterOfTwoWords() {
        assertEquals("BL", castInitials("Bryan  Lee Cranston"))
    }
}
