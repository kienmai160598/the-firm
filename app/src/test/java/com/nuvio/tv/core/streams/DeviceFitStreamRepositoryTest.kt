package com.nuvio.tv.core.streams

import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.repository.StreamRepositoryImpl
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.repository.MetaRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFitStreamRepositoryTest {
    private val heavy = torrent(name = "Heavy", videoSize = 60L * GIBIBYTE)
    private val light = torrent(name = "Light", videoSize = 10L * GIBIBYTE)
    private val addonOrder =
        NetworkResult.Success(listOf(AddonStreams("Addon", null, listOf(heavy, light))))
    private val delegate = mockk<StreamRepositoryImpl>()
    private val metaRepository = mockk<MetaRepository> {
        every { getCachedMeta(any(), any()) } returns null
    }
    private val repository = DeviceFitStreamRepository(delegate, TitleRuntimeLookup(metaRepository))

    @Test
    fun `a cached runtime ranks the first result`() = runTest {
        searchReturns(flowOf(NetworkResult.Loading, addonOrder))
        every { metaRepository.getCachedMeta("movie", FILM_ID) } returns titleMeta("120 min")

        val emissions = search().toList()

        assertEquals(NetworkResult.Loading, emissions.first())
        assertEquals(listOf(listOf("Light", "Heavy")), emissions.drop(1).map(::streamNames))
    }

    @Test
    fun `the first result waits for the runtime so an over-budget stream is marked`() = runTest {
        val metaAnswer = CompletableDeferred<Unit>()
        searchReturns(flowOf(NetworkResult.Loading, addonOrder))
        every { metaRepository.getMetaFromAllAddons(any(), any(), any()) } returns flow {
            metaAnswer.await()
            emit(NetworkResult.Success(titleMeta("120 min")))
        }
        val emissions = mutableListOf<NetworkResult<List<AddonStreams>>>()
        val collection = launch { search().toList(emissions) }

        runCurrent()
        assertEquals(listOf(NetworkResult.Loading), emissions)

        metaAnswer.complete(Unit)
        runCurrent()
        assertEquals(listOf("Light", "Heavy"), streamNames(emissions.last()))
        assertTrue(collection.isCompleted)
    }

    @Test
    fun `a failing runtime lookup keeps the addon order and ends the search`() = runTest {
        searchReturns(flowOf(addonOrder))
        every { metaRepository.getMetaFromAllAddons(any(), any(), any()) } returns flow {
            throw IllegalStateException("meta addon down")
        }

        val emissions = search().toList()

        assertEquals(listOf(listOf("Heavy", "Light")), emissions.map(::streamNames))
    }

    @Test
    fun `an error passes through without waiting for the runtime`() = runTest {
        val failure = NetworkResult.Error("addons failed")
        searchReturns(flowOf(failure))
        every { metaRepository.getMetaFromAllAddons(any(), any(), any()) } returns flow {
            CompletableDeferred<Unit>().await()
        }

        assertEquals(listOf(failure), search().toList())
    }

    @Test
    fun `every other call reaches the search unchanged`() = runTest {
        val cached = listOf(AddonStreams("Addon", null, listOf(heavy)))
        coEvery {
            delegate.getCachedStreamsFromAllAddons("movie", FILM_ID, null, null)
        } returns cached
        every { delegate.setLocalPluginSearchPaused(true) } returns Unit

        repository.setLocalPluginSearchPaused(true)

        assertSame(cached, repository.getCachedStreamsFromAllAddons("movie", FILM_ID))
        verify { delegate.setLocalPluginSearchPaused(true) }
    }

    private fun searchReturns(results: Flow<NetworkResult<List<AddonStreams>>>) {
        every {
            delegate.getStreamsFromAllAddons(any(), any(), any(), any(), any(), any(), any(), any())
        } returns results
    }

    private fun search() = repository.getStreamsFromAllAddons(type = "movie", videoId = FILM_ID)

    private fun streamNames(result: NetworkResult<List<AddonStreams>>): List<String?> {
        val success = result as NetworkResult.Success<List<AddonStreams>>

        return success.data.flatMap { it.streams }.map { it.name }
    }

    private companion object {
        const val FILM_ID = "tt1341338"
    }
}
