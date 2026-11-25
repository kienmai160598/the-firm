package com.nuvio.tvos.rules

/**
 * The one line a Continue Watching card shows on its artwork, as in the Apple TV app: the episode
 * and what is left of it ("S1, E1 · 17m"), the time left of a movie ("1h 11m"), or the episode and
 * its state when nothing is left to show ("S1, E2 · Next Up").
 */
object ContinueWatchingCaption {
    private const val SEPARATOR = " · "

    fun of(seasonNumber: Int?, episodeNumber: Int?, state: String): String =
        listOfNotNull(episodeCode(seasonNumber, episodeNumber), state.ifBlank { null })
            .joinToString(SEPARATOR)

    fun timeLeft(hours: Long, minutes: Long): String =
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    private fun episodeCode(seasonNumber: Int?, episodeNumber: Int?): String? =
        if (seasonNumber != null && episodeNumber != null) "S$seasonNumber, E$episodeNumber" else null
}
