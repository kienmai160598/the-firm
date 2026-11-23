package com.kien.movies.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteViewModelStoresTest {
    private val stores = RouteViewModelStores()

    @Test
    fun twoEntriesGetTwoViewModels() {
        val first = probeIn(entryId = 1)
        val second = probeIn(entryId = 2)

        assertNotSame(first, second)
    }

    @Test
    fun theSameEntryKeepsItsViewModel() {
        assertSame(probeIn(entryId = 1), probeIn(entryId = 1))
    }

    @Test
    fun aRemovedEntryIsCleared() {
        val probe = probeIn(entryId = 1)

        stores.retainOnly(setOf(Router.tabEntryId(RootTab.Home)))

        assertTrue(probe.cleared)
    }

    @Test
    fun tabRootsSurviveWhileAnotherTabIsShown() {
        val home = probeIn(entryId = Router.tabEntryId(RootTab.Home))

        stores.retainOnly(setOf(Router.tabEntryId(RootTab.Search)))

        assertFalse(home.cleared)
    }

    private fun probeIn(entryId: Long): Probe = ViewModelProvider.create(stores.owner(entryId), ProbeFactory)[Probe::class]

    class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private object ProbeFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = Probe() as T
    }
}
