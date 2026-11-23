package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.AddonSubtitle
import com.nuvio.app.features.player.SubtitleAddonRequest
import com.nuvio.app.features.player.SubtitleRequestExtras
import com.nuvio.app.features.player.addonSubtitleRequests
import com.nuvio.app.features.player.buildSubtitleRequestUrl
import com.nuvio.app.features.player.getLanguageLabelForCode
import com.nuvio.app.features.player.loadAddonSubtitles
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val OPEN_SUBTITLES_MANIFEST_URL = "https://opensubtitles-v3.strem.io/manifest.json"
private const val OPEN_SUBTITLES_ID = "org.stremio.opensubtitlesv3"
private const val OPEN_SUBTITLES_NAME = "OpenSubtitles"
private const val IMDB_PREFIX = "tt"

object TvosSubtitles {
    private val mainScope = MainScope()
    private val log = Logger.withTag("TvosSubtitles")

    fun load(
        target: TvosStreamTarget,
        stream: TvosStream,
        onResult: (List<TvosSubtitle>) -> Unit,
    ): TvosCancellable = TvosCancellable(
        mainScope.launch { onResult(fetch(target, stream)) },
    )

    private suspend fun fetch(target: TvosStreamTarget, stream: TvosStream): List<TvosSubtitle> {
        val extras = SubtitleRequestExtras(filename = stream.filename)
            .takeIf { it.toPathSegment() != null }
        val requests = addonSubtitleRequests(target.type, target.videoId, extras)
            .ifEmpty { fallbackRequests(target, extras) }
        val subtitles = loadAddonSubtitles(requests).map { it.toTvosSubtitle() }
        log.i { "Loaded ${subtitles.size} subtitles from ${requests.size} addons" }
        return subtitles
    }

    private fun fallbackRequests(
        target: TvosStreamTarget,
        extras: SubtitleRequestExtras?,
    ): List<SubtitleAddonRequest> {
        if (!target.videoId.startsWith(IMDB_PREFIX)) return emptyList()
        val url = buildSubtitleRequestUrl(
            OPEN_SUBTITLES_MANIFEST_URL,
            target.type,
            target.videoId,
            extras,
        )
        return listOf(SubtitleAddonRequest(url, OPEN_SUBTITLES_ID, OPEN_SUBTITLES_NAME))
    }

    private suspend fun AddonSubtitle.toTvosSubtitle(): TvosSubtitle = TvosSubtitle(
        url = url,
        language = language,
        languageLabel = getLanguageLabelForCode(language),
        source = addonName ?: OPEN_SUBTITLES_NAME,
    )
}

data class TvosSubtitle(
    val url: String,
    val language: String,
    val languageLabel: String,
    val source: String,
)
