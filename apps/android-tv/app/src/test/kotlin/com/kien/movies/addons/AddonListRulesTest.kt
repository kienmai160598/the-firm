package com.kien.movies.addons

import com.nuvio.tvos.TvosAddon
import org.junit.Assert.assertEquals
import org.junit.Test

class AddonListRulesTest {
    private val addons = listOf(addon("a"), addon("b"), addon("c"))

    @Test
    fun anAddonMovesOneStepAndNotPastEitherEnd() {
        val destinations = listOf(
            moveDestination(addons, "a", AddonMove.Up),
            moveDestination(addons, "a", AddonMove.Down),
            moveDestination(addons, "c", AddonMove.Up),
            moveDestination(addons, "c", AddonMove.Down),
            moveDestination(addons, "missing", AddonMove.Up)
        )

        assertEquals(listOf(null, 1, 1, null, null), destinations)
    }

    @Test
    fun focusAfterARemovalGoesToTheNextRowElseTheOneAbove() {
        val neighbours = listOf(
            neighbourAfterRemoval(addons, "a"),
            neighbourAfterRemoval(addons, "c"),
            neighbourAfterRemoval(listOf(addon("a")), "a")
        )

        assertEquals(listOf("b", "b", null), neighbours)
    }

    private fun addon(url: String) = TvosAddon(
        manifestUrl = url,
        name = url,
        version = "",
        summary = "",
        logoUrl = null,
        capabilities = emptyList(),
        enabled = true,
        isRefreshing = false,
        errorMessage = null
    )
}
