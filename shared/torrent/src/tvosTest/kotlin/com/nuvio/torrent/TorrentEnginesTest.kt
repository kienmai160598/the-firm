package com.nuvio.torrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

private val semanticVersion = Regex("""\d+\.\d+\.\d+""")

// A torrent nobody seeds: its stream stays resolving.
private const val LONELY_MAGNET = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"

@OptIn(ExperimentalForeignApi::class)
class TorrentEnginesTest {
    @Test
    fun theVersionComesFromTheNativeEngine() {
        assertTrue(semanticVersion.matches(TorrentEngines.version), TorrentEngines.version)
    }

    @Test
    fun openingAnEngineCreatesItsStateDirectory() {
        val directory = NSTemporaryDirectory() + "torrent-engine-test"
        NSFileManager.defaultManager.removeItemAtPath(directory, error = null)

        TorrentEngines.open(TorrentEngineConfig(directory)).use { }

        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(directory))
    }

    @Test
    fun aRelativeDirectoryFailsWithTheNativeReasonAndMessage() {
        val error = assertFailsWith<TorrentEngineException> {
            TorrentEngines.open(TorrentEngineConfig("relative/state"))
        }

        assertEquals(TorrentEngineException.Reason.InvalidArgument, error.reason)
        assertTrue(error.message.orEmpty().contains("not absolute"), error.message)
    }

    @Test
    fun aStreamResolvesUntilClosedAndIsThenUnknown() {
        TorrentEngines.open(TorrentEngineConfig(NSTemporaryDirectory() + "torrent-stream-test")).use { engine ->
            val stream = engine.openStream(LONELY_MAGNET)

            assertEquals(StreamInfo.Resolving, engine.streamInfo(stream))
            engine.closeStream(stream)
            val error = assertFailsWith<TorrentEngineException> { engine.streamInfo(stream) }
            assertEquals(TorrentEngineException.Reason.NotFound, error.reason)
        }
    }

    @Test
    fun aBadMagnetLinkIsAnInvalidArgument() {
        TorrentEngines.open(TorrentEngineConfig(NSTemporaryDirectory() + "torrent-stream-test")).use { engine ->
            val error = assertFailsWith<TorrentEngineException> { engine.openStream("not a magnet") }

            assertEquals(TorrentEngineException.Reason.InvalidArgument, error.reason)
        }
    }
}
