package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosSubtitle
import kotlin.test.Test
import kotlin.test.assertEquals

// Ported from apps/apple-tv/Tests/MpvSubtitleDownloadTests.swift.
class OnlineSubtitleChoiceTest {
    @Test
    fun keepsSourceOrderWithAtMostFourPerLanguage() {
        val subtitles = listOf(subtitle("fr", "French", 1), subtitle("en", "English", 1)) +
            (1..6).map { number -> subtitle("vi", "Vietnamese", number) }

        val choices = OnlineSubtitleChoice.inSourceOrder(subtitles)

        assertEquals(
            listOf("French", "English", "Vietnamese", "Vietnamese 2", "Vietnamese 3", "Vietnamese 4"),
            choices.map(OnlineSubtitle::label),
        )
        assertEquals("https://subs.test/vi/1.srt", choices[2].url)
        assertEquals("Vietnamese (OpenSubtitles)", choices[2].trackTitle)
    }

    @Test
    fun regionalCodesCountAsTheirLanguage() {
        val subtitles = listOf("pt", "pt-BR", "pt_PT", "PT", "pt-br").mapIndexed { index, code ->
            subtitle(code, "Portuguese", index)
        }

        val choices = OnlineSubtitleChoice.inSourceOrder(subtitles)

        assertEquals(4, choices.size)
    }

    private fun subtitle(language: String, label: String, number: Int) = TvosSubtitle(
        url = "https://subs.test/$language/$number.srt",
        language = language,
        languageLabel = label,
        source = "OpenSubtitles",
    )
}
