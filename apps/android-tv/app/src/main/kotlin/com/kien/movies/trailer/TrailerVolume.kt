package com.kien.movies.trailer

import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** The trailer's timing, as tvOS HeroTrailer.swift. */
object TrailerMotion {
    /** Focus held this long before the trailer plays. */
    const val START_DELAY_MILLIS = 1_000L

    /** The backdrop fades into the trailer, and its sound in, over this. */
    const val FADE_IN_MILLIS = 1_200

    /** Picture and sound fade out over this. */
    const val FADE_OUT_MILLIS = 600
}

/** Fades a trailer's sound in or out in small steps, so it never starts or stops with a jump (tvOS TrailerVolume.swift). */
internal object TrailerVolume {
    private const val STEPS = 24

    suspend fun ramp(player: Player, to: Float, millis: Int) {
        val start = player.volume
        val step = millis.toLong() / STEPS
        for (index in 1..STEPS) {
            delay(step)
            player.volume = start + (to - start) * index / STEPS
        }
    }
}
