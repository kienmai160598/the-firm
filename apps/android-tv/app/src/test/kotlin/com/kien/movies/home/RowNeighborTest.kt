package com.kien.movies.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Port of apps/apple-tv/Tests/RowNeighborTests.swift. */
class RowNeighborTest {
    @Test
    fun focusMovesToTheNextCardElseThePreviousElseNowhere() {
        val keys = listOf("a", "b", "c")

        val neighbours = listOf("a", "b", "c", "x").map { key -> RowNeighbor.key(beside = key, keys = keys) }

        assertEquals(listOf("b", "c", "b", null), neighbours)
        assertNull(RowNeighbor.key(beside = "a", keys = listOf("a")))
    }
}
