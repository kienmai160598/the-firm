package com.nuvio.torrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

private val semanticVersion = Regex("""\d+\.\d+\.\d+""")

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
}
