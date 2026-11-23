package com.kien.movies.ui.screens.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterSkipDetectionTest {

    private val durationMs = 24 * 60_000L
    private val chapters = PlayerChapters.normalize(
        listOf(
            PlayerChapter(0L, "Prologue"),
            PlayerChapter(90_000L, "OP"),
            PlayerChapter(180_000L, "Episode")
        )
    )
    private val detection = ChapterSkipDetection()

    @Test
    fun `intervals are computed once per duration`() {
        detection.load(chapters)

        val first = detection.computeIntervals(durationMs)
        val second = detection.computeIntervals(durationMs)

        assertTrue(first)
        assertFalse(second)
        assertTrue(detection.intervals.any { it.type == "intro" })
    }

    @Test
    fun `nothing is computed before the duration is known`() {
        detection.load(chapters)

        val computed = detection.computeIntervals(durationMs = 0L)

        assertFalse(computed)
        assertFalse(detection.isComputed)
    }

    @Test
    fun `forgetting the intervals keeps the chapters for a later compute`() {
        detection.load(chapters)
        detection.computeIntervals(durationMs)

        detection.forgetIntervals()

        assertTrue(detection.intervals.isEmpty())
        assertTrue(detection.hasChapters)
        assertTrue(detection.computeIntervals(durationMs))
    }
}
