package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosSubtitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun startsWithTheFirstOnlineSubtitleInTheFirstPreferredLanguageNoTrackHas() {
        val online = OnlineSubtitleChoice.inSourceOrder(
            listOf(subtitle("eng", "English", 1), subtitle("vie", "Vietnamese", 1), subtitle("vie", "Vietnamese", 2)),
        )

        val choice = OnlineSubtitleChoice.startingChoice(online, preferred = listOf("vi", "en"), embeddedLanguages = listOf("eng"))

        assertEquals("https://subs.test/vie/1.srt", choice?.url)
    }

    @Test
    fun startsWithTheEmbeddedTrackWhenItHasThePreferredLanguage() {
        val online = OnlineSubtitleChoice.inSourceOrder(listOf(subtitle("vi", "Vietnamese", 1)))

        val choice = OnlineSubtitleChoice.startingChoice(online, preferred = listOf("vi"), embeddedLanguages = listOf("vie"))

        assertNull(choice)
    }

    private fun subtitle(language: String, label: String, number: Int) = TvosSubtitle(
        url = "https://subs.test/$language/$number.srt",
        language = language,
        languageLabel = label,
        source = "OpenSubtitles",
    )
}
