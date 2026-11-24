package com.nuvio.tvos

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvosSeasonWatchedTest {
    @Test
    fun aSeasonIsWatchedWhenEveryReleasedEpisodeIsWatched() {
        val season = season(
            episode("e1", released = true, watched = true),
            episode("e2", released = true, watched = true),
            episode("e3", released = false, watched = false),
        )

        assertTrue(season.isWatched)
    }

    @Test
    fun oneUnwatchedReleasedEpisodeLeavesTheSeasonUnwatched() {
        val season = season(
            episode("e1", released = true, watched = true),
            episode("e2", released = true, watched = false),
        )

        assertFalse(season.isWatched)
    }

    @Test
    fun aSeasonWithNothingReleasedIsNotWatched() {
        val season = season(episode("e1", released = false, watched = false))

        assertFalse(season.isWatched)
        assertFalse(season().isWatched)
    }

    private fun season(vararg episodes: TvosEpisode) =
        TvosSeason(number = 1, title = "Season 1", episodes = episodes.toList())

    private fun episode(videoId: String, released: Boolean, watched: Boolean) =
        TvosEpisode(
            videoId = videoId,
            code = "E1",
            title = videoId,
            overview = null,
            thumbnailUrl = null,
            releaseDate = null,
            runtime = null,
            isReleased = released,
            isWatched = watched,
            progressFraction = 0f,
            streamTarget = TvosStreamTarget(
                type = "series",
                videoId = videoId,
                parentMetaId = "show",
                seasonNumber = 1,
                episodeNumber = 1,
                title = "Show",
                subtitle = null,
                backdropUrl = null,
                posterUrl = null,
                logoUrl = null,
                episodeTitle = null,
                episodeThumbnailUrl = null,
            ),
        )
}
