package com.nuvio.tvos.rules

/**
 * The rating and genre line shown once when playback starts (tvOS PlaybackAdvisory.swift).
 * Build it with [of]: null when there is nothing to show.
 */
class PlaybackAdvisory private constructor(
    val ageRating: String?,
    val genres: List<String>,
) {
    val genreLine: String?
        get() = genres.takeIf { it.isNotEmpty() }?.joinToString(", ")

    override fun equals(other: Any?): Boolean =
        other is PlaybackAdvisory && other.ageRating == ageRating && other.genres == genres

    override fun hashCode(): Int = 31 * ageRating.hashCode() + genres.hashCode()

    companion object {
        private const val GENRE_LIMIT = 3

        /** Trims the rating and the genres, keeps at most three genres; null when both are empty. */
        fun of(ageRating: String?, genres: List<String>): PlaybackAdvisory? {
            val rating = ageRating?.trim()?.takeIf { it.isNotEmpty() }
            val cleanGenres = genres.map { it.trim() }.filter { it.isNotEmpty() }
            if (rating == null && cleanGenres.isEmpty()) return null
            return PlaybackAdvisory(rating, cleanGenres.take(GENRE_LIMIT))
        }
    }
}
