package com.kien.movies.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleUriMarkerTest {
    @Test
    fun aMarkedUrlUnmarksToTheOriginal() {
        val url = "https://subs.example/file.srt?lang=en#t=1"

        val roundTrip = SubtitleUriMarker.unmark(SubtitleUriMarker.mark(url))

        assertEquals(url, roundTrip)
    }

    @Test
    fun aStreamUrlIsNotMarked() {
        val unmarked = SubtitleUriMarker.unmark("https://cdn.example/movie.mkv?token=abc")

        assertNull(unmarked)
    }
}
