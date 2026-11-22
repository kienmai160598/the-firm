package com.kien.movies.core.streams

import com.kien.movies.core.network.NetworkResult
import com.kien.movies.domain.repository.MetaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleRuntimeLookupTest {
    private val metaRepository = mockk<MetaRepository> {
        every { getCachedMeta(any(), any()) } returns null
    }
    private val lookup = TitleRuntimeLookup(metaRepository)

    @Test
    fun `an episode's own runtime wins over the show's`() {
        val show = titleMeta(
            runtime = "45 min",
            episodes = listOf(episodeVideo(1, 2, runtime = 61))
        )

        assertEquals(61, titleRuntimeMinutes(show, season = 1, episode = 2))
    }

    @Test
    fun `an episode without a runtime falls back to the show's`() {
        val show = titleMeta(
            runtime = "45 min",
            episodes = listOf(episodeVideo(1, 2, runtime = null))
        )

        assertEquals(45, titleRuntimeMinutes(show, season = 1, episode = 2))
    }

    @Test
    fun `a film's runtime is parsed from the meta and missing stays unknown`() {
        val film = titleMeta(runtime = "2h 28min")
        val unknown = titleMeta(runtime = null)

        assertEquals(148, titleRuntimeMinutes(film, season = null, episode = null))
        assertNull(titleRuntimeMinutes(unknown, season = null, episode = null))
    }

    @Test
    fun `the meta id drops the season and episode of an episode's video id`() {
        assertEquals("tt0903747", metaIdOfVideo("tt0903747:1:2", season = 1, episode = 2))
        assertEquals("kitsu:42", metaIdOfVideo("kitsu:42:1:7", season = 1, episode = 7))
        assertEquals("tt0111161", metaIdOfVideo("tt0111161", season = null, episode = null))
    }

    @Test
    fun `a cached meta answers without asking the meta addons`() = runTest {
        every { metaRepository.getCachedMeta("series", "tt0903747") } returns
            titleMeta(runtime = "45 min", episodes = listOf(episodeVideo(1, 2, runtime = 58)))

        val duration = lookup.titleDuration("series", "tt0903747:1:2", season = 1, episode = 2)

        assertEquals(TitleDuration(runtimeMinutes = 58, isEpisode = true), duration)
    }

    @Test
    fun `an uncached meta is fetched from the meta addons`() = runTest {
        every { metaRepository.getMetaFromAllAddons("movie", "tt0111161", any()) } returns
            flowOf(NetworkResult.Loading, NetworkResult.Success(titleMeta(runtime = "142 min")))

        val duration = lookup.titleDuration("movie", "tt0111161", season = null, episode = null)

        assertEquals(TitleDuration(runtimeMinutes = 142, isEpisode = false), duration)
    }

    @Test
    fun `a failing meta addon leaves the runtime unknown`() = runTest {
        every { metaRepository.getMetaFromAllAddons(any(), any(), any()) } returns flow {
            throw IllegalStateException("meta addon down")
        }

        val duration = lookup.titleDuration("movie", "tt0111161", season = null, episode = null)

        assertEquals(TitleDuration(runtimeMinutes = null, isEpisode = false), duration)
    }

    @Test
    fun `a meta addon that never answers is given up on`() = runTest {
        every { metaRepository.getMetaFromAllAddons(any(), any(), any()) } returns flow {
            awaitCancellation()
        }

        val duration = lookup.titleDuration("movie", "tt0111161", season = null, episode = null)

        assertNull(duration.runtimeMinutes)
    }
}
