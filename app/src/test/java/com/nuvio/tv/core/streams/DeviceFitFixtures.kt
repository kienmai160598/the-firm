package com.nuvio.tv.core.streams

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.Video

internal const val GIBIBYTE = 1024L * 1024L * 1024L

internal fun torrent(
    name: String? = null,
    title: String? = null,
    videoSize: Long? = null,
    fileIdx: Int? = 0
): Stream = Stream(
    name = name,
    title = title,
    description = null,
    url = null,
    ytId = null,
    infoHash = "0123456789abcdef0123456789abcdef01234567",
    fileIdx = fileIdx,
    externalUrl = null,
    behaviorHints = StreamBehaviorHints(
        notWebReady = null,
        bingeGroup = null,
        countryWhitelist = null,
        proxyHeaders = null,
        videoSize = videoSize
    ),
    addonName = "Addon",
    addonLogo = null
)

internal fun titleMeta(runtime: String?, episodes: List<Video> = emptyList()): Meta = Meta(
    id = "title", type = ContentType.MOVIE, name = "Title", poster = null,
    posterShape = PosterShape.POSTER, background = null, logo = null, description = null,
    releaseInfo = null, imdbRating = null, genres = emptyList(), runtime = runtime,
    director = emptyList(), cast = emptyList(), videos = episodes, country = null,
    awards = null, language = null, links = emptyList()
)

internal fun episodeVideo(season: Int, episode: Int, runtime: Int?): Video = Video(
    id = "title:$season:$episode", title = "Episode $episode", released = null,
    thumbnail = null, season = season, episode = episode, overview = null, runtime = runtime
)
