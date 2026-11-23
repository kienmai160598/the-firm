package com.kien.movies.details

import com.nuvio.tvos.TvosCastMember
import com.nuvio.tvos.TvosEpisode
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosPlayAction
import kotlin.math.roundToInt

// The texts Details builds from the bridge's values, as pure functions. Each mirrors a computed
// property of the tvOS view named in its comment; labels the bridge already gives (play label,
// resume label) are shown as they are.

/** Genres (two at most), release line and run time (tvOS DetailsHeroView.facts). */
internal fun heroFacts(details: TvosMetaDetails): List<String> {
    val genres = details.genres.take(HERO_GENRES).joinToString(", ")
    return listOfNotNull(genres.ifEmpty { null }, details.releaseLine, details.runtime)
}

/** "Starring" and the first three cast names (tvOS DetailsHeroView.starring). */
internal fun starring(cast: List<TvosCastMember>): String? {
    val names = cast.take(STARRING_NAMES).map(TvosCastMember::name)
    return if (names.isEmpty()) null else "Starring " + names.joinToString(", ")
}

/** The bar and label under the play button (tvOS ResumeProgressView). */
data class ResumeProgress(val fraction: Float, val label: String)

/** Null when the play action has no resume point; the fraction is clamped as WatchProgressBar does. */
internal fun resumeProgress(action: TvosPlayAction?): ResumeProgress? {
    val resume = action?.resume ?: return null
    return ResumeProgress(fraction = progressFraction(resume.watchedFraction), label = resume.label)
}

/** A progress value safe to draw: 0 to 1, and 0 for NaN. */
internal fun progressFraction(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)

/** Code and title, two spaces apart (tvOS EpisodeCard.heading), e.g. "E1  Pilot". */
internal fun episodeHeading(episode: TvosEpisode): String = listOf(episode.code, episode.title).filter(String::isNotEmpty).joinToString("  ")

/** "Upcoming", release date and run time (tvOS EpisodeCard.details). */
internal fun episodeFacts(episode: TvosEpisode): List<String> {
    val upcoming = if (episode.isReleased) null else "Upcoming"
    return listOfNotNull(upcoming, episode.releaseDate, episode.runtime)
}

/** The episode's watch state for the screen reader and UI tests (tvOS EpisodeCard.watchState). */
internal fun episodeWatchState(episode: TvosEpisode): String = when {
    episode.isWatched -> "Watched"
    episode.progressFraction > 0f -> "${(episode.progressFraction * 100).roundToInt()}% watched"
    else -> ""
}

/** A part watched episode draws a bar over its thumbnail (tvOS EpisodeCard.progressBar). */
internal fun episodeShowsProgress(episode: TvosEpisode): Boolean = !episode.isWatched && episode.progressFraction > 0f

/** Up to two initials, shown when a cast member has no photo (tvOS CastCard.initials). */
internal fun castInitials(name: String): String = name.split(' ')
    .filter(String::isNotEmpty)
    .take(INITIALS)
    .joinToString("") { word -> word.first().toString() }

/** One column of the Information section. */
data class InfoEntry(val title: String, val value: String)

/** Released, Run Time, Rated, Genres, Director; missing ones are left out (tvOS DetailsInfoSection.entries). */
internal fun infoEntries(details: TvosMetaDetails): List<InfoEntry> = listOf(
    "Released" to details.releaseLine,
    "Run Time" to details.runtime,
    "Rated" to details.ageRating,
    "Genres" to joinedOrNull(details.genres),
    "Director" to joinedOrNull(details.directors)
).mapNotNull { (title, value) -> value?.let { InfoEntry(title, it) } }

private fun joinedOrNull(values: List<String>): String? = if (values.isEmpty()) null else values.joinToString(", ")

private const val HERO_GENRES = 2
private const val STARRING_NAMES = 3
private const val INITIALS = 2
