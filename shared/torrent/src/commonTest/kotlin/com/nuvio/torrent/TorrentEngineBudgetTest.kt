package com.nuvio.torrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val MIB = 1024L * 1024L

class TorrentEngineBudgetTest {
    @Test
    fun theCapIsAQuarterOfTheFreeMemory() {
        val budget = budgetFor(availableBytes = 600 * MIB)

        assertEquals(TorrentEngineBudget(150 * MIB, 75 * MIB, 16 * MIB, 150 * MIB * 3 / 4), budget)
    }

    @Test
    fun theCapStopsAt192MiBHoweverMuchIsFree() {
        assertEquals(MAX_STORE_CAP_BYTES, budgetFor(availableBytes = 8L * 1024 * MIB).storeCapBytes)
    }

    @Test
    fun theCapNeverDropsBelow64MiB() {
        assertEquals(MIN_STORE_CAP_BYTES, budgetFor(availableBytes = 100 * MIB).storeCapBytes)
        assertEquals(MIN_STORE_CAP_BYTES, budgetFor(availableBytes = 0).storeCapBytes)
        assertEquals(MIN_STORE_CAP_BYTES, budgetFor(availableBytes = -1).storeCapBytes)
    }

    @Test
    fun everyBudgetPassesTheEngineChecks() {
        val samples = listOf(-1L, 0L, 1L, 255 * MIB, 256 * MIB + 3, 777 * MIB + 1, 768 * MIB, Long.MAX_VALUE)

        samples.map(::budgetFor).forEach { budget ->
            with(budget) {
                assertTrue(storeCapBytes > 0 && aheadBytes > 0 && behindBytes > 0 && evictionThresholdBytes > 0, "$budget")
                assertTrue(aheadBytes + behindBytes < storeCapBytes, "ahead and behind fit: $budget")
                assertTrue(evictionThresholdBytes < storeCapBytes, "eviction below the cap: $budget")
            }
        }
    }
}
