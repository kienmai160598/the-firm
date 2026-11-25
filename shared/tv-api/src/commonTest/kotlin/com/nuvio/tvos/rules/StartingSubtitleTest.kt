package com.nuvio.tvos.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartingSubtitleTest {
    private val vietnamese = OnlineSubtitle(url = "https://subs.test/vi.srt", language = "vie", languageLabel = "Vietnamese", source = "OpenSubtitles", number = 1)

    @Test
    fun theSubtitleLoadsWhenTheTracksArriveAfterTheOnlineList() {
        val starting = StartingSubtitle(preferred = listOf("vi"))
        starting.online(listOf(vietnamese))

        val choice = starting.tracks(embeddedLanguages = listOf("eng"))

        assertEquals("https://subs.test/vi.srt", choice?.url)
    }

    @Test
    fun nothingLoadsOnceTheViewerPickedASubtitle() {
        val starting = StartingSubtitle(preferred = listOf("vi"))
        starting.tracks(embeddedLanguages = listOf("eng"))
        starting.viewerChose()

        val choice = starting.online(listOf(vietnamese))

        assertNull(choice)
    }
}
