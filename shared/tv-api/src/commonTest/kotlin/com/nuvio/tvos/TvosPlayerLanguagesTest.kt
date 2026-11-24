package com.nuvio.tvos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TvosPlayerLanguagesTest {
    @Test
    fun automaticBecomesTheDeviceLanguage() {
        val languages = playerLanguages(listOf("device", null), listOf("device", "en"), "fr")

        assertEquals(listOf("fr"), languages.audio)
        assertEquals(listOf("fr", "en"), languages.subtitles)
    }

    @Test
    fun offTurnsSubtitlesOffEvenWithAFallback() {
        val languages = playerLanguages(listOf("device"), listOf("none", "en"), "en")

        assertFalse(languages.showsSubtitles)
    }

    @Test
    fun regionalCodesBecomeTheirBaseLanguageOnce() {
        val languages = playerLanguages(listOf("pt-BR", "pt"), listOf("zh-CN", "en"), "en")

        assertEquals(listOf("pt"), languages.audio)
        assertEquals(listOf("zh", "en"), languages.subtitles)
    }

    @Test
    fun theFilesDefaultAudioIsNoPreference() {
        val languages = playerLanguages(listOf("original", "default"), listOf("vi"), "en")

        assertEquals(emptyList(), languages.audio)
        assertEquals(listOf("vi"), languages.subtitles)
    }
}
