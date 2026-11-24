package com.kien.movies.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterTest {
    private val details = AppRoute.Details(type = "movie", id = "tt1")

    @Test
    fun startsAtTheHomeRoot() {
        val router = Router()

        assertEquals(listOf(AppRoute.Tab(RootTab.Home)), router.routes())
        assertTrue(router.isAtTabRoot)
    }

    @Test
    fun openPushesOverTheCurrentTab() {
        val router = Router()
        router.selectTab(RootTab.Search)

        router.open(details)

        assertEquals(listOf(AppRoute.Tab(RootTab.Search), details), router.routes())
        assertEquals(RootTab.Search, router.tab)
    }

    @Test
    fun theSameRouteTwiceGetsTwoEntries() {
        val router = Router()

        router.open(details)
        router.open(details)

        val (first, second) = router.stack.value.drop(1)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun backPopsTheTopEntry() {
        val router = Router()
        router.open(details)

        val result = router.back()

        assertEquals(BackResult.Popped, result)
        assertEquals(listOf(AppRoute.Tab(RootTab.Home)), router.routes())
    }

    @Test
    fun backAtAnotherTabRootGoesHome() {
        val router = Router()
        router.selectTab(RootTab.Library)

        val result = router.back()

        assertEquals(BackResult.WentHome, result)
        assertEquals(listOf(AppRoute.Tab(RootTab.Home)), router.routes())
    }

    @Test
    fun backAtHomeSignalsExitAndKeepsTheStack() {
        val router = Router()

        val result = router.back()

        assertEquals(BackResult.Exit, result)
        assertEquals(listOf(AppRoute.Tab(RootTab.Home)), router.routes())
    }

    @Test
    fun backFromADeepStackEndsInExit() {
        val router = Router()
        router.selectTab(RootTab.Search)
        router.open(details)
        router.open(details)

        val results = List(4) { router.back() }

        assertEquals(
            listOf(BackResult.Popped, BackResult.Popped, BackResult.WentHome, BackResult.Exit),
            results
        )
    }

    @Test
    fun selectingATabDropsWhatWasPushed() {
        val router = Router()
        router.open(details)

        router.selectTab(RootTab.Library)

        assertEquals(listOf(AppRoute.Tab(RootTab.Library)), router.routes())
    }

    @Test
    fun openingATabRouteSelectsTheTab() {
        val router = Router()
        router.open(details)

        router.open(AppRoute.Tab(RootTab.Settings))

        assertEquals(listOf(AppRoute.Tab(RootTab.Settings)), router.routes())
    }

    @Test
    fun aTabRootKeepsItsEntryIdAcrossVisits() {
        val router = Router()
        val homeId = router.top.id

        router.selectTab(RootTab.Search)
        router.selectTab(RootTab.Home)

        assertEquals(homeId, router.top.id)
    }

    private fun Router.routes(): List<AppRoute> = stack.value.map(RouteEntry::route)
}
