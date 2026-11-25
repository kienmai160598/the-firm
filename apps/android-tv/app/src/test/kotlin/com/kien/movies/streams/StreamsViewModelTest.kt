package com.kien.movies.streams

import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamGroup
import com.nuvio.tvos.TvosStreamsEmptyState
import com.nuvio.tvos.TvosStreamsState
import org.junit.Assert.assertEquals
import org.junit.Test

/** The body of Streams for a bridge state (tvOS StreamsModel phase, visibleGroups, searchingAddonCount). */
class StreamsViewModelTest {
    @Test
    fun s1_groupsShowAsTheyLoadAndTheRestAreCountedAsSearching() {
        val streams = TvosStreamsState(
            groups = listOf(group("a", streams = listOf(stream("a#0"))), group("b", isLoading = true), group("c", isLoading = true)),
            isLoading = true,
            emptyState = null
        )

        val content = streamsContent(streams) as StreamsContent.Groups

        assertEquals(listOf("a"), content.groups.map(StreamGroupItem::addonId))
        assertEquals(1, content.sourceCount)
        assertEquals(2, content.searchingAddonCount)
    }

    @Test
    fun noGroupWithStreamsOrAnErrorIsLoading() {
        val streams = TvosStreamsState(groups = listOf(group("a", isLoading = true)), isLoading = true, emptyState = null)

        assertEquals(StreamsContent.Loading, streamsContent(streams))
    }

    @Test
    fun s3_theEmptyStateWinsOverGroups() {
        val empty = TvosStreamsEmptyState("No streams found", "Try again later", isMissingStreamAddon = true)
        val streams = TvosStreamsState(groups = listOf(group("a", error = "Timed out")), isLoading = false, emptyState = empty)

        assertEquals(StreamsContent.Empty(empty), streamsContent(streams))
    }

    @Test
    fun anAddonThatFailedShowsItsErrorOnlyWhenItFoundNothing() {
        val streams = TvosStreamsState(
            groups = listOf(group("a", error = "Timed out"), group("b", streams = listOf(stream("b#0")), error = "Partly failed")),
            isLoading = false,
            emptyState = null
        )

        val content = streamsContent(streams) as StreamsContent.Groups

        assertEquals(listOf("Timed out", null), content.groups.map(StreamGroupItem::errorMessage))
    }

    @Test
    fun theDebugFixtureComesFirstOnceLoadingEndsAndHidesTheEmptyState() {
        val empty = TvosStreamsEmptyState("No streams found", "", isMissingStreamAddon = false)
        val streams = TvosStreamsState(groups = listOf(group("a", error = "Timed out")), isLoading = false, emptyState = empty)

        val content = streamsContent(streams, fixture = StreamsDebugFixture.group) as StreamsContent.Groups

        assertEquals(listOf("debug-fixture"), content.groups.map(StreamGroupItem::addonId))
    }

    @Test
    fun theDebugFixtureWaitsWhileAddonsAreLoading() {
        val streams = TvosStreamsState(groups = listOf(group("a", isLoading = true)), isLoading = true, emptyState = null)

        assertEquals(StreamsContent.Loading, streamsContent(streams, fixture = StreamsDebugFixture.group))
    }
}

internal fun group(
    addonId: String,
    streams: List<TvosStream> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null
) = TvosStreamGroup(addonId = addonId, addonName = "Addon $addonId", streams = streams, isLoading = isLoading, errorMessage = error)

internal fun stream(
    key: String,
    name: String = "Addon\n1080p",
    title: String? = null,
    detail: String? = null,
    filename: String? = null,
    playableUrl: String? = "https://example.com/$key.mp4",
    infoHash: String? = null
) = TvosStream(
    key = key,
    name = name,
    title = title,
    detail = detail,
    badges = emptyList(),
    sizeLabel = null,
    kind = "Direct link",
    url = playableUrl,
    externalUrl = null,
    infoHash = infoHash,
    fileIndex = null,
    filename = filename,
    trackers = emptyList(),
    addonId = key.substringBefore('#'),
    addonName = "Addon",
    playableUrl = playableUrl,
    requestHeaders = emptyMap()
)
