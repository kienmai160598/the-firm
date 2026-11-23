package com.kien.movies.search

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The typing pause and recent search rules of tvOS SearchModel.swift. No real time: the pause is a
 * gate the test opens, and the scope is unconfined, so a released search runs inside the call.
 */
class SearchQueriesTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private val bridge = RecordingActions()
    private val pause = ManualPause()
    private val queries = SearchQueries(scope, bridge, pause::await)

    @After
    fun cancelScope() = scope.cancel()

    @Test
    fun nothingIsSearchedUntilThe350MsPauseEnds() {
        queries.queryChanged("Breaking Bad")

        assertEquals(listOf(350L), pause.requested)
        assertEquals(emptyList<String>(), bridge.searches)
    }

    @Test
    fun theTrimmedQueryIsSearchedWhenThePauseEnds() {
        queries.queryChanged("  Breaking Bad \n")

        pause.releaseAll()

        assertEquals(listOf("Breaking Bad"), bridge.searches)
    }

    @Test
    fun aNewerKeystrokeCancelsTheOlderSearch() {
        queries.queryChanged("Brea")
        queries.queryChanged("Breaking")

        pause.releaseAll()

        assertEquals(listOf("Breaking"), bridge.searches)
    }

    @Test
    fun aBlankQueryClearsAtOnceAndDropsThePendingSearch() {
        queries.queryChanged("Brea")

        queries.queryChanged("   ")
        pause.releaseAll()

        assertEquals(1, bridge.clears)
        assertEquals(emptyList<String>(), bridge.searches)
    }

    @Test
    fun theSameTrimmedQueryAgainDoesNotSearchAgain() {
        queries.queryChanged("Breaking Bad")
        pause.releaseAll()

        queries.queryChanged("Breaking Bad ")
        pause.releaseAll()

        assertEquals(listOf("Breaking Bad"), bridge.searches)
    }

    @Test
    fun aQueryTypedAgainAfterClearingIsSearchedAgain() {
        queries.queryChanged("Breaking Bad")
        pause.releaseAll()
        queries.queryChanged("")

        queries.queryChanged("Breaking Bad")
        pause.releaseAll()

        assertEquals(listOf("Breaking Bad", "Breaking Bad"), bridge.searches)
    }

    @Test
    fun openingAResultRecordsTheTrimmedQuery() {
        queries.resultOpened(" Breaking Bad ")

        assertEquals(listOf("Breaking Bad"), bridge.recorded)
    }

    @Test
    fun openingAResultWithABlankQueryRecordsNothing() {
        queries.resultOpened("  ")

        assertEquals(emptyList<String>(), bridge.recorded)
    }

    private class RecordingActions : SearchActions {
        val searches = mutableListOf<String>()
        val recorded = mutableListOf<String>()
        var clears = 0

        override fun search(query: String) {
            searches += query
        }

        override fun clear() {
            clears++
        }

        override fun recordRecentSearch(query: String) {
            recorded += query
        }

        override fun removeRecentSearch(query: String) = Unit
    }

    /** Stands in for `delay`: records each requested pause and holds it until released. */
    private class ManualPause {
        val requested = mutableListOf<Long>()
        private val gates = mutableListOf<CompletableDeferred<Unit>>()

        suspend fun await(millis: Long) {
            requested += millis
            val gate = CompletableDeferred<Unit>()
            gates += gate
            gate.await()
        }

        fun releaseAll() {
            val open = gates.toList()
            gates.clear()
            open.forEach { gate -> gate.complete(Unit) }
        }
    }
}
