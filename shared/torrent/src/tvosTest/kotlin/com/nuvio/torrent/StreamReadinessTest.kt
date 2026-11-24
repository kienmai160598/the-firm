package com.nuvio.torrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking

private val ready = StreamInfo.Ready("http://127.0.0.1:1/stream/1", 0, 10L, "movie.mkv")

/** Answers each poll with the next scripted info, repeating the last one. */
private class ScriptedEngine(private vararg val answers: StreamInfo) : TorrentEngine {
    var polls = 0

    override fun streamInfo(stream: Long): StreamInfo = answers[minOf(polls++, answers.lastIndex)]

    override fun openStream(magnetUri: String, fileIndex: Int): Long = 1

    override fun closeStream(stream: Long) = Unit

    override fun close() = Unit
}

class StreamReadinessTest {
    @Test
    fun aStreamIsPolledUntilItIsReady() = runBlocking {
        val engine = ScriptedEngine(StreamInfo.Resolving, StreamInfo.Resolving, ready)

        val result = engine.awaitReady(1, pollInterval = 1.milliseconds)

        assertEquals(ready, result)
        assertEquals(3, engine.polls)
    }

    @Test
    fun aFailedStreamThrowsItsMessage() = runBlocking {
        val engine = ScriptedEngine(StreamInfo.Resolving, StreamInfo.Failed("tracker said no"))

        val error = assertFailsWith<TorrentStreamException> {
            engine.awaitReady(1, pollInterval = 1.milliseconds)
        }

        assertEquals("tracker said no", error.message)
    }

    @Test
    fun aStreamThatNeverResolvesTimesOut() = runBlocking {
        val engine = ScriptedEngine(StreamInfo.Resolving)

        assertFailsWith<TorrentStreamException> {
            engine.awaitReady(1, pollInterval = 1.milliseconds, timeout = 20.milliseconds)
        }
        Unit
    }
}
