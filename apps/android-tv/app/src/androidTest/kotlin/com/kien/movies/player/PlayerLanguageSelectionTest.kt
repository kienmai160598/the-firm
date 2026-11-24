package com.kien.movies.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nuvio.tvos.TvosPlayerLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented: media3 normalises language codes through android.text.TextUtils.
@RunWith(AndroidJUnit4::class)
class PlayerLanguageSelectionTest {
    private val defaults = TrackSelectionParameters.Builder().build()

    @Test
    fun theSettingsLanguagesBecomeThePreferredOnesInOrder() {
        val languages = TvosPlayerLanguages(audio = listOf("vi", "en"), subtitles = listOf("vi", "en"))

        val parameters = PlayerLanguageSelection.applied(defaults, languages)

        assertEquals(listOf("vi", "en"), parameters.preferredAudioLanguages)
        assertEquals(listOf("vi", "en"), parameters.preferredTextLanguages)
        assertFalse(C.TRACK_TYPE_TEXT in parameters.disabledTrackTypes)
    }

    @Test
    fun subtitlesOffDisablesTheTextTracks() {
        val languages = TvosPlayerLanguages(audio = listOf("en"), subtitles = emptyList())

        val parameters = PlayerLanguageSelection.applied(defaults, languages)

        assertTrue(C.TRACK_TYPE_TEXT in parameters.disabledTrackTypes)
    }
}
