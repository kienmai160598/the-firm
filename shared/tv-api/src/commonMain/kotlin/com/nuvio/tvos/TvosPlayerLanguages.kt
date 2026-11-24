package com.nuvio.tvos

import com.nuvio.app.features.player.AudioLanguageOption
import com.nuvio.app.features.player.SubtitleLanguageOption

const val TVOS_LANGUAGE_DEVICE = AudioLanguageOption.DEVICE
const val TVOS_LANGUAGE_OFF = SubtitleLanguageOption.NONE

/**
 * Base language codes (ISO 639-1, "pt" for "pt-BR") a player tries in order. Empty audio keeps
 * the file's default track; empty subtitles turn them off.
 */
data class TvosPlayerLanguages(
    val audio: List<String>,
    val subtitles: List<String>,
) {
    val showsSubtitles: Boolean get() = subtitles.isNotEmpty()
}

internal fun playerLanguages(
    audio: List<String?>,
    subtitles: List<String?>,
    deviceLanguage: String,
): TvosPlayerLanguages {
    val subtitlesOff = subtitles.firstOrNull() == SubtitleLanguageOption.NONE
    return TvosPlayerLanguages(
        audio = baseCodes(audio, deviceLanguage),
        subtitles = if (subtitlesOff) emptyList() else baseCodes(subtitles, deviceLanguage),
    )
}

private val notALanguage = setOf(
    AudioLanguageOption.DEFAULT,
    AudioLanguageOption.ORIGINAL,
    SubtitleLanguageOption.NONE,
    SubtitleLanguageOption.FORCED,
)

private fun baseCodes(codes: List<String?>, deviceLanguage: String): List<String> =
    codes.asSequence()
        .filterNotNull()
        .map { code -> if (code == TVOS_LANGUAGE_DEVICE) deviceLanguage else code }
        .filterNot { code -> code.isBlank() || code in notALanguage }
        .map { code -> code.substringBefore('-').lowercase() }
        .distinct()
        .toList()
