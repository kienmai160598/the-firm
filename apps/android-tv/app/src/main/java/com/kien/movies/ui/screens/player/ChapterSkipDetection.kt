package com.kien.movies.ui.screens.player

import com.kien.movies.data.repository.SkipInterval

internal class ChapterSkipDetection {
    var intervals: List<SkipInterval> = emptyList()
        private set
    private var chapters: List<PlayerChapter> = emptyList()
    private var computedForDurationMs: Long = 0L

    val hasChapters: Boolean
        get() = chapters.isNotEmpty()
    val isComputed: Boolean
        get() = computedForDurationMs > 0L

    fun load(chapters: List<PlayerChapter>) {
        this.chapters = chapters
        computedForDurationMs = 0L
    }

    fun computeIntervals(durationMs: Long): Boolean {
        val usableChapters = chapters.takeIf { it.size >= 2 } ?: return false
        if (durationMs <= 0L) return false
        if (computedForDurationMs == durationMs) return false
        computedForDurationMs = durationMs
        intervals = ChapterSkipIntervals.fromChapters(usableChapters, durationMs)
        return true
    }

    fun forgetIntervals() {
        intervals = emptyList()
        computedForDurationMs = 0L
    }

    fun clear() {
        chapters = emptyList()
        forgetIntervals()
    }
}
