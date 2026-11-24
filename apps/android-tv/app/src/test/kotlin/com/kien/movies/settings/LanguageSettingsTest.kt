package com.kien.movies.settings

import com.nuvio.tvos.TVOS_LANGUAGE_DEVICE
import com.nuvio.tvos.TVOS_LANGUAGE_OFF
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageSettingsTest {
    @Test
    fun fixedChoicesStayFirstAndLanguagesSortByName() {
        val codes = listOf(TVOS_LANGUAGE_OFF, TVOS_LANGUAGE_DEVICE, "vi", "de", "fr")

        val choices = LanguageKind.Subtitles.choices(codes)

        // French, German, Vietnamese: by English name, not by code.
        assertEquals(listOf(TVOS_LANGUAGE_OFF, TVOS_LANGUAGE_DEVICE, "fr", "de", "vi"), choices)
    }

    @Test
    fun fixedChoicesReadAsAutomaticAndOff() {
        assertEquals("Automatic", LanguageName.of(TVOS_LANGUAGE_DEVICE))
        assertEquals("Off", LanguageName.of(TVOS_LANGUAGE_OFF))
    }

    @Test
    fun aLanguageCodeReadsAsItsEnglishName() {
        assertEquals("Vietnamese", LanguageName.of("vi"))
        assertEquals("Portuguese (Brazil)", LanguageName.of("pt-BR"))
    }
}
