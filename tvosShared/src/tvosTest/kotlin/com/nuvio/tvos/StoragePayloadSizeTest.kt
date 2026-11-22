package com.nuvio.tvos

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryLocalSnapshot
import com.nuvio.app.features.library.LibraryProfileToken
import com.nuvio.app.features.library.LibraryStoragePayloadCodec
import com.nuvio.app.features.watchprogress.WatchProgressCodec
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PROGRESS_ITEMS = 300
private const val LIBRARY_ITEMS = 500
private const val DEFAULTS_BUDGET_BYTES = 1_000
private const val PROGRESS_KEY = "watch_progress_payload_1"
private const val LIBRARY_KEY = "library_payload_1"
private const val IMAGE_HOST = "https://images.metahub.space"
private const val DESCRIPTION_WORD = "storyline "

class StoragePayloadSizeTest {
    private val fixture = PayloadStoreFixture()

    @AfterTest
    fun tearDown() = fixture.remove()

    @Test
    fun largePayloadsLeaveOnlyMarkersInDefaults() {
        val progress = progressPayload(PROGRESS_ITEMS)
        val library = libraryPayload(LIBRARY_ITEMS)
        printProjection(progress.encodeToByteArray().size, library.encodeToByteArray().size)

        fixture.store.save(PROGRESS_KEY, progress)
        fixture.store.save(LIBRARY_KEY, library)
        val defaultsBytes = listOf(PROGRESS_KEY, LIBRARY_KEY).sumOf(::defaultsEntryBytes)
        println("Storage defaults after move=$defaultsBytes")

        assertTrue(defaultsBytes < DEFAULTS_BUDGET_BYTES)
        assertEquals(progress, fixture.freshStore().load(PROGRESS_KEY))
        assertEquals(library, fixture.freshStore().load(LIBRARY_KEY))
    }

    private fun defaultsEntryBytes(key: String): Int =
        key.encodeToByteArray().size +
            (fixture.defaults.stringForKey(key)?.encodeToByteArray()?.size ?: 0)
}

private fun printProjection(progressBytes: Int, libraryBytes: Int) {
    val progressPerEntry = progressBytes / PROGRESS_ITEMS
    val libraryPerEntry = libraryBytes / LIBRARY_ITEMS
    println(
        "Storage projection progress=$progressBytes ($progressPerEntry per entry) " +
            "library=$libraryBytes ($libraryPerEntry per entry) " +
            "total=${progressBytes + libraryBytes}",
    )
}

private fun progressPayload(count: Int): String {
    val entries = (1..count).map(::episodeProgress)
    return WatchProgressCodec.encodePayload(
        entries = entries,
        lastSuccessfulPushEpochMs = 0L,
        deltaCursorEventId = 0L,
        deltaInitialized = false,
        dirtyProgressKeys = entries.mapTo(mutableSetOf()) { entry -> entry.videoId },
    )
}

private fun episodeProgress(index: Int): WatchProgressEntry {
    val seriesId = "tt${9_000_000 + index}"
    return WatchProgressEntry(
        contentType = "series",
        parentMetaId = seriesId,
        parentMetaType = "series",
        videoId = "$seriesId:2:7",
        title = "A Series Title Of Typical Length $index",
        logo = "$IMAGE_HOST/logo/medium/$seriesId/img",
        poster = "$IMAGE_HOST/poster/medium/$seriesId/img",
        background = "$IMAGE_HOST/background/medium/$seriesId/img",
        seasonNumber = 2,
        episodeNumber = 7,
        episodeTitle = "An Episode Title",
        episodeThumbnail = "https://episodes.metahub.space/$seriesId/2/7/w780.jpg",
        lastPositionMs = 1_234_000L,
        durationMs = 2_940_000L,
        lastUpdatedEpochMs = 1_795_000_000_000L + index,
        providerName = "Torrentio",
        providerAddonId = "com.stremio.torrentio.addon",
        lastStreamTitle = "Series.Title.S02E07.1080p.WEB-DL.DDP5.1.H.264-GROUP",
        lastStreamSubtitle = "1080p WEB-DL 2.1 GB",
        lastSourceUrl = "https://example-debrid.invalid/dl/" + "a1b2c3d4".repeat(12) + "/file.mkv",
    )
}

private fun libraryPayload(count: Int): String =
    LibraryStoragePayloadCodec.encode(
        LibraryLocalSnapshot(
            token = LibraryProfileToken(profileId = 1, generation = 0L),
            revision = 0L,
            contentRevision = 0L,
            hasLoaded = true,
            isLoading = false,
            items = (1..count).map(::libraryItem),
            deltaCursorEventId = 0L,
            deltaInitialized = false,
            pendingUpsertKeys = emptyList(),
            pendingDeleteKeys = emptyList(),
        ),
    )

private fun libraryItem(index: Int): LibraryItem {
    val metaId = "tt${8_000_000 + index}"
    return LibraryItem(
        id = metaId,
        type = "movie",
        name = "A Movie Title Of Typical Length $index",
        poster = "$IMAGE_HOST/poster/medium/$metaId/img",
        banner = "$IMAGE_HOST/background/medium/$metaId/img",
        logo = "$IMAGE_HOST/logo/medium/$metaId/img",
        description = DESCRIPTION_WORD.repeat(25).trim(),
        releaseInfo = "2021",
        imdbRating = "7.4",
        genres = listOf("Drama", "Thriller", "Mystery"),
        savedAtEpochMs = 1_795_000_000_000L + index,
    )
}
