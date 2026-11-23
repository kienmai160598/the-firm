package com.kien.movies.home

import com.kien.movies.design.DpadRepeatGate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Held arrows: repeats pass at most once per gate, vertical moves get the longer gate. */
class DpadRepeatGateTest {
    @Test
    fun horizontalRepeatsPassOncePerGate() {
        val gate = DpadRepeatGate(horizontalGateMs = 80, verticalGateMs = 112)

        val blocked = listOf(1_000L, 1_050L, 1_080L, 1_100L).map { time -> gate.blocks(isVertical = false, eventTimeMs = time) }

        assertEquals(listOf(false, true, false, true), blocked)
    }

    @Test
    fun verticalRepeatsWaitForTheLongerGate() {
        val gate = DpadRepeatGate(horizontalGateMs = 80, verticalGateMs = 112)

        val blocked = listOf(1_000L, 1_100L, 1_112L).map { time -> gate.blocks(isVertical = true, eventTimeMs = time) }

        assertEquals(listOf(false, true, false), blocked)
    }
}
