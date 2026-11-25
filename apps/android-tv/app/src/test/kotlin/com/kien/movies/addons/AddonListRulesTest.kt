package com.kien.movies.addons

import com.nuvio.tvos.TvosAddon
import com.nuvio.tvos.phone.AddonInstallOutcome
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
            neighbourAfterRemoval(listOf("a", "b", "c"), "a"),
            neighbourAfterRemoval(listOf("a", "b", "c"), "c"),
            neighbourAfterRemoval(listOf("a"), "a"),
            neighbourAfterRemoval(listOf("a", "b"), "missing")
        )

        assertEquals(listOf("b", "b", null, null), neighbours)
    }

    @Test
    fun aPhoneInstallShowsEveryNameOrTheFirstFailure() {
        val installed = AddonInstallOutcome.Installed("u1", "One")
        val alsoInstalled = AddonInstallOutcome.Installed("u2", "Two")
        val failed = AddonInstallOutcome.Failed("u3", "Not a manifest")

        val statuses = listOf(
            phoneInstallStatus(listOf(installed, alsoInstalled)),
            phoneInstallStatus(listOf(failed)),
            phoneInstallStatus(listOf(installed, failed)),
            phoneInstallStatus(emptyList())
        )

        val expected = listOf(
            AddonInstallStatus.Installed("One, Two"),
            AddonInstallStatus.Failed("Not a manifest"),
            AddonInstallStatus.Failed("u3: Not a manifest"),
            null
        )
        assertEquals(expected, statuses)
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
