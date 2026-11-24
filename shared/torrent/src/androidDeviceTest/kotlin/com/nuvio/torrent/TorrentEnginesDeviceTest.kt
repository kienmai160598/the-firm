package com.nuvio.torrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private val semanticVersion = Regex("""\d+\.\d+\.\d+""")

// A torrent nobody seeds: its stream stays resolving.
private const val LONELY_MAGNET = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"

/**
 * The engine on the device through JNI (E6): libmovies_engine.so loads, opens with the box's
 * budget, refuses a bad magnet link with its reason, and closes. No network is needed.
 */
@RunWith(AndroidJUnit4::class)
class TorrentEnginesDeviceTest {
    private val cacheDir: File = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir

    @Test
    fun theVersionComesFromTheNativeEngine() {
        assertTrue(semanticVersion.matches(TorrentEngines.version), TorrentEngines.version)
    }

    @Test
    fun anEngineOpensWithABudgetAndMakesItsStateDirectory() {
        val directory = cacheDir.resolve("torrent-engine-test").apply { deleteRecursively() }

        TorrentEngines.open(TorrentEngineConfig(directory.absolutePath, budgetFor(600L shl 20))).use { }

        assertTrue(directory.isDirectory)
    }

    @Test
    fun aBudgetThatDoesNotFitIsAnInvalidArgument() {
        val budget = TorrentEngineBudget(64L shl 20, 64L shl 20, 16L shl 20, 32L shl 20)

        val error = assertFailsWith<TorrentEngineException> {
            TorrentEngines.open(TorrentEngineConfig(cacheDir.resolve("torrent-engine-test").absolutePath, budget))
        }

        assertEquals(TorrentEngineException.Reason.InvalidArgument, error.reason)
        assertTrue(error.message.orEmpty().contains("below the store cap"), error.message)
    }

    @Test
    fun aBadMagnetLinkFailsCleanlyAndALonelyStreamResolvesUntilClosed() {
        TorrentEngines.open(TorrentEngineConfig(cacheDir.resolve("torrent-stream-test").absolutePath)).use { engine ->
            val bad = assertFailsWith<TorrentEngineException> { engine.openStream("not a magnet") }
            assertEquals(TorrentEngineException.Reason.InvalidArgument, bad.reason)

            val stream = engine.openStream(LONELY_MAGNET)
            assertEquals(StreamInfo.Resolving, engine.streamInfo(stream))
            engine.closeStream(stream)
            val gone = assertFailsWith<TorrentEngineException> { engine.streamInfo(stream) }
            assertEquals(TorrentEngineException.Reason.NotFound, gone.reason)
        }
    }
}
