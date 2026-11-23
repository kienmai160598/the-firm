package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosSubtitle

/**
 * One online subtitle the player offers in its subtitle menu (tvOS MpvSubtitleDownload.swift).
 * [number] counts the subtitles of the same language, from 1.
 */
data class OnlineSubtitle(
    val url: String,
    val language: String,
    val languageLabel: String,
    val source: String,
    val number: Int,
) {
    /** Stable menu id, distinct from embedded track ids. */
    val id: String get() = "online:$url"

    /** Menu label: "Vietnamese", then "Vietnamese 2" and on. */
    val label: String get() = if (number > 1) "$languageLabel $number" else languageLabel

    val detail: String get() = "Online from $source"

    /** Title of the track once loaded, e.g. "Vietnamese (OpenSubtitles)". */
    val trackTitle: String get() = "$languageLabel ($source)"
}

/** Which online subtitles the menu lists: source order kept, at most four per language. */
object OnlineSubtitleChoice {
    private const val PER_LANGUAGE_LIMIT = 4

    fun inSourceOrder(subtitles: List<TvosSubtitle>): List<OnlineSubtitle> {
        val countByLanguage = mutableMapOf<String, Int>()
        return subtitles.mapNotNull { subtitle ->
            val language = languagePrefix(subtitle.language)
            val number = (countByLanguage[language] ?: 0) + 1
            if (number > PER_LANGUAGE_LIMIT) return@mapNotNull null
            countByLanguage[language] = number
            OnlineSubtitle(
                url = subtitle.url,
                language = subtitle.language,
                languageLabel = subtitle.languageLabel,
                source = subtitle.source,
                number = number,
            )
        }
    }

    /** The letters before the first non letter: "pt-BR" and "pt" count as one language. */
    private fun languagePrefix(code: String): String = code.lowercase().takeWhile { it.isLetter() }
}
