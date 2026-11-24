package com.nuvio.tvos

import com.nuvio.app.features.trailer.LetterboxDetector
import com.nuvio.app.features.trailer.LetterboxTracker

/**
 * The tvOS spotlight keeps a trailer hidden until its crop is known, so it samples faster than
 * the phone app, which crops a trailer that is already showing.
 */
private const val SAMPLE_INTERVAL_MS = 150L

/** [LetterboxTracker] calls bars stable after four equal samples; two more cover a slow frame. */
private const val SAMPLE_LIMIT = 6

/**
 * Crops the black bars of a letterboxed trailer, with the detector the phone app uses. Feed it
 * small ARGB frames every [sampleIntervalMs], at most [sampleLimit] of them; it answers the zoom
 * once the bars hold still.
 */
class TvosLetterbox {
    private val tracker = LetterboxTracker()

    val sampleIntervalMs: Long = SAMPLE_INTERVAL_MS
    val sampleLimit: Int = SAMPLE_LIMIT
    val sampleWidth: Int = LetterboxDetector.SAMPLE_WIDTH
    val sampleHeight: Int = LetterboxDetector.SAMPLE_HEIGHT

    /** The zoom that fills the screen, or null until the same bars were seen a few times. */
    fun onFrame(argbPixels: IntArray): Float? {
        val bar = LetterboxDetector.detectBar(argbPixels, sampleWidth, sampleHeight) ?: return null
        return tracker.onSample(bar)
    }
}
